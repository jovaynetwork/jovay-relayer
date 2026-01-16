package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;

import static org.junit.Assert.*;

/**
 * Unit tests for L1MsgRawTransactionWrapper class
 * Tests L1 message raw transaction wrapper functionality
 */
public class L1MsgRawTransactionWrapperTest {

    private L1MsgTransaction testTransaction;
    private L1MsgRawTransactionWrapper wrapper;

    @Before
    public void setUp() {
        testTransaction = new L1MsgTransaction(
                BigInteger.valueOf(1),
                BigInteger.valueOf(21000),
                "0x1234567890abcdef"
        );
        wrapper = new L1MsgRawTransactionWrapper(testTransaction);
    }

    // ==================== Constant Tests ====================

    /**
     * Test MAGIC_NUM constant
     * Should be 0x7f
     */
    @Test
    public void testMagicNumConstant() {
        assertEquals((byte) 0x7f, L1MsgRawTransactionWrapper.MAGIC_NUM);
    }

    // ==================== Constructor Tests ====================

    /**
     * Test constructor with valid transaction
     * Should create wrapper successfully
     */
    @Test
    public void testConstructor() {
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(testTransaction);
        assertNotNull(wrapper);
        assertNotNull(wrapper.getTransaction());
        assertEquals(testTransaction, wrapper.getTransaction());
    }

    /**
     * Test constructor with different transaction types
     * Should handle various ITransaction implementations
     */
    @Test
    public void testConstructorWithDifferentTransactions() {
        L1MsgTransaction tx1 = new L1MsgTransaction(
                BigInteger.ZERO,
                BigInteger.valueOf(21000),
                "0x"
        );
        L1MsgRawTransactionWrapper wrapper1 = new L1MsgRawTransactionWrapper(tx1);
        assertNotNull(wrapper1);

        L1MsgTransaction tx2 = new L1MsgTransaction(
                BigInteger.valueOf(999),
                BigInteger.valueOf(100000),
                "0xabcdef"
        );
        L1MsgRawTransactionWrapper wrapper2 = new L1MsgRawTransactionWrapper(tx2);
        assertNotNull(wrapper2);
    }

    // ==================== encodeWithoutSig Tests ====================

