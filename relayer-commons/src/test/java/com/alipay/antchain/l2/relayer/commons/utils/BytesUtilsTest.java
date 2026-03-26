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

package com.alipay.antchain.l2.relayer.commons.utils;

import java.math.BigInteger;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BytesUtils
 * Tests both positive and negative cases for byte conversion utilities
 */
public class BytesUtilsTest {

    // ==================== Positive Tests ====================

    /**
     * Test fromUint64 with valid positive values
     */
    @Test
    public void testFromUint64_ValidPositiveValues() {
        // Test zero
        byte[] result = BytesUtils.fromUint64(BigInteger.ZERO);
        assertEquals(8, result.length);
        assertArrayEquals(new byte[8], result);

        // Test small value
        result = BytesUtils.fromUint64(BigInteger.valueOf(255));
        assertEquals(8, result.length);
        assertEquals((byte) 255, result[7]);

        // Test max uint64 value
        BigInteger maxUint64 = new BigInteger("18446744073709551615");
        result = BytesUtils.fromUint64(maxUint64);
        assertEquals(8, result.length);
    }

    /**
     * Test fromUint256 with valid positive values
     */
    @Test
    public void testFromUint256_ValidPositiveValues() {
        // Test zero
        byte[] result = BytesUtils.fromUint256(BigInteger.ZERO);
        assertEquals(32, result.length);
        assertArrayEquals(new byte[32], result);

        // Test small value
        result = BytesUtils.fromUint256(BigInteger.valueOf(12345));
        assertEquals(32, result.length);

        // Test large value
        BigInteger largeValue = new BigInteger("123456789012345678901234567890");
        result = BytesUtils.fromUint256(largeValue);
        assertEquals(32, result.length);
    }

    /**
     * Test getUint8 with valid byte array
     */
    @Test
    public void testGetUint8_ValidByteArray() {
        byte[] data = {0, 1, 2, 127, -128, -1};
        
        assertEquals(0, BytesUtils.getUint8(data, 0));
        assertEquals(1, BytesUtils.getUint8(data, 1));
        assertEquals(2, BytesUtils.getUint8(data, 2));
        assertEquals(127, BytesUtils.getUint8(data, 3));
        assertEquals(-128, BytesUtils.getUint8(data, 4));
        assertEquals(-1, BytesUtils.getUint8(data, 5));
    }

    /**
     * Test getUint16 with valid byte array
     */
    @Test
    public void testGetUint16_ValidByteArray() {
        // Test value 256 (0x0100)
        byte[] data = {0x01, 0x00};
        int result = BytesUtils.getUint16(data, 0);
        assertEquals(256, result);

        // Test value 65535 (0xFFFF)
        byte[] data2 = {(byte) 0xFF, (byte) 0xFF};
        result = BytesUtils.getUint16(data2, 0);
        assertEquals(65535, result);

        // Test zero
        byte[] data3 = {0x00, 0x00};
        result = BytesUtils.getUint16(data3, 0);
        assertEquals(0, result);
    }

    /**
     * Test getUint24 with valid byte array
     */
    @Test
    public void testGetUint24_ValidByteArray() {
        // Test value 256 (0x000100)
        byte[] data = {0x00, 0x01, 0x00};
        int result = BytesUtils.getUint24(data, 0);
        assertEquals(256, result);

        // Test max 24-bit value (0xFFFFFF = 16777215)
        byte[] data2 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        result = BytesUtils.getUint24(data2, 0);
        assertEquals(16777215, result);

        // Test zero
        byte[] data3 = {0x00, 0x00, 0x00};
        result = BytesUtils.getUint24(data3, 0);
        assertEquals(0, result);
    }

    /**
     * Test getUint32 with valid byte array
     */
    @Test
    public void testGetUint32_ValidByteArray() {
        // Test value 256 (0x00000100)
        byte[] data = {0x00, 0x00, 0x01, 0x00};
        long result = BytesUtils.getUint32(data, 0);
        assertEquals(256L, result);

        // Test max uint32 value (0xFFFFFFFF = 4294967295)
        byte[] data2 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        result = BytesUtils.getUint32(data2, 0);
        assertEquals(4294967295L, result);

        // Test zero
        byte[] data3 = {0x00, 0x00, 0x00, 0x00};
        result = BytesUtils.getUint32(data3, 0);
        assertEquals(0L, result);
    }

