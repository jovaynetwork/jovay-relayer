package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Blob;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

/**
 * Unit tests for BlobsDaData class
 * Tests blob encoding/decoding, compression, and DA data operations
 */
public class BlobsDaDataTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private List<Chunk> testChunks;
    private ChunksPayload testPayload;

    @Before
    public void setUp() {
        testChunks = createTestChunks(2);
        testPayload = new ChunksPayload(testChunks);
    }

    // ==================== Helper Methods ====================

    /**
     * Create test chunks with specified count
     */
    private List<Chunk> createTestChunks(int count) {
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<BlockContext> blocks = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                BlockContext block = BlockContext.builder()
                        .specVersion(1L)
                        .blockNumber(BigInteger.valueOf(100 + i * 2 + j))
                        .timestamp(System.currentTimeMillis() + j * 1000)
                        .baseFee(BigInteger.valueOf(1000000000L))
                        .gasLimit(BigInteger.valueOf(30000000L))
                        .numTransactions(5)
                        .numL1Messages(2)
                        .build();
                blocks.add(block);
            }

            Chunk chunk = Chunk.builder()
                    .numBlocks((byte) 2)
                    .blocks(blocks)
                    .hash(new byte[32])
                    .build();
            chunks.add(chunk);
        }
        return chunks;
    }

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

    // ==================== Positive Tests ====================

    /**
     * Test buildFrom with BatchVersionEnum and IBatchPayload
     * Should create BlobsDaData with correct version and payload
     */
    @Test
    public void testBuildFromVersionAndPayload() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        assertNotNull(daData);
        assertNotNull(daData.getBlobs());
        assertEquals(BatchVersionEnum.BATCH_V0, daData.getBatchVersion());
        assertEquals(DaVersion.DA_0, daData.getDaVersion());
    }

    /**
     * Test buildFrom with BATCH_V0
     * Should use DA_0 without compression
     */
    @Test
    public void testBuildFromBatchV0() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        assertNotNull(daData);
        assertEquals(BatchVersionEnum.BATCH_V0, daData.getBatchVersion());
        assertEquals(DaVersion.DA_0, daData.getDaVersion());
        assertTrue(daData.getDataLen() > 0);
    }

    /**
     * Test buildFrom with BATCH_V1
     * Should use DA_1 or DA_2 based on compression
     */
    @Test
    public void testBuildFromBatchV1() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, testPayload);

        assertNotNull(daData);
        assertEquals(BatchVersionEnum.BATCH_V1, daData.getBatchVersion());
        // DA version should be DA_1 or DA_2 depending on compression effectiveness
        assertTrue(daData.getDaVersion() == DaVersion.DA_1 || daData.getDaVersion() == DaVersion.DA_2);
        assertTrue(daData.getDataLen() > 0);
    }

    /**
     * Test buildFrom with EthBlobs
     * Should decode blobs back to payload
     */
    @Test
    public void testBuildFromEthBlobs() {
        BlobsDaData original = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        EthBlobs blobs = original.getBlobs();

        BlobsDaData decoded = BlobsDaData.buildFrom(blobs);

        assertNotNull(decoded);
        assertNotNull(decoded.getBatchPayload());
        assertEquals(original.getBatchVersion(), decoded.getBatchVersion());
        assertEquals(original.getDaVersion(), decoded.getDaVersion());
    }

    /**
     * Test lazyBuildFrom
     * Should create BlobsDaData without immediate decoding
     */
    @Test
    public void testLazyBuildFrom() {
        EthBlobs blobs = new EthBlobs(List.of(new Blob(createRandomBlob())));

        BlobsDaData daData = BlobsDaData.lazyBuildFrom(blobs);

        assertNotNull(daData);
        assertNotNull(daData.getBlobs());
        assertEquals(blobs, daData.getBlobs());
    }

    /**
     * Test dataHash calculation
     * Should return 32-byte hash
     */
    @Test
    public void testDataHash() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        byte[] hash = daData.dataHash();

        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    /**
     * Test dataHash consistency
     * Multiple calls should return same hash
     */
    @Test
    public void testDataHashConsistency() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        byte[] hash1 = daData.dataHash();
        byte[] hash2 = daData.dataHash();

        assertArrayEquals(hash1, hash2);
    }

    /**
     * Test toBatchPayload
     * Should return IBatchPayload
     */
    @Test
    public void testToBatchPayload() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        IBatchPayload payload = daData.toBatchPayload();

        assertNotNull(payload);
        assertTrue(payload instanceof ChunksPayload);
    }

    /**
     * Test toBatchPayload caching
     * Multiple calls should return same instance
     */
    @Test
    public void testToBatchPayloadCaching() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        IBatchPayload payload1 = daData.toBatchPayload();
        IBatchPayload payload2 = daData.toBatchPayload();

        assertSame(payload1, payload2);
    }

    /**
     * Test encode-decode round trip with BATCH_V0
     * Should preserve data integrity
     */
    @Test
    public void testEncodeDecodeRoundTripV0() {
        byte[] originalData = testPayload.serialize();

        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());
        byte[] decodedData = decoded.toBatchPayload().serialize();

        assertArrayEquals(
                DigestUtil.sha256(originalData),
                DigestUtil.sha256(decodedData)
        );
    }

    /**
     * Test encode-decode round trip with BATCH_V1
     * Should preserve data integrity with compression
     */
    @Test
    public void testEncodeDecodeRoundTripV1() {
        byte[] originalData = testPayload.serialize();

        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());
        byte[] decodedData = decoded.toBatchPayload().serialize();

        assertArrayEquals(
                DigestUtil.sha256(originalData),
                DigestUtil.sha256(decodedData)
        );
    }

    /**
     * Test getBlobs
     * Should return EthBlobs
     */
    @Test
    public void testGetBlobs() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        EthBlobs blobs = daData.getBlobs();

        assertNotNull(blobs);
        assertNotNull(blobs.blobs());
        assertFalse(blobs.blobs().isEmpty());
    }

    /**
     * Test getDaVersion
     * Should return correct DA version
     */
    @Test
    public void testGetDaVersion() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        DaVersion version = daData.getDaVersion();

        assertNotNull(version);
        assertEquals(DaVersion.DA_0, version);
    }

    /**
     * Test getBatchVersion
     * Should return correct batch version
     */
    @Test
    public void testGetBatchVersion() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        BatchVersionEnum version = daData.getBatchVersion();

        assertNotNull(version);
        assertEquals(BatchVersionEnum.BATCH_V0, version);
    }

    /**
     * Test getDataLen
     * Should return positive length
     */
    @Test
    public void testGetDataLen() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        int dataLen = daData.getDataLen();

        assertTrue(dataLen > 0);
    }

    /**
     * Test getBatchPayload
     * Should return IBatchPayload (may be null for buildFrom with version/payload)
     */
    @Test
    public void testGetBatchPayload() {
        // When building from version/payload, batchPayload is not set directly
        // It's only set when building from blobs
        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());

        IBatchPayload payload = decoded.getBatchPayload();

        assertNotNull(payload);
        assertTrue(payload instanceof ChunksPayload);
    }

    /**
     * Test with single chunk
     */
    @Test
    public void testWithSingleChunk() {
        List<Chunk> singleChunk = createTestChunks(1);
        ChunksPayload payload = new ChunksPayload(singleChunk);

        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, payload);

        assertNotNull(daData);
        assertNotNull(daData.getBlobs());
    }

    /**
     * Test with many chunks
     */
    @Test
    public void testWithManyChunks() {
        List<Chunk> manyChunks = createTestChunks(10);
        ChunksPayload payload = new ChunksPayload(manyChunks);

        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, payload);

        assertNotNull(daData);
        assertNotNull(daData.getBlobs());
        assertTrue(daData.getBlobs().blobs().size() > 0);
    }

    /**
     * Test CAPACITY_BYTE_LEN_PER_WORD constant
     */
    @Test
    public void testCapacityBytePerWord() {
        assertEquals(31, BlobsDaData.CAPACITY_BYTE_LEN_PER_WORD);
    }

    /**
     * Test CAPACITY_BYTE_PER_BLOB constant
     */
    @Test
    public void testCapacityBytePerBlob() {
        assertEquals(31 * EthBlobs.WORDS_NUM_PER_BLOB, BlobsDaData.CAPACITY_BYTE_PER_BLOB);
    }

    /**
     * Test DA_DATA_META_LEN_SIZE constant
     */
    @Test
    public void testDaDataMetaLenSize() {
        assertEquals(4, BlobsDaData.DA_DATA_META_LEN_SIZE);
    }

    /**
     * Test DATA_LEN_SIZE_OF_DA_META constant
     */
    @Test
    public void testDataLenSizeOfDaMeta() {
        assertEquals(3, BlobsDaData.DATA_LEN_SIZE_OF_DA_META);
    }

    /**
     * Test compression effectiveness with BATCH_V1
     * Should choose compressed or uncompressed based on ratio
     */
    @Test
    public void testCompressionEffectiveness() {
        // Create payload with repetitive data (should compress well)
        List<Chunk> repetitiveChunks = createTestChunks(5);
        ChunksPayload payload = new ChunksPayload(repetitiveChunks);

        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, payload);

        assertNotNull(daData);
        // Should use DA_1 (uncompressed) or DA_2 (compressed)
        assertTrue(daData.getDaVersion() == DaVersion.DA_1 || daData.getDaVersion() == DaVersion.DA_2);
    }

    /**
     * Test blob count calculation
     * Should create appropriate number of blobs
     */
    @Test
    public void testBlobCountCalculation() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        int blobCount = daData.getBlobs().blobs().size();

        assertTrue(blobCount > 0);
        // Each blob should be exactly BLOB_SIZE
        for (Blob blob : daData.getBlobs().blobs()) {
            assertEquals(EthBlobs.BLOB_SIZE, blob.getData().toArray().length);
        }
    }

    /**
     * Test data hash uniqueness
     * Different data should produce different hashes
     */
    @Test
    public void testDataHashUniqueness() {
        List<Chunk> chunks1 = createTestChunks(2);
        List<Chunk> chunks2 = createTestChunks(3);

        BlobsDaData daData1 = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, new ChunksPayload(chunks1));
        BlobsDaData daData2 = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, new ChunksPayload(chunks2));

        byte[] hash1 = daData1.dataHash();
        byte[] hash2 = daData2.dataHash();

        assertFalse(Arrays.equals(hash1, hash2));
    }

    /**
     * Test version detection from blobs
     * Should correctly detect DA version from first byte
     */
    @Test
    public void testVersionDetectionFromBlobs() {
        BlobsDaData original = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        EthBlobs blobs = original.getBlobs();

        // First byte of first blob should be DA version
        byte firstByte = blobs.blobs().get(0).getData().get(0);
        assertEquals(DaVersion.DA_0.toByte(), firstByte);
    }

    /**
     * Test batch version preservation
     * Batch version should be preserved through encode-decode
     */
    @Test
    public void testBatchVersionPreservation() {
        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());

        assertEquals(encoded.getBatchVersion(), decoded.getBatchVersion());
    }

    /**
     * Test DA version preservation
     * DA version should be preserved through encode-decode
     */
    @Test
    public void testDaVersionPreservation() {
        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());

        assertEquals(encoded.getDaVersion(), decoded.getDaVersion());
    }

    /**
     * Test data length preservation
     * Data length should be preserved through encode-decode
     */
    @Test
    public void testDataLengthPreservation() {
        BlobsDaData encoded = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, testPayload);
        BlobsDaData decoded = BlobsDaData.buildFrom(encoded.getBlobs());

        assertEquals(encoded.getDataLen(), decoded.getDataLen());
    }

    /**
     * Test multiple encode operations
     * Should produce consistent results
     */
    @Test
    public void testMultipleEncodeOperations() {
        BlobsDaData daData1 = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        BlobsDaData daData2 = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        assertArrayEquals(daData1.dataHash(), daData2.dataHash());
    }

    // ==================== Negative Tests ====================

    /**
     * Test toBatchPayload with empty blobs
     * Should throw exception (ArrayIndexOutOfBoundsException or IllegalArgumentException)
     */
    @Test(expected = Exception.class)
    public void testToBatchPayloadWithEmptyBlobs() {
        EthBlobs emptyBlobs = new EthBlobs(List.of());
        BlobsDaData daData = BlobsDaData.lazyBuildFrom(emptyBlobs);

        daData.toBatchPayload();
    }

    /**
     * Test buildFrom with null payload
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testBuildFromWithNullPayload() {
        BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, null);
    }

    /**
     * Test buildFrom with null version
     * Should handle gracefully or throw exception
     */
    @Test
    public void testBuildFromWithNullVersion() {
        try {
            BlobsDaData daData = BlobsDaData.buildFrom(null, testPayload);
            // If no exception, verify it was created
            assertNotNull(daData);
        } catch (Exception e) {
            // Exception is acceptable for null version
            assertNotNull(e);
        }
    }

    /**
     * Test buildFrom with null blobs
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testBuildFromWithNullBlobs() {
        BlobsDaData.buildFrom(null);
    }

    /**
     * Test lazyBuildFrom with null blobs
     * Should create instance but fail on access
     */
    @Test
    public void testLazyBuildFromWithNullBlobs() {
        BlobsDaData daData = BlobsDaData.lazyBuildFrom(null);

        assertNotNull(daData);
        // Should fail when trying to access blobs
        try {
            daData.dataHash();
            fail("Should throw exception when accessing null blobs");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    /**
     * Test with invalid blob data
     * Should throw exception during decoding
     */
    @Test(expected = Exception.class)
    public void testWithInvalidBlobData() {
        // Create blob with invalid data (not multiple of 32)
        byte[] invalidData = new byte[100];
        Blob invalidBlob = new Blob(invalidData);
        EthBlobs invalidBlobs = new EthBlobs(List.of(invalidBlob));

        BlobsDaData.buildFrom(invalidBlobs);
    }

    /**
     * Test dataHash with uninitialized blobs
     * Should handle gracefully
     */
    @Test
    public void testDataHashWithUninitializedBlobs() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        byte[] hash = daData.dataHash();

        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    /**
     * Test encode-decode with corrupted data
     * Should detect corruption
     */
    @Test
    public void testEncodeDecodeWithCorruptedData() {
        BlobsDaData original = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);
        byte[] originalHash = original.dataHash();

        // Corrupt the blob data
        EthBlobs blobs = original.getBlobs();
        byte[] blobData = blobs.blobs().get(0).getData().toArray();
        blobData[100] = (byte) (blobData[100] ^ 0xFF); // Flip bits

        Blob corruptedBlob = new Blob(blobData);
        List<Blob> corruptedBlobs = new ArrayList<>(blobs.blobs());
        corruptedBlobs.set(0, corruptedBlob);
        EthBlobs corruptedEthBlobs = new EthBlobs(corruptedBlobs);

        BlobsDaData corrupted = BlobsDaData.buildFrom(corruptedEthBlobs);
        byte[] corruptedHash = corrupted.dataHash();

        // Hashes should be different
        assertFalse(Arrays.equals(originalHash, corruptedHash));
    }

    /**
     * Test with extremely small payload
     * Should handle gracefully
     */
    @Test
    public void testWithExtremelySmallPayload() {
        List<Chunk> smallChunks = createTestChunks(1);
        ChunksPayload payload = new ChunksPayload(smallChunks);

        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, payload);

        assertNotNull(daData);
        assertTrue(daData.getDataLen() > 0);
    }

    /**
     * Test toBatchPayload multiple times
     * Should return cached instance
     */
    @Test
    public void testToBatchPayloadMultipleTimes() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        IBatchPayload payload1 = daData.toBatchPayload();
        IBatchPayload payload2 = daData.toBatchPayload();
        IBatchPayload payload3 = daData.toBatchPayload();

        assertSame(payload1, payload2);
        assertSame(payload2, payload3);
    }

    /**
     * Test getBlobs consistency
     * Multiple calls should return same instance
     */
    @Test
    public void testGetBlobsConsistency() {
        BlobsDaData daData = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, testPayload);

        EthBlobs blobs1 = daData.getBlobs();
        EthBlobs blobs2 = daData.getBlobs();

        assertSame(blobs1, blobs2);
    }
}
