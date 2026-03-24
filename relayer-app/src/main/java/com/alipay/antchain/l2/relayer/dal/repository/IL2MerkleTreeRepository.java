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

import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;

/**
 * Repository interface for managing L2 Merkle tree data structures.
 * <p>This interface provides methods for persisting and retrieving Merkle trees
 * associated with L2 batches. Merkle trees are fundamental data structures used
 * in the rollup system for efficient verification of L2Msgs inclusion and
 * state transitions.</p>
 * <p>Each Merkle tree is associated with a specific batch index, allowing for
 * efficient lookup and verification of historical batch data.</p>
 */
public interface IL2MerkleTreeRepository {
    /**
     * Saves a Merkle tree associated with a specific batch.
     * <p>This method persists the complete Merkle tree structure to storage,
     * indexed by the batch number. The stored tree can later be retrieved for
     * proof generation or verification purposes.</p>
     *
     * @param merkleTree the append-only Merkle tree to save
     * @param batchIndex the batch index associated with this Merkle tree
     */
    void saveMerkleTree(AppendMerkleTree merkleTree, BigInteger batchIndex);

    /**
     * Retrieves a Merkle tree by its associated batch index.
     * <p>This method loads the complete Merkle tree structure from storage
     * for the specified batch. The retrieved tree can be used for generating
     * inclusion proofs or verifying transaction data.</p>
     *
     * @param batchIndex the batch index of the Merkle tree to retrieve
     * @return the append-only Merkle tree associated with the batch, or null if not found
     */
    AppendMerkleTree getMerkleTree(BigInteger batchIndex);
}
