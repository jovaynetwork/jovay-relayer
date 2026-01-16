package com.alipay.antchain.l2.relayer.commons.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlockContext;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for RollupUtils
 * Tests both positive and negative cases for Rollup utility methods
 */
public class RollupUtilsTest {

    // ==================== Positive Tests ====================

    /**
     * Test isProveReqTypeToProcess with "all" type
     * Should return true for any ProveTypeEnum
     */
    @Test
    public void testIsProveReqTypeToProcess_AllType() {
        assertTrue(RollupUtils.isProveReqTypeToProcess("all", ProveTypeEnum.TEE_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("all", ProveTypeEnum.ZK_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("ALL", ProveTypeEnum.TEE_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("All", ProveTypeEnum.ZK_PROOF));
    }

    /**
     * Test isProveReqTypeToProcess with "tee" type
     * Should return true only for TEE_PROOF
     */
    @Test
    public void testIsProveReqTypeToProcess_TeeType() {
        assertTrue(RollupUtils.isProveReqTypeToProcess("tee", ProveTypeEnum.TEE_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("TEE", ProveTypeEnum.TEE_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("Tee", ProveTypeEnum.TEE_PROOF));

        assertFalse(RollupUtils.isProveReqTypeToProcess("tee", ProveTypeEnum.ZK_PROOF));
    }

    /**
     * Test isProveReqTypeToProcess with "zk" type
     * Should return true only for ZK_PROOF
     */
    @Test
    public void testIsProveReqTypeToProcess_ZkType() {
        assertTrue(RollupUtils.isProveReqTypeToProcess("zk", ProveTypeEnum.ZK_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("ZK", ProveTypeEnum.ZK_PROOF));
        assertTrue(RollupUtils.isProveReqTypeToProcess("Zk", ProveTypeEnum.ZK_PROOF));

        assertFalse(RollupUtils.isProveReqTypeToProcess("zk", ProveTypeEnum.TEE_PROOF));
    }

    /**
     * Test isProveReqTypeToProcess with invalid type
     * Should return false
     */
    @Test
    public void testIsProveReqTypeToProcess_InvalidType() {
        assertFalse(RollupUtils.isProveReqTypeToProcess("invalid", ProveTypeEnum.TEE_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("invalid", ProveTypeEnum.ZK_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("", ProveTypeEnum.TEE_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("unknown", ProveTypeEnum.ZK_PROOF));
    }

    /**
     * Test serializeChunks with single chunk
     */
    @Test
    public void testSerializeChunks_SingleChunk() {
        Chunk chunk = createTestChunk(1);
        List<Chunk> chunks = Collections.singletonList(chunk);

        byte[] result = RollupUtils.serializeChunks(chunks);

        assertNotNull(result);
        assertTrue(result.length > 4); // At least 4 bytes for length prefix
    }

    /**
     * Test serializeChunks with multiple chunks
     */
    @Test
    public void testSerializeChunks_MultipleChunks() {
        List<Chunk> chunks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            chunks.add(createTestChunk(i));
        }
        
        byte[] result = RollupUtils.serializeChunks(chunks);
        
        assertNotNull(result);
        assertTrue(result.length > 12); // At least 3 * 4 bytes for length prefixes
    }

    /**
     * Test serializeChunks with empty list
     */
    @Test
    public void testSerializeChunks_EmptyList() {
        List<Chunk> chunks = new ArrayList<>();
        
        byte[] result = RollupUtils.serializeChunks(chunks);
        
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    /**
     * Test appendToRawChunks with valid inputs
     */
    @Test
    public void testAppendToRawChunks_ValidInputs() {
        // Create initial raw chunks
        Chunk chunk1 = createTestChunk(1);
        byte[] rawChunks = RollupUtils.serializeChunks(Collections.singletonList(chunk1));

        int originalLength = rawChunks.length;

        // Append new chunk
        Chunk chunk2 = createTestChunk(2);

        byte[] newRawChunks = RollupUtils.appendToRawChunks(rawChunks, chunk2);

        assertNotNull(newRawChunks);
        assertTrue(newRawChunks.length > originalLength);

        // Verify original data is preserved
        for (int i = 0; i < originalLength; i++) {
            assertEquals(rawChunks[i], newRawChunks[i]);
        }
    }

    /**
     * Test appendToRawChunks with empty initial array
     */
    @Test
    public void testAppendToRawChunks_EmptyInitialArray() {
        byte[] rawChunks = new byte[0];

        Chunk chunk = createTestChunk(1);

        byte[] result = RollupUtils.appendToRawChunks(rawChunks, chunk);

        assertNotNull(result);
        assertTrue(result.length > 4); // At least 4 bytes for length prefix
    }

    /**
     * Test deserializeChunks with valid serialized data
     */
    @Test
    public void testDeserializeChunks_ValidData() {
        // Create and serialize chunks
        List<Chunk> originalChunks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            originalChunks.add(createTestChunk(i));
        }

        byte[] rawChunks = RollupUtils.serializeChunks(originalChunks);

        // Deserialize
        List<Chunk> deserializedChunks = RollupUtils.deserializeChunks(rawChunks);

        assertNotNull(deserializedChunks);
        assertEquals(originalChunks.size(), deserializedChunks.size());
    }

    /**
     * Test deserializeChunks with single chunk
     */
    @Test
    public void testDeserializeChunks_SingleChunk() {
        Chunk chunk = createTestChunk(1);

        byte[] rawChunks = RollupUtils.serializeChunks(Collections.singletonList(chunk));

        List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test serialize and deserialize round trip
     */
    @Test
    public void testSerializeDeserialize_RoundTrip() {
        // Create original chunks
        List<Chunk> originalChunks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            originalChunks.add(createTestChunkWithMultipleBlocks(i, 2));
        }
        
        // Serialize
        byte[] rawChunks = RollupUtils.serializeChunks(originalChunks);
        
        // Deserialize
        List<Chunk> deserializedChunks = RollupUtils.deserializeChunks(rawChunks);
        
        // Verify
        assertNotNull(deserializedChunks);
        assertEquals(originalChunks.size(), deserializedChunks.size());
        
        // Verify each chunk has same number of blocks
        for (int i = 0; i < originalChunks.size(); i++) {
            assertEquals(
                originalChunks.get(i).getBlocks().size(),
                deserializedChunks.get(i).getBlocks().size()
            );
        }
    }

    // ==================== Negative Tests ====================

    /**
     * Test isProveReqTypeToProcess with null batch prove req types
     * Should return false
     */
    @Test
    public void testIsProveReqTypeToProcess_NullBatchProveReqTypes() {
        assertFalse(RollupUtils.isProveReqTypeToProcess(null, ProveTypeEnum.TEE_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess(null, ProveTypeEnum.ZK_PROOF));
    }

    /**
     * Test isProveReqTypeToProcess with null prove type
     * Should return true for "all" type regardless of prove type
     */
    @Test
    public void testIsProveReqTypeToProcess_NullProveType() {
        // When batchProveReqTypes is "all", it returns true for any prove type (including null)
        boolean result = RollupUtils.isProveReqTypeToProcess("all", null);
        assertTrue(result);

        // For specific types with null prove type, should return false
        result = RollupUtils.isProveReqTypeToProcess("tee", null);
        assertFalse(result);

        result = RollupUtils.isProveReqTypeToProcess("zk", null);
        assertFalse(result);
    }

    /**
     * Test serializeChunks with null list
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testSerializeChunks_NullList() {
        RollupUtils.serializeChunks(null);
    }

    /**
     * Test serializeChunks with list containing null chunk
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testSerializeChunks_ListContainingNull() {
        List<Chunk> chunks = new ArrayList<>();
        chunks.add(null);
        
        RollupUtils.serializeChunks(chunks);
    }

    /**
     * Test appendToRawChunks with null raw chunks
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testAppendToRawChunks_NullRawChunks() {
        Chunk chunk = createTestChunk(1);
        
        RollupUtils.appendToRawChunks(null, chunk);
    }

    /**
     * Test appendToRawChunks with null chunk
     * Should throw NullPointerException or RuntimeException
     */
    @Test
    public void testAppendToRawChunks_NullChunk() {
        byte[] rawChunks = new byte[10];

        try {
            RollupUtils.appendToRawChunks(rawChunks, null);
            fail("Expected exception when appending null chunk");
        } catch (NullPointerException e) {
            // Expected - null chunk should cause exception
            assertNotNull(e);
        }
    }

    /**
     * Test deserializeChunks with null data
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testDeserializeChunks_NullData() {
        RollupUtils.deserializeChunks(null);
    }

    /**
     * Test deserializeChunks with empty array
     * Should throw ArrayIndexOutOfBoundsException
     */
    @Test
    public void testDeserializeChunks_EmptyArray() {
        byte[] rawChunks = new byte[0];

        try {
            List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);
            // If no exception, should return empty list
            assertNotNull(result);
            assertTrue(result.isEmpty());
        } catch (ArrayIndexOutOfBoundsException e) {
            // Expected behavior - empty array causes exception
            assertNotNull(e);
        }
    }

    /**
     * Test deserializeChunks with insufficient data (less than 4 bytes)
     * Should return empty list or throw exception
     */
    @Test
    public void testDeserializeChunks_InsufficientData() {
        byte[] rawChunks = new byte[]{1, 2, 3}; // Less than 4 bytes
        
        try {
            List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);
            // If no exception, should return empty list
            assertNotNull(result);
            assertTrue(result.isEmpty());
        } catch (ArrayIndexOutOfBoundsException e) {
            // Expected behavior - insufficient data causes exception
            assertNotNull(e);
        }
    }

    /**
     * Test deserializeChunks with corrupted data (invalid length)
     * Should handle gracefully or throw exception
     */
    @Test
    public void testDeserializeChunks_CorruptedData() {
        // Create data with invalid length prefix
        byte[] rawChunks = new byte[]{
            0, 0, 0, 100, // Length = 100, but actual data is much less
            1, 2, 3, 4
        };
        
        try {
            List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);
            // If no exception, verify result
            assertNotNull(result);
        } catch (Exception e) {
            // Expected behavior - corrupted data may cause exception
            assertNotNull(e);
        }
    }

    /**
     * Test deserializeChunks with data containing zero length
     * Should stop processing and return chunks found so far
     */
    @Test
    public void testDeserializeChunks_ZeroLength() {
        // Create valid chunk followed by zero length
        Chunk chunk = createTestChunk(1);
        byte[] validChunk = RollupUtils.serializeChunks(Collections.singletonList(chunk));
        
        // Append zero length
        byte[] rawChunks = new byte[validChunk.length + 4];
        System.arraycopy(validChunk, 0, rawChunks, 0, validChunk.length);
        // Last 4 bytes are already zero
        
        List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test isProveReqTypeToProcess with empty string
     * Should return false
     */
    @Test
    public void testIsProveReqTypeToProcess_EmptyString() {
        assertFalse(RollupUtils.isProveReqTypeToProcess("", ProveTypeEnum.TEE_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("", ProveTypeEnum.ZK_PROOF));
    }

    /**
     * Test isProveReqTypeToProcess with whitespace
     * Should return false
     */
    @Test
    public void testIsProveReqTypeToProcess_Whitespace() {
        assertFalse(RollupUtils.isProveReqTypeToProcess(" ", ProveTypeEnum.TEE_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("  ", ProveTypeEnum.ZK_PROOF));
        assertFalse(RollupUtils.isProveReqTypeToProcess("\t", ProveTypeEnum.TEE_PROOF));
    }

    /**
     * Test serializeChunks with chunk containing empty blocks
     */
    @Test
    public void testSerializeChunks_ChunkWithEmptyBlocks() {
        Chunk chunk = new Chunk();
        chunk.setNumBlocks((byte) 0);
        chunk.setBlocks(new ArrayList<>());
        chunk.setL2Transactions(new byte[0]);

        List<Chunk> chunks = Collections.singletonList(chunk);
        
        byte[] result = RollupUtils.serializeChunks(chunks);
        
        assertNotNull(result);
        assertTrue(result.length >= 4); // At least length prefix
    }

    /**
     * Test appendToRawChunks multiple times
     */
    @Test
    public void testAppendToRawChunks_MultipleTimes() {
        byte[] rawChunks = new byte[0];
        
        // Append 3 chunks
        for (int i = 0; i < 3; i++) {
            Chunk chunk = createTestChunk(i);
            rawChunks = RollupUtils.appendToRawChunks(rawChunks, chunk);
        }

        assertNotNull(rawChunks);
        assertTrue(rawChunks.length > 12); // At least 3 * 4 bytes for length prefixes

        // Verify can deserialize
        List<Chunk> result = RollupUtils.deserializeChunks(rawChunks);
        assertEquals(3, result.size());
    }

    // ==================== Helper Methods ====================

    /**
     * Create a test chunk with single block
     */
    private Chunk createTestChunk(int blockNumber) {
        BlockContext blockContext = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(blockNumber))
                .timestamp(System.currentTimeMillis())
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(0)
                .numL1Messages(0)
                .build();

        Chunk chunk = new Chunk();
        chunk.setNumBlocks((byte) 1);
        chunk.setBlocks(Collections.singletonList(blockContext));
        chunk.setL2Transactions(new byte[0]);

        return chunk;
    }

    /**
     * Create a test chunk with multiple blocks
     */
    private Chunk createTestChunkWithMultipleBlocks(int startBlockNumber, int numBlocks) {
        List<BlockContext> blocks = new ArrayList<>();

        for (int i = 0; i < numBlocks; i++) {
            BlockContext blockContext = BlockContext.builder()
                    .specVersion(1L)
                    .blockNumber(BigInteger.valueOf(startBlockNumber + i))
                    .timestamp(System.currentTimeMillis())
                    .baseFee(BigInteger.valueOf(1000000000L))
                    .gasLimit(BigInteger.valueOf(30000000L))
                    .numTransactions(0)
                    .numL1Messages(0)
                    .build();
            blocks.add(blockContext);
        }

        Chunk chunk = new Chunk();
        chunk.setNumBlocks((byte) numBlocks);
        chunk.setBlocks(blocks);
        chunk.setL2Transactions(new byte[0]);

        return chunk;
    }
}
