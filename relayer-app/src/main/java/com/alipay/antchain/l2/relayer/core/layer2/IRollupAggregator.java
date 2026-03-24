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

package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;

import com.alipay.antchain.l2.trace.BasicBlockTrace;

/**
 * Rollup aggregator interface for processing L2 blocks and constructing chunks and batches.
 * <p>
 * This aggregator is responsible for the core logic of aggregating L2 blocks into the Rollup
 * data structures (Chunk and Batch) according to configured rules and limits.
 * </p>
 */
public interface IRollupAggregator {

    /**
     * Process current block trace and keep constructing latest chunk and batch.
     * <p>
     * This method is the core of the Rollup aggregation logic. It processes each L2 block
     * and determines whether to:
     * <ul>
     *     <li>Add the block to the current growing chunk</li>
     *     <li>Seal the current chunk and start a new one</li>
     *     <li>Seal the current batch (containing multiple chunks) and start a new batch</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Aggregation Rules:</strong>
     * </p>
     * <ol>
     *     <li><strong>Chunk Sealing:</strong> A chunk is sealed when the accumulated gas usage
     *         reaches the configured gas limit per chunk (e.g., 30M gas)</li>
     *     <li><strong>Batch Sealing:</strong> A batch is sealed when:
     *         <ul>
     *             <li>The total blob size (compressed data) exceeds the blob size limit, OR</li>
     *             <li>The time interval from the first block in the batch exceeds the maximum time interval</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * <p>
     * <strong>Post-Sealing Actions:</strong>
     * </p>
     * <ul>
     *     <li>Sealed chunks are notified to the Prover Controller for block/chunk tracking</li>
     *     <li>Sealed batches are saved to the repository with complete metadata</li>
     *     <li>Proof generation requests (TEE/ZK) are created for sealed batches</li>
     *     <li>Batch prove requests are sent to the Prover Controller system</li>
     * </ul>
     *
     * @param blockTrace current block trace containing block header, transactions, and execution results
     * @return batch index that this block belongs to (the batch may still be growing or already sealed)
     */
    BigInteger process(BasicBlockTrace blockTrace);
}
