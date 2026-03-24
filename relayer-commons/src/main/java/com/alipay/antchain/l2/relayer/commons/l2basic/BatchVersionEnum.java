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

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import com.github.luben.zstd.Zstd;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of batch versions for L2 rollup batches.
 * <p>
 * This enum defines different versions of batch encoding formats, each with its own
 * characteristics and capabilities. Different versions may support different compression
 * algorithms, chunk encoding formats, and maximum block sizes.
 * </p>
 * <p>
 * Each version includes:
 * <ul>
 *   <li>A byte value identifier</li>
 *   <li>An optional data compressor for batch data compression</li>
 *   <li>A chunk codec for serializing/deserializing chunks</li>
 *   <li>Maximum number of blocks allowed in a single chunk</li>
 * </ul>
 * </p>
 */
@Getter
@AllArgsConstructor
public enum BatchVersionEnum {

    /**
     * Batch version 0 - the initial version.
     * <p>
     * This is the baseline version with no compression support.
     * Chunks are serialized in the basic format with a maximum of 255 blocks per chunk.
     * </p>
     */
    BATCH_V0((byte) 0, null, new IChunkCodec() {
        @Override
        public byte[] serialize(Chunk chunk) {
            return chunk.serialize(false);
        }

        @Override
        public Chunk deserialize(byte[] raw) {
            return Chunk.deserializeFrom(raw, false);
        }
    }, (1 << 8) - 1),

    /**
     * Batch version 1 - adds compression support.
     * <p>
     * This version introduces batch data compression using the Zstandard (zstd) algorithm
     * with default parameters. The chunk format remains the same as V0, with a maximum
     * of 255 blocks per chunk.
     * </p>
     * <p>
     * Features:
     * <ul>
     *   <li>Zstd compression for batch data</li>
     *   <li>Same chunk encoding as V0</li>
     *   <li>Maximum 255 blocks per chunk</li>
     * </ul>
     * </p>
     */
    BATCH_V1(
            (byte) 1,
            new IDaCompressor() {
                @Override
                public byte[] compress(byte[] payload) {
                    return Zstd.compress(payload);
                }

                @Override
                public byte[] decompress(byte[] payload) {
                    return Zstd.decompress(payload);
                }
            },
            new IChunkCodec() {
                @Override
                public byte[] serialize(Chunk chunk) {
                    return chunk.serialize(false);
                }

                @Override
                public Chunk deserialize(byte[] raw) {
                    return Chunk.deserializeFrom(raw, false);
                }
            },
            (1 << 8) - 1
    ),

    /**
     * Batch version 2 - extends block capacity.
     * <p>
     * This version extends the number of blocks field in chunks from 1 byte to 4 bytes,
     * allowing for much larger chunks. It maintains the zstd compression support from V1.
     * </p>
     * <p>
     * Features:
     * <ul>
     *   <li>Zstd compression for batch data</li>
     *   <li>Extended chunk format with 4-byte block count</li>
     *   <li>Maximum 4,294,967,295 blocks per chunk (2^32 - 1)</li>
     * </ul>
     * </p>
     *
     * @see Chunk#getNumBlocks()
     */
    BATCH_V2(
            (byte) 2,
            new IDaCompressor() {
                @Override
                public byte[] compress(byte[] payload) {
                    return Zstd.compress(payload);
                }

                @Override
                public byte[] decompress(byte[] payload) {
                    return Zstd.decompress(payload);
                }
            },
            new IChunkCodec() {
                @Override
                public byte[] serialize(Chunk chunk) {
                    return chunk.serialize(true);
                }

                @Override
                public Chunk deserialize(byte[] raw) {
                    return Chunk.deserializeFrom(raw, true);
                }
            },
            (1L << 32) - 1
    );

    /**
     * The byte value identifier for this batch version.
     */
    @JSONField
    private final byte value;

    /**
     * The data compressor for batch data compression.
     * <p>
     * Null if this version does not support compression.
     * </p>
     */
    private final IDaCompressor daCompressor;

    /**
     * The codec for serializing and deserializing chunks.
     */
    private final IChunkCodec chunkCodec;

    /**
     * The maximum number of blocks allowed in a single chunk for this version.
     */
    private final long maxBlockSizeSingleChunk;

    /**
     * Checks if this batch version supports data compression.
     *
     * @return true if compression is supported, false otherwise
     */
    public boolean isBatchDataCompressionSupport() {
        return ObjectUtil.isNotNull(daCompressor);
    }

    /**
     * Gets the version value as an unsigned 8-bit integer.
     *
     * @return the version value as an integer (0-255)
     */
    public int getValueAsUint8() {
        return BytesUtils.getUint8AsInteger(new byte[]{value}, 0);
    }

    /**
     * Creates a BatchVersionEnum from a byte value.
     * <p>
     * This method is used for JSON deserialization.
     * </p>
     *
     * @param value the byte value of the version
     * @return the corresponding BatchVersionEnum, or null if not found
     */
    @JSONCreator
    public static BatchVersionEnum from(byte value) {
        for (BatchVersionEnum version : BatchVersionEnum.values()) {
            if (version.getValue() == value) {
                return version;
            }
        }
        return null;
    }

    /**
     * Creates a BatchVersionEnum from an integer value.
     * <p>
     * Convenience method that converts the integer to a byte and calls {@link #from(byte)}.
     * </p>
     *
     * @param val the integer value of the version
     * @return the corresponding BatchVersionEnum, or null if not found
     */
    public static BatchVersionEnum from(int val) {
        return from((byte) val);
    }
}
