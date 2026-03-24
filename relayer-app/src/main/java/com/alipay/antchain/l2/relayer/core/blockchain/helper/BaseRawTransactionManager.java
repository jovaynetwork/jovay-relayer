/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.FusakaTransaction4844;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.*;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.service.TxSignService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.exceptions.TxHashMismatchException;
import org.web3j.utils.Numeric;

@Slf4j
@Getter
public abstract class BaseRawTransactionManager extends RawTransactionManager implements ITransactionManager {

    private final RLock sendTxLock;

    private final RLock sendL1MsgLock;

    private final Web3j web3j;

    private final long chainId;

    private final TxSignService txSignService;

    private final EthBlobForkConfig ethBlobForkConfig;

    public BaseRawTransactionManager(Web3j web3j, TxSignService txSignService, long chainId, RedissonClient redisson, EthBlobForkConfig ethBlobForkConfig) {
        super(web3j, txSignService, chainId);
        sendTxLock = redisson.getLock(getSendTxLockKey(chainId, txSignService.getAddress()));
        sendL1MsgLock = redisson.getLock(getSendTxLockKey(chainId, L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString()));
        this.web3j = web3j;
        this.chainId = chainId;
        this.txSignService = txSignService;
        this.ethBlobForkConfig = ethBlobForkConfig;
    }

    @Override
    public SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger nonce, BigInteger value, boolean constructor) throws IOException {
        var rawTransaction = L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice) ?
                new L1MsgRawTransactionWrapper(new L1MsgTransaction(nonce, gasLimit, data)) :
                RawTransaction.createTransaction(getChainId(), nonce, gasLimit, to, value, data, gasPrice.maxPriorityFeePerGas(), gasPrice.maxFeePerGas());
        var hexValue = sign(rawTransaction);
        EthSendTransaction ethSendTransaction;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        } catch (Throwable t) {
            if (L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice)) {
                throw t;
            }
            return handleSendRawTxFailedRpc(t, hexValue);
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
    @SneakyThrows
    public SendTxResult sendTx(List<Blob> blobs, IGasPrice gasPrice, BigInteger gasLimit, String to, BigInteger nonce, BigInteger value, String data) throws IOException {
        RawTransaction rawTransaction;
        if (ethBlobForkConfig.getCurrConfig().getBlobSidecarVersion() != 0) {
            var constructor = ReflectUtil.getConstructor(RawTransaction.class, ITransaction.class);
            constructor.setAccessible(true);
            rawTransaction = constructor.newInstance(new FusakaTransaction4844(
                    blobs,
                    getChainId(),
                    nonce,
                    gasPrice.maxPriorityFeePerGas(),
                    gasPrice.maxFeePerGas(),
                    gasLimit,
                    to,
                    value,
                    data,
                    gasPrice.maxFeePerBlobGas()
            ));
        } else {
            rawTransaction = RawTransaction.createTransaction(
                    blobs,
                    getChainId(),
                    nonce,
                    gasPrice.maxPriorityFeePerGas(),
                    gasPrice.maxFeePerGas(),
                    gasLimit,
                    to,
                    value,
                    data,
                    gasPrice.maxFeePerBlobGas()
            );
        }

        var signedRawTx = sign(rawTransaction);
        EthSendTransaction ethSendTransaction;
        try {
            ethSendTransaction = getWeb3j().ethSendRawTransaction(signedRawTx).send();
        } catch (Throwable t) {
            if (L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.equals(new Address(to)) && ObjectUtil.isNull(gasPrice)) {
                throw t;
            }
            return handleSendRawTxFailedRpc(t, signedRawTx);
        }
        return SendTxResult.builder()
                .rawTxHex(Numeric.cleanHexPrefix(signedRawTx))
                .ethSendTransaction(ethSendTransaction)
                .nonce(rawTransaction.getNonce())
                .txSendTime(new Date())
                .build();
    }

    @Override
    public String getAddress() {
        return txSignService.getAddress();
    }

    private SendTxResult handleSendRawTxFailedRpc(Throwable e, String hexRawTx) {
        var rawTransactionWithSig = (SignedRawTransaction) EthTxDecoder.decode(hexRawTx);
        var txhash = rawTransactionWithSig.getTransaction().getType().isEip4844() ?
                Utils.calcEip4844TxHash(
                        (Transaction4844) rawTransactionWithSig.getTransaction(),
                        rawTransactionWithSig.getSignatureData()
                ) : Hash.sha3(hexRawTx);
        var retryCount = 3;
        var queryTxRetryCount = 3;
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
            } else {
                if (queryTxRetryCount-- != 0) {
                    // if query tx successful, do not consume the count of retries
                    retryCount++;
                    continue;
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
        }
        log.error("rpc to send raw tx failed, try to search the tx from node and rpc still failed: {}", txhash);
        throw new RuntimeException(e);
    }
}
