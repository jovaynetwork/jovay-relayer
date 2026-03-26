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

import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Sign;
import org.web3j.crypto.transaction.type.TransactionType;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import static org.junit.Assert.*;

/**
 * Unit tests for L1MsgTransaction class
 * Tests L1 message transaction functionality
 */
public class L1MsgTransactionTest {

    private BigInteger testNonce;
    private BigInteger testGasLimit;
    private String testData;

    @Before
    public void setUp() {
        testNonce = BigInteger.valueOf(1);
        testGasLimit = BigInteger.valueOf(21000);
        testData = "0x1234567890abcdef";
    }

    // ==================== Constant Tests ====================

    /**
     * Test L1_MAILBOX_AS_SENDER constant
     * Should be 0x5100000000000000000000000000000000000000
     */
    @Test
    public void testL1MailboxAsSenderConstant() {
        Address expected = new Address("0x5100000000000000000000000000000000000000");
        assertEquals(expected.toString(), L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString());
    }

    /**
     * Test L2_MAILBOX_AS_RECEIVER constant
     * Should be 0x6100000000000000000000000000000000000000
     */
    @Test
    public void testL2MailboxAsReceiverConstant() {
        Address expected = new Address("0x6100000000000000000000000000000000000000");
        assertEquals(expected.toString(), L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString());
    }

    // ==================== Constructor Tests ====================

