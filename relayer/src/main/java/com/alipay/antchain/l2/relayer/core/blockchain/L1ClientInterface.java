package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionBatch;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import io.reactivex.Flowable;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.core.DefaultBlockParameterName;

public interface L1ClientInterface {

    TransactionInfo commitBatch(BatchWrapper batchWrapper, BatchHeader parentBatchHeader) throws L2RelayerException;

    TransactionInfo verifyBatch(ProveTypeEnum proveType, BatchWrapper batchWrapper, byte[] proof) throws L2RelayerException;

    BigInteger lastTeeVerifiedBatch();

    BigInteger lastCommittedBatch();

    BigInteger lastCommittedBatch(DefaultBlockParameterName blockLevel);

    BigInteger lastZkVerifiedBatch();

    BigInteger maxTxsInChunk();

    BigInteger maxBlockInChunk();

    BigInteger maxCallDataInChunk();

    BigInteger maxZkCircleInChunk();

    long l1BlobNumLimit();

    long maxTimeIntervalBetweenBatches();

    byte[] committedBatchHash(BigInteger batchIndex);

    TransactionInfo resendRollupTx(Transaction4844 transaction4844);

    TransactionInfo resendRollupTx(String encodedFunc);

    TransactionInfo speedUpRollupTx(ReliableTransactionDO tx);

    Flowable<L1MsgTransactionBatch> flowableL1MsgFromMailbox(BigInteger start, BigInteger end);
}
