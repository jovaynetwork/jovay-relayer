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

package com.alipay.antchain.l2.relayer.core.layer2.cache;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import lombok.NonNull;

/**
 * A pointer record that tracks the location and metadata of a chunk within the serialized chunks byte array.
 * <p>
 * This record serves as a lightweight reference to chunk data stored in the continuous byte array,
 * enabling efficient index-based access without deserializing the entire chunk.
 * </p>
 * <p>
 * The pointer contains both metadata (batch version, batch index, chunk index, etc.) and
 * location information (offset and size) to quickly locate and extract chunk data.
 * </p>
 *
 * @param batchVersion   the version of the batch protocol
 * @param batchIndex     the index of the batch this chunk belongs to
 * @param chunkIndex     the index of this chunk within the batch
 * @param endBlockNumber the last block number included in this chunk
 * @param gasSum         the total gas consumed by all blocks in this chunk
 * @param offset         the starting position of this chunk in the serialized byte array
 * @param size           the total size of this chunk in bytes (including the 4-byte length prefix)
 * @author Aone Copilot
 * @since 1.0
 */
public record ChunkPointer(@NonNull BatchVersionEnum batchVersion, @NonNull BigInteger batchIndex, long chunkIndex,
                           @NonNull BigInteger endBlockNumber, long gasSum, int offset, int size) {

    /**
     * Creates a ChunkPointer from a ChunkWrapper with specified offset and size.
     * <p>
     * This factory method extracts metadata from the ChunkWrapper and combines it
     * with the provided location information to create a pointer.
     * </p>
     *
     * @param chunk  the chunk wrapper containing chunk metadata
     * @param offset the starting position of the chunk in the serialized byte array
     * @param size   the total size of the chunk in bytes
     * @return a new ChunkPointer instance
     */
    public static ChunkPointer from(ChunkWrapper chunk, int offset, int size) {
        return new ChunkPointer(chunk.getBatchVersion(), chunk.getBatchIndex(), chunk.getChunkIndex(), chunk.getEndBlockNumber(), chunk.getGasSum(), offset, size);
    }

    /**
     * Returns the actual start offset of the chunk data, excluding the 4-byte length prefix.
     * <p>
     * The serialized chunk format includes a 4-byte length prefix before the actual chunk data.
     * This method returns the offset where the actual chunk data begins.
     * </p>
     *
     * @return the offset of the chunk data (offset + 4)
     */
    public int chunkStartOffset() {
        return offset + 4;
    }

    /**
     * Returns the end offset of the chunk data (exclusive).
     * <p>
     * This offset points to the position immediately after the last byte of this chunk,
     * which is also the starting position of the next chunk (if any).
     * </p>
     *
     * @return the exclusive end offset of the chunk (offset + size)
     */
    public int chunkEndOffset() {
        return offset + size;
    }
}