    /**
     * Test getUint64 with valid byte array
     */
    @Test
    public void testGetUint64_ValidByteArray() {
        // Test small value
        byte[] data = new byte[8];
        data[7] = (byte) 0xFF;
        BigInteger result = BytesUtils.getUint64(data, 0);
        assertEquals(BigInteger.valueOf(255), result);

        // Test zero
        byte[] data2 = new byte[8];
        result = BytesUtils.getUint64(data2, 0);
        assertEquals(BigInteger.ZERO, result);

        // Test large value
        byte[] data3 = new byte[8];
        for (int i = 0; i < 8; i++) {
            data3[i] = (byte) 0xFF;
        }
        result = BytesUtils.getUint64(data3, 0);
        assertEquals(new BigInteger("18446744073709551615"), result);
    }

    /**
     * Test getUint256 with valid byte array
     */
    @Test
    public void testGetUint256_ValidByteArray() {
        // Test zero
        byte[] data = new byte[32];
        BigInteger result = BytesUtils.getUint256(data, 0);
        assertEquals(BigInteger.ZERO, result);

        // Test small value
        byte[] data2 = new byte[32];
        data2[31] = (byte) 0xFF;
        result = BytesUtils.getUint256(data2, 0);
        assertEquals(BigInteger.valueOf(255), result);

        // Test all ones
        byte[] data3 = new byte[32];
        for (int i = 0; i < 32; i++) {
            data3[i] = (byte) 0xFF;
        }
        result = BytesUtils.getUint256(data3, 0);
        assertTrue(result.compareTo(BigInteger.ZERO) > 0);
    }

