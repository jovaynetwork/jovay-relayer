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
 * Unit tests for DaVersion
 */
public class DaVersionTest {

    // ==================== Positive Tests ====================

    /**
     * Test DA_0 enum value
     * Should have correct value and not be compressed
     */
    @Test
    public void testDa0() {
        DaVersion version = DaVersion.DA_0;

        assertNotNull(version);
        assertEquals((byte) 0, version.toByte());
        assertFalse(version.isCompressed());
    }

    /**
     * Test DA_1 enum value
     * Should have correct value and not be compressed
     */
    @Test
    public void testDa1() {
        DaVersion version = DaVersion.DA_1;

        assertNotNull(version);
        assertEquals((byte) 1, version.toByte());
        assertFalse(version.isCompressed());
    }

    /**
     * Test DA_2 enum value
     * Should have correct value and be compressed
     */
    @Test
    public void testDa2() {
        DaVersion version = DaVersion.DA_2;

        assertNotNull(version);
        assertEquals((byte) 2, version.toByte());
        assertTrue(version.isCompressed());
    }

    /**
     * Test from method with byte value 0
     * Should return DA_0
     */
    @Test
    public void testFromByte0() {
        DaVersion version = DaVersion.from((byte) 0);

        assertNotNull(version);
        assertEquals(DaVersion.DA_0, version);
    }

    /**
     * Test from method with byte value 1
     * Should return DA_1
     */
    @Test
    public void testFromByte1() {
        DaVersion version = DaVersion.from((byte) 1);

        assertNotNull(version);
        assertEquals(DaVersion.DA_1, version);
    }

    /**
     * Test from method with byte value 2
     * Should return DA_2
     */
    @Test
    public void testFromByte2() {
        DaVersion version = DaVersion.from((byte) 2);

        assertNotNull(version);
        assertEquals(DaVersion.DA_2, version);
    }

    /**
     * Test values method
     * Should return all enum values
     */
    @Test
    public void testValues() {
        DaVersion[] values = DaVersion.values();

        assertNotNull(values);
        assertEquals(3, values.length);
        assertEquals(DaVersion.DA_0, values[0]);
        assertEquals(DaVersion.DA_1, values[1]);
        assertEquals(DaVersion.DA_2, values[2]);
    }

    /**
     * Test valueOf method
     * Should return correct enum by name
     */
    @Test
    public void testValueOf() {
        DaVersion da0 = DaVersion.valueOf("DA_0");
        DaVersion da1 = DaVersion.valueOf("DA_1");
        DaVersion da2 = DaVersion.valueOf("DA_2");

        assertEquals(DaVersion.DA_0, da0);
        assertEquals(DaVersion.DA_1, da1);
        assertEquals(DaVersion.DA_2, da2);
    }

    /**
     * Test isCompressed for DA_0
     * Should not be compressed
     */
    @Test
    public void testIsCompressedDa0() {
        assertFalse(DaVersion.DA_0.isCompressed());
    }

    /**
     * Test isCompressed for DA_1
     * Should not be compressed
     */
    @Test
    public void testIsCompressedDa1() {
        assertFalse(DaVersion.DA_1.isCompressed());
    }

    /**
     * Test isCompressed for DA_2
     * Should be compressed
     */
    @Test
    public void testIsCompressedDa2() {
        assertTrue(DaVersion.DA_2.isCompressed());
    }

    /**
     * Test toByte consistency
     * Should return consistent byte values
     */
    @Test
    public void testToByteConsistency() {
        assertEquals((byte) 0, DaVersion.DA_0.toByte());
        assertEquals((byte) 1, DaVersion.DA_1.toByte());
        assertEquals((byte) 2, DaVersion.DA_2.toByte());
    }

    /**
     * Test enum equality
     * Same enum values should be equal
     */
    @Test
    public void testEnumEquality() {
        DaVersion da0_1 = DaVersion.from((byte) 0);
        DaVersion da0_2 = DaVersion.from((byte) 0);

        assertEquals(da0_1, da0_2);
        assertSame(da0_1, da0_2);
    }

    /**
     * Test enum inequality
     * Different enum values should not be equal
     */
    @Test
    public void testEnumInequality() {
        DaVersion da0 = DaVersion.DA_0;
        DaVersion da1 = DaVersion.DA_1;
        DaVersion da2 = DaVersion.DA_2;

        assertNotEquals(da0, da1);
        assertNotEquals(da1, da2);
        assertNotEquals(da0, da2);
    }

    /**
     * Test from method with valid boundary values
     * Should return correct versions for 0, 1, 2
     */
    @Test
    public void testFromValidBoundaryValues() {
        assertEquals(DaVersion.DA_0, DaVersion.from((byte) 0));
        assertEquals(DaVersion.DA_1, DaVersion.from((byte) 1));
        assertEquals(DaVersion.DA_2, DaVersion.from((byte) 2));
    }

