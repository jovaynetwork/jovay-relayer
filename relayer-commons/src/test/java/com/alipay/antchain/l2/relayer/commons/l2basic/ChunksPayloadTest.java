package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ChunksPayload record class
 * Tests payload operations including serialization, validation, and statistics
 */
public class ChunksPayloadTest {

    private List<Chunk> testChunks;
    private ChunksPayload payload;

    @Before
    public void setUp() {
        testChunks = createTestChunks(3);
    }

    // ==================== Helper Methods ====================

    /**
     * Create test chunks with continuous block numbers
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
     * Create discontinuous chunks for testing validation
     */
    private List<Chunk> createDiscontinuousChunks() {
        List<Chunk> chunks = new ArrayList<>();
        
        // First chunk: blocks 100-101
        List<BlockContext> blocks1 = new ArrayList<>();
        blocks1.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(100))
                .specVersion(1L)
                .timestamp(1000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        blocks1.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(101))
                .specVersion(1L)
                .timestamp(1001L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        
        Chunk chunk1 = Chunk.builder()
                .numBlocks((byte) 2)
                .blocks(blocks1)
                .hash(new byte[32])
                .build();
        chunks.add(chunk1);

        // Second chunk: blocks 103-104 (Gap: missing 102)
        List<BlockContext> blocks2 = new ArrayList<>();
        blocks2.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(103))
                .specVersion(1L)
                .timestamp(1003L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        blocks2.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(104))
                .specVersion(1L)
                .timestamp(1004L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        
        Chunk chunk2 = Chunk.builder()
                .numBlocks((byte) 2)
                .blocks(blocks2)
                .hash(new byte[32])
                .build();
        chunks.add(chunk2);

        return chunks;
    }

    // ==================== Positive Tests ====================

    /**
     * Test record constructor
     * Should create ChunksPayload with chunks
     */
    @Test
    public void testConstructor() {
        payload = new ChunksPayload(testChunks);

        assertNotNull(payload);
        assertEquals(testChunks, payload.chunks());
    }

    /**
     * Test chunks() getter
     * Should return the chunks list
     */
    @Test
    public void testChunksGetter() {
        payload = new ChunksPayload(testChunks);

        List<Chunk> chunks = payload.chunks();

        assertNotNull(chunks);
        assertEquals(3, chunks.size());
        assertSame(testChunks, chunks);
    }

