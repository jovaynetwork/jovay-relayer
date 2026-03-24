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
import java.io.DataOutputStream;
import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import lombok.*;
import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * Batch header data structure for L2 rollup batches.
 * <p>
 * This class represents the header information of a batch in the L2 rollup system.
 * A batch is a collection of chunks that are committed to L1. The batch header
 * contains metadata about the batch including version, index, message hashes,
 * and parent batch reference.
 * </p>
 * <p>
 * The batch header is serialized and its hash is used as the batch identifier
 * on the L1 chain. The serialization format is:
 * <ul>
 *   <li>1 byte: version</li>
 *   <li>8 bytes: batch index (uint64)</li>
 *   <li>32 bytes: L1 message rolling hash</li>
 *   <li>32 bytes: data hash</li>
 *   <li>32 bytes: parent batch hash</li>
 * </ul>
 * Total: 73 bytes
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHeader {

    /**
     * Deserializes a batch header from raw bytes.
     * <p>
     * Parses the byte array according to the batch header format and constructs
     * a BatchHeader object. The byte array must be at least 73 bytes long.
     * </p>
     *
     * @param raw the raw byte array containing serialized batch header data
     * @return the deserialized BatchHeader object
     * @throws IndexOutOfBoundsException if the raw byte array is too short
     */
    public static BatchHeader deserializeFrom(byte[] raw) {
        BatchHeader header = new BatchHeader();

        int offset = 0;
        header.setVersion(BatchVersionEnum.from(BytesUtils.getUint8(raw, offset++)));

        header.setBatchIndex(BytesUtils.getUint64(raw, offset));
        offset += 8;

        header.setL1MsgRollingHash(BytesUtils.getBytes32(raw, offset));
        offset += 32;

        header.setDataHash(BytesUtils.getBytes32(raw, offset));
        offset += 32;

        header.setParentBatchHash(BytesUtils.getBytes32(raw, offset));

        return header;
    }

    /**
     * The version of the batch format.
     * <p>
     * Indicates the encoding version of the batch data structure.
     * Different versions may have different serialization formats or features.
     * </p>
     */
    private BatchVersionEnum version;

    /**
     * The sequential index of this batch.
     * <p>
     * A monotonically increasing number that uniquely identifies the batch
     * in the rollup sequence. Batch index starts from 0.
     * </p>
     */
    private BigInteger batchIndex;

    /**
     * Rolling hash of L1 messages included in this batch.
     * <p>
     * A cumulative hash that incorporates all L1-to-L2 messages processed
     * up to and including this batch. Used for verifying message inclusion
     * and ordering on L1.
     * </p>
     * <p>
     * Size: 32 bytes
     * </p>
     */
    private byte[] l1MsgRollingHash;

    /**
     * Hash of the batch data content.
     * <p>
     * A cryptographic hash of all the transaction data and state changes
     * contained in this batch. This is used to verify the integrity of
     * the batch data when it's posted to L1.
     * </p>
     * <p>
     * Size: 32 bytes
     * </p>
     */
    private byte[] dataHash;

    /**
     * Hash of the parent batch header.
     * <p>
     * Reference to the previous batch in the chain, forming a linked list
     * of batches. This ensures the ordering and continuity of batches.
     * For the genesis batch (index 0), this is typically a zero hash.
     * </p>
     * <p>
     * Size: 32 bytes
     * </p>
     */
    private byte[] parentBatchHash;

    /**
     * Cached hash of this batch header.
     * <p>
     * The Keccak256 hash of the serialized batch header. This serves as
     * the unique identifier for the batch on L1. Computed lazily on first access.
     * </p>
     * <p>
     * Size: 32 bytes
     * </p>
     */
    private byte[] hash;

    /**
     * Gets the hash of this batch header.
     * <p>
     * Computes the Keccak256 hash of the serialized batch header if not already cached.
     * The hash is computed only once and cached for subsequent calls.
     * </p>
     *
     * @return the 32-byte Keccak256 hash of this batch header
     */
    public byte[] getHash() {
        if (ObjectUtil.isEmpty(hash)) {
            this.hash = new Keccak.Digest256().digest(this.serialize());
        }
        return hash;
    }

    /**
     * Gets the hexadecimal string representation of the batch header hash.
     * <p>
     * Convenience method that returns the hash as a hex string without the "0x" prefix.
     * </p>
     *
     * @return the hex-encoded hash string
     */
    public String getHashHex() {
        return HexUtil.encodeHexStr(getHash());
    }

    /**
     * Serializes this batch header to bytes.
     * <p>
     * Encodes the batch header fields in the following order:
     * <ol>
     *   <li>version (1 byte)</li>
     *   <li>batchIndex (8 bytes, big-endian uint64)</li>
     *   <li>l1MsgRollingHash (32 bytes)</li>
     *   <li>dataHash (32 bytes)</li>
     *   <li>parentBatchHash (32 bytes)</li>
     * </ol>
     * </p>
     *
     * @return the serialized byte array (73 bytes total)
     * @throws Exception if serialization fails
     */
    @SneakyThrows
    public byte[] serialize() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        stream.writeByte(version.getValue());
        stream.write(BytesUtils.fromUint64(batchIndex));
        stream.write(l1MsgRollingHash);
        stream.write(dataHash);
        stream.write(parentBatchHash);

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts this batch header to a JSON string representation.
     * <p>
     * Creates a JSON object containing all batch header fields with appropriate formatting:
     * <ul>
     *   <li>version: enum value</li>
     *   <li>batchIndex: decimal string</li>
     *   <li>l1MsgRollingHash: raw byte array</li>
     *   <li>dataHash: hex string</li>
     *   <li>parentBatchHash: hex string</li>
     *   <li>hash: hex string (computed)</li>
     * </ul>
     * </p>
     *
     * @return JSON string representation of this batch header
     */
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        jsonObject.put("batchIndex", batchIndex.toString());
        jsonObject.put("l1MsgRollingHash", l1MsgRollingHash);
        jsonObject.put("dataHash", HexUtil.encodeHexStr(dataHash));
        jsonObject.put("parentBatchHash", HexUtil.encodeHexStr(parentBatchHash));
        jsonObject.put("hash", HexUtil.encodeHexStr(getHash()));
        return jsonObject.toString();
    }
}
