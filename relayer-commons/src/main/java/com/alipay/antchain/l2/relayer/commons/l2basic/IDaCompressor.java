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
 * Data Availability (DA) compressor interface for compressing and decompressing batch data.
 * <p>
 * This interface provides methods to compress batch payload data before submitting to L1
 * for data availability, and to decompress data when needed. Compression reduces the
 * amount of data that needs to be stored on L1, lowering costs.
 * </p>
 */
public interface IDaCompressor {

    /**
     * Compress the payload data.
     * <p>
     * This compresses the batch payload using a specific compression algorithm
     * (e.g., zlib, brotli) to reduce data size for L1 submission.
     * </p>
     *
     * @param payload the uncompressed payload bytes
     * @return the compressed payload bytes
     */
    byte[] compress(byte[] payload);

    /**
     * Decompress the payload data.
     * <p>
     * This decompresses previously compressed batch payload data back to its
     * original form for processing or verification.
     * </p>
     *
     * @param payload the compressed payload bytes
     * @return the decompressed payload bytes
     */
    byte[] decompress(byte[] payload);
}
