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
import java.nio.ByteOrder;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import lombok.*;

/**
 * Represents the context information for a single block in a chunk.
 * <p>
 * BlockContext contains essential metadata about a block that is used for
 * chunk serialization and batch processing. This includes block identification,
 * timing, gas parameters, and transaction counts.
 * <p>
 * The serialized size of a BlockContext is fixed at 40 bytes.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockContext {

    /**
     * The fixed size in bytes of a serialized BlockContext.
     */
    public static final int BLOCK_CONTEXT_SIZE = 40;

    /**
     * Deserializes a BlockContext from a raw byte array.
     * <p>
     * The byte array must be at least 40 bytes and contains the following fields in order:
     * <ul>
     *   <li>specVersion (4 bytes, uint32)</li>
     *   <li>blockNumber (8 bytes, uint64)</li>
     *   <li>timestamp (8 bytes, int64, big-endian)</li>
     *   <li>baseFee (8 bytes, uint64)</li>
     *   <li>gasLimit (8 bytes, uint64)</li>
     *   <li>numTransactions (2 bytes, uint16)</li>
     *   <li>numL1Messages (2 bytes, uint16)</li>
     * </ul>
     *
     * @param raw the raw byte array containing serialized block context data
     * @return the deserialized BlockContext instance
     */
    public static BlockContext deserializeFrom(byte[] raw) {
        BlockContext context = new BlockContext();

        int offset = 0;
        context.setSpecVersion(BytesUtils.getUint32(raw, offset));
        offset += 4;

        context.setBlockNumber(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setTimestamp(ByteUtil.bytesToLong(raw, offset, ByteOrder.BIG_ENDIAN));
        offset += 8;

        context.setBaseFee(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setGasLimit(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setNumTransactions(BytesUtils.getUint16(raw, offset));
        offset += 2;

        context.setNumL1Messages(BytesUtils.getUint16(raw, offset));

        return context;
    }

    /**
     * The specification version of the block context format.
     */
    private long specVersion;

    /**
     * The block number in the blockchain.
     */
    private BigInteger blockNumber;

    /**
     * The timestamp of the block in Unix epoch time.
     */
    private long timestamp;

    /**
     * The base fee per gas for this block (EIP-1559).
     */
    private BigInteger baseFee;

    /**
     * The gas limit for this block.
     */
    private BigInteger gasLimit;

    /**
     * The number of L2 transactions in this block.
     */
    private int numTransactions;

    /**
     * The number of L1 messages included in this block.
     */
    private int numL1Messages;

    /**
     * Serializes this BlockContext to a byte array.
     * <p>
     * The serialization produces a 40-byte array with fields in the following order:
     * <ul>
     *   <li>specVersion (4 bytes, uint32)</li>
     *   <li>blockNumber (8 bytes, uint64)</li>
     *   <li>timestamp (8 bytes, int64)</li>
     *   <li>baseFee (8 bytes, uint64)</li>
     *   <li>gasLimit (8 bytes, uint64)</li>
     *   <li>numTransactions (2 bytes, uint16)</li>
     *   <li>numL1Messages (2 bytes, uint16)</li>
     * </ul>
     *
     * @return the serialized byte array (40 bytes)
     */
    @SneakyThrows
    public byte[] serialize() {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var stream = new DataOutputStream(byteArrayOutputStream);

        stream.writeInt((int) specVersion);
        stream.write(BytesUtils.fromUint64(blockNumber));
        stream.writeLong(timestamp);
        stream.write(BytesUtils.fromUint64(baseFee));
        stream.write(BytesUtils.fromUint64(gasLimit));
        stream.writeShort(numTransactions);
        stream.writeShort(numL1Messages);

        return byteArrayOutputStream.toByteArray();
    }
}
