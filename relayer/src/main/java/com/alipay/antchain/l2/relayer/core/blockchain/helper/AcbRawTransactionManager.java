package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Blob;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.service.TxSignService;

public class AcbRawTransactionManager extends BaseRawTransactionManager implements ITransactionManager {

    public AcbRawTransactionManager(Web3j web3j, TxSignService txSignService, long chainId, RedissonClient redisson, int blobSidecarVersion) {
        super(web3j, txSignService, chainId, redisson, blobSidecarVersion);
    }

    protected synchronized BigInteger getNonce() throws IOException {
        return super.getNonce();
    }

    @Override
    public SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        // do not use this method to send L1Msg
        Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
        getSendTxLock().lock();
        try {
            return sendTx(gasPrice, gasLimit, to, data, getNonce(), value, constructor);
        } finally {
            getSendTxLock().unlock();
        }
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        getSendTxLock().lock();
        try {
            return super.sendTransaction(gasPrice, gasLimit, to, data, value, constructor);
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
            return sendTx(blobs, gasPrice, gasLimit, to, getNonce(), value, data);
        } finally {
            getSendTxLock().unlock();
        }
    }
}
