package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import org.web3j.crypto.Blob;

public interface ITransactionManager {

    String RELAYER_ETH_NONCE_KEY_PREFIX = "RELAYER_ETH_NONCE@";

    String RELAYER_ETH_SEND_TX_KEY_PREFIX = "RELAYER_ETH_SEND_TX@";

    default String getEthNonceLockKey(long chainId, String account) {
        return StrUtil.format("{}lock@{}-{}", RELAYER_ETH_NONCE_KEY_PREFIX, chainId, account);
    }

    default String getSendTxLockKey(long chainId, String account) {
        return StrUtil.format("{}lock@{}-{}", RELAYER_ETH_SEND_TX_KEY_PREFIX, chainId, account);
    }

    default String getEthNonceValKey(long chainId, String account) {
        return StrUtil.format("{}val@{}-{}", RELAYER_ETH_NONCE_KEY_PREFIX, chainId, account);
    }

    SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException;

    SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger nonce, BigInteger value, boolean constructor) throws IOException;

    SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data
    ) throws IOException;

    SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger nonce,
            BigInteger value,
            String data
    ) throws IOException;

    String getAddress();

    SendTxResult sendL1MsgTx(BigInteger gasLimit, BigInteger nonce, String data) throws IOException;
}
