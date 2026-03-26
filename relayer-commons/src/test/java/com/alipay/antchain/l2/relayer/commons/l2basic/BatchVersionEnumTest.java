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
 * Unit tests for BatchVersionEnum
 */
public class BatchVersionEnumTest {

    // ==================== Positive Tests ====================

    /**
     * Test BATCH_V0 enum value
     * Should have correct value and no compression support
     */
    @Test
    public void testBatchV0() {
        BatchVersionEnum version = BatchVersionEnum.BATCH_V0;

        assertNotNull(version);
        assertEquals((byte) 0, version.getValue());
        assertFalse(version.isBatchDataCompressionSupport());
        assertNull(version.getDaCompressor());
    }

    /**
     * Test BATCH_V1 enum value
     * Should have correct value and compression support
     */
    @Test
    public void testBatchV1() {
        BatchVersionEnum version = BatchVersionEnum.BATCH_V1;

        assertNotNull(version);
        assertEquals((byte) 1, version.getValue());
        assertTrue(version.isBatchDataCompressionSupport());
        assertNotNull(version.getDaCompressor());
        assertEquals(IDaCompressor.ZSTD_DEFAULT_COMPRESSOR, version.getDaCompressor());
    }

    /**
     * Test from method with byte value 0
     * Should return BATCH_V0
     */
    @Test
    public void testFromByte0() {
        BatchVersionEnum version = BatchVersionEnum.from((byte) 0);

        assertNotNull(version);
        assertEquals(BatchVersionEnum.BATCH_V0, version);
    }

    /**
     * Test from method with byte value 1
     * Should return BATCH_V1
     */
    @Test
    public void testFromByte1() {
        BatchVersionEnum version = BatchVersionEnum.from((byte) 1);

        assertNotNull(version);
        assertEquals(BatchVersionEnum.BATCH_V1, version);
    }

    /**
     * Test from method with int value 0
     * Should return BATCH_V0
     */
    @Test
    public void testFromInt0() {
        BatchVersionEnum version = BatchVersionEnum.from(0);

        assertNotNull(version);
        assertEquals(BatchVersionEnum.BATCH_V0, version);
    }

    /**
     * Test from method with int value 1
     * Should return BATCH_V1
     */
    @Test
    public void testFromInt1() {
        BatchVersionEnum version = BatchVersionEnum.from(1);

        assertNotNull(version);
        assertEquals(BatchVersionEnum.BATCH_V1, version);
    }

    /**
     * Test values method
     * Should return all enum values
     */
    @Test
    public void testValues() {
        BatchVersionEnum[] values = BatchVersionEnum.values();

        assertNotNull(values);
        assertEquals(3, values.length);
        assertEquals(BatchVersionEnum.BATCH_V0, values[0]);
        assertEquals(BatchVersionEnum.BATCH_V1, values[1]);
        assertEquals(BatchVersionEnum.BATCH_V2, values[2]);
    }

    /**
     * Test valueOf method
     * Should return correct enum by name
     */
    @Test
    public void testValueOf() {
        BatchVersionEnum v0 = BatchVersionEnum.valueOf("BATCH_V0");
        BatchVersionEnum v1 = BatchVersionEnum.valueOf("BATCH_V1");

        assertEquals(BatchVersionEnum.BATCH_V0, v0);
        assertEquals(BatchVersionEnum.BATCH_V1, v1);
    }

    /**
     * Test compression support for V0
     * Should not support compression
     */
    @Test
    public void testCompressionSupportV0() {
        assertFalse(BatchVersionEnum.BATCH_V0.isBatchDataCompressionSupport());
    }

    /**
     * Test compression support for V1
     * Should support compression
     */
    @Test
    public void testCompressionSupportV1() {
        assertTrue(BatchVersionEnum.BATCH_V1.isBatchDataCompressionSupport());
    }

    /**
     * Test getDaCompressor for V0
     * Should return null
     */
    @Test
    public void testGetDaCompressorV0() {
        assertNull(BatchVersionEnum.BATCH_V0.getDaCompressor());
    }

