package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Blob;
import org.web3j.crypto.BlobUtils;
import org.web3j.crypto.Sign;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

/**
 * Unit tests for FusakaTransaction4844 class
 * Tests EIP-4844 blob transaction functionality
 */
public class FusakaTransaction4844Test {

    private static final String HEX_RAW_TX = FileUtil.readString("raw_eip4844_fusaka_tx", Charset.defaultCharset());
    private static final SecureRandom RANDOM = new SecureRandom();

    private List<Blob> testBlobs;
    private long testChainId;
    private BigInteger testNonce;
    private BigInteger testMaxPriorityFeePerGas;
    private BigInteger testMaxFeePerGas;
    private BigInteger testGasLimit;
    private String testTo;
    private BigInteger testValue;
    private String testData;
    private BigInteger testMaxFeePerBlobGas;

    @Before
    public void setUp() {
        testBlobs = createTestBlobs(1);
        testChainId = 1L;
        testNonce = BigInteger.valueOf(1);
        testMaxPriorityFeePerGas = BigInteger.valueOf(1000000000L);
        testMaxFeePerGas = BigInteger.valueOf(2000000000L);
        testGasLimit = BigInteger.valueOf(21000L);
        testTo = "0x1234567890123456789012345678901234567890";
        testValue = BigInteger.ZERO;
        testData = "0x";
        testMaxFeePerBlobGas = BigInteger.valueOf(1000000L);
    }

    // ==================== Helper Methods ====================

    /**
     * Create random blob data for testing
     */
    private static byte[] createRandomBlob() {
        final byte[][] blob =
                range(0, CKZG4844JNI.FIELD_ELEMENTS_PER_BLOB)
                        .mapToObj(__ -> randomBLSFieldElement())
                        .map(fieldElement -> fieldElement.toArray(ByteOrder.BIG_ENDIAN))
                        .toArray(byte[][]::new);
        return flatten(blob);
    }

    private static UInt256 randomBLSFieldElement() {
        final BigInteger attempt = new BigInteger(CKZG4844JNI.BLS_MODULUS.bitLength(), RANDOM);
        if (attempt.compareTo(CKZG4844JNI.BLS_MODULUS) < 0) {
            return UInt256.valueOf(attempt);
        }
        return randomBLSFieldElement();
    }

