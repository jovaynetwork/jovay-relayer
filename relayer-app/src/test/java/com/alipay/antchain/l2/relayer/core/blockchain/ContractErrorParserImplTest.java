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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.lang.reflect.Field;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.abi.AbiCustomerError;
import com.alipay.antchain.l2.relayer.commons.abi.AbiDecoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Negative case tests for ContractErrorParserImpl
 * Tests error handling for invalid inputs and edge cases
 */
public class ContractErrorParserImplTest {

    private ContractErrorParserImpl parser;

    @Before
    public void setUp() {
        parser = new ContractErrorParserImpl();
    }

    @Test
    public void testParseWithNullInput() {
        // Act & Assert - parsing null should not throw exception
        AbiCustomerError result = parser.parse(null);
        
        // Should return null when no decoder matches
        Assert.assertNull("Parsing null input should return null", result);
    }

    @Test
    public void testParseWithEmptyString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act
        AbiCustomerError result = parser.parse("");
        
        // Assert - empty string should return null as no error matches
        Assert.assertNull("Parsing empty string should return null", result);
    }

    @Test
    public void testParseWithInvalidHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act
        AbiCustomerError result = parser.parse("not_a_hex_string");
        
        // Assert - invalid hex should return null
        Assert.assertNull("Parsing invalid hex string should return null", result);
    }

    @Test
    public void testParseWithNonExistentErrorSelector() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - use a valid hex string but with non-existent error selector
        AbiCustomerError result = parser.parse("0xffffffff");
        
        // Assert
        Assert.assertNull("Parsing non-existent error selector should return null", result);
    }

    @Test
    public void testParseWithNoDecodersAdded() {
        // Act - parse without adding any contract ABI
        AbiCustomerError result = parser.parse("0x12345678");
        
        // Assert
        Assert.assertNull("Parsing with no decoders should return null", result);
    }

    @Test
    public void testParseWithQuotedHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - input with quotes should be handled
        AbiCustomerError result = parser.parse("\"0x12345678\"");
        
        // Assert - quotes should be removed but still return null if no match
        Assert.assertNull("Parsing quoted hex string should return null when no match", result);
    }

    @Test
    public void testParseWithMultipleQuotes() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - input with multiple quotes
        AbiCustomerError result = parser.parse("\"\"0x12345678\"\"");
        
        // Assert
        Assert.assertNull("Parsing string with multiple quotes should return null when no match", result);
    }

    @Test
    public void testParseWithInvalidAbiJson() {
        // Act & Assert - adding invalid ABI JSON should throw exception
        Assert.assertThrows("Adding invalid ABI JSON should throw exception",
            RuntimeException.class,
            () -> parser.addContractAbi("InvalidContract", "invalid json"));
    }

    @Test
    public void testParseWithEmptyAbiJson() {
        // Act & Assert - adding empty ABI JSON should not throw exception
        parser.addContractAbi("EmptyContract", "[]");
        
        // Parse should return null as no errors defined
        AbiCustomerError result = parser.parse("0x12345678");
        Assert.assertNull("Parsing with empty ABI should return null", result);
    }

    @Test
    public void testParseWithMultipleDecodersNoMatch() {
        // Arrange - add multiple contract ABIs
        parser.addContractAbi("Contract1", "[{\"type\":\"error\",\"name\":\"Error1\",\"inputs\":[]}]");
        parser.addContractAbi("Contract2", "[{\"type\":\"error\",\"name\":\"Error2\",\"inputs\":[]}]");
        parser.addContractAbi("Contract3", "[{\"type\":\"error\",\"name\":\"Error3\",\"inputs\":[]}]");
        
        // Act - parse with selector that doesn't match any decoder
        AbiCustomerError result = parser.parse("0xffffffff");
        
        // Assert
        Assert.assertNull("Parsing with multiple decoders but no match should return null", result);
    }

    @Test
    public void testParseWithVeryLongHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - very long hex string
        StringBuilder longHex = new StringBuilder("0x");
        for (int i = 0; i < 1000; i++) {
            longHex.append("00");
        }
        AbiCustomerError result = parser.parse(longHex.toString());
        
        // Assert
        Assert.assertNull("Parsing very long hex string should return null when no match", result);
    }

    @Test
    public void testParseWithHexStringMissingPrefix() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - hex string without 0x prefix
        AbiCustomerError result = parser.parse("12345678");
        
        // Assert
        Assert.assertNull("Parsing hex string without prefix should return null when no match", result);
    }

    @Test
    public void testParseWithWhitespaceInHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - hex string with whitespace
        AbiCustomerError result = parser.parse("0x 12 34 56 78");
        
        // Assert
        Assert.assertNull("Parsing hex string with whitespace should return null", result);
    }

    @Test
    public void testAddContractAbiWithNullContractName() {
        // Act & Assert - adding contract with null name should not throw exception
        parser.addContractAbi(null, "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Parser should still work
        AbiCustomerError result = parser.parse("0x12345678");
        Assert.assertNull("Parser should work even with null contract name", result);
    }

    @Test
    public void testAddContractAbiWithEmptyContractName() {
        // Act & Assert - adding contract with empty name should not throw exception
        parser.addContractAbi("", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Parser should still work
        AbiCustomerError result = parser.parse("0x12345678");
        Assert.assertNull("Parser should work even with empty contract name", result);
    }

    @Test
    public void testDecoderListGrowth() throws Exception {
        // Arrange - add many contracts to test list growth
        for (int i = 0; i < 100; i++) {
            parser.addContractAbi("Contract" + i, 
                "[{\"type\":\"error\",\"name\":\"Error" + i + "\",\"inputs\":[]}]");
        }
        
        // Act - use reflection to verify decoder list size
        Field decodersField = ContractErrorParserImpl.class.getDeclaredField("decoders");
        decodersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<AbiDecoder> decoders = (List<AbiDecoder>) decodersField.get(parser);
        
        // Assert
        Assert.assertEquals("Should have 100 decoders", 100, decoders.size());
        
        // Parse should still work
        AbiCustomerError result = parser.parse("0xffffffff");
        Assert.assertNull("Parsing with many decoders should return null when no match", result);
    }

    @Test
    public void testParseWithSpecialCharactersInHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - hex string with special characters
        AbiCustomerError result = parser.parse("0x!@#$%^&*()");
        
        // Assert
        Assert.assertNull("Parsing hex string with special characters should return null", result);
    }

    @Test
    public void testParseWithMixedCaseHexString() {
        // Arrange
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Act - mixed case hex string
        AbiCustomerError result = parser.parse("0xAbCdEf12");
        
        // Assert
        Assert.assertNull("Parsing mixed case hex string should return null when no match", result);
    }

    @Test
    public void testParseAfterClearingDecoders() throws Exception {
        // Arrange - add a decoder
        parser.addContractAbi("TestContract", "[{\"type\":\"error\",\"name\":\"TestError\",\"inputs\":[]}]");
        
        // Clear decoders using reflection
        Field decodersField = ContractErrorParserImpl.class.getDeclaredField("decoders");
        decodersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<AbiDecoder> decoders = (List<AbiDecoder>) decodersField.get(parser);
        decoders.clear();
        
        // Act
        AbiCustomerError result = parser.parse("0x12345678");
        
        // Assert
        Assert.assertNull("Parsing after clearing decoders should return null", result);
    }
}
