package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import org.redisson.api.RedissonClient;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Blob;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class AcbRawTransactionManager extends BaseRawTransactionManager implements ITransactionManager {

    public AcbRawTransactionManager(Web3j web3j, Credentials credentials, long chainId, RedissonClient redisson) {
        super(web3j, credentials, chainId, redisson);
    }

    protected synchronized BigInteger getNonce() throws IOException {
        return super.getNonce();
    }

    @Override
    public SendTxResult sendTx(Eip1559GasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
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
            Eip1559GasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas
    ) throws IOException {
        // do not use this method to send L1Msg
        Assert.notEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER, new Address(to));
        getSendTxLock().lock();
        try {
            return sendTx(blobs, gasPrice, gasLimit, to, getNonce(), value, data, maxFeePerBlobGas);
        } finally {
            getSendTxLock().unlock();
        }
    }
}
