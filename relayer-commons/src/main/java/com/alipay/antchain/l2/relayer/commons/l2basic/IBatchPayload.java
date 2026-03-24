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

package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;

/**
 * Batch payload interface representing the data structure of a batch.
 * <p>
 * This interface defines the contract for batch payload objects that contain
 * aggregated L2 block data. The payload includes transaction data, block ranges,
 * and metadata required for batch submission to L1.
 * </p>
 */
public interface IBatchPayload {

    /**
     * Serialize the batch payload to bytes.
     * <p>
     * This converts the batch payload into a byte array format suitable for
     * transmission to L1 or storage.
     * </p>
     *
     * @return the serialized batch payload bytes
     */
    byte[] serialize();

    /**
     * Get the starting block number of this batch.
     *
     * @return the first block number included in this batch
     */
    BigInteger getStartBlockNumber();

    /**
     * Get the ending block number of this batch.
     *
     * @return the last block number included in this batch
     */
    BigInteger getEndBlockNumber();

    /**
     * Validate the batch payload.
     * <p>
     * This checks the integrity and correctness of the batch payload,
     * ensuring all required data is present and valid.
     * </p>
     *
     * @throws InvalidBatchException if the batch payload is invalid
     */
    void validate() throws InvalidBatchException;

    /**
     * Get the total size of raw transaction data in this batch.
     *
     * @return the total size in bytes of all raw transactions
     */
    long getRawTxTotalSize();

    /**
     * Get the total number of L2 transactions in this batch.
     *
     * @return the L2 transaction count
     */
    int getL2TxCount();
}
