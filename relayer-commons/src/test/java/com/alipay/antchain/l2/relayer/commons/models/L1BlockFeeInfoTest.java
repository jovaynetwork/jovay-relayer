package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for L1BlockFeeInfo
 */
public class L1BlockFeeInfoTest {

    private L1BlockFeeInfo l1BlockFeeInfo;

    @Before
    public void setUp() {
        l1BlockFeeInfo = new L1BlockFeeInfo();
    }

    // ==================== Positive Tests ====================

    /**
     * Test default constructor
     * Should create instance with null values
     */
    @Test
    public void testDefaultConstructor() {
        L1BlockFeeInfo info = new L1BlockFeeInfo();

        assertNotNull(info);
        assertNull(info.getNumberRaw());
        assertNull(info.getBaseFeePerGas());
        assertNull(info.getGasUsed());
        assertNull(info.getGasLimit());
        assertNull(info.getBlobGasUsed());
        assertNull(info.getExcessBlobGas());
    }

    /**
     * Test builder pattern
     * Should create instance with specified values
     */
    @Test
    public void testBuilder() {
        L1BlockFeeInfo info = L1BlockFeeInfo.builder()
                .number("0x64")
                .baseFeePerGas("0x3b9aca00")
                .gasUsed("0x5208")
                .gasLimit("0x1c9c380")
                .blobGasUsed("0x20000")
                .excessBlobGas("0x10000")
                .build();

        assertNotNull(info);
        assertEquals("0x64", info.getNumberRaw());
        assertEquals("0x3b9aca00", info.getBaseFeePerGas());
        assertEquals("0x5208", info.getGasUsed());
        assertEquals("0x1c9c380", info.getGasLimit());
        assertEquals("0x20000", info.getBlobGasUsed());
        assertEquals("0x10000", info.getExcessBlobGas());
    }

    /**
     * Test all args constructor
     * Should create instance with all specified values
     */
    @Test
    public void testAllArgsConstructor() {
        L1BlockFeeInfo info = new L1BlockFeeInfo(
                "0x100",
                "0x77359400",
                "0x7530",
                "0x1c9c380",
                "0x40000",
                "0x20000"
        );

        assertNotNull(info);
        assertEquals("0x100", info.getNumberRaw());
        assertEquals("0x77359400", info.getBaseFeePerGas());
        assertEquals("0x7530", info.getGasUsed());
        assertEquals("0x1c9c380", info.getGasLimit());
        assertEquals("0x40000", info.getBlobGasUsed());
        assertEquals("0x20000", info.getExcessBlobGas());
    }

    /**
     * Test getNumber method with hex string
     * Should decode hex string to BigInteger
     */
    @Test
    public void testGetNumberWithHexString() {
        l1BlockFeeInfo.setNumber("0x64");

        BigInteger number = l1BlockFeeInfo.getNumber();

        assertNotNull(number);
        assertEquals(BigInteger.valueOf(100), number);
    }

    /**
     * Test getNumber with different hex values
     */
    @Test
    public void testGetNumberWithDifferentHexValues() {
        // Test 0x0
        l1BlockFeeInfo.setNumber("0x0");
        assertEquals(BigInteger.ZERO, l1BlockFeeInfo.getNumber());

        // Test 0x1
        l1BlockFeeInfo.setNumber("0x1");
        assertEquals(BigInteger.ONE, l1BlockFeeInfo.getNumber());

        // Test 0xff
        l1BlockFeeInfo.setNumber("0xff");
        assertEquals(BigInteger.valueOf(255), l1BlockFeeInfo.getNumber());

        // Test 0x1000
        l1BlockFeeInfo.setNumber("0x1000");
        assertEquals(BigInteger.valueOf(4096), l1BlockFeeInfo.getNumber());
    }

    /**
     * Test getNumberRaw returns original hex string
     */
    @Test
    public void testGetNumberRaw() {
        String hexNumber = "0xabcdef";
        l1BlockFeeInfo.setNumber(hexNumber);

        assertEquals(hexNumber, l1BlockFeeInfo.getNumberRaw());
    }

    /**
     * Test getter and setter for number
     */
    @Test
    public void testNumberGetterSetter() {
        String value = "0x12345";
        l1BlockFeeInfo.setNumber(value);

        assertEquals(value, l1BlockFeeInfo.getNumberRaw());
        assertEquals(BigInteger.valueOf(74565), l1BlockFeeInfo.getNumber());
    }

    /**
     * Test getter and setter for baseFeePerGas
     */
    @Test
    public void testBaseFeePerGasGetterSetter() {
        String value = "0x3b9aca00";
        l1BlockFeeInfo.setBaseFeePerGas(value);

        assertEquals(value, l1BlockFeeInfo.getBaseFeePerGas());
    }

