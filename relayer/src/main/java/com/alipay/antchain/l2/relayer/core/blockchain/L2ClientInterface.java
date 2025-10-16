package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import org.web3j.protocol.core.methods.response.Transaction;

public interface L2ClientInterface {

    TransactionInfo sendL1MsgTx(L1MsgTransaction l1MsgTransaction);

    TransactionInfo resendGasFeedTx(String encodedFunc);

    BigInteger queryL2MailboxPendingNonce();

    BigInteger queryL2MailboxLatestNonce();

    BigInteger queryL2GasOracleLastBatchDaFee();

    BigInteger queryL2GasOracleLastBatchExecFee();

    BigInteger queryL2GasOracleLastBatchByteLength();

    TransactionInfo updateBatchRollupFee(BigInteger lastBatchDaFee, BigInteger lastBatchExecFee, BigInteger lastBatchByteLength);

    TransactionInfo updateBaseFeeScala(BigInteger baseFeeScala, BigInteger blobBaseFeeScala) throws IOException;

    TransactionInfo updateFixedProfit(BigInteger fixedProfit);

    TransactionInfo updateTotalScala(BigInteger totalScala);

    TransactionInfo withdrawVault(String account, BigInteger amount);

    Transaction queryTxWithRetry(String from, String txHash, BigInteger nonce);
}
