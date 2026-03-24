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

/**
 * Chunk codec interface for serializing and deserializing chunk data.
 * <p>
 * This interface provides methods to convert chunk objects to byte arrays
 * and vice versa, enabling efficient storage and transmission of chunk data.
 * </p>
 */
public interface IChunkCodec {

    /**
     * Serialize a chunk to bytes.
     * <p>
     * This converts a chunk object into a byte array representation
     * suitable for storage or transmission.
     * </p>
     *
     * @param chunk the chunk to serialize
     * @return the serialized chunk bytes
     */
    byte[] serialize(Chunk chunk);

    /**
     * Deserialize bytes to a chunk.
     * <p>
     * This reconstructs a chunk object from its byte array representation.
     * </p>
     *
     * @param raw the serialized chunk bytes
     * @return the deserialized chunk object
     */
    Chunk deserialize(byte[] raw);
}