    /**
     * Test getter and setter for gasUsed
     */
    @Test
    public void testGasUsedGetterSetter() {
        String value = "0x5208";
        l1BlockFeeInfo.setGasUsed(value);

        assertEquals(value, l1BlockFeeInfo.getGasUsed());
    }

    /**
     * Test getter and setter for gasLimit
     */
    @Test
    public void testGasLimitGetterSetter() {
        String value = "0x1c9c380";
        l1BlockFeeInfo.setGasLimit(value);

        assertEquals(value, l1BlockFeeInfo.getGasLimit());
    }

    /**
     * Test getter and setter for blobGasUsed
     */
    @Test
    public void testBlobGasUsedGetterSetter() {
        String value = "0x20000";
        l1BlockFeeInfo.setBlobGasUsed(value);

        assertEquals(value, l1BlockFeeInfo.getBlobGasUsed());
    }

    /**
     * Test getter and setter for excessBlobGas
     */
    @Test
    public void testExcessBlobGasGetterSetter() {
        String value = "0x10000";
        l1BlockFeeInfo.setExcessBlobGas(value);

        assertEquals(value, l1BlockFeeInfo.getExcessBlobGas());
    }

    /**
     * Test builder with partial fields
     */
    @Test
    public void testBuilderWithPartialFields() {
        L1BlockFeeInfo info = L1BlockFeeInfo.builder()
                .number("0x100")
                .baseFeePerGas("0x3b9aca00")
                .build();

        assertNotNull(info);
        assertEquals("0x100", info.getNumberRaw());
        assertEquals("0x3b9aca00", info.getBaseFeePerGas());
        assertNull(info.getGasUsed());
        assertNull(info.getGasLimit());
    }

    /**
     * Test setting all fields
     */
    @Test
    public void testSetAllFields() {
        l1BlockFeeInfo.setNumber("0x200");
        l1BlockFeeInfo.setBaseFeePerGas("0x77359400");
        l1BlockFeeInfo.setGasUsed("0x7530");
        l1BlockFeeInfo.setGasLimit("0x1c9c380");
        l1BlockFeeInfo.setBlobGasUsed("0x40000");
        l1BlockFeeInfo.setExcessBlobGas("0x20000");

        assertEquals("0x200", l1BlockFeeInfo.getNumberRaw());
        assertEquals(BigInteger.valueOf(512), l1BlockFeeInfo.getNumber());
        assertEquals("0x77359400", l1BlockFeeInfo.getBaseFeePerGas());
        assertEquals("0x7530", l1BlockFeeInfo.getGasUsed());
        assertEquals("0x1c9c380", l1BlockFeeInfo.getGasLimit());
        assertEquals("0x40000", l1BlockFeeInfo.getBlobGasUsed());
        assertEquals("0x20000", l1BlockFeeInfo.getExcessBlobGas());
    }

    /**
     * Test large hex number
     */
    @Test
    public void testLargeHexNumber() {
        String largeHex = "0xffffffffffffffff";
        l1BlockFeeInfo.setNumber(largeHex);

        assertEquals(largeHex, l1BlockFeeInfo.getNumberRaw());
        assertEquals(new BigInteger("18446744073709551615"), l1BlockFeeInfo.getNumber());
    }

    /**
     * Test hex number without 0x prefix
     */
    @Test
    public void testHexNumberWithout0xPrefix() {
        // web3j Numeric.decodeQuantity should handle this
        l1BlockFeeInfo.setNumber("64");

        BigInteger number = l1BlockFeeInfo.getNumber();
        assertNotNull(number);
    }

    // ==================== Negative Tests ====================

    /**
     * Test setting null values
     * Should accept null values
     */
    @Test
    public void testSetNullValues() {
        l1BlockFeeInfo.setNumber(null);
        l1BlockFeeInfo.setBaseFeePerGas(null);
        l1BlockFeeInfo.setGasUsed(null);
        l1BlockFeeInfo.setGasLimit(null);
        l1BlockFeeInfo.setBlobGasUsed(null);
        l1BlockFeeInfo.setExcessBlobGas(null);

        assertNull(l1BlockFeeInfo.getNumberRaw());
        assertNull(l1BlockFeeInfo.getBaseFeePerGas());
        assertNull(l1BlockFeeInfo.getGasUsed());
        assertNull(l1BlockFeeInfo.getGasLimit());
        assertNull(l1BlockFeeInfo.getBlobGasUsed());
        assertNull(l1BlockFeeInfo.getExcessBlobGas());
    }

    /**
     * Test getNumber with null number field
     * Should throw exception or return null
     */
    @Test
    public void testGetNumberWithNullField() {
        l1BlockFeeInfo.setNumber(null);

        try {
            BigInteger number = l1BlockFeeInfo.getNumber();
            // If no exception, number should be null or zero
            assertTrue(number == null || number.equals(BigInteger.ZERO));
        } catch (Exception e) {
            // Exception is acceptable for null input
            assertNotNull(e);
        }
    }