    /**
     * Test compression logic
     * Only DA_2 should be compressed
     */
    @Test
    public void testCompressionLogic() {
        assertFalse(DaVersion.DA_0.isCompressed());
        assertFalse(DaVersion.DA_1.isCompressed());
        assertTrue(DaVersion.DA_2.isCompressed());
    }

    /**
     * Test from method with value 3
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValue3() {
        DaVersion.from((byte) 3);
    }

    /**
     * Test from method with value 10
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValue10() {
        DaVersion.from((byte) 10);
    }

    // ==================== Negative Tests ====================

    /**
     * Test from method with value 115 (BLS modulus boundary)
     * Should throw IllegalArgumentException due to assertion
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValue115() {
        DaVersion.from((byte) 115);
    }

    /**
     * Test from method with value greater than 115
     * Should throw IllegalArgumentException due to assertion
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueGreaterThan115() {
        DaVersion.from((byte) 120);
    }

    /**
     * Test from method with max byte value (127)
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromMaxByte() {
        DaVersion.from(Byte.MAX_VALUE);
    }

    /**
     * Test from method with negative value
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromNegativeValue() {
        DaVersion.from((byte) -1);
    }

    /**
     * Test from method with min byte value (-128)
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromMinByte() {
        DaVersion.from(Byte.MIN_VALUE);
    }

    /**
     * Test from method with invalid value 4
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromInvalidValue4() {
        DaVersion.from((byte) 4);
    }

    /**
     * Test from method with invalid value 50
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromInvalidValue50() {
        DaVersion.from((byte) 50);
    }

    /**
     * Test from method with invalid value 100
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromInvalidValue100() {
        DaVersion.from((byte) 100);
    }

    /**
     * Test from method with invalid value 114 (just below BLS modulus)
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValue114() {
        DaVersion.from((byte) 114);
    }

    /**
     * Test valueOf with invalid name
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidName() {
        DaVersion.valueOf("INVALID_VERSION");
    }

    /**
     * Test valueOf with null name
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testValueOfNullName() {
        DaVersion.valueOf(null);
    }

    /**
     * Test valueOf with empty name
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfEmptyName() {
        DaVersion.valueOf("");
    }

    /**
     * Test isCompressed consistency
     * Should always return same result for same version
     */
    @Test
    public void testIsCompressedConsistency() {
        // Test multiple times to ensure consistency
        for (int i = 0; i < 10; i++) {
            assertFalse(DaVersion.DA_0.isCompressed());
            assertFalse(DaVersion.DA_1.isCompressed());
            assertTrue(DaVersion.DA_2.isCompressed());
        }
    }

    /**
     * Test toByte consistency across multiple calls
     * Should always return same value
     */
    @Test
    public void testToByteMultipleCalls() {
        for (int i = 0; i < 10; i++) {
            assertEquals((byte) 0, DaVersion.DA_0.toByte());
            assertEquals((byte) 1, DaVersion.DA_1.toByte());
            assertEquals((byte) 2, DaVersion.DA_2.toByte());
        }
    }

    /**
     * Test from method with various invalid values
     * All should throw IllegalArgumentException
     */
    @Test
    public void testFromVariousInvalidValues() {
        byte[] invalidValues = {3, 5, 10, 20, 50, 100, 114, 115, 120, 127};

        for (byte value : invalidValues) {
            try {
                DaVersion.from(value);
                fail("Expected IllegalArgumentException for value: " + value);
            } catch (IllegalArgumentException e) {
                // Expected
                assertNotNull(e);
            }
        }
    }

    /**
     * Test from method with negative values
     * All should throw IllegalArgumentException
     */
    @Test
    public void testFromNegativeValues() {
        byte[] negativeValues = {-1, -2, -10, -50, -100, -128};

        for (byte value : negativeValues) {
            try {
                DaVersion.from(value);
                fail("Expected IllegalArgumentException for value: " + value);
            } catch (IllegalArgumentException e) {
                // Expected
                assertNotNull(e);
            }
        }
    }

    /**
     * Test BLS modulus boundary (value 115)
     * Should throw IllegalArgumentException with assertion message
     */
    @Test
    public void testBlsModulusBoundary() {
        try {
            DaVersion.from((byte) 115);
            fail("Expected IllegalArgumentException for value 115");
        } catch (IllegalArgumentException e) {
            // Expected - should contain assertion message about BLS modulus
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("BLS modulus") || 
                      e.getMessage().contains("Can't be greater"));
        }
    }

    /**
     * Test compression boundary
     * Only values > 1 should be compressed
     */
    @Test
    public void testCompressionBoundary() {
        // DA_0 (value 0) and DA_1 (value 1) should not be compressed
        assertFalse(DaVersion.DA_0.isCompressed());
        assertFalse(DaVersion.DA_1.isCompressed());
        
        // DA_2 (value 2) should be compressed
        assertTrue(DaVersion.DA_2.isCompressed());
    }
}
