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

package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.trace.BasicBlockTrace;

/**
 * Repository interface for managing rollup-related data operations.
 * <p>This interface provides methods for handling L2 block traces, chunks, batches,
 * proof requests, and reliable transactions in the rollup system.</p>
 */
public interface IRollupRepository {

    /**
     * Caches an L2 block trace in memory.
     *
     * @param blockTrace the block trace to cache
     */
    void cacheL2BlockTrace(BasicBlockTrace blockTrace);

    /**
     * Retrieves an L2 block trace from cache by block number.
     *
     * @param blockNumber the block number to retrieve
     * @return the cached block trace, or null if not found
     */
    BasicBlockTrace getL2BlockTraceFromCache(BigInteger blockNumber);

    /**
     * Deletes L2 block traces from cache within the specified range.
     *
     * @param start the starting block number (inclusive)
     * @param end the ending block number (inclusive)
     */
    void deleteL2BlockTracesFromCache(BigInteger start, BigInteger end);

    /**
     * Clears all L2 block traces cache for the current chunk.
     */
    void clearL2BlockTracesCacheForCurrChunk();

    /**
     * Retrieves a range of L2 block traces from storage.
     *
     * @param start the starting block number (inclusive)
     * @param end the ending block number (inclusive)
     * @return list of block traces in the specified range
     */
    List<BasicBlockTrace> getL2BlockTraceRange(BigInteger start, BigInteger end);

    /**
     * Retrieves an L2 block trace by block height from storage.
     *
     * @param height the block height
     * @return the block trace at the specified height
     */
    BasicBlockTrace getL2BlockTrace(BigInteger height);