    /**
     * Test constructor with valid parameters
     * Should create transaction successfully
     */
    @Test
    public void testConstructor() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);

        assertNotNull(tx);
        assertEquals(testNonce, tx.getNonce());
        assertEquals(testGasLimit, tx.getGasLimit());
        assertEquals(testData, tx.getData());
    }

    /**
     * Test constructor with zero nonce
     * Should create transaction successfully
     */
    @Test
    public void testConstructorWithZeroNonce() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ZERO, testGasLimit, testData);

        assertNotNull(tx);
        assertEquals(BigInteger.ZERO, tx.getNonce());
    }

    /**
     * Test constructor with large nonce
     * Should create transaction successfully
     */
    @Test
    public void testConstructorWithLargeNonce() {
        BigInteger largeNonce = new BigInteger("999999999999999999");
        L1MsgTransaction tx = new L1MsgTransaction(largeNonce, testGasLimit, testData);

        assertNotNull(tx);
        assertEquals(largeNonce, tx.getNonce());
    }

    /**
     * Test constructor with empty data
     * Should create transaction successfully
     */
    @Test
    public void testConstructorWithEmptyData() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, "0x");

        assertNotNull(tx);
        assertEquals("0x", tx.getData());
    }

    // ==================== Getter Method Tests ====================

    /**
     * Test getNonce
     * Should return the nonce value
     */
    @Test
    public void testGetNonce() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(testNonce, tx.getNonce());
    }

    /**
     * Test getGasPrice
     * Should always return ZERO for L1 message transactions
     */
    @Test
    public void testGetGasPrice() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(BigInteger.ZERO, tx.getGasPrice());
    }

    /**
     * Test getGasLimit
     * Should return the gas limit value
     */
    @Test
    public void testGetGasLimit() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(testGasLimit, tx.getGasLimit());
    }

    /**
     * Test getTo
     * Should always return L2_MAILBOX_AS_RECEIVER address
     */
    @Test
    public void testGetTo() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(), tx.getTo());
    }

    /**
     * Test getValue
     * Should always return ZERO for L1 message transactions
     */
    @Test
    public void testGetValue() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(BigInteger.ZERO, tx.getValue());
    }

    /**
     * Test getData
     * Should return the data value
     */
    @Test
    public void testGetData() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(testData, tx.getData());
    }

    /**
     * Test getType
     * Should return LEGACY transaction type
     */
    @Test
    public void testGetType() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        assertEquals(TransactionType.LEGACY, tx.getType());
    }

    // ==================== asRlpValues Tests ====================

    /**
     * Test asRlpValues without signature
     * Should return RLP values list with 3 elements
     */
    @Test
    public void testAsRlpValuesWithoutSignature() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        List<RlpType> rlpValues = tx.asRlpValues(null);

        assertNotNull(rlpValues);
        assertEquals(3, rlpValues.size());
        
        // Verify nonce
        assertEquals(testNonce, ((RlpString) rlpValues.get(0)).asPositiveBigInteger());
        
        // Verify gas limit
        assertEquals(testGasLimit, ((RlpString) rlpValues.get(1)).asPositiveBigInteger());
        
        // Verify data
        byte[] expectedData = Numeric.hexStringToByteArray(testData);
        assertArrayEquals(expectedData, ((RlpString) rlpValues.get(2)).getBytes());
    }

    /**
     * Test asRlpValues with signature
     * Should return RLP values list with 6 elements
     */
    @Test
    public void testAsRlpValuesWithSignature() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        List<RlpType> rlpValues = tx.asRlpValues(signature);

        assertNotNull(rlpValues);
        assertEquals(6, rlpValues.size());
    }

    /**
     * Test asRlpValues with empty signature R value
     * Should return RLP values list with 3 elements (signature ignored)
     */
    @Test
    public void testAsRlpValuesWithEmptySignatureR() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[0],  // Empty R
                new byte[32]
        );

        List<RlpType> rlpValues = tx.asRlpValues(signature);

        assertNotNull(rlpValues);
        // Should only have 3 elements because R is empty
        assertEquals(3, rlpValues.size());
    }

    /**
     * Test asRlpValues with different data values
     * Should handle various data formats
     */
    @Test
    public void testAsRlpValuesWithDifferentData() {
        String[] testDataValues = {
                "0x",
                "0x00",
                "0x1234",
                "0xabcdef0123456789"
        };

        for (String data : testDataValues) {
            L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, data);
            List<RlpType> rlpValues = tx.asRlpValues(null);

            assertNotNull(rlpValues);
            assertEquals(3, rlpValues.size());
            
            byte[] expectedData = Numeric.hexStringToByteArray(data);
            assertArrayEquals(expectedData, ((RlpString) rlpValues.get(2)).getBytes());
        }
    }

    // ==================== decode Tests ====================

    /**
     * Test decode with valid raw transaction
     * Should decode successfully
     */
    @Test
    public void testDecodeValidTransaction() {
        // Create a transaction
        L1MsgTransaction originalTx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        
        // Encode it
        List<RlpType> rlpValues = originalTx.asRlpValues(null);
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpValues));
        
        // Add magic number prefix
        byte[] raw = new byte[encoded.length + 1];
        raw[0] = L1MsgRawTransactionWrapper.MAGIC_NUM;
        System.arraycopy(encoded, 0, raw, 1, encoded.length);
        
        // Decode
        L1MsgTransaction decodedTx = L1MsgTransaction.decode(raw);
        
        // Verify
        assertNotNull(decodedTx);
        assertEquals(originalTx.getNonce(), decodedTx.getNonce());
        assertEquals(originalTx.getGasLimit(), decodedTx.getGasLimit());
        assertEquals(originalTx.getData(), decodedTx.getData());
    }

    /**
     * Test decode with zero nonce
     * Should decode successfully
     */
    @Test
    public void testDecodeWithZeroNonce() {
        L1MsgTransaction originalTx = new L1MsgTransaction(BigInteger.ZERO, testGasLimit, testData);
        
        List<RlpType> rlpValues = originalTx.asRlpValues(null);
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpValues));
        
        byte[] raw = new byte[encoded.length + 1];
        raw[0] = L1MsgRawTransactionWrapper.MAGIC_NUM;
        System.arraycopy(encoded, 0, raw, 1, encoded.length);
        
        L1MsgTransaction decodedTx = L1MsgTransaction.decode(raw);
        
        assertEquals(BigInteger.ZERO, decodedTx.getNonce());
    }

    /**
     * Test decode with empty data
     * Should decode successfully
     */
    @Test
    public void testDecodeWithEmptyData() {
        L1MsgTransaction originalTx = new L1MsgTransaction(testNonce, testGasLimit, "0x");
        
        List<RlpType> rlpValues = originalTx.asRlpValues(null);
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpValues));
        
        byte[] raw = new byte[encoded.length + 1];
        raw[0] = L1MsgRawTransactionWrapper.MAGIC_NUM;
        System.arraycopy(encoded, 0, raw, 1, encoded.length);
        
        L1MsgTransaction decodedTx = L1MsgTransaction.decode(raw);
        
        assertEquals("0x", decodedTx.getData());
    }

    /**
     * Test encode-decode round trip
     * Should maintain data integrity
     */
    @Test
    public void testEncodeDecodeRoundTrip() {
        L1MsgTransaction originalTx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        
        // Encode
        List<RlpType> rlpValues = originalTx.asRlpValues(null);
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpValues));
        
        byte[] raw = new byte[encoded.length + 1];
        raw[0] = L1MsgRawTransactionWrapper.MAGIC_NUM;
        System.arraycopy(encoded, 0, raw, 1, encoded.length);
        
        // Decode
        L1MsgTransaction decodedTx = L1MsgTransaction.decode(raw);
        
        // Verify all fields match
        assertEquals(originalTx.getNonce(), decodedTx.getNonce());
        assertEquals(originalTx.getGasLimit(), decodedTx.getGasLimit());
        assertEquals(originalTx.getData(), decodedTx.getData());
        assertEquals(originalTx.getGasPrice(), decodedTx.getGasPrice());
        assertEquals(originalTx.getValue(), decodedTx.getValue());
        assertEquals(originalTx.getTo(), decodedTx.getTo());
        assertEquals(originalTx.getType(), decodedTx.getType());
    }

    // ==================== Negative Tests ====================

    /**
     * Test decode with invalid magic number
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWithInvalidMagicNumber() {
        L1MsgTransaction originalTx = new L1MsgTransaction(testNonce, testGasLimit, testData);
        
        List<RlpType> rlpValues = originalTx.asRlpValues(null);
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpValues));
        
        byte[] raw = new byte[encoded.length + 1];
        raw[0] = (byte) 0xFF;  // Invalid magic number
        System.arraycopy(encoded, 0, raw, 1, encoded.length);
        
        L1MsgTransaction.decode(raw);
    }

    /**
     * Test decode with empty byte array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeWithEmptyArray() {
        L1MsgTransaction.decode(new byte[0]);
    }

    /**
     * Test decode with only magic number
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeWithOnlyMagicNumber() {
        L1MsgTransaction.decode(new byte[]{L1MsgRawTransactionWrapper.MAGIC_NUM});
    }

    /**
     * Test decode with corrupted RLP data
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeWithCorruptedRlpData() {
        byte[] raw = new byte[10];
        raw[0] = L1MsgRawTransactionWrapper.MAGIC_NUM;
        // Rest is garbage data
        for (int i = 1; i < raw.length; i++) {
            raw[i] = (byte) 0xFF;
        }
        
        L1MsgTransaction.decode(raw);
    }

    /**
     * Test constructor with null nonce
     * Should create transaction (validation is caller's responsibility)
     */
    @Test
    public void testConstructorWithNullNonce() {
        L1MsgTransaction tx = new L1MsgTransaction(null, testGasLimit, testData);
        assertNotNull(tx);
        assertNull(tx.getNonce());
    }

    /**
     * Test constructor with null gas limit
     * Should create transaction (validation is caller's responsibility)
     */
    @Test
    public void testConstructorWithNullGasLimit() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, null, testData);
        assertNotNull(tx);
        assertNull(tx.getGasLimit());
    }

    /**
     * Test constructor with null data
     * Should create transaction (data can be null)
     */
    @Test
    public void testConstructorWithNullData() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, null);
        assertNotNull(tx);
        assertNull(tx.getData());
    }

    /**
     * Test asRlpValues with null data
     * Should handle gracefully
     */
    @Test(expected = Exception.class)
    public void testAsRlpValuesWithNullData() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, testGasLimit, null);
        tx.asRlpValues(null);
    }

    /**
     * Test with negative nonce
     * Should create transaction (validation is caller's responsibility)
     */
    @Test
    public void testWithNegativeNonce() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.valueOf(-1), testGasLimit, testData);
        assertNotNull(tx);
        assertEquals(BigInteger.valueOf(-1), tx.getNonce());
    }

    /**
     * Test with zero gas limit
     * Should create transaction
     */
    @Test
    public void testWithZeroGasLimit() {
        L1MsgTransaction tx = new L1MsgTransaction(testNonce, BigInteger.ZERO, testData);
        assertNotNull(tx);
        assertEquals(BigInteger.ZERO, tx.getGasLimit());
    }

    /**
     * Test constant immutability
     * Constants should not be modifiable
     */
    @Test
    public void testConstantImmutability() {
        Address l1Mailbox = L1MsgTransaction.L1_MAILBOX_AS_SENDER;
        Address l2Mailbox = L1MsgTransaction.L2_MAILBOX_AS_RECEIVER;
        
        // Verify constants are the same instance
        assertSame(l1Mailbox, L1MsgTransaction.L1_MAILBOX_AS_SENDER);
        assertSame(l2Mailbox, L1MsgTransaction.L2_MAILBOX_AS_RECEIVER);
    }

    /**
     * Test getTo always returns L2 mailbox
     * Should be consistent regardless of transaction parameters
     */
    @Test
    public void testGetToConsistency() {
        L1MsgTransaction tx1 = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(21000), "0x");
        L1MsgTransaction tx2 = new L1MsgTransaction(BigInteger.TEN, BigInteger.valueOf(50000), "0x1234");
        
        assertEquals(tx1.getTo(), tx2.getTo());
        assertEquals(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString(), tx1.getTo());
    }

    /**
     * Test getGasPrice always returns zero
     * Should be consistent regardless of transaction parameters
     */
    @Test
    public void testGetGasPriceConsistency() {
        L1MsgTransaction tx1 = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(21000), "0x");
        L1MsgTransaction tx2 = new L1MsgTransaction(BigInteger.TEN, BigInteger.valueOf(50000), "0x1234");
        
        assertEquals(BigInteger.ZERO, tx1.getGasPrice());
        assertEquals(BigInteger.ZERO, tx2.getGasPrice());
    }

    /**
     * Test getValue always returns zero
     * Should be consistent regardless of transaction parameters
     */
    @Test
    public void testGetValueConsistency() {
        L1MsgTransaction tx1 = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(21000), "0x");
        L1MsgTransaction tx2 = new L1MsgTransaction(BigInteger.TEN, BigInteger.valueOf(50000), "0x1234");
        
        assertEquals(BigInteger.ZERO, tx1.getValue());
        assertEquals(BigInteger.ZERO, tx2.getValue());
    }
}