    /**
     * Test getBytes32 with valid byte array
     */
    @Test
    public void testGetBytes32_ValidByteArray() {
        byte[] data = new byte[64];
        for (int i = 0; i < 64; i++) {
            data[i] = (byte) i;
        }

        byte[] result = BytesUtils.getBytes32(data, 0);
        assertEquals(32, result.length);
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) i, result[i]);
        }

        result = BytesUtils.getBytes32(data, 32);
        assertEquals(32, result.length);
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) (i + 32), result[i]);
        }
    }

    /**
     * Test getBytes with valid parameters
     */
    @Test
    public void testGetBytes_ValidParameters() {
        byte[] data = new byte[100];
        for (int i = 0; i < 100; i++) {
            data[i] = (byte) i;
        }

        // Get first 10 bytes
        byte[] result = BytesUtils.getBytes(data, 0, 10);
        assertEquals(10, result.length);
        for (int i = 0; i < 10; i++) {
            assertEquals((byte) i, result[i]);
        }

        // Get middle 20 bytes
        result = BytesUtils.getBytes(data, 40, 20);
        assertEquals(20, result.length);
        for (int i = 0; i < 20; i++) {
            assertEquals((byte) (i + 40), result[i]);
        }

        // Get single byte
        result = BytesUtils.getBytes(data, 50, 1);
        assertEquals(1, result.length);
        assertEquals((byte) 50, result[0]);
    }

    /**
     * Test toUnsignedByteArray with valid parameters
     */
    @Test
    public void testToUnsignedByteArray_ValidParameters() {
        // Test zero
        byte[] result = BytesUtils.toUnsignedByteArray(8, BigInteger.ZERO);
        assertEquals(8, result.length);
        assertArrayEquals(new byte[8], result);

        // Test small value
        result = BytesUtils.toUnsignedByteArray(8, BigInteger.valueOf(255));
        assertEquals(8, result.length);
        assertEquals((byte) 255, result[7]);

        // Test exact length match
        BigInteger value = new BigInteger("256");
        result = BytesUtils.toUnsignedByteArray(2, value);
        assertEquals(2, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 0, result[1]);
    }

    /**
     * Test calcBytesInEvmWord with valid inputs
     */
    @Test
    public void testCalcBytesInEvmWord_ValidInputs() {
        // 0 bytes -> 0 words -> 0 bytes
        assertEquals(0, BytesUtils.calcBytesInEvmWord(0));

        // 1-32 bytes -> 1 word -> 32 bytes
        assertEquals(32, BytesUtils.calcBytesInEvmWord(1));
        assertEquals(32, BytesUtils.calcBytesInEvmWord(16));
        assertEquals(32, BytesUtils.calcBytesInEvmWord(32));

        // 33-64 bytes -> 2 words -> 64 bytes
        assertEquals(64, BytesUtils.calcBytesInEvmWord(33));
        assertEquals(64, BytesUtils.calcBytesInEvmWord(50));
        assertEquals(64, BytesUtils.calcBytesInEvmWord(64));

        // 65-96 bytes -> 3 words -> 96 bytes
        assertEquals(96, BytesUtils.calcBytesInEvmWord(65));
        assertEquals(96, BytesUtils.calcBytesInEvmWord(96));
    }

    /**
     * Test calcEvmWordNum with valid inputs
     */
    @Test
    public void testCalcEvmWordNum_ValidInputs() {
        // 0 bytes -> 0 words
        assertEquals(0, BytesUtils.calcEvmWordNum(0));

        // 1-32 bytes -> 1 word
        assertEquals(1, BytesUtils.calcEvmWordNum(1));
        assertEquals(1, BytesUtils.calcEvmWordNum(16));
        assertEquals(1, BytesUtils.calcEvmWordNum(32));

        // 33-64 bytes -> 2 words
        assertEquals(2, BytesUtils.calcEvmWordNum(33));
        assertEquals(2, BytesUtils.calcEvmWordNum(50));
        assertEquals(2, BytesUtils.calcEvmWordNum(64));

        // 65-96 bytes -> 3 words
        assertEquals(3, BytesUtils.calcEvmWordNum(65));
        assertEquals(3, BytesUtils.calcEvmWordNum(96));

        // Large number
        assertEquals(10, BytesUtils.calcEvmWordNum(320));
        assertEquals(11, BytesUtils.calcEvmWordNum(321));
    }

    // ==================== Negative Tests ====================

    /**
     * Test fromUint64 with negative value
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromUint64_NegativeValue() {
        BytesUtils.fromUint64(BigInteger.valueOf(-1));
    }

    /**
     * Test fromUint64 with large negative value
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromUint64_LargeNegativeValue() {
        BytesUtils.fromUint64(BigInteger.valueOf(-1000000));
    }

    /**
     * Test fromUint256 with negative value
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromUint256_NegativeValue() {
        BytesUtils.fromUint256(BigInteger.valueOf(-1));
    }

    /**
     * Test fromUint256 with large negative value
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromUint256_LargeNegativeValue() {
        BytesUtils.fromUint256(new BigInteger("-123456789012345678901234567890"));
    }

    /**
     * Test getUint8 with invalid offset (out of bounds)
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint8_InvalidOffset() {
        byte[] data = {1, 2, 3};
        BytesUtils.getUint8(data, 10);
    }

    /**
     * Test getUint8 with negative offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint8_NegativeOffset() {
        byte[] data = {1, 2, 3};
        BytesUtils.getUint8(data, -1);
    }

    /**
     * Test getUint16 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint16_InsufficientData() {
        byte[] data = {1};
        BytesUtils.getUint16(data, 0);
    }

    /**
     * Test getUint16 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint16_InvalidOffset() {
        byte[] data = {1, 2, 3};
        BytesUtils.getUint16(data, 2);
    }

    /**
     * Test getUint24 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint24_InsufficientData() {
        byte[] data = {1, 2};
        BytesUtils.getUint24(data, 0);
    }

    /**
     * Test getUint24 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint24_InvalidOffset() {
        byte[] data = {1, 2, 3, 4};
        BytesUtils.getUint24(data, 2);
    }

    /**
     * Test getUint32 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint32_InsufficientData() {
        byte[] data = {1, 2, 3};
        BytesUtils.getUint32(data, 0);
    }

    /**
     * Test getUint32 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint32_InvalidOffset() {
        byte[] data = {1, 2, 3, 4, 5};
        BytesUtils.getUint32(data, 2);
    }

    /**
     * Test getUint64 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint64_InsufficientData() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7};
        BytesUtils.getUint64(data, 0);
    }

    /**
     * Test getUint64 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint64_InvalidOffset() {
        byte[] data = new byte[10];
        BytesUtils.getUint64(data, 3);
    }

    /**
     * Test getUint256 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint256_InsufficientData() {
        byte[] data = new byte[31];
        BytesUtils.getUint256(data, 0);
    }

    /**
     * Test getUint256 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetUint256_InvalidOffset() {
        byte[] data = new byte[40];
        BytesUtils.getUint256(data, 9);
    }

    /**
     * Test getBytes32 with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetBytes32_InsufficientData() {
        byte[] data = new byte[31];
        BytesUtils.getBytes32(data, 0);
    }

    /**
     * Test getBytes32 with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetBytes32_InvalidOffset() {
        byte[] data = new byte[40];
        BytesUtils.getBytes32(data, 9);
    }

    /**
     * Test getBytes with invalid length (negative)
     * Should throw NegativeArraySizeException
     */
    @Test(expected = NegativeArraySizeException.class)
    public void testGetBytes_NegativeLength() {
        byte[] data = new byte[10];
        BytesUtils.getBytes(data, 0, -1);
    }

    /**
     * Test getBytes with insufficient data
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetBytes_InsufficientData() {
        byte[] data = new byte[10];
        BytesUtils.getBytes(data, 5, 10);
    }

    /**
     * Test getBytes with invalid offset
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetBytes_InvalidOffset() {
        byte[] data = new byte[10];
        BytesUtils.getBytes(data, 15, 5);
    }

    /**
     * Test toUnsignedByteArray with value exceeding length
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToUnsignedByteArray_ValueExceedsLength() {
        // Try to fit 256 (requires 2 bytes) into 1 byte
        BytesUtils.toUnsignedByteArray(1, BigInteger.valueOf(256));
    }

    /**
     * Test toUnsignedByteArray with large value exceeding length
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToUnsignedByteArray_LargeValueExceedsLength() {
        // Try to fit a large value into insufficient space
        BigInteger largeValue = new BigInteger("123456789012345678901234567890");
        BytesUtils.toUnsignedByteArray(8, largeValue);
    }

    /**
     * Test toUnsignedByteArray with zero length
     * Should throw IllegalArgumentException when value is not zero
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToUnsignedByteArray_ZeroLengthNonZeroValue() {
        BytesUtils.toUnsignedByteArray(0, BigInteger.ONE);
    }

    /**
     * Test getUint8 with null array
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetUint8_NullArray() {
        BytesUtils.getUint8(null, 0);
    }

    /**
     * Test getUint16 with null array
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetUint16_NullArray() {
        BytesUtils.getUint16(null, 0);
    }

    /**
     * Test getUint64 with null array
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetUint64_NullArray() {
        BytesUtils.getUint64(null, 0);
    }

    /**
     * Test getBytes with null array
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetBytes_NullArray() {
        BytesUtils.getBytes(null, 0, 10);
    }

    /**
     * Test fromUint64 with null value
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testFromUint64_NullValue() {
        BytesUtils.fromUint64(null);
    }

    /**
     * Test fromUint256 with null value
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testFromUint256_NullValue() {
        BytesUtils.fromUint256(null);
    }

    /**
     * Test calcEvmWordNum with negative input
     * Result should be based on bit operations (maybe unexpected)
     */
    @Test
    public void testCalcEvmWordNum_NegativeInput() {
        // Negative numbers will have unexpected results due to bit operations
        // This test documents the behavior rather than validates correctness
        int result = BytesUtils.calcEvmWordNum(-1);
        // -1 >> 5 = -1, and (-1 & 31) = 31, so result = -1 + 1 = 0
        // The actual behavior with negative numbers may vary
        // Just verify the method doesn't throw exception
        assertEquals(0, result);
    }

    /**
     * Test calcBytesInEvmWord with negative input
     * Result should be based on bit operations (may be unexpected)
     */
    @Test
    public void testCalcBytesInEvmWord_NegativeInput() {
        // Negative numbers will have unexpected results due to bit operations
        // This test documents the behavior rather than validates correctness
        int result = BytesUtils.calcBytesInEvmWord(-1);
        // calcBytesInEvmWord calls calcEvmWordNum and then << 5
        // The actual behavior with negative numbers may vary
        // Just verify the method doesn't throw exception
        assertEquals(0, result);
    }
}