    /**
     * Updates a rollup number record for the specified chain and record type.
     *
     * @param chainType the chain type
     * @param type the rollup number record type
     * @param number the number value to update
     */
    void updateRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type, BigInteger number);

    /**
     * Retrieves a rollup number record for the specified chain and record type.
     *
     * @param chainType the chain type
     * @param type the rollup number record type
     * @return the rollup number record value
     */
    BigInteger getRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type);

    /**
     * Retrieves rollup number information for the specified chain and record type.
     *
     * @param chainType the chain type
     * @param type the rollup number record type
     * @return the rollup number information object
     */
    RollupNumberInfo getRollupNumberInfo(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type);

    /**
     * Saves a chunk to storage.
     *
     * @param chunkWrapper the chunk wrapper to save
     */
    void saveChunk(ChunkWrapper chunkWrapper);

    /**
     * Retrieves a specific chunk by batch index and chunk index.
     *
     * @param batchIndex the batch index
     * @param chunkIndex the chunk index within the batch
     * @return the chunk wrapper
     */
    ChunkWrapper getChunk(BigInteger batchIndex, long chunkIndex);

    /**
     * Retrieves all chunks for a specific batch.
     *
     * @param batchIndex the batch index
     * @return list of chunk wrappers in the batch
     */
    List<ChunkWrapper> getChunks(BigInteger batchIndex);

    /**
     * Deletes chunks from cache within the specified range for a batch.
     *
     * @param batchIndex the batch index
     * @param start the starting chunk index (inclusive)
     * @param end the ending chunk index (inclusive)
     */
    void deleteChunksFromCache(BigInteger batchIndex, long start, long end);

    /**
     * Saves a batch to storage.
     *
     * @param batchWrapper the batch wrapper to save
     */
    void saveBatch(BatchWrapper batchWrapper);

    /**
     * Saves a partial batch header with L1 message information.
     *
     * @param batchHeader the batch header
     * @param totalL1MessagePopped total number of L1 messages popped
     * @param l1MessagePopped number of L1 messages popped in this batch
     * @param startBlockNum the starting block number
     * @param endBlockNum the ending block number
     */
    void savePartialBatchHeader(BatchHeader batchHeader, long totalL1MessagePopped, long l1MessagePopped, BigInteger startBlockNum, BigInteger endBlockNum);

    /**
     * Checks if a batch exists by batch index.
     *
     * @param batchIndex the batch index to check
     * @return true if the batch exists, false otherwise
     */
    boolean hasBatch(BigInteger batchIndex);

    /**
     * Retrieves a batch by batch index.
     *
     * @param batchIndex the batch index
     * @return the batch wrapper
     */
    BatchWrapper getBatch(BigInteger batchIndex);

    /**
     * Retrieves a batch by batch index with optional chunk data.
     *
     * @param batchIndex the batch index
     * @param withChunksOrNot whether to include chunk data
     * @return the batch wrapper
     */
    BatchWrapper getBatch(BigInteger batchIndex, boolean withChunksOrNot);

    /**
     * Retrieves a batch header by batch index.
     *
     * @param batchIndex the batch index
     * @return the batch header
     */
    BatchHeader getBatchHeader(BigInteger batchIndex);

    /**
     * Calculates the count of waiting batches beyond the specified index.
     *
     * @param beyondIndex the index threshold
     * @return the count of waiting batches
     */
    int calcWaitingBatchCountBeyondIndex(BigInteger beyondIndex);

    /**
     * Creates a batch prove request.
     *
     * @param batchIndex the batch index
     * @param proveType the proof type
     */
    void createBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType);

    /**
     * Retrieves pending batch prove requests up to the specified batch size.
     *
     * @param batchSize the maximum number of requests to retrieve
     * @param proveType the proof type
     * @return list of pending batch prove requests
     */
    List<BatchProveRequestDO> peekPendingBatchProveRequest(int batchSize, ProveTypeEnum proveType);

    /**
     * Retrieves a specific batch prove request.
     *
     * @param batchIndex the batch index
     * @param proveType the proof type
     * @return the batch prove request
     */
    BatchProveRequestDO getBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType);

    /**
     * Updates the state of a batch prove request.
     *
     * @param batchIndex the batch index
     * @param proveType the proof type
     * @param state the new state
     */
    void updateBatchProveRequestState(BigInteger batchIndex, ProveTypeEnum proveType, BatchProveRequestStateEnum state);

    /**
     * Saves a batch proof and updates the request state.
     *
     * @param batchIndex the batch index
     * @param proveType the proof type
     * @param rawProof the raw proof data
     */
    void saveBatchProofAndUpdateReqState(BigInteger batchIndex, ProveTypeEnum proveType, byte[] rawProof);

    /**
     * Calculates the count of waiting proofs beyond the specified index.
     *
     * @param type the proof type
     * @param beyondIndex the index threshold
     * @return the count of waiting proofs
     */
    int calcWaitingProofCountBeyondIndex(ProveTypeEnum type, BigInteger beyondIndex);

    /**
     * Inserts a new reliable transaction record.
     *
     * @param reliableTransactionDO the reliable transaction data object
     */
    void insertReliableTransaction(ReliableTransactionDO reliableTransactionDO);

    /**
     * Updates an existing reliable transaction record.
     *
     * @param reliableTransactionDO the reliable transaction data object
     */
    void updateReliableTransaction(ReliableTransactionDO reliableTransactionDO);

    /**
     * Updates the state of a reliable transaction by chain type, batch index, and transaction type.
     *
     * @param chainType the chain type
     * @param batchIndex the batch index
     * @param transactionType the transaction type
     * @param state the new state
     */
    void updateReliableTransactionState(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType, ReliableTransactionStateEnum state);

    /**
     * Updates the state of a reliable transaction by original transaction hash.
     *
     * @param originalTxHash the original transaction hash
     * @param state the new state
     */
    void updateReliableTransactionState(String originalTxHash, ReliableTransactionStateEnum state);

    /**
     * Removes the raw transaction data.
     *
     * @param chainType the chain type
     * @param batchIndex the batch index
     * @param transactionType the transaction type
     */
    void removeRawTx(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType);

    /**
     * Retrieves pending reliable transactions up to the specified batch size.
     *
     * @param batchSize the maximum number of transactions to retrieve
     * @return list of pending reliable transactions
     */
    List<ReliableTransactionDO> getTxPendingReliableTransactions(int batchSize);

    /**
     * Retrieves not finalized reliable transactions for a specific chain.
     *
     * @param chainType the chain type
     * @param batchSize the maximum number of transactions to retrieve
     * @return list of not finalized reliable transactions
     */
    List<ReliableTransactionDO> getNotFinalizedReliableTransactions(ChainTypeEnum chainType, int batchSize);

    /**
     * Retrieves reliable transactions by specific state for a given chain type.
     *
     * @param chainType the chain type (LAYER_ONE or LAYER_TWO)
     * @param state the transaction state to filter by
     * @param batchSize the maximum number of transactions to retrieve
     * @return list of reliable transactions matching the specified state
     */
    List<ReliableTransactionDO> getReliableTransactionsByState(ChainTypeEnum chainType, ReliableTransactionStateEnum state, int batchSize);

    /**
     * Retrieves failed reliable transactions up to the specified batch size and retry limit.
     *
     * @param batchSize the maximum number of transactions to retrieve
     * @param retryCountLimit the retry count limit
     * @return list of failed reliable transactions
     */
    List<ReliableTransactionDO> getFailedReliableTransactions(int batchSize, int retryCountLimit);

    /**
     * Retrieves a reliable transaction by original transaction hash.
     *
     * @param originalTxHash the original transaction hash
     * @return the reliable transaction data object
     */
    ReliableTransactionDO getReliableTransaction(String originalTxHash);

    /**
     * Retrieves a reliable transaction by chain type, batch index, and transaction type.
     *
     * @param chainType the chain type
     * @param batchIndex the batch index
     * @param transactionType the transaction type
     * @return the reliable transaction data object
     */
    ReliableTransactionDO getReliableTransaction(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType);

    /**
     * Queries the latest nonce for a sender on a specific chain.
     *
     * @param chainType the chain type
     * @param sender the sender address
     * @return the latest nonce value
     */
    BigInteger queryLatestNonce(ChainTypeEnum chainType, String sender);

    // ==================== Rollback related methods ====================

    /**
     * Deletes all batches with batch_index >= the specified index.
     *
     * @param fromBatchIndex the starting batch index (inclusive)
     * @return the number of deleted records
     */
    int deleteBatchesFrom(BigInteger fromBatchIndex);

    /**
     * Deletes chunks for rollback operation.
     * Deletes all chunks where batch_index > targetBatchIndex,
     * or (batch_index = targetBatchIndex AND chunk_index >= targetChunkIndex).
     *
     * @param targetBatchIndex the target batch index
     * @param targetChunkIndex the target chunk index within the target batch
     * @return the number of deleted records
     */
    int deleteChunksForRollback(BigInteger targetBatchIndex, long targetChunkIndex);

    /**
     * Deletes all batch prove requests with batch_index >= the specified index.
     *
     * @param fromBatchIndex the starting batch index (inclusive)
     * @return the number of deleted records
     */
    int deleteBatchProveRequestsFrom(BigInteger fromBatchIndex);

    /**
     * Deletes reliable transactions for rollback operation.
     * Deletes rollup transactions (BATCH_COMMIT_TX, BATCH_TEE_PROOF_COMMIT_TX, BATCH_ZK_PROOF_COMMIT_TX)
     * with batch_index >= fromBatchIndex.
     *
     * @param fromBatchIndex the starting batch index (inclusive)
     * @return the number of deleted records
     */
    int deleteRollupReliableTransactionsFrom(BigInteger fromBatchIndex);

    /**
     * Deletes L1 message reliable transactions with nonce > the specified threshold.
     *
     * @param nonceThreshold the nonce threshold
     * @return the number of deleted records
     */
    int deleteL1MsgReliableTransactionsAboveNonce(long nonceThreshold);

    /**
     * Deletes L2 oracle batch fee feed transactions with batch_index >= the specified index.
     *
     * @param fromBatchIndex the starting batch index (inclusive)
     * @return the number of deleted records
     */
    int deleteOracleBatchFeeFeedTransactionsFrom(BigInteger fromBatchIndex);

    /**
     * Finds the chunk that contains the specified block height within a batch.
     * A chunk contains a block if startNumber <= blockHeight <= endNumber.
     *
     * @param batchIndex the batch index to search within
     * @param blockHeight the block height to search for
     * @return the chunk wrapper containing the block, or null if not found
     */
    ChunkWrapper findChunkByBlockHeight(BigInteger batchIndex, BigInteger blockHeight);
}