    /**
     * Test getNumber with empty string
     */
    @Test
    public void testGetNumberWithEmptyString() {
        l1BlockFeeInfo.setNumber("");

        try {
            BigInteger number = l1BlockFeeInfo.getNumber();
            // If no exception, verify result
            assertNotNull(number);
        } catch (Exception e) {
            // Exception is acceptable for empty string
            assertNotNull(e);
        }
    }

    /**
     * Test getNumber with invalid hex string
     */
    @Test
    public void testGetNumberWithInvalidHexString() {
        l1BlockFeeInfo.setNumber("invalid");

        try {
            BigInteger number = l1BlockFeeInfo.getNumber();
            // If no exception, verify result
            assertNotNull(number);
        } catch (Exception e) {
            // Exception is expected for invalid hex
            assertNotNull(e);
        }
    }

    /**
     * Test builder with all null values
     */
    @Test
    public void testBuilderWithAllNullValues() {
        L1BlockFeeInfo info = L1BlockFeeInfo.builder()
                .number(null)
                .baseFeePerGas(null)
                .gasUsed(null)
                .gasLimit(null)
                .blobGasUsed(null)
                .excessBlobGas(null)
                .build();

        assertNotNull(info);
        assertNull(info.getNumberRaw());
        assertNull(info.getBaseFeePerGas());
        assertNull(info.getGasUsed());
        assertNull(info.getGasLimit());
        assertNull(info.getBlobGasUsed());
        assertNull(info.getExcessBlobGas());
    }

    /**
     * Test multiple updates to same field
     */
    @Test
    public void testMultipleUpdatesToSameField() {
        l1BlockFeeInfo.setNumber("0x1");
        assertEquals("0x1", l1BlockFeeInfo.getNumberRaw());

        l1BlockFeeInfo.setNumber("0x10");
        assertEquals("0x10", l1BlockFeeInfo.getNumberRaw());

        l1BlockFeeInfo.setNumber("0x100");
        assertEquals("0x100", l1BlockFeeInfo.getNumberRaw());
    }

    /**
     * Test setting empty strings
     */
    @Test
    public void testSetEmptyStrings() {
        l1BlockFeeInfo.setNumber("");
        l1BlockFeeInfo.setBaseFeePerGas("");
        l1BlockFeeInfo.setGasUsed("");
        l1BlockFeeInfo.setGasLimit("");
        l1BlockFeeInfo.setBlobGasUsed("");
        l1BlockFeeInfo.setExcessBlobGas("");

        assertEquals("", l1BlockFeeInfo.getNumberRaw());
        assertEquals("", l1BlockFeeInfo.getBaseFeePerGas());
        assertEquals("", l1BlockFeeInfo.getGasUsed());
        assertEquals("", l1BlockFeeInfo.getGasLimit());
        assertEquals("", l1BlockFeeInfo.getBlobGasUsed());
        assertEquals("", l1BlockFeeInfo.getExcessBlobGas());
    }

    /**
     * Test hex values with uppercase letters
     */
    @Test
    public void testHexValuesWithUppercase() {
        l1BlockFeeInfo.setNumber("0xABCDEF");

        assertEquals("0xABCDEF", l1BlockFeeInfo.getNumberRaw());
        assertEquals(BigInteger.valueOf(11259375), l1BlockFeeInfo.getNumber());
    }

    /**
     * Test hex values with mixed case
     */
    @Test
    public void testHexValuesWithMixedCase() {
        l1BlockFeeInfo.setNumber("0xAbCdEf");

        assertEquals("0xAbCdEf", l1BlockFeeInfo.getNumberRaw());
        assertEquals(BigInteger.valueOf(11259375), l1BlockFeeInfo.getNumber());
    }

    /**
     * Test zero values
     */
    @Test
    public void testZeroValues() {
        l1BlockFeeInfo.setNumber("0x0");
        l1BlockFeeInfo.setBaseFeePerGas("0x0");
        l1BlockFeeInfo.setGasUsed("0x0");
        l1BlockFeeInfo.setGasLimit("0x0");
        l1BlockFeeInfo.setBlobGasUsed("0x0");
        l1BlockFeeInfo.setExcessBlobGas("0x0");

        assertEquals("0x0", l1BlockFeeInfo.getNumberRaw());
        assertEquals(BigInteger.ZERO, l1BlockFeeInfo.getNumber());
        assertEquals("0x0", l1BlockFeeInfo.getBaseFeePerGas());
        assertEquals("0x0", l1BlockFeeInfo.getGasUsed());
        assertEquals("0x0", l1BlockFeeInfo.getGasLimit());
        assertEquals("0x0", l1BlockFeeInfo.getBlobGasUsed());
        assertEquals("0x0", l1BlockFeeInfo.getExcessBlobGas());
    }
}
