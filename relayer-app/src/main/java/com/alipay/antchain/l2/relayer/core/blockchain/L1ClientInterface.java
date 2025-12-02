package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.models.*;
import io.reactivex.Flowable;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.core.DefaultBlockParameter;

public interface L1ClientInterface {

    TransactionInfo commitBatch(BatchWrapper batchWrapper, BatchHeader parentBatchHeader) throws L2RelayerException;

    TransactionInfo verifyBatch(BatchWrapper batchWrapper, BatchProveRequestDO proveReq) throws L2RelayerException;

    BigInteger lastTeeVerifiedBatch();

    BigInteger lastCommittedBatch();

    BigInteger lastCommittedBatch(DefaultBlockParameter blockParam);

    BigInteger lastZkVerifiedBatch();

    BigInteger maxTxsInChunk();

    BigInteger maxBlockInChunk();

    BigInteger maxCallDataInChunk();

    BigInteger maxZkCircleInChunk();

    long l1BlobNumLimit();

    long maxTimeIntervalBetweenBatches();

    BigInteger zkVerificationStartBatch();

    byte[] committedBatchHash(BigInteger batchIndex);

    TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, Transaction4844 transaction4844);

    TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, String encodedFunc);

    TransactionInfo speedUpRollupTx(ReliableTransactionDO tx);

    TransactionInfo speedUpRollupTx(ReliableTransactionDO tx, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerBlobGas);

    Flowable<L1MsgTransactionBatch> flowableL1MsgFromMailbox(BigInteger start, BigInteger end);
}
