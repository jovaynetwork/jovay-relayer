package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Blob;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.service.TxSignService;

@Slf4j
@Getter
public class AcbFastRawTransactionManager extends BaseRawTransactionManager implements ITransactionManager {

    private final RLock updateNonceLock;

    private final String account;

    private final RedissonClient redisson;

    public AcbFastRawTransactionManager(Web3j web3j, TxSignService txSignService, long chainId, RedissonClient redisson, int blobSidecarVersion) {
        super(web3j, txSignService, chainId, redisson, blobSidecarVersion);
        this.redisson = redisson;
        updateNonceLock = redisson.getLock(getEthNonceLockKey(chainId, txSignService.getAddress()));
        this.account = txSignService.getAddress();
    }

    @Override
    public SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        // do not use this method to send L1Msg
        Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
        getSendTxLock().lock();
        try {
            BigInteger nonce = getNonce();
            SendTxResult result = sendTx(gasPrice, gasLimit, to, data, nonce, value, constructor);
            if (ObjectUtil.isNull(result.getEthSendTransaction()) || result.getEthSendTransaction().hasError()) {
                if (ifResetNonce(result.getEthSendTransaction())) {
                    resetNonce();
                    return result;
                }
                returnNonce(nonce);
            }
            return result;
        } finally {
            getSendTxLock().unlock();
        }
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        getSendTxLock().lock();
        try {
            BigInteger nonce = getNonce();
            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
            EthSendTransaction result = signAndSend(rawTransaction);
            if (ObjectUtil.isNull(result) || result.hasError()) {
                if (ifResetNonce(result)) {
                    resetNonce();
                    return result;
                }
                returnNonce(nonce);
            }
            return result;
        } finally {
            getSendTxLock().unlock();
        }
    }

    @Override
    public SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data
    ) throws IOException {
        // do not use this method to send L1Msg
        Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
        getSendTxLock().lock();
        try {
            var nonce = getNonce();
            var result = sendTx(blobs, gasPrice, gasLimit, to, nonce, value, data);
            if (ObjectUtil.isNull(result.getEthSendTransaction()) || result.getEthSendTransaction().hasError()) {
                if (ifResetNonce(result.getEthSendTransaction())) {
                    resetNonce();
                    return result;
                }
                returnNonce(nonce);
            }
            return result;
        } finally {
            getSendTxLock().unlock();
        }
    }

    @Override
    protected BigInteger getNonce() throws IOException {
        updateNonceLock.lock();
        try {
            var nonce = redisson.getAtomicLong(getEthNonceValKey(getChainId(), account));
            if (!nonce.isExists()) {
                BigInteger nonceFromChain = super.getNonce();
                nonce.set(nonceFromChain.longValue() + 1);
                return nonceFromChain;
            }
            return BigInteger.valueOf(nonce.getAndIncrement());
        } finally {
            updateNonceLock.unlock();
        }
    }

    private void resetNonce() {
        updateNonceLock.lock();
        try {
            var nonceKey = getEthNonceValKey(getChainId(), account);
            log.info("try to reset nonce {} to redis", nonceKey);
            var nonce = redisson.getAtomicLong(nonceKey);
            if (!nonce.isExists()) {
                log.error("nonce {} not exist on redis!", nonceKey);
                return;
            }
            nonce.delete();
        } finally {
            updateNonceLock.unlock();
        }
    }

    private boolean ifResetNonce(EthSendTransaction result) {
        return result.getError().getCode() == -32000
               && StrUtil.containsAny(result.getError().getMessage(), "nonce too low");
    }

    private void returnNonce(BigInteger nonceToReturn) {
        updateNonceLock.lock();
        try {
            log.info("rpc failed and try to return nonce {} to redis", nonceToReturn);
            var nonce = redisson.getAtomicLong(getEthNonceValKey(getChainId(), account));
            if (!nonce.isExists()) {
                throw new RuntimeException(StrUtil.format("nonce {} not exist on redis!", getEthNonceValKey(getChainId(), account)));
            }
            if (!nonce.compareAndSet(nonceToReturn.add(BigInteger.ONE).longValue(), nonceToReturn.longValue())) {
                throw new RuntimeException(StrUtil.format("nonce {} not match on redis! expected {}+1 but not! ", getEthNonceValKey(getChainId(), account), nonceToReturn));
            }
        } finally {
            updateNonceLock.unlock();
        }
    }
}
