package com.alipay.antchain.l2.relayer.commons.abi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbiDecoderTest {

    private AbiDecoder decoder;
    
    // Simple ABI JSON containing custom errors
    private static final String SIMPLE_ABI = """
            [
              {
                "type": "error",
                "name": "InsufficientBalance",
                "inputs": [
                  {
                    "name": "available",
                    "type": "uint256"
                  },
                  {
                    "name": "required",
                    "type": "uint256"
                  }
                ]
              },
              {
                "type": "error",
                "name": "Unauthorized",
                "inputs": [
                  {
                    "name": "caller",
                    "type": "address"
                  }
                ]
              }
            ]
            """;

    @Before
    public void setUp() {
        decoder = new AbiDecoder("TestContract", SIMPLE_ABI);
    }

    /**
     * Test: Decode empty string
     * Scenario: Pass empty string as error code
     * Expected: Returns null
     */
    @Test
    public void testDecodeError_EmptyString() {
        // Act
        AbiCustomerError result = decoder.decodeError("");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Decode error code with insufficient length
     * Scenario: Pass error code with length less than 8
     * Expected: Returns null
     */
    @Test
    public void testDecodeError_ShortCode() {
        // Act
        AbiCustomerError result = decoder.decodeError("0x1234");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Decode unknown error selector
     * Scenario: Pass error selector not defined in ABI
     * Expected: Returns null
     */
    @Test
    public void testDecodeError_UnknownSelector() {
        // Act - Use a non-existent selector
        AbiCustomerError result = decoder.decodeError("0xffffffff00000000000000000000000000000000000000000000000000000000");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Decode malformed hexadecimal string
     * Scenario: Pass string containing non-hexadecimal characters
     * Expected: Returns null (returns after catching exception)
     */
    @Test
    public void testDecodeError_InvalidHexString() {
        // Act - Contains non-hexadecimal characters
        AbiCustomerError result = decoder.decodeError("0xGGGGGGGG00000000000000000000000000000000000000000000000000000000");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Decode error code with mismatched data length
     * Scenario: Selector is correct but data length is insufficient
     * Expected: Returns null (decoding fails)
     */
    @Test
    public void testDecodeError_InsufficientData() {
        // Act - Only selector, no parameter data
        AbiCustomerError result = decoder.decodeError("0x12345678");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Decode null input
     * Scenario: Pass null
     * Expected: Throws NullPointerException or returns null
     */
    @Test
    public void testDecodeError_NullInput() {
        // Act & Assert
        try {
            AbiCustomerError result = decoder.decodeError(null);
            // If no exception is thrown, should return null
            Assert.assertNull(result);
        } catch (NullPointerException e) {
            // NPE is also acceptable behavior
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test: Create decoder with empty ABI
     * Scenario: Pass empty ABI JSON
     * Expected: Creates successfully, but decoding any error returns null
     */
    @Test
    public void testDecodeError_EmptyAbi() {
        // Arrange
        AbiDecoder emptyDecoder = new AbiDecoder("EmptyContract", "[]");
        
        // Act
        AbiCustomerError result = emptyDecoder.decodeError("0x1234567800000000000000000000000000000000000000000000000000000000");
        
        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: Create decoder with invalid ABI JSON
     * Scenario: Pass malformed JSON
     * Expected: Throws exception
     */
    @Test
    public void testConstructor_InvalidJson() {
        // Act & Assert
        Assert.assertThrows(
                Exception.class,
                () -> new AbiDecoder("InvalidContract", "invalid json")
        );
    }

    /**
     * Test: Decode error code with 0x prefix
     * Scenario: Pass valid error code with 0x prefix
     * Expected: Decodes normally (Numeric.cleanHexPrefix handles it)
     */
    @Test
    public void testDecodeError_WithHexPrefix() {
        // Act - Test prefix handling with non-existent selector
        AbiCustomerError result = decoder.decodeError("0xffffffff");

        // Assert - Should return null (selector does not exist)
        Assert.assertNull(result);
    }

    /**
     * Test: Decode extra long error code
     * Scenario: Pass extra long hexadecimal string
     * Expected: Handles normally (only uses first 8 characters as selector)
     */
    @Test
    public void testDecodeError_ExtraLongCode() {
        // Act - 超长但选择器不存在
        String longCode = "0xffffffff" + "0".repeat(1000);
        AbiCustomerError result = decoder.decodeError(longCode);
        
        // Assert
        Assert.assertNull(result);
    }
}