    /**
     * Test getDaCompressor for V1
     * Should return ZSTD compressor
     */
    @Test
    public void testGetDaCompressorV1() {
        IDaCompressor compressor = BatchVersionEnum.BATCH_V1.getDaCompressor();

        assertNotNull(compressor);
        assertEquals(IDaCompressor.ZSTD_DEFAULT_COMPRESSOR, compressor);
    }

    /**
     * Test enum equality
     * Same enum values should be equal
     */
    @Test
    public void testEnumEquality() {
        BatchVersionEnum v0_1 = BatchVersionEnum.from((byte) 0);
        BatchVersionEnum v0_2 = BatchVersionEnum.from(0);

        assertEquals(v0_1, v0_2);
        assertSame(v0_1, v0_2);
    }

    /**
     * Test enum inequality
     * Different enum values should not be equal
     */
    @Test
    public void testEnumInequality() {
        BatchVersionEnum v0 = BatchVersionEnum.BATCH_V0;
        BatchVersionEnum v1 = BatchVersionEnum.BATCH_V1;

        assertNotEquals(v0, v1);
    }

    /**
     * Test getValue consistency
     * getValue should return consistent values
     */
    @Test
    public void testGetValueConsistency() {
        assertEquals((byte) 0, BatchVersionEnum.BATCH_V0.getValue());
        assertEquals((byte) 1, BatchVersionEnum.BATCH_V1.getValue());
    }

    // ==================== Negative Tests ====================

    /**
     * Test from method with invalid byte value
     * Should return null for unknown version
     */
    @Test
    public void testFromInvalidByte() {
        BatchVersionEnum version = BatchVersionEnum.from((byte) 99);

        assertNull(version);
    }

    /**
     * Test from method with negative byte value
     * Should return null
     */
    @Test
    public void testFromNegativeByte() {
        BatchVersionEnum version = BatchVersionEnum.from((byte) -1);

        assertNull(version);
    }

    /**
     * Test from method with invalid int value
     * Should return null
     */
    @Test
    public void testFromInvalidInt() {
        BatchVersionEnum version = BatchVersionEnum.from(100);

        assertNull(version);
    }

    /**
     * Test from method with negative int value
     * Should return null
     */
    @Test
    public void testFromNegativeInt() {
        BatchVersionEnum version = BatchVersionEnum.from(-1);

        assertNull(version);
    }

    /**
     * Test from method with max byte value
     * Should return null
     */
    @Test
    public void testFromMaxByte() {
        BatchVersionEnum version = BatchVersionEnum.from(Byte.MAX_VALUE);

        assertNull(version);
    }

    /**
     * Test from method with min byte value
     * Should return null
     */
    @Test
    public void testFromMinByte() {
        BatchVersionEnum version = BatchVersionEnum.from(Byte.MIN_VALUE);

        assertNull(version);
    }

    /**
     * Test valueOf with invalid name
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidName() {
        BatchVersionEnum.valueOf("INVALID_VERSION");
    }

    /**
     * Test valueOf with null name
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testValueOfNullName() {
        BatchVersionEnum.valueOf(null);
    }

    /**
     * Test valueOf with empty name
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfEmptyName() {
        BatchVersionEnum.valueOf("");
    }

    /**
     * Test from method with boundary values
     * Should return null for values 2-255
     */
    @Test
    public void testFromBoundaryValues() {
        assertNull(BatchVersionEnum.from((byte) 3));
        assertNull(BatchVersionEnum.from((byte) 10));
        assertNull(BatchVersionEnum.from((byte) 100));
        assertNull(BatchVersionEnum.from((byte) 255));
    }

    /**
     * Test compression support consistency
     * V0 should always not support, V1 should always support
     */
    @Test
    public void testCompressionSupportConsistency() {
        // Test multiple times to ensure consistency
        for (int i = 0; i < 10; i++) {
            assertFalse(BatchVersionEnum.BATCH_V0.isBatchDataCompressionSupport());
            assertTrue(BatchVersionEnum.BATCH_V1.isBatchDataCompressionSupport());
        }
    }

    /**
     * Test from method with various invalid values
     */
    @Test
    public void testFromVariousInvalidValues() {
        byte[] invalidValues = {3, 5, 10, 20, 50, 100, 127, -128, -1};

        for (byte value : invalidValues) {
            assertNull("Expected null for value: " + value, BatchVersionEnum.from(value));
        }
    }
}
