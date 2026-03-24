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

package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;

/**
 * Rollup service interface for managing the core Rollup workflow.
 * <p>
 * This service is responsible for the complete lifecycle of L2 batch processing and submission to L1:
 * <ul>
 *     <li>Polling L2 blocks and aggregating them into chunks and batches</li>
 *     <li>Requesting proof generation (TEE/ZK) from the Prover system</li>
 *     <li>Committing batches and proofs to the L1 Rollup contract</li>
 * </ul>
 * </p>
 */
public interface IRollupService {

    /**
     * Set the anchor batch with batch header and merkle tree.
     * <p>
     * This method initializes the Rollup service with a starting batch (typically batch zero).
     * It sets up the initial state including batch index, merkle tree, and processed block height.
     * </p>
     *
     * @param anchorBatch the anchor batch header
     * @param anchorBatchMerkleTree the merkle tree for the anchor batch
     * @throws RuntimeException if anchor batch has already been set or batch index is not zero
     */
    void setAnchorBatch(BatchHeader anchorBatch, AppendMerkleTree anchorBatchMerkleTree);

    /**
     * Set the anchor batch by batch index.
     * <p>
     * <strong>Deprecated:</strong> This method is deprecated. Use {@link #setAnchorBatch(BatchHeader, AppendMerkleTree)} instead.
     * </p>
     *
     * @param batchIndex the batch index
     * @throws RuntimeException always throws as this method is deprecated
     * @deprecated Use {@link #setAnchorBatch(BatchHeader, AppendMerkleTree)} instead
     */
    @Deprecated
    void setAnchorBatch(BigInteger batchIndex);

    /**
     * Poll and process L2 blocks.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves the latest processed L2 block height</li>
     *     <li>Fetches new L2 block traces from the trace service</li>
     *     <li>Aggregates blocks into chunks and batches through the Rollup aggregator</li>
     *     <li>Processes L2 messages and updates processed block height</li>
     * </ul>
     * </p>
     */
    void pollL2Blocks();

    /**
     * Request TEE proof generation for L2 batches.
     * <p>
     * This method retrieves batches that are ready for TEE proof generation
     * and sends proof requests to the Prover Controller system.
     * </p>
     */
    void proveTeeL2Batch();

    /**
     * Request ZK proof generation for L2 batches.
     * <p>
     * This method retrieves batches that are ready for ZK proof generation
     * and sends proof requests to the Prover Controller system.
     * </p>
     */
    void proveZkL2Batch();

    /**
     * Commit L2 batches to the L1 Rollup contract.
     * <p>
     * This method:
     * <ul>
     *     <li>Checks L1 blob pool traffic to avoid congestion</li>
     *     <li>Queries the last committed batch index from L1 Rollup contract</li>
     *     <li>Processes batches within the commit window</li>
     *     <li>Sends batch commit transactions to L1</li>
     *     <li>Creates reliable transaction records for tracking</li>
     * </ul>
     * </p>
     */
    void commitL2Batch();

    /**
     * Commit TEE proofs to the L1 Rollup contract.
     * <p>
     * This method retrieves batches with completed TEE proofs and submits
     * them to the L1 Rollup contract for verification.
     * </p>
     */
    void commitL2TeeProof();

    /**
     * Commit ZK proofs to the L1 Rollup contract.
     * <p>
     * This method retrieves batches with completed ZK proofs and submits
     * them to the L1 Rollup contract for verification.
     * </p>
     */
    void commitL2ZkProof();
}
