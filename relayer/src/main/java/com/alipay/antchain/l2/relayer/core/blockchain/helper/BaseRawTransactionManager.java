package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.*;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.exceptions.TxHashMismatchException;
import org.web3j.utils.Numeric;

@Slf4j
@Getter
public abstract class BaseRawTransactionManager extends RawTransactionManager implements ITransactionManager {

    private final RLock sendTxLock;

    private final Web3j web3j;

    private final long chainId;

    private final Credentials credentials;

    public BaseRawTransactionManager(Web3j web3j, Credentials credentials, long chainId, RedissonClient redisson) {
        super(web3j, new RelayerTxSignServiceImpl(credentials), chainId);
        sendTxLock = redisson.getLock(getSendTxLockKey(chainId, credentials.getAddress()));
        this.web3j = web3j;
        this.chainId = chainId;
        this.credentials = credentials;
    }

    @Override
    public SendTxResult sendTx(Eip1559GasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger nonce, BigInteger value, boolean constructor) throws IOException {
        var rawTransaction = L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice) ?
                new L1MsgRawTransactionWrapper(new L1MsgTransaction(nonce, gasLimit, data)) :
                RawTransaction.createTransaction(getChainId(), nonce, gasLimit, to, value, data, gasPrice.maxPriorityFeePerGas(), gasPrice.maxFeePerGas());
        var hexValue = sign(rawTransaction);
        EthSendTransaction ethSendTransaction;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        } catch (Exception e) {
            if (L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice)) {
                throw e;
            }
            return handleSendRawTxFailedRpc(e, hexValue);
        }

        if (ethSendTransaction != null && !ethSendTransaction.hasError()) {
            String txHashLocal;
            if (rawTransaction instanceof L1MsgRawTransactionWrapper) {
                txHashLocal = Numeric.toHexString(((L1MsgRawTransactionWrapper) rawTransaction).calcHash());
                log.debug("sent l1Msg tx hash is {}", txHashLocal);
            } else {
                txHashLocal = Hash.sha3(hexValue);
            }

            var txHashRemote = ethSendTransaction.getTransactionHash();
            if (!txHashVerifier.verify(txHashLocal, txHashRemote)) {
                throw new TxHashMismatchException(txHashLocal, txHashRemote);
            }
        }

        return SendTxResult.builder()
                .rawTxHex(Numeric.cleanHexPrefix(hexValue))
                .ethSendTransaction(ethSendTransaction)
                .nonce(rawTransaction.getNonce())
                .txSendTime(new Date())
                .build();
    }

    @Override
    public SendTxResult sendTx(List<Blob> blobs, Eip1559GasPrice gasPrice, BigInteger gasLimit, String to, BigInteger nonce, BigInteger value, String data, BigInteger maxFeePerBlobGas) throws IOException {
        var rawTransaction = RawTransaction.createTransaction(
                blobs,
                getChainId(),
                nonce,
                gasPrice.maxPriorityFeePerGas(),
                gasPrice.maxFeePerGas(),
                gasLimit,
                to,
                value,
                data,
                maxFeePerBlobGas
        );
        var signedRawTx = sign(rawTransaction);
        EthSendTransaction ethSendTransaction;
        try {
            ethSendTransaction = getWeb3j().ethSendRawTransaction(signedRawTx).send();
        } catch (Exception e) {
            if (L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice)) {
                throw e;
            }
            return handleSendRawTxFailedRpc(e, signedRawTx);
        }
        return SendTxResult.builder()
                .rawTxHex(Numeric.cleanHexPrefix(signedRawTx))
                .ethSendTransaction(ethSendTransaction)
                .nonce(rawTransaction.getNonce())
                .txSendTime(new Date())
                .build();
    }

    @Override
    public SendTxResult sendL1MsgTx(BigInteger gasLimit, BigInteger nonce, String data) throws IOException {
        return sendTx(null, gasLimit, L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(), data, nonce, BigInteger.ZERO, false);
    }

    @Override
    public String getAddress() {
        return credentials.getAddress();
    }

    private SendTxResult handleSendRawTxFailedRpc(Exception e, String hexRawTx) {
        var rawTransactionWithSig = (SignedRawTransaction) TransactionDecoder.decode(hexRawTx);
        var txhash = rawTransactionWithSig.getTransaction().getType().isEip4844() ?
                Utils.calcEip4844TxHash(
                        (Transaction4844) rawTransactionWithSig.getTransaction(),
                        rawTransactionWithSig.getSignatureData()
                ) : Hash.sha3(hexRawTx);
        var retryCount = 3;
        while (retryCount-- != 0) {
            EthTransaction txSent;
            try {
                txSent = web3j.ethGetTransactionByHash(txhash).send();
            } catch (Throwable t) {
                log.error("query tx by hash failed: {}", txhash, t);
                continue;
            }
            if (txSent.hasError()) {
                log.error("query tx by hash failed: {}", txhash);
                continue;
            }
            if (txSent.getTransaction().isPresent()) {
                // rpc failed, but we have sent the tx to the node.
                log.warn("rpc to send tx failed, but found it on eth node: {}", txhash, e);
                var res = new EthSendTransaction();
                res.setResult(txhash);
                return SendTxResult.builder()
                        .rawTxHex(Numeric.cleanHexPrefix(hexRawTx))
                        .txSendTime(new Date())
                        .ethSendTransaction(res)
                        .nonce(rawTransactionWithSig.getNonce())
                        .build();
            }
            log.error("rpc to send tx failed for tx {} and no tx found on eth node: ", txhash, e);
            var res = new EthSendTransaction();
            res.setResult(txhash);
            res.setError(new Response.Error(-1, "rpc failed"));
            return SendTxResult.builder()
                    .rawTxHex(Numeric.cleanHexPrefix(hexRawTx))
                    .txSendTime(new Date())
                    .ethSendTransaction(res)
                    .nonce(rawTransactionWithSig.getNonce())
                    .build();
        }
        log.error("rpc to send raw tx failed, try to search the tx from node and rpc still failed: {}", txhash);
        throw new RuntimeException(e);
    }
}
