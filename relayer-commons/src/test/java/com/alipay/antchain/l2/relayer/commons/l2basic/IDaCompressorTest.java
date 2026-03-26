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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for IDaCompressor
 */
public class IDaCompressorTest {

    @Test
    public void testCompressAndDecompressConsistency() {
        byte[] originalData = "This is a test string for compression and decompression.".getBytes();

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(originalData);
        byte[] decompressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.decompress(compressed);

        assertArrayEquals(originalData, decompressed);
    }

    @Test
    public void testCompressAndDecompressEmptyData() {
        byte[] emptyData = new byte[0];

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(emptyData);
        byte[] decompressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.decompress(compressed);

        assertArrayEquals(emptyData, decompressed);
    }

    @Test
    public void testCompressAndDecompressLargeData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("This is a repeated string for testing large data compression. ");
        }
        byte[] largeData = sb.toString().getBytes();

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(largeData);
        byte[] decompressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.decompress(compressed);

        assertArrayEquals(largeData, decompressed);
    }

    @Test
    public void testCompressReducesSizeForCompressibleData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("AAAAABBBBBCCCCCDDDDDEEEEE");
        }
        byte[] compressibleData = sb.toString().getBytes();

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(compressibleData);

        assertTrue(compressed.length < compressibleData.length);
    }

    @Test
    public void testCompressReturnsSmallerArrayForRepeatedData() {
        byte[] repeatedData = new byte[1000];
        for (int i = 0; i < repeatedData.length; i++) {
            repeatedData[i] = (byte) (i % 10);
        }

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(repeatedData);

        assertTrue(compressed.length < repeatedData.length);
    }

    @Test
    public void testDecompressRoundTripWithBinaryData() {
        byte[] binaryData = new byte[256];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) i;
        }

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(binaryData);
        byte[] decompressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.decompress(compressed);

        assertArrayEquals(binaryData, decompressed);
    }

    @Test
    public void testCompressSingleByte() {
        byte[] singleByte = new byte[]{42};

        byte[] compressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.compress(singleByte);
        byte[] decompressed = IDaCompressor.ZSTD_DEFAULT_COMPRESSOR.decompress(compressed);

        assertArrayEquals(singleByte, decompressed);
    }
}