    private static byte[] flatten(final byte[]... bytes) {
        final int capacity = Arrays.stream(bytes).mapToInt(b -> b.length).sum();
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);
        Arrays.stream(bytes).forEach(buffer::put);
        return buffer.array();
    }

    private List<Blob> createTestBlobs(int count) {
        List<Blob> blobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            blobs.add(new Blob(createRandomBlob()));
        }
        return blobs;
    }

    // ==================== Existing Test ====================

    /**
     * Test encode-decode round trip with real transaction data
     * This is the original test
     */
    @Test
    public void testEncodeDecodeRoundTrip() {
        var rawTx = (SignedRawTransaction) EthTxDecoder.decode(HEX_RAW_TX);
        Assert.assertArrayEquals(
                Numeric.hexStringToByteArray(HEX_RAW_TX),
                TransactionEncoder.encode(rawTx, rawTx.getSignatureData())
        );
    }

    // ==================== Positive Tests ====================

    /**
     * Test simplified constructor (auto-generates commitments and proofs)
     * Should create transaction with all required fields
     */
    @Test
    public void testSimplifiedConstructor() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        assertEquals(1, tx.getVersion());
        assertTrue(tx.getBlobs().isPresent());
        assertEquals(testBlobs.size(), tx.getBlobs().get().size());
        assertTrue(tx.getKzgCommitments().isPresent());
        assertTrue(tx.getKzgProofs().isPresent());
        assertNotNull(tx.getVersionedHashes());
        assertEquals(testBlobs.size(), tx.getVersionedHashes().size());
    }

    /**
     * Test full constructor (provides all parameters)
     * Should create transaction with provided values
     */
    @Test
    public void testFullConstructor() {
        List<Bytes> kzgCommitments = testBlobs.stream()
                .map(BlobUtils::getCommitment)
                .toList();
        List<Bytes> versionedHashes = kzgCommitments.stream()
                .map(BlobUtils::kzgToVersionedHash)
                .toList();
        List<Bytes> kzgProofs = Collections.emptyList(); // Simplified for test

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                kzgCommitments,
                kzgProofs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                versionedHashes
        );

        assertNotNull(tx);
        assertEquals(1, tx.getVersion());
        assertTrue(tx.getBlobs().isPresent());
        assertTrue(tx.getKzgCommitments().isPresent());
        assertTrue(tx.getKzgProofs().isPresent());
        assertEquals(versionedHashes, tx.getVersionedHashes());
    }

    /**
     * Test getVersion
     * Should always return 1
     */
    @Test
    public void testGetVersion() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertEquals(1, tx.getVersion());
    }

    /**
     * Test getBlobs
     * Should return Optional with blobs
     */
    @Test
    public void testGetBlobs() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertTrue(tx.getBlobs().isPresent());
        assertEquals(testBlobs, tx.getBlobs().get());
    }

    /**
     * Test getKzgCommitments
     * Should return Optional with commitments
     */
    @Test
    public void testGetKzgCommitments() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertTrue(tx.getKzgCommitments().isPresent());
        assertEquals(testBlobs.size(), tx.getKzgCommitments().get().size());
    }

    /**
     * Test getKzgProofs
     * Should return Optional with proofs
     */
    @Test
    public void testGetKzgProofs() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertTrue(tx.getKzgProofs().isPresent());
        // Each blob generates 128 proofs
        assertEquals(testBlobs.size() * 128, tx.getKzgProofs().get().size());
    }

    /**
     * Test getVersionedHashes
     * Should return list of versioned hashes
     */
    @Test
    public void testGetVersionedHashes() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx.getVersionedHashes());
        assertEquals(testBlobs.size(), tx.getVersionedHashes().size());
    }

    /**
     * Test getRlpVersionedHashes
     * Should return RLP encoded versioned hashes
     */
    @Test
    public void testGetRlpVersionedHashes() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlpHashes = tx.getRlpVersionedHashes();

        assertNotNull(rlpHashes);
        assertEquals(testBlobs.size(), rlpHashes.size());
    }

    /**
     * Test getRlpKzgCommitments
     * Should return RLP encoded commitments
     */
    @Test
    public void testGetRlpKzgCommitments() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlpCommitments = tx.getRlpKzgCommitments();

        assertNotNull(rlpCommitments);
        assertEquals(testBlobs.size(), rlpCommitments.size());
    }

    /**
     * Test getRlpKzgProofs
     * Should return RLP encoded proofs
     */
    @Test
    public void testGetRlpKzgProofs() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlpProofs = tx.getRlpKzgProofs();

        assertNotNull(rlpProofs);
        assertEquals(testBlobs.size() * 128, rlpProofs.size());
    }

    /**
     * Test getRlpBlobs
     * Should return RLP encoded blobs
     */
    @Test
    public void testGetRlpBlobs() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlpBlobs = tx.getRlpBlobs();

        assertNotNull(rlpBlobs);
        assertEquals(testBlobs.size(), rlpBlobs.size());
    }

    /**
     * Test asRlpValues without signature
     * Should return RLP values list
     */
    @Test
    public void testAsRlpValuesWithoutSignature() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlpValues = tx.asRlpValues(null);

        assertNotNull(rlpValues);
        // Should contain: transaction list, version, blobs, commitments, proofs
        assertEquals(5, rlpValues.size());
    }

    /**
     * Test asRlpValues with signature
     * Should include signature data
     */
    @Test
    public void testAsRlpValuesWithSignature() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        Sign.SignatureData signature = new Sign.SignatureData(
                new byte[]{0x1c},
                new byte[32],
                new byte[32]
        );

        List<RlpType> rlpValues = tx.asRlpValues(signature);

        assertNotNull(rlpValues);
        assertEquals(5, rlpValues.size());
    }

    /**
     * Test with multiple blobs
     */
    @Test
    public void testWithMultipleBlobs() {
        List<Blob> multipleBlobs = createTestBlobs(3);

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                multipleBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertEquals(3, tx.getBlobs().get().size());
        assertEquals(3, tx.getVersionedHashes().size());
        assertEquals(3, tx.getKzgCommitments().get().size());
        assertEquals(3 * 128, tx.getKzgProofs().get().size());
    }

    /**
     * Test with empty 'to' address
     * Should handle contract creation
     */
    @Test
    public void testWithEmptyToAddress() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                "",
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        List<RlpType> rlpValues = tx.asRlpValues(null);
        assertNotNull(rlpValues);
    }

    /**
     * Test with non-zero value
     */
    @Test
    public void testWithNonZeroValue() {
        BigInteger value = BigInteger.valueOf(1000000000000000000L); // 1 ETH

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                value,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        assertEquals(value, tx.getValue());
    }

    /**
     * Test with non-empty data
     */
    @Test
    public void testWithNonEmptyData() {
        String data = "0x1234567890abcdef";

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                data,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        // getData() returns data without "0x" prefix
        assertEquals("1234567890abcdef", tx.getData());
    }

    // ==================== Negative Tests ====================

    /**
     * Test simplified constructor with null blobs
     * Should throw AssertionError due to assert statement
     */
    @Test(expected = AssertionError.class)
    public void testSimplifiedConstructorWithNullBlobs() {
        new FusakaTransaction4844(
                null,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );
    }

    /**
     * Test full constructor with null blobs
     * Should create transaction with empty Optional
     */
    @Test
    public void testFullConstructorWithNullBlobs() {
        List<Bytes> kzgCommitments = Collections.emptyList();
        List<Bytes> kzgProofs = Collections.emptyList();
        List<Bytes> versionedHashes = Collections.emptyList();

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                null,
                kzgCommitments,
                kzgProofs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                versionedHashes
        );

        assertNotNull(tx);
        assertFalse(tx.getBlobs().isPresent());
    }

    /**
     * Test full constructor with null commitments
     * Should create transaction with empty Optional
     */
    @Test
    public void testFullConstructorWithNullCommitments() {
        List<Bytes> versionedHashes = Collections.emptyList();

        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                null,
                null,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                versionedHashes
        );

        assertNotNull(tx);
        assertFalse(tx.getKzgCommitments().isPresent());
        assertFalse(tx.getKzgProofs().isPresent());
    }

    /**
     * Test getRlpKzgCommitments with empty Optional
     * Should return empty list
     */
    @Test
    public void testGetRlpKzgCommitmentsWithEmptyOptional() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                null,
                null,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                Collections.emptyList()
        );

        List<RlpType> rlpCommitments = tx.getRlpKzgCommitments();

        assertNotNull(rlpCommitments);
        assertTrue(rlpCommitments.isEmpty());
    }

    /**
     * Test getRlpKzgProofs with empty Optional
     * Should return empty list
     */
    @Test
    public void testGetRlpKzgProofsWithEmptyOptional() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                null,
                null,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                Collections.emptyList()
        );

        List<RlpType> rlpProofs = tx.getRlpKzgProofs();

        assertNotNull(rlpProofs);
        assertTrue(rlpProofs.isEmpty());
    }

    /**
     * Test getRlpBlobs with empty Optional
     * Should return empty list
     */
    @Test
    public void testGetRlpBlobsWithEmptyOptional() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                null,
                null,
                null,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas,
                Collections.emptyList()
        );

        List<RlpType> rlpBlobs = tx.getRlpBlobs();

        assertNotNull(rlpBlobs);
        assertTrue(rlpBlobs.isEmpty());
    }

    /**
     * Test with empty blobs list
     * Should create transaction but with empty commitments/proofs
     */
    @Test
    public void testWithEmptyBlobsList() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                Collections.emptyList(),
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        assertTrue(tx.getBlobs().isPresent());
        assertEquals(0, tx.getBlobs().get().size());
        assertEquals(0, tx.getVersionedHashes().size());
    }

    /**
     * Test with null 'to' address
     * Should handle gracefully
     */
    @Test
    public void testWithNullToAddress() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                null,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        List<RlpType> rlpValues = tx.asRlpValues(null);
        assertNotNull(rlpValues);
    }

    /**
     * Test with zero gas limit
     * Should create transaction but may be invalid
     */
    @Test
    public void testWithZeroGasLimit() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                BigInteger.ZERO,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        assertEquals(BigInteger.ZERO, tx.getGasLimit());
    }

    /**
     * Test with negative nonce
     * Should create transaction but may be invalid
     */
    @Test
    public void testWithNegativeNonce() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                BigInteger.valueOf(-1),
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertNotNull(tx);
        assertEquals(BigInteger.valueOf(-1), tx.getNonce());
    }

    /**
     * Test version consistency
     * Version should always be 1
     */
    @Test
    public void testVersionConsistency() {
        FusakaTransaction4844 tx1 = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        FusakaTransaction4844 tx2 = new FusakaTransaction4844(
                createTestBlobs(2),
                2L,
                BigInteger.TEN,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        assertEquals(tx1.getVersion(), tx2.getVersion());
        assertEquals(1, tx1.getVersion());
    }

    /**
     * Test RLP encoding consistency
     * Multiple calls should return consistent results
     */
    @Test
    public void testRlpEncodingConsistency() {
        FusakaTransaction4844 tx = new FusakaTransaction4844(
                testBlobs,
                testChainId,
                testNonce,
                testMaxPriorityFeePerGas,
                testMaxFeePerGas,
                testGasLimit,
                testTo,
                testValue,
                testData,
                testMaxFeePerBlobGas
        );

        List<RlpType> rlp1 = tx.asRlpValues(null);
        List<RlpType> rlp2 = tx.asRlpValues(null);

        assertEquals(rlp1.size(), rlp2.size());
    }
}
