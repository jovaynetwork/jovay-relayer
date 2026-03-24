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
import java.util.Map;

/**
 * L2 Merkle tree aggregator interface for constructing Merkle trees from L2 messages.
 * <p>
 * This interface is responsible for aggregating L2 to L1 messages into Merkle trees,
 * which are used to generate Merkle proofs for cross-chain message verification.
 * The Merkle root is committed to L1 as part of the batch data.
 * </p>
 */
public interface IL2MerkleTreeAggregator {

    /**
     * Aggregate L2 to L1 messages into a Merkle tree for the current batch.
     * <p>
     * This method collects all L2 messages in the specified batch, constructs a Merkle tree,
     * and returns a mapping of message indices to their corresponding Merkle roots or hashes.
     * </p>
     *
     * @param currBatchIndex the current batch index to aggregate messages for
     * @return a map of message indices to their Merkle tree node hashes
     */
    Map<BigInteger, byte[]> aggregate(BigInteger currBatchIndex);
}
