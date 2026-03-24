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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.models.*;
import io.reactivex.Flowable;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.core.DefaultBlockParameter;

/**
 * L1 client interface for interacting with Layer 1 blockchain and Rollup contracts.
 * <p>
 * This interface provides methods for:
 * <ul>
 *     <li>Committing L2 batches to L1 Rollup contract</li>
 *     <li>Verifying batches with TEE or ZK proofs</li>
 *     <li>Querying Rollup contract state and configuration</li>
 *     <li>Managing transaction lifecycle (resend, speed up)</li>
 *     <li>Monitoring L1 to L2 message events</li>
 * </ul>
 * </p>
 */
public interface L1ClientInterface {

    /**
     * Commit a batch to the L1 Rollup contract.
     * <p>
     * This sends the batch data (including chunks and blocks) to L1 for data availability.
     * </p>
     *
     * @param batchWrapper the batch wrapper containing batch data and metadata
     * @return transaction information of the commit transaction
     * @throws L2RelayerException if the commit operation fails
     */
    TransactionInfo commitBatch(BatchWrapper batchWrapper) throws L2RelayerException;

    /**
     * Commit a batch to the L1 Rollup contract with eth_call simulation first.
     * <p>
     * This performs an eth_call simulation before sending the actual transaction
     * to validate the transaction will succeed.
     * </p>
     *
     * @param batchWrapper the batch wrapper containing batch data and metadata
     * @return transaction information of the commit transaction
     * @throws L2RelayerException if the commit operation fails
     */
    TransactionInfo commitBatchWithEthCall(BatchWrapper batchWrapper) throws L2RelayerException;

    /**
     * Verify a batch with proof (TEE or ZK) on the L1 Rollup contract.
     *
     * @param batchWrapper the batch wrapper containing batch data
     * @param proveReq     the proof request containing the proof data
     * @return transaction information of the verify transaction
     * @throws L2RelayerException if the verify operation fails
     */
    TransactionInfo verifyBatch(BatchWrapper batchWrapper, BatchProveRequestDO proveReq) throws L2RelayerException;

    /**
     * Verify a batch with proof (TEE or ZK) on the L1 Rollup contract with eth_call simulation first.
     *
     * @param batchWrapper the batch wrapper containing batch data
     * @param proveReq     the proof request containing the proof data
     * @return transaction information of the verify transaction
     * @throws L2RelayerException if the verify operation fails
     */
    TransactionInfo verifyBatchWithEthCall(BatchWrapper batchWrapper, BatchProveRequestDO proveReq) throws L2RelayerException;

    /**
     * Query the last TEE verified batch index from L1 Rollup contract.
     *
     * @return the last TEE verified batch index
     */
    BigInteger lastTeeVerifiedBatch();

    /**
     * Query the last committed batch index from L1 Rollup contract.
     *
     * @return the last committed batch index
     */
    BigInteger lastCommittedBatch();

    /**
     * Query the last committed batch index at a specific block.
     *
     * @param blockParam the block parameter specifying which block to query
     * @return the last committed batch index at the specified block
     */
    BigInteger lastCommittedBatch(DefaultBlockParameter blockParam);

    /**
     * Query the last ZK verified batch index from L1 Rollup contract.
     *
     * @return the last ZK verified batch index
     */
    BigInteger lastZkVerifiedBatch();

    /**
     * Query the L1 blob number limit per batch.
     *
     * @return the blob number limit
     */
    long l1BlobNumLimit();

    /**
     * Query the maximum time interval allowed between batches.
     *
     * @return the maximum time interval in seconds
     */
    long maxTimeIntervalBetweenBatches();

    /**
     * Query the batch index from which ZK verification starts.
     *
     * @return the ZK verification start batch index
     */
    BigInteger zkVerificationStartBatch();

    /**
     * Query the committed batch hash for a specific batch index.
     *
     * @param batchIndex the batch index
     * @return the batch hash bytes
     */
    byte[] committedBatchHash(BigInteger batchIndex);

    /**
     * Resend a Rollup transaction with EIP-4844 blob transaction.
     *
     * @param reliableTx      the reliable transaction record
     * @param transaction4844 the EIP-4844 transaction to resend
     * @return transaction information of the resent transaction
     */
    TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, Transaction4844 transaction4844);

    /**
     * Resend a Rollup transaction with encoded function call.
     *
     * @param reliableTx  the reliable transaction record
     * @param encodedFunc the encoded function call data
     * @return transaction information of the resent transaction
     */
    TransactionInfo resendRollupTx(ReliableTransactionDO reliableTx, String encodedFunc);

    /**
     * Speed up a Rollup transaction by increasing gas price.
     * <p>
     * This uses default gas price increase strategy.
     * </p>
     *
     * @param tx the reliable transaction to speed up
     * @return transaction information of the sped-up transaction
     */
    TransactionInfo speedUpRollupTx(ReliableTransactionDO tx);

    /**
     * Speed up a Rollup transaction with custom gas prices.
     *
     * @param tx                   the reliable transaction to speed up
     * @param maxFeePerGas         the maximum fee per gas
     * @param maxPriorityFeePerGas the maximum priority fee per gas
     * @param maxFeePerBlobGas     the maximum fee per blob gas (for EIP-4844)
     * @return transaction information of the sped-up transaction
     */
    TransactionInfo speedUpRollupTx(ReliableTransactionDO tx, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerBlobGas);

    /**
     * Create a flowable stream of L1 to L2 messages from the Mailbox contract.
     * <p>
     * This monitors L1 events and emits L1MsgTransactionBatch objects for processing.
     * </p>
     *
     * @param start the start block number
     * @param end   the end block number
     * @return a flowable stream of L1 message transaction batches
     */
    Flowable<L1MsgTransactionBatch> flowableL1MsgFromMailbox(BigInteger start, BigInteger end);
}
