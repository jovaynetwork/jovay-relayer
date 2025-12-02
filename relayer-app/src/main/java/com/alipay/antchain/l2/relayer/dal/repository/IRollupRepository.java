package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.trace.BasicBlockTrace;

public interface IRollupRepository {

    void cacheL2BlockTrace(BasicBlockTrace blockTrace);

    BasicBlockTrace getL2BlockTraceFromCache(BigInteger blockNumber);

    List<BasicBlockTrace> getL2BlockTraceRange(BigInteger start, BigInteger end);

    BasicBlockTrace getL2BlockTrace(BigInteger height);

    void updateRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type, BigInteger number);

    BigInteger getRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type);

    RollupNumberInfo getRollupNumberInfo(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type);

    void saveChunk(ChunkWrapper chunkWrapper);

    ChunkWrapper getChunk(BigInteger batchIndex, long chunkIndex);

    List<ChunkWrapper> getChunks(BigInteger batchIndex);

    void saveBatch(BatchWrapper batchWrapper);

    void savePartialBatchHeader(BatchHeader batchHeader, long totalL1MessagePopped, long l1MessagePopped, BigInteger startBlockNum, BigInteger endBlockNum);

    boolean hasBatch(BigInteger batchIndex);

    BatchWrapper getBatch(BigInteger batchIndex);

    BatchWrapper getBatch(BigInteger batchIndex, boolean withChunksOrNot);

    BatchHeader getBatchHeader(BigInteger batchIndex);

    int calcWaitingBatchCountBeyondIndex(BigInteger beyondIndex);

    void createBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType);

    List<BatchProveRequestDO> peekPendingBatchProveRequest(int batchSize, ProveTypeEnum proveType);

    BatchProveRequestDO getBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType);

    void updateBatchProveRequestState(BigInteger batchIndex, ProveTypeEnum proveType, BatchProveRequestStateEnum state);

    void saveBatchProofAndUpdateReqState(BigInteger batchIndex, ProveTypeEnum proveType, byte[] rawProof);

    int calcWaitingProofCountBeyondIndex(ProveTypeEnum type, BigInteger beyondIndex);

    void insertReliableTransaction(ReliableTransactionDO reliableTransactionDO);

    void updateReliableTransaction(ReliableTransactionDO reliableTransactionDO);

    void updateReliableTransactionState(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType, ReliableTransactionStateEnum state);

    void updateReliableTransactionState(String originalTxHash, ReliableTransactionStateEnum state);

    List<ReliableTransactionDO> getTxPendingReliableTransactions(int batchSize);

    List<ReliableTransactionDO> getNotFinalizedReliableTransactions(ChainTypeEnum chainType, int batchSize);

    List<ReliableTransactionDO> getFailedReliableTransactions(int batchSize, int retryCountLimit);

    ReliableTransactionDO getReliableTransaction(String originalTxHash);

    ReliableTransactionDO getReliableTransaction(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType);
}