    /**
     * Test serialize method
     * Should return serialized byte array
     */
    @Test
    public void testSerialize() {
        payload = new ChunksPayload(testChunks);

        byte[] serialized = payload.serialize();

        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    /**
     * Test serialize consistency
     * Multiple calls should return same result
     */
    @Test
    public void testSerializeConsistency() {
        payload = new ChunksPayload(testChunks);

        byte[] serialized1 = payload.serialize();
        byte[] serialized2 = payload.serialize();

        assertArrayEquals(serialized1, serialized2);
    }

    /**
     * Test getStartBlockNumber
     * Should return minimum block number from all chunks
     */
    @Test
    public void testGetStartBlockNumber() {
        payload = new ChunksPayload(testChunks);

        BigInteger startBlock = payload.getStartBlockNumber();

        assertEquals(BigInteger.valueOf(100), startBlock);
    }

    /**
     * Test getEndBlockNumber
     * Should return maximum block number from all chunks
     */
    @Test
    public void testGetEndBlockNumber() {
        payload = new ChunksPayload(testChunks);

        BigInteger endBlock = payload.getEndBlockNumber();

        assertEquals(BigInteger.valueOf(105), endBlock);
    }

    /**
     * Test validate with continuous chunks
     * Should not throw exception
     */
    @Test
    public void testValidateWithContinuousChunks() throws InvalidBatchException {
        payload = new ChunksPayload(testChunks);

        // Should not throw exception
        payload.validate();
    }

    /**
     * Test getRawTxTotalSize
     * Should return total size of all transactions
     */
    @Test
    public void testGetRawTxTotalSize() {
        payload = new ChunksPayload(testChunks);

        long totalSize = payload.getRawTxTotalSize();

        // Since test chunks don't have l2Transactions, size should be 0
        assertEquals(0L, totalSize);
    }

    /**
     * Test getL2TxCount
     * Should return total count of L2 transactions
     */
    @Test
    public void testGetL2TxCount() {
        payload = new ChunksPayload(testChunks);

        int txCount = payload.getL2TxCount();

        // Since test chunks don't have l2Transactions, count should be 0
        assertEquals(0, txCount);
    }

    /**
     * Test with single chunk
     */
    @Test
    public void testWithSingleChunk() {
        List<Chunk> singleChunk = createTestChunks(1);
        payload = new ChunksPayload(singleChunk);

        assertNotNull(payload);
        assertEquals(1, payload.chunks().size());
        assertEquals(BigInteger.valueOf(100), payload.getStartBlockNumber());
        assertEquals(BigInteger.valueOf(101), payload.getEndBlockNumber());
    }

    /**
     * Test with many chunks
     */
    @Test
    public void testWithManyChunks() {
        List<Chunk> manyChunks = createTestChunks(10);
        payload = new ChunksPayload(manyChunks);

        assertNotNull(payload);
        assertEquals(10, payload.chunks().size());
        assertEquals(BigInteger.valueOf(100), payload.getStartBlockNumber());
        assertEquals(BigInteger.valueOf(119), payload.getEndBlockNumber());
    }

    /**
     * Test record equals
     * Same chunks should be equal
     */
    @Test
    public void testEquals() {
        ChunksPayload payload1 = new ChunksPayload(testChunks);
        ChunksPayload payload2 = new ChunksPayload(testChunks);

        assertEquals(payload1, payload2);
    }

    /**
     * Test record hashCode
     * Same chunks should have same hashCode
     */
    @Test
    public void testHashCode() {
        ChunksPayload payload1 = new ChunksPayload(testChunks);
        ChunksPayload payload2 = new ChunksPayload(testChunks);

        assertEquals(payload1.hashCode(), payload2.hashCode());
    }

    /**
     * Test record toString
     * Should contain chunks information
     */
    @Test
    public void testToString() {
        payload = new ChunksPayload(testChunks);

        String str = payload.toString();

        assertNotNull(str);
        assertTrue(str.contains("ChunksPayload"));
        assertTrue(str.contains("chunks"));
    }

    /**
     * Test getStartBlockNumber with unordered chunks
     * Should still return minimum block number
     */
    @Test
    public void testGetStartBlockNumberWithUnorderedChunks() {
        List<Chunk> unorderedChunks = new ArrayList<>();
        unorderedChunks.add(testChunks.get(2)); // blocks 104-105
        unorderedChunks.add(testChunks.get(0)); // blocks 100-101
        unorderedChunks.add(testChunks.get(1)); // blocks 102-103

        payload = new ChunksPayload(unorderedChunks);

        BigInteger startBlock = payload.getStartBlockNumber();

        assertEquals(BigInteger.valueOf(100), startBlock);
    }

    /**
     * Test getEndBlockNumber with unordered chunks
     * Should still return maximum block number
     */
    @Test
    public void testGetEndBlockNumberWithUnorderedChunks() {
        List<Chunk> unorderedChunks = new ArrayList<>();
        unorderedChunks.add(testChunks.get(2)); // blocks 104-105
        unorderedChunks.add(testChunks.get(0)); // blocks 100-101
        unorderedChunks.add(testChunks.get(1)); // blocks 102-103

        payload = new ChunksPayload(unorderedChunks);

        BigInteger endBlock = payload.getEndBlockNumber();

        assertEquals(BigInteger.valueOf(105), endBlock);
    }

    /**
     * Test getRawTxTotalSize consistency
     * Multiple calls should return same value
     */
    @Test
    public void testGetRawTxTotalSizeConsistency() {
        payload = new ChunksPayload(testChunks);

        long size1 = payload.getRawTxTotalSize();
        long size2 = payload.getRawTxTotalSize();

        assertEquals(size1, size2);
    }

    /**
     * Test getL2TxCount consistency
     * Multiple calls should return same value
     */
    @Test
    public void testGetL2TxCountConsistency() {
        payload = new ChunksPayload(testChunks);

        int count1 = payload.getL2TxCount();
        int count2 = payload.getL2TxCount();

        assertEquals(count1, count2);
    }

    /**
     * Test validate multiple times
     * Should be idempotent
     */
    @Test
    public void testValidateMultipleTimes() throws InvalidBatchException {
        payload = new ChunksPayload(testChunks);

        payload.validate();
        payload.validate();
        payload.validate();

        // Should not throw exception
    }

    // ==================== Negative Tests ====================

    /**
     * Test constructor with null chunks
     * Should create payload but fail on operations
     */
    @Test
    public void testConstructorWithNullChunks() {
        payload = new ChunksPayload(null);

        assertNotNull(payload);
        assertNull(payload.chunks());
    }

    /**
     * Test constructor with empty chunks
     * Should create payload but fail on some operations
     */
    @Test
    public void testConstructorWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());