    /**
     * Test encodeWithoutSig
     * Should encode transaction without signature
     */
    @Test
    public void testEncodeWithoutSig() {
        byte[] encoded = wrapper.encodeWithoutSig();

        assertNotNull(encoded);
        assertTrue(encoded.length > 1);
        // First byte should be magic number
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encoded[0]);
    }

    /**
     * Test encodeWithoutSig produces valid RLP
     * Should be decodable
     */
    @Test
    public void testEncodeWithoutSigProducesValidRlp() {
        byte[] encoded = wrapper.encodeWithoutSig();

        // Skip magic number and decode RLP
        byte[] rlpData = new byte[encoded.length - 1];
        System.arraycopy(encoded, 1, rlpData, 0, rlpData.length);

        RlpList rlpList = RlpDecoder.decode(rlpData);
        assertNotNull(rlpList);
        assertFalse(rlpList.getValues().isEmpty());
    }

    /**
     * Test encodeWithoutSig consistency
     * Multiple calls should return same result
     */
    @Test
    public void testEncodeWithoutSigConsistency() {
        byte[] encoded1 = wrapper.encodeWithoutSig();
        byte[] encoded2 = wrapper.encodeWithoutSig();

        assertArrayEquals(encoded1, encoded2);
    }

    /**
     * Test encodeWithoutSig with different transactions
     * Different transactions should produce different encodings
     */
    @Test
    public void testEncodeWithoutSigWithDifferentTransactions() {
        L1MsgTransaction tx1 = new L1MsgTransaction(
                BigInteger.valueOf(1),
                BigInteger.valueOf(21000),
                "0x1234"
        );
        L1MsgRawTransactionWrapper wrapper1 = new L1MsgRawTransactionWrapper(tx1);

        L1MsgTransaction tx2 = new L1MsgTransaction(
                BigInteger.valueOf(2),
                BigInteger.valueOf(21000),
                "0x1234"
        );
        L1MsgRawTransactionWrapper wrapper2 = new L1MsgRawTransactionWrapper(tx2);

        byte[] encoded1 = wrapper1.encodeWithoutSig();
        byte[] encoded2 = wrapper2.encodeWithoutSig();

        assertFalse(java.util.Arrays.equals(encoded1, encoded2));
    }

    // ==================== encodeWithSig Tests ====================

    /**
     * Test encodeWithSig with null signature
     * Should behave same as encodeWithoutSig
     */
    @Test
    public void testEncodeWithSigNullSignature() {
        byte[] encodedWithNull = wrapper.encodeWithSig(null);
        byte[] encodedWithoutSig = wrapper.encodeWithoutSig();

        assertArrayEquals(encodedWithoutSig, encodedWithNull);
    }

    /**
     * Test encodeWithSig with valid signature
     * Should include signature in encoding
     */
    @Test
    public void testEncodeWithSigValidSignature() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] encoded = wrapper.encodeWithSig(signature);

        assertNotNull(encoded);
        assertTrue(encoded.length > 1);
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encoded[0]);
    }

    /**
     * Test encodeWithSig with signature produces longer encoding
     * Encoding with signature should be longer than without
     */
    @Test
    public void testEncodeWithSigProducesLongerEncoding() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] encodedWithSig = wrapper.encodeWithSig(signature);
        byte[] encodedWithoutSig = wrapper.encodeWithoutSig();

        assertTrue(encodedWithSig.length > encodedWithoutSig.length);
    }

    /**
     * Test encodeWithSig consistency
     * Multiple calls with same signature should return same result
     */
    @Test
    public void testEncodeWithSigConsistency() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] encoded1 = wrapper.encodeWithSig(signature);
        byte[] encoded2 = wrapper.encodeWithSig(signature);

        assertArrayEquals(encoded1, encoded2);
    }

    /**
     * Test encodeWithSig with different signatures
     * Different signatures should produce different encodings
     */
    @Test
    public void testEncodeWithSigWithDifferentSignatures() {
        Sign.SignatureData signature1 = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] rBytes = new byte[32];
        rBytes[0] = 0x01;
        Sign.SignatureData signature2 = new Sign.SignatureData(
                new byte[]{0x1c},
                rBytes,
                new byte[32]
        );

        byte[] encoded1 = wrapper.encodeWithSig(signature1);
        byte[] encoded2 = wrapper.encodeWithSig(signature2);

        assertFalse(java.util.Arrays.equals(encoded1, encoded2));
    }

    /**
     * Test encodeWithSig produces valid RLP
     * Should be decodable
     */
    @Test
    public void testEncodeWithSigProducesValidRlp() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] encoded = wrapper.encodeWithSig(signature);

        // Skip magic number and decode RLP
        byte[] rlpData = new byte[encoded.length - 1];
        System.arraycopy(encoded, 1, rlpData, 0, rlpData.length);

        RlpList rlpList = RlpDecoder.decode(rlpData);
        assertNotNull(rlpList);
        assertFalse(rlpList.getValues().isEmpty());
    }

    // ==================== calcHash Tests ====================

    /**
     * Test calcHash
     * Should return non-null hash
     */
    @Test
    public void testCalcHash() {
        byte[] hash = wrapper.calcHash();

        assertNotNull(hash);
        assertEquals(32, hash.length); // SHA3-256 produces 32 bytes
    }

    /**
     * Test calcHash consistency
     * Multiple calls should return same hash
     */
    @Test
    public void testCalcHashConsistency() {
        byte[] hash1 = wrapper.calcHash();
        byte[] hash2 = wrapper.calcHash();

        assertArrayEquals(hash1, hash2);
    }

    /**
     * Test calcHash with different transactions
     * Different transactions should produce different hashes
     */
    @Test
    public void testCalcHashWithDifferentTransactions() {
        L1MsgTransaction tx1 = new L1MsgTransaction(
                BigInteger.valueOf(1),
                BigInteger.valueOf(21000),
                "0x1234"
        );
        L1MsgRawTransactionWrapper wrapper1 = new L1MsgRawTransactionWrapper(tx1);

        L1MsgTransaction tx2 = new L1MsgTransaction(
                BigInteger.valueOf(2),
                BigInteger.valueOf(21000),
                "0x1234"
        );
        L1MsgRawTransactionWrapper wrapper2 = new L1MsgRawTransactionWrapper(tx2);

        byte[] hash1 = wrapper1.calcHash();
        byte[] hash2 = wrapper2.calcHash();

        assertFalse(java.util.Arrays.equals(hash1, hash2));
    }

    /**
     * Test calcHash matches manual calculation
     * Should match Hash.sha3 of encodeWithoutSig
     */
    @Test
    public void testCalcHashMatchesManualCalculation() {
        byte[] encoded = wrapper.encodeWithoutSig();
        byte[] expectedHash = Hash.sha3(encoded);
        byte[] actualHash = wrapper.calcHash();

        assertArrayEquals(expectedHash, actualHash);
    }

    /**
     * Test calcHash is deterministic
     * Same transaction should always produce same hash
     */
    @Test
    public void testCalcHashIsDeterministic() {
        L1MsgTransaction tx = new L1MsgTransaction(
                BigInteger.valueOf(123),
                BigInteger.valueOf(50000),
                "0xdeadbeef"
        );

        L1MsgRawTransactionWrapper wrapper1 = new L1MsgRawTransactionWrapper(tx);
        L1MsgRawTransactionWrapper wrapper2 = new L1MsgRawTransactionWrapper(tx);

        byte[] hash1 = wrapper1.calcHash();
        byte[] hash2 = wrapper2.calcHash();

        assertArrayEquals(hash1, hash2);
    }

    // ==================== Integration Tests ====================

    /**
     * Test encode-decode round trip
     * Should be able to decode encoded transaction
     */
    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] encoded = wrapper.encodeWithoutSig();

        // Decode using L1MsgTransaction.decode
        L1MsgTransaction decoded = L1MsgTransaction.decode(encoded);

        assertNotNull(decoded);
        assertEquals(testTransaction.getNonce(), decoded.getNonce());
        assertEquals(testTransaction.getGasLimit(), decoded.getGasLimit());
        assertEquals(testTransaction.getData(), decoded.getData());
    }

    /**
     * Test encode-decode round trip with signature
     * Should be able to decode encoded transaction with signature
     */
    @Test
    public void testEncodeDecodeRoundTripWithSignature() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        byte[] encoded = wrapper.encodeWithSig(signature);

        // Decode using L1MsgTransaction.decode
        L1MsgTransaction decoded = L1MsgTransaction.decode(encoded);

        assertNotNull(decoded);
        assertEquals(testTransaction.getNonce(), decoded.getNonce());
        assertEquals(testTransaction.getGasLimit(), decoded.getGasLimit());
        assertEquals(testTransaction.getData(), decoded.getData());
    }

    /**
     * Test magic number is always first byte
     * All encoding methods should start with magic number
     */
    @Test
    public void testMagicNumberIsAlwaysFirstByte() {
        byte[] encodedWithoutSig = wrapper.encodeWithoutSig();
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encodedWithoutSig[0]);

        byte[] encodedWithNullSig = wrapper.encodeWithSig(null);
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encodedWithNullSig[0]);

        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );
        byte[] encodedWithSig = wrapper.encodeWithSig(signature);
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encodedWithSig[0]);
    }

    // ==================== Negative Tests ====================

    /**
     * Test constructor with null transaction
     * Should create wrapper (parent class accepts null)
     */
    @Test
    public void testConstructorWithNullTransaction() {
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(null);
        assertNotNull(wrapper);
        assertNull(wrapper.getTransaction());
    }

    /**
     * Test encodeWithSig with empty signature R value
     * Should behave like no signature
     */
    @Test
    public void testEncodeWithSigWithEmptySignatureR() {
        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[0],  // Empty R
                new byte[32]
        );

        byte[] encodedWithEmptySig = wrapper.encodeWithSig(signature);
        byte[] encodedWithoutSig = wrapper.encodeWithoutSig();

        // Should be same as without signature
        assertArrayEquals(encodedWithoutSig, encodedWithEmptySig);
    }

    /**
     * Test hash length is always 32 bytes
     * SHA3-256 always produces 32 bytes
     */
    @Test
    public void testHashLengthIsAlways32Bytes() {
        L1MsgTransaction[] transactions = {
                new L1MsgTransaction(BigInteger.ZERO, BigInteger.valueOf(21000), "0x"),
                new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(50000), "0x1234"),
                new L1MsgTransaction(BigInteger.TEN, BigInteger.valueOf(100000), "0xabcdef0123456789")
        };

        for (L1MsgTransaction tx : transactions) {
            L1MsgRawTransactionWrapper w = new L1MsgRawTransactionWrapper(tx);
            byte[] hash = w.calcHash();
            assertEquals(32, hash.length);
        }
    }

    /**
     * Test encoding with zero nonce transaction
     * Should handle edge case
     */
    @Test
    public void testEncodingWithZeroNonceTransaction() {
        L1MsgTransaction zeroNonceTx = new L1MsgTransaction(
                BigInteger.ZERO,
                BigInteger.valueOf(21000),
                "0x"
        );
        L1MsgRawTransactionWrapper zeroNonceWrapper = new L1MsgRawTransactionWrapper(zeroNonceTx);

        byte[] encoded = zeroNonceWrapper.encodeWithoutSig();
        assertNotNull(encoded);
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encoded[0]);

        byte[] hash = zeroNonceWrapper.calcHash();
        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    /**
     * Test encoding with large nonce transaction
     * Should handle large numbers
     */
    @Test
    public void testEncodingWithLargeNonceTransaction() {
        L1MsgTransaction largeNonceTx = new L1MsgTransaction(
                new BigInteger("999999999999999999"),
                BigInteger.valueOf(21000),
                "0x"
        );
        L1MsgRawTransactionWrapper largeNonceWrapper = new L1MsgRawTransactionWrapper(largeNonceTx);

        byte[] encoded = largeNonceWrapper.encodeWithoutSig();
        assertNotNull(encoded);
        assertEquals(L1MsgRawTransactionWrapper.MAGIC_NUM, encoded[0]);

        byte[] hash = largeNonceWrapper.calcHash();
        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    /**
     * Test getTransaction returns correct transaction
     * Should return the transaction passed to constructor
     */
    @Test
    public void testGetTransactionReturnsCorrectTransaction() {
        ITransaction retrievedTx = wrapper.getTransaction();
        assertNotNull(retrievedTx);
        assertEquals(testTransaction, retrievedTx);
        assertEquals(testTransaction.getNonce(), retrievedTx.getNonce());
        assertEquals(testTransaction.getGasLimit(), retrievedTx.getGasLimit());
        assertEquals(testTransaction.getData(), retrievedTx.getData());
    }
}
