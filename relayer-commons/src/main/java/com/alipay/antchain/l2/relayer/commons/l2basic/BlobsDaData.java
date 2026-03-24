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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.web3j.crypto.Blob;
import org.web3j.crypto.BlobUtils;

/**
 * Implementation of Data Availability (DA) data using EIP-4844 blobs.
 * <p>
 * This class manages the encoding and decoding of batch data into Ethereum blobs
 * for data availability purposes. It supports multiple DA versions and batch versions,
 * with optional compression for efficient storage.
 * <p>
 * Key features:
 * <ul>
 *   <li>Encodes batch payload into EIP-4844 blobs</li>
 *   <li>Decodes blobs back to batch payload</li>
 *   <li>Supports data compression (DA version 2)</li>
 *   <li>Computes data hash for verification</li>
 * </ul>
 */
@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlobsDaData implements IDaData {

    /**
     * The number of usable bytes per 32-byte word in a blob.
     * One byte is reserved for encoding purposes.
     */
    public static final int CAPACITY_BYTE_LEN_PER_WORD = 31;

    /**
     * The total capacity in bytes per blob.
     * Calculated as usable bytes per word multiplied by the number of words per blob.
     */
    public static final int CAPACITY_BYTE_PER_BLOB = CAPACITY_BYTE_LEN_PER_WORD * EthBlobs.WORDS_NUM_PER_BLOB;

    /**
     * The size in bytes of the DA data metadata header.
     * Includes batch version (1 byte) and data length (3 bytes).
     */
    public static final int DA_DATA_META_LEN_SIZE = 4;

    /**
     * The size in bytes used to store the data length in the DA metadata.
     */
    public static final int DATA_LEN_SIZE_OF_DA_META = 3;

    /**
     * Builds BlobsDaData from a batch payload.
     * <p>
     * This method encodes the batch payload into blobs, applying compression
     * if supported by the batch version and beneficial.
     *
     * @param version the batch version
     * @param payload the batch payload to encode
     * @return a new BlobsDaData instance containing the encoded blobs
     */
    public static BlobsDaData buildFrom(BatchVersionEnum version, IBatchPayload payload) {
        return new BlobsDaData(version, payload);
    }

    /**
     * Builds BlobsDaData from existing blobs.
     * <p>
     * This method decodes the blobs to extract the batch payload.
     *
     * @param blobs the EIP-4844 blobs containing encoded batch data
     * @return a new BlobsDaData instance with decoded batch payload
     */
    public static BlobsDaData buildFrom(EthBlobs blobs) {
        return new BlobsDaData(blobs);
    }

    /**
     * Lazily builds BlobsDaData from existing blobs.
     * <p>
     * This method creates a BlobsDaData instance without immediately decoding
     * the batch payload. The payload will be decoded on first access.
     *
     * @param blobs the EIP-4844 blobs containing encoded batch data
     * @return a new BlobsDaData instance with lazy payload decoding
     */
    public static BlobsDaData lazyBuildFrom(EthBlobs blobs) {
        var daData = new BlobsDaData();
        daData.blobs = blobs;
        return daData;
    }

    /**
     * The Data Availability version used for encoding.
     */
    private DaVersion daVersion;

    /**
     * The batch version of the encoded data.
     */
    private BatchVersionEnum batchVersion;

    /**
     * The length of the actual data (excluding metadata).
     */
    private int dataLen;

    /**
     * The decoded batch payload.
     */
    private IBatchPayload batchPayload;

    /**
     * The EIP-4844 blobs containing the encoded data.
     */
    private EthBlobs blobs;

    /**
     * Constructs BlobsDaData by decoding blobs to batch payload.
     * <p>
     * This constructor is used when reconstructing batch data from existing blobs.
     *
     * @param blobs the EIP-4844 blobs to decode
     */
    private BlobsDaData(EthBlobs blobs) {
        this.blobs = blobs;
        this.batchPayload = toBatchPayload();
    }

    /**
     * Constructs BlobsDaData by encoding batch payload into blobs.
     * <p>
     * This constructor handles the encoding process, including:
     * <ul>
     *   <li>Serializing the batch payload</li>
     *   <li>Applying compression if supported and beneficial</li>
     *   <li>Adding metadata (version and data length)</li>
     *   <li>Encoding into EIP-4844 blobs</li>
     * </ul>
     *
     * @param version the batch version
     * @param payload the batch payload to encode
     */
    private BlobsDaData(BatchVersionEnum version, IBatchPayload payload) {
        this.batchVersion = version;
        var rawPayload = payload.serialize();
        this.daVersion = DaVersion.DA_0;

        log.info("try to build the blob DA data for batch of version {} and blocks from {} to {}",
                version, payload.getStartBlockNumber(), payload.getEndBlockNumber());
        // If batch version supports batch data compression, try to compress the batch body
        if (batchVersion.isBatchDataCompressionSupport()) {
            var compressed = batchVersion.getDaCompressor().compress(rawPayload);
            byte[] data;
            if (compressed.length < rawPayload.length) {
                log.info("choose to use compressed batch body for compression ratio: {}/{} = {}",
                        rawPayload.length, compressed.length, rawPayload.length / (double) compressed.length);
                // if compressed is smaller than rawChunks, use compressed
                data = compressed;
                // mark codecVersion as DA_2, means that compression on
                this.daVersion = DaVersion.DA_2;
            } else {
                log.info("compression ratio is not improved, use raw batch payload");
                // use uncompressed as data
                data = rawPayload;
                // it's da version one
                this.daVersion = DaVersion.DA_1;
            }
            this.dataLen = data.length;
            rawPayload = new byte[DA_DATA_META_LEN_SIZE + this.dataLen];
            rawPayload[0] = batchVersion.getValue();
            // copy the data length into first 3 bytes
            System.arraycopy(
                    BytesUtils.toUnsignedByteArray(DATA_LEN_SIZE_OF_DA_META, BigInteger.valueOf(this.dataLen)),
                    0, rawPayload, 1, DATA_LEN_SIZE_OF_DA_META
            );
            // copy the data into the rest of the bytes
            System.arraycopy(data, 0, rawPayload, DA_DATA_META_LEN_SIZE, this.dataLen);
        } else {
            this.dataLen = rawPayload.length;
        }

        this.blobs = new EthBlobs(sinkIntoBlobs(rawPayload));
    }

    /**
     * Computes the data hash for this DA data.
     * <p>
     * The hash is computed from the versioned hashes of all blob commitments.
     * This involves:
     * <ul>
     *   <li>Computing KZG commitments for each blob</li>
     *   <li>Converting commitments to versioned hashes</li>
     *   <li>Concatenating all versioned hashes</li>
     *   <li>Computing Keccak-256 hash of the concatenated data</li>
     * </ul>
     *
     * @return the 32-byte data hash
     */
    @Override
    @SneakyThrows
    public byte[] dataHash() {
        var versionedHashesStream = new ByteArrayOutputStream();
        var futures = new ArrayList<CompletableFuture<Bytes>>();
        for (var blob : blobs.blobs()) {
            futures.add(CompletableFuture.supplyAsync(() -> BlobUtils.getCommitment(blob)));
        }
        var commitments = new ArrayList<Bytes>();
        for (var future : futures) {
            commitments.add(future.get(10, TimeUnit.SECONDS));
        }
        commitments.stream().map(BlobUtils::kzgToVersionedHash)
                .forEach(versionedHash -> versionedHashesStream.writeBytes(versionedHash.toArray()));
        return new Keccak.Digest256().digest(versionedHashesStream.toByteArray());
    }

    /**
     * Decodes the blobs to extract the batch payload.
     * <p>
     * This method handles different DA versions:
     * <ul>
     *   <li>DA_0: Direct decoding without metadata</li>
     *   <li>DA_1: Decoding with metadata, no compression</li>
     *   <li>DA_2: Decoding with metadata and decompression</li>
     * </ul>
     * The result is cached for subsequent calls.
     *
     * @return the decoded batch payload
     * @throws IllegalArgumentException if blobs are empty
     */
    @Override
    public IBatchPayload toBatchPayload() {
        if (ObjectUtil.isNotNull(batchPayload)) {
            return batchPayload;
        }
        if (ObjectUtil.isEmpty(blobs)) {
            throw new IllegalArgumentException("blobs is empty");
        }

        var buf = ByteBuffer.allocate(blobs.blobs().size() * EthBlobs.WORDS_NUM_PER_BLOB * CAPACITY_BYTE_LEN_PER_WORD);
        // first byte of blobs is the DA codec version.
        this.daVersion = DaVersion.from(blobs.blobs().get(0).getData().get(0));
        for (var blob : blobs.blobs()) {
            var data = blob.getData().toArray();
            Assert.isTrue(data.length % 32 == 0, "blob data must be multiple of 32");
            for (int i = 0; i < data.length / 32; i++) {
                var realData = new byte[CAPACITY_BYTE_LEN_PER_WORD];
                System.arraycopy(
                        ArrayUtil.sub(data, i * 32, (i + 1) * 32), 1,
                        realData, 0,
                        CAPACITY_BYTE_LEN_PER_WORD
                );
                buf.put(realData);
            }
        }
        var rawChunks = switch (this.daVersion) {
            case DA_0 -> {
                this.batchVersion = BatchVersionEnum.BATCH_V0;
                yield buf.array();
            }
            case DA_1 -> {
                var rawDaData = buf.array();
                this.batchVersion = BatchVersionEnum.from(rawDaData[0]);
                this.dataLen = BytesUtils.getUint24(rawDaData, 1);
                yield ArrayUtil.sub(rawDaData, DA_DATA_META_LEN_SIZE, DA_DATA_META_LEN_SIZE + this.dataLen);
            }
            case DA_2 -> {
                var rawDaData = buf.array();
                this.batchVersion = BatchVersionEnum.from(rawDaData[0]);
                this.dataLen = BytesUtils.getUint24(rawDaData, 1);
                yield this.batchVersion.getDaCompressor().decompress(ArrayUtil.sub(rawDaData, DA_DATA_META_LEN_SIZE, DA_DATA_META_LEN_SIZE + this.dataLen));
            }
        };
        this.batchPayload = new ChunksPayload(batchVersion, RollupUtils.deserializeChunks(batchVersion, rawChunks));
        return batchPayload;
    }

    /**
     * Encodes raw payload data into EIP-4844 blobs.
     * <p>
     * The encoding process:
     * <ul>
     *   <li>Splits data into 31-byte chunks (one byte reserved per 32-byte word)</li>
     *   <li>First byte of first word contains DA version</li>
     *   <li>Pads the last word if necessary</li>
     *   <li>Groups words into 128KB blobs</li>
     * </ul>
     *
     * @param rawPayload the raw data to encode
     * @return a list of EIP-4844 blobs containing the encoded data
     */
    private List<Blob> sinkIntoBlobs(byte[] rawPayload) {
        var blobs = new ArrayList<Blob>();
        var wordsNum = (int) Math.ceil(rawPayload.length / (double) CAPACITY_BYTE_LEN_PER_WORD);
        var buf = ByteBuffer.allocate(wordsNum * 32);

        for (int i = 0; i < wordsNum; i += 1) {
            var rawWord = new byte[32];
            if (i == 0) {
                // first byte of the blobs is supposed to be the DA codec version field
                rawWord[0] = this.daVersion.toByte();
            }
            System.arraycopy(
                    rawPayload,
                    i * CAPACITY_BYTE_LEN_PER_WORD,
                    rawWord,
                    1,
                    i == wordsNum - 1 ? rawPayload.length - i * CAPACITY_BYTE_LEN_PER_WORD : CAPACITY_BYTE_LEN_PER_WORD
            );
            buf.put(rawWord);
        }

        var raw = buf.array();
        for (int i = 0; i < raw.length; i += EthBlobs.BLOB_SIZE) {
            var blobData = new byte[EthBlobs.BLOB_SIZE];
            System.arraycopy(raw, i, blobData, 0, Math.min(EthBlobs.BLOB_SIZE, raw.length - i));
            blobs.add(new Blob(blobData));
        }
        return blobs;
    }
}