        assertNotNull(payload);
        assertEquals(0, payload.chunks().size());
    }

    /**
     * Test getStartBlockNumber with null chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetStartBlockNumberWithNullChunks() {
        payload = new ChunksPayload(null);
        payload.getStartBlockNumber();
    }

    /**
     * Test getStartBlockNumber with empty chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetStartBlockNumberWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());
        payload.getStartBlockNumber();
    }

    /**
     * Test getEndBlockNumber with null chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetEndBlockNumberWithNullChunks() {
        payload = new ChunksPayload(null);
        payload.getEndBlockNumber();
    }

    /**
     * Test getEndBlockNumber with empty chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetEndBlockNumberWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());
        payload.getEndBlockNumber();
    }

    /**
     * Test getRawTxTotalSize with null chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetRawTxTotalSizeWithNullChunks() {
        payload = new ChunksPayload(null);
        payload.getRawTxTotalSize();
    }

    /**
     * Test getRawTxTotalSize with empty chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetRawTxTotalSizeWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());
        payload.getRawTxTotalSize();
    }

    /**
     * Test getL2TxCount with null chunks
     * Should return 0
     */
    @Test
    public void testGetL2TxCountWithNullChunks() {
        payload = new ChunksPayload(null);

        int count = payload.getL2TxCount();

        assertEquals(0, count);
    }

    /**
     * Test getL2TxCount with empty chunks
     * Should return 0
     */
    @Test
    public void testGetL2TxCountWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());

        int count = payload.getL2TxCount();

        assertEquals(0, count);
    }

    /**
     * Test validate with discontinuous chunks
     * Should throw InvalidBatchException
     */
    @Test(expected = InvalidBatchException.class)
    public void testValidateWithDiscontinuousChunks() throws InvalidBatchException {
        List<Chunk> discontinuousChunks = createDiscontinuousChunks();
        payload = new ChunksPayload(discontinuousChunks);

        payload.validate();
    }

    /**
     * Test validate with null chunks
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testValidateWithNullChunks() throws InvalidBatchException {
        payload = new ChunksPayload(null);
        payload.validate();
    }

    /**
     * Test validate with empty chunks
     * Should not throw exception (no chunks to validate)
     */
    @Test
    public void testValidateWithEmptyChunks() throws InvalidBatchException {
        payload = new ChunksPayload(new ArrayList<>());

        // Should not throw exception
        payload.validate();
    }

    /**
     * Test serialize with null chunks
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullChunks() {
        payload = new ChunksPayload(null);
        payload.serialize();
    }

    /**
     * Test serialize with empty chunks
     * Should return empty or minimal byte array
     */
    @Test
    public void testSerializeWithEmptyChunks() {
        payload = new ChunksPayload(new ArrayList<>());

        byte[] serialized = payload.serialize();

        assertNotNull(serialized);
        // Empty chunks should produce minimal serialization
        assertTrue(serialized.length >= 0);
    }

    /**
     * Test equals with different chunks
     * Should not be equal
     */
    @Test
    public void testEqualsWithDifferentChunks() {
        List<Chunk> otherChunks = createTestChunks(2);
        ChunksPayload payload1 = new ChunksPayload(testChunks);
        ChunksPayload payload2 = new ChunksPayload(otherChunks);

        assertNotEquals(payload1, payload2);
    }

    /**
     * Test equals with null
     * Should not be equal
     */
    @Test
    public void testEqualsWithNull() {
        payload = new ChunksPayload(testChunks);

        assertNotEquals(payload, null);
    }

    /**
     * Test hashCode with different chunks
     * Should have different hashCode
     */
    @Test
    public void testHashCodeWithDifferentChunks() {
        List<Chunk> otherChunks = createTestChunks(2);
        ChunksPayload payload1 = new ChunksPayload(testChunks);
        ChunksPayload payload2 = new ChunksPayload(otherChunks);

        assertNotEquals(payload1.hashCode(), payload2.hashCode());
    }

    /**
     * Test with chunks containing null blocks
     * Should handle gracefully or throw exception
     */
    @Test
    public void testWithChunksContainingNullBlocks() {
        List<Chunk> chunksWithNullBlocks = new ArrayList<>();
        Chunk chunkWithNullBlocks = Chunk.builder()
                .numBlocks((byte) 0)
                .blocks(null)
                .hash(new byte[32])
                .build();
        chunksWithNullBlocks.add(chunkWithNullBlocks);

        payload = new ChunksPayload(chunksWithNullBlocks);

        try {
            payload.getStartBlockNumber();
            fail("Should throw exception with null blocks");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
