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
import java.util.List;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a batch in the Layer 2 rollup system.
 * <p>
 * A batch is a collection of chunks that are grouped together for submission to Layer 1.
 * It contains a batch header with metadata and a payload containing the actual chunk data.
 * The batch also manages Data Availability (DA) data, typically in the form of EIP-4844 blobs.
 * <p>
 * Key components:
 * <ul>
 *   <li>Batch header: Contains version, index, hashes, and other metadata</li>
 *   <li>Payload: Contains the chunks and their serialized data</li>
 *   <li>DA data: Data availability layer data (e.g., EIP-4844 blobs)</li>
 * </ul>
 */
@Setter
@Builder
@AllArgsConstructor
@Slf4j
public class Batch {

    /**
     * Creates a batch with existing Data Availability (DA) data.
     * <p>
     * This factory method constructs a new batch instance with pre-existing DA data,
     * typically used when blobs have already been generated from chunks.
     *
     * @param batchVersion      the version of the batch protocol
     * @param batchIndex        the sequential index of this batch in the rollup chain
     * @param parentBatchHeader the header of the parent batch for chain continuity
     * @param l1MsgRollingHash  the rolling hash of L1 messages from the last block trace of this batch
     * @param chunks            the list of chunks to include in this batch; each chunk must have its hash pre-calculated
     * @param daData            the pre-existing EIP-4844 blobs or other DA data structure
     * @return a newly created batch instance
     */
    public static Batch createBatch(BatchVersionEnum batchVersion, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, IDaData daData) {
        return new Batch(batchVersion, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, daData);
    }

    /**
     * The batch header containing metadata such as version, index, and hashes.
     * This header is used for batch identification and verification on Layer 1.
     */
    @Getter
    private BatchHeader batchHeader;

    /**
     * The batch payload containing the chunks and their serialized data.
     * This represents the actual transaction data being rolled up.
     */
    @Getter
    private IBatchPayload payload;

    /**
     * Data Availability (DA) data for this batch, typically EIP-4844 blobs.
     * This ensures that the batch data can be reconstructed if needed.
     */
    private IDaData daData;

    /**
     * Constructs a batch without pre-existing DA data.
     * <p>
     * The DA data will be automatically generated from the chunks during construction.
     *
     * @param version           the version of the batch protocol
     * @param batchIndex        the sequential index of this batch
     * @param parentBatchHeader the header of the parent batch
     * @param l1MsgRollingHash  the rolling hash of L1 messages from block trace
     * @param chunks            the list of chunks to include in this batch
     */
    public Batch(BatchVersionEnum version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks) {
        this(version, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, null);
    }

    /**
     * Constructs a batch with optional pre-existing DA data.
     * <p>
     * This constructor initializes a batch with the provided chunks and optionally uses
     * pre-existing DA data. If DA data is not provided, it will be generated from the chunks.
     * The batch header is constructed with metadata including version, index, parent hash,
     * and data hash.
     *
     * @param version           the version of the batch protocol
     * @param batchIndex        the sequential index of this batch
     * @param parentBatchHeader the header of the parent batch
     * @param l1MsgRollingHash  the rolling hash of L1 messages from the last block trace
     * @param chunks            the list of chunks to include in this batch (must have hashes calculated)
     * @param daData            optional pre-existing DA data (blobs); if null, will be generated from chunks
     * @throws RuntimeException if the chunks list is empty
     */
    public Batch(BatchVersionEnum version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, IDaData daData) {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        this.payload = new ChunksPayload(version, chunks);
        if (ObjectUtil.isNull(daData)) {
            this.daData = BlobsDaData.buildFrom(version, this.payload);
        }

        this.batchHeader = BatchHeader.builder()
                .version(version)
                .batchIndex(batchIndex)
                .l1MsgRollingHash(l1MsgRollingHash)
                .parentBatchHash(parentBatchHeader.getHash())
                .dataHash(this.daData.dataHash())
                .build();
    }

    /**
     * Returns the total size of all raw transactions in this batch.
     *
     * @return the total byte size of all transactions in the batch
     */
    public long getBatchTxsLength() {
        return this.payload.getRawTxTotalSize();
    }

    /**
     * Returns the Data Availability data for this batch.
     * <p>
     * If the DA data has not been initialized yet, it will be lazily generated
     * from the batch payload using the batch version.
     *
     * @return the DA data structure containing blobs or other DA information
     */
    public IDaData getDaData() {
        if (ObjectUtil.isNull(this.daData)) {
            this.daData = BlobsDaData.buildFrom(this.batchHeader.getVersion(), this.payload);
        }
        return this.daData;
    }

    /**
     * Returns the hash of this batch.
     *
     * @return the batch hash as a byte array
     */
    public byte[] getBatchHash() {
        return batchHeader.getHash();
    }

    /**
     * Returns the hash of this batch as a hexadecimal string.
     *
     * @return the batch hash encoded as a hex string
     */
    public String getBatchHashHex() {
        return HexUtil.encodeHexStr(batchHeader.getHash());
    }

    /**
     * Returns the sequential index of this batch in the rollup chain.
     *
     * @return the batch index
     */
    public BigInteger getBatchIndex() {
        return batchHeader.getBatchIndex();
    }

    /**
     * Returns the starting block number included in this batch.
     *
     * @return the first block number in this batch
     */
    public BigInteger getStartBlockNumber() {
        return this.payload.getStartBlockNumber();
    }

    /**
     * Gets the ending block number included in this batch.
     * <p>
     * This represents the highest block number contained within this batch.
     *
     * @return the last block number in this batch
     */
    public BigInteger getEndBlockNumber() {
        return this.payload.getEndBlockNumber();
    }

    /**
     * Validates the integrity and correctness of this batch.
     * <p>This method delegates to the payload's validation logic to ensure
     * all chunks and their data are valid.</p>
     *
     * @throws InvalidBatchException if the batch or its payload is invalid
     */
    public void validate() throws InvalidBatchException {
        this.payload.validate();
    }

    /**
     * Returns the EIP-4844 blobs for this batch.
     * <p>
     * If the DA data has not been initialized or is empty, it will be generated
     * from the batch payload. Currently, only EIP-4844 blobs are supported as
     * the Data Availability mechanism.
     *
     * @return the EIP-4844 blobs containing the batch data
     */
    public EthBlobs getEthBlobs() {
        if (ObjectUtil.isNull(this.daData) || 0 == this.daData.getDataLen()) {
            this.daData = BlobsDaData.buildFrom(this.batchHeader.getVersion(), this.payload);
        }
        // for now, we only have eip4844 blobs as DA
        return ((BlobsDaData) this.daData).getBlobs();
    }
}
