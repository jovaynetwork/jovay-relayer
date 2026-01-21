package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.l2basic.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BatchWrapper class
 * Tests batch wrapper functionality
 */
public class BatchWrapperTest {

    private BatchVersionEnum testBatchVersion;
    private BigInteger testBatchIndex;
    private BatchWrapper testParentBatchWrapper;
    private byte[] testPostStateRoot;
    private byte[] testL1MsgRollingHash;
    private byte[] testL2MsgRoot;
    private long testFinalizedL1MsgIndex;
    private List<ChunkWrapper> testChunks;

    @Before
    public void setUp() {
        testBatchVersion = BatchVersionEnum.BATCH_V0;
        testBatchIndex = BigInteger.valueOf(1);
        testPostStateRoot = new byte[32];
        Arrays.fill(testPostStateRoot, (byte) 0x01);
        testL1MsgRollingHash = new byte[32];
        Arrays.fill(testL1MsgRollingHash, (byte) 0x02);
        testL2MsgRoot = new byte[32];
        Arrays.fill(testL2MsgRoot, (byte) 0x03);
        testFinalizedL1MsgIndex = 10L;

        // Create parent batch wrapper
        testParentBatchWrapper = createParentBatchWrapper();

        // Create test chunks
        testChunks = createTestChunks(3);
    }

    // ==================== Helper Methods ====================

    /**
     * Create a parent batch wrapper for testing
     */
    private BatchWrapper createParentBatchWrapper() {
        BatchWrapper parent = new BatchWrapper();
        
        // Create a simple batch header for parent using builder
        byte[] parentHash = new byte[32];
        Arrays.fill(parentHash, (byte) 0xFF);

        BatchHeader parentHeader = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.ZERO)
                .l1MsgRollingHash(new byte[32])
                .dataHash(new byte[32])
                .parentBatchHash(parentHash)
                .build();

        // Create a simple batch for parent with one chunk
        List<Chunk> parentChunks = Collections.singletonList(
                createTestChunk(BigInteger.ZERO, 0).getChunk()
        );

        Batch parentBatch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.ZERO,
                parentHeader,
                new byte[32],
                parentChunks
        );
        
        parent.setBatch(parentBatch);
        parent.setTotalL1MessagePopped(5L);
        parent.setL1MessagePopped(5L);
        
        return parent;
    }

    /**
     * Create test chunks with sequential indices
     */
    private List<ChunkWrapper> createTestChunks(int count) {
        List<ChunkWrapper> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ChunkWrapper chunk = createTestChunk(testBatchIndex, i);
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Create a single test chunk
     */
    private ChunkWrapper createTestChunk(BigInteger batchIndex, long chunkIndex) {
        // Create a simple chunk with minimal data
        byte[] rawChunk = new byte[100];
        rawChunk[0] = 1; // numBlocks = 1
        // Fill with block context data (58 bytes per block)
        Arrays.fill(rawChunk, 1, 59, (byte) 0x00);
        
        byte[] chunkHash = new byte[32];
        Arrays.fill(chunkHash, (byte) chunkIndex);
        
        return new ChunkWrapper(batchIndex, chunkIndex, chunkHash, 1000L, rawChunk);
    }

    // ==================== Constructor Tests ====================

    /**
     * Test no-arg constructor
     * Should create empty wrapper
     */
    @Test
    public void testNoArgConstructor() {
        BatchWrapper wrapper = new BatchWrapper();
        
        assertNotNull(wrapper);
        assertNull(wrapper.getBatch());
        assertNull(wrapper.getPostStateRoot());
        assertNull(wrapper.getL2MsgRoot());
    }

    // ==================== createBatch Tests ====================

    /**
     * Test createBatch with valid parameters
     * Should create batch wrapper successfully
     */
    @Test
    public void testCreateBatch() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        assertNotNull(wrapper);
        assertNotNull(wrapper.getBatch());
        assertEquals(testBatchIndex, wrapper.getBatchIndex());
        assertArrayEquals(testPostStateRoot, wrapper.getPostStateRoot());
        assertArrayEquals(testL2MsgRoot, wrapper.getL2MsgRoot());
        assertEquals(testFinalizedL1MsgIndex, wrapper.getTotalL1MessagePopped());
        assertEquals(5L, wrapper.getL1MessagePopped()); // 10 - 5 = 5
    }

    /**
     * Test createBatch with single chunk
     * Should handle single chunk correctly
     */
    @Test
    public void testCreateBatchWithSingleChunk() {
        List<ChunkWrapper> singleChunk = Collections.singletonList(createTestChunk(testBatchIndex, 0));

        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                singleChunk
        );

        assertNotNull(wrapper);
        assertEquals(1, wrapper.getChunks().size());
    }

    /**
     * Test createBatch with multiple chunks
     * Should handle multiple chunks correctly
     */
    @Test
    public void testCreateBatchWithMultipleChunks() {
        List<ChunkWrapper> multipleChunks = createTestChunks(5);

        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                multipleChunks
        );

        assertNotNull(wrapper);
        assertEquals(5, wrapper.getChunks().size());
    }

    /**
     * Test createBatch with BATCH_V1
     * Should work with different batch versions
     */
    @Test
    public void testCreateBatchWithV1() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        assertNotNull(wrapper);
        assertNotNull(wrapper.getBatch());
    }

    /**
     * Test createBatch with zero l1MessagePopped
     * Should handle when finalizedL1MsgIndex equals parent's totalL1MessagePopped
     */
    @Test
    public void testCreateBatchWithZeroL1MessagePopped() {
        long finalizedIndex = testParentBatchWrapper.getTotalL1MessagePopped();

        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                finalizedIndex,
                testChunks
        );

        assertNotNull(wrapper);
        assertEquals(0L, wrapper.getL1MessagePopped());
    }

    /**
     * Test createBatch with large l1MessagePopped
     * Should handle large difference correctly
     */
    @Test
    public void testCreateBatchWithLargeL1MessagePopped() {
        long largeFinalizedIndex = 1000L;

        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                largeFinalizedIndex,
                testChunks
        );

        assertNotNull(wrapper);
        assertEquals(995L, wrapper.getL1MessagePopped()); // 1000 - 5 = 995
    }

    // ==================== Getter Tests ====================

    /**
     * Test getBatchIndex
     * Should return batch index from batch
     */
    @Test
    public void testGetBatchIndex() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        assertEquals(testBatchIndex, wrapper.getBatchIndex());
    }

    /**
     * Test getStartBlockNumber when startBlockNumber is null
     * Should return value from batch
     */
    @Test
    public void testGetStartBlockNumberFromBatch() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        BigInteger startBlockNumber = wrapper.getStartBlockNumber();
        assertNotNull(startBlockNumber);
        // Should get from batch's chunks
        assertEquals(wrapper.getBatch().getStartBlockNumber(), startBlockNumber);
    }

    /**
     * Test getStartBlockNumber when startBlockNumber is set
     * Should return the set value
     */
    @Test
    public void testGetStartBlockNumberWhenSet() {
        BatchWrapper wrapper = new BatchWrapper();
        BigInteger customStartBlock = BigInteger.valueOf(100);
        wrapper.setStartBlockNumber(customStartBlock);

        assertEquals(customStartBlock, wrapper.getStartBlockNumber());
    }

    /**
     * Test getEndBlockNumber when endBlockNumber is null
     * Should return value from batch
     */
    @Test
    public void testGetEndBlockNumberFromBatch() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        BigInteger endBlockNumber = wrapper.getEndBlockNumber();
        assertNotNull(endBlockNumber);
        // Should get from batch's chunks
        assertEquals(wrapper.getBatch().getEndBlockNumber(), endBlockNumber);
    }

    /**
     * Test getEndBlockNumber when endBlockNumber is set
     * Should return the set value
     */
    @Test
    public void testGetEndBlockNumberWhenSet() {
        BatchWrapper wrapper = new BatchWrapper();
        BigInteger customEndBlock = BigInteger.valueOf(200);
        wrapper.setEndBlockNumber(customEndBlock);

        assertEquals(customEndBlock, wrapper.getEndBlockNumber());
    }

    /**
     * Test getBatchHeader
     * Should return batch header from batch
     */
    @Test
    public void testGetBatchHeader() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        BatchHeader header = wrapper.getBatchHeader();
        assertNotNull(header);
        assertEquals(wrapper.getBatch().getBatchHeader(), header);
    }

    /**
     * Test getChunks
     * Should return chunks from batch payload
     */
    @Test
    public void testGetChunks() {
        BatchWrapper wrapper = BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                testChunks
        );

        List<Chunk> chunks = wrapper.getChunks();
        assertNotNull(chunks);
        assertEquals(testChunks.size(), chunks.size());
    }

    /**
     * Test all setters and getters
     * Should work correctly
     */
    @Test
    public void testSettersAndGetters() {
        BatchWrapper wrapper = new BatchWrapper();
        
        // Create a valid batch for testing
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.ZERO)
                .l1MsgRollingHash(new byte[32])
                .dataHash(new byte[32])
                .parentBatchHash(new byte[32])
                .build();

        List<Chunk> chunks = Collections.singletonList(
                createTestChunk(BigInteger.ZERO, 0).getChunk()
        );

        Batch batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.ZERO,
                header,
                new byte[32],
                chunks
        );

        wrapper.setBatch(batch);
        assertEquals(batch, wrapper.getBatch());

        BigInteger startBlock = BigInteger.valueOf(1);
        wrapper.setStartBlockNumber(startBlock);
        assertEquals(startBlock, wrapper.getStartBlockNumber());

        BigInteger endBlock = BigInteger.valueOf(10);
        wrapper.setEndBlockNumber(endBlock);
        assertEquals(endBlock, wrapper.getEndBlockNumber());

        byte[] postStateRoot = new byte[32];
        wrapper.setPostStateRoot(postStateRoot);
        assertArrayEquals(postStateRoot, wrapper.getPostStateRoot());

        byte[] l2MsgRoot = new byte[32];
        wrapper.setL2MsgRoot(l2MsgRoot);
        assertArrayEquals(l2MsgRoot, wrapper.getL2MsgRoot());

        long totalPopped = 100L;
        wrapper.setTotalL1MessagePopped(totalPopped);
        assertEquals(totalPopped, wrapper.getTotalL1MessagePopped());

        long popped = 50L;
        wrapper.setL1MessagePopped(popped);
        assertEquals(popped, wrapper.getL1MessagePopped());

        long gmtCreate = System.currentTimeMillis();
        wrapper.setGmtCreate(gmtCreate);
        assertEquals(gmtCreate, wrapper.getGmtCreate());
    }

    // ==================== Negative Tests ====================

    /**
     * Test createBatch with mismatched chunk indices
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testCreateBatchWithMismatchedChunkIndices() {
        List<ChunkWrapper> badChunks = new ArrayList<>();
        badChunks.add(createTestChunk(testBatchIndex, 0));
        badChunks.add(createTestChunk(testBatchIndex, 2)); // Index 2 instead of 1
        badChunks.add(createTestChunk(testBatchIndex, 3)); // Index 3 instead of 2

        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                badChunks
        );
    }

    /**
     * Test createBatch with wrong chunk index at middle
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testCreateBatchWithWrongMiddleChunkIndex() {
        List<ChunkWrapper> badChunks = new ArrayList<>();
        badChunks.add(createTestChunk(testBatchIndex, 0));
        badChunks.add(createTestChunk(testBatchIndex, 5)); // Wrong index
        badChunks.add(createTestChunk(testBatchIndex, 2));

        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                badChunks
        );
    }

    /**
     * Test createBatch with finalizedL1MsgIndex smaller than parent
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateBatchWithSmallerFinalizedIndex() {
        long smallerIndex = testParentBatchWrapper.getTotalL1MessagePopped() - 1;

        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                smallerIndex,
                testChunks
        );
    }

    /**
     * Test createBatch with negative finalizedL1MsgIndex difference
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateBatchWithNegativeDifference() {
        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                0L, // Less than parent's 5
                testChunks
        );
    }

    /**
     * Test createBatch with null chunks
     * Should throw NullPointerException due to @NonNull
     */
    @Test(expected = NullPointerException.class)
    public void testCreateBatchWithNullChunks() {
        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                null
        );
    }

    /**
     * Test createBatch with empty chunks list
     * Should throw RuntimeException as batch requires at least one chunk
     */
    @Test(expected = RuntimeException.class)
    public void testCreateBatchWithEmptyChunks() {
        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                Collections.emptyList()
        );
    }

    /**
     * Test getBatchIndex with null batch
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetBatchIndexWithNullBatch() {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.getBatchIndex();
    }

    /**
     * Test getBatchHeader with null batch
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetBatchHeaderWithNullBatch() {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.getBatchHeader();
    }

    /**
     * Test getChunks with null batch
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetChunksWithNullBatch() {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.getChunks();
    }

    /**
     * Test getStartBlockNumber with null batch and null startBlockNumber
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetStartBlockNumberWithNullBatch() {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.getStartBlockNumber();
    }

    /**
     * Test getEndBlockNumber with null batch and null endBlockNumber
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetEndBlockNumberWithNullBatch() {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.getEndBlockNumber();
    }

    // ==================== Edge Cases ====================

    /**
     * Test createBatch with chunks starting from non-zero index
     * Should throw RuntimeException as first chunk must have index 0
     */
    @Test(expected = RuntimeException.class)
    public void testCreateBatchWithNonZeroStartIndex() {
        List<ChunkWrapper> badChunks = new ArrayList<>();
        badChunks.add(createTestChunk(testBatchIndex, 1)); // Starts from 1 instead of 0

        BatchWrapper.createBatch(
                testBatchVersion,
                testBatchIndex,
                testParentBatchWrapper,
                testPostStateRoot,
                testL1MsgRollingHash,
                testL2MsgRoot,
                testFinalizedL1MsgIndex,
                badChunks
        );
    }

    /**
     * Test l1MessagePopped calculation
     * Should correctly calculate difference
     */
    @Test
    public void testL1MessagePoppedCalculation() {
        long[] testCases = {5L, 10L, 20L, 100L, 1000L};

        for (long finalizedIndex : testCases) {
            if (finalizedIndex >= testParentBatchWrapper.getTotalL1MessagePopped()) {
                BatchWrapper wrapper = BatchWrapper.createBatch(
                        testBatchVersion,
                        testBatchIndex,
                        testParentBatchWrapper,
                        testPostStateRoot,
                        testL1MsgRollingHash,
                        testL2MsgRoot,
                        finalizedIndex,
                        testChunks
                );

                long expected = finalizedIndex - testParentBatchWrapper.getTotalL1MessagePopped();
                assertEquals(expected, wrapper.getL1MessagePopped());
            }
        }
    }

    /**
     * Test with different batch versions
     * Should work with both V0 and V1
     */
    @Test
    public void testWithDifferentBatchVersions() {
        BatchVersionEnum[] versions = {BatchVersionEnum.BATCH_V0, BatchVersionEnum.BATCH_V1};

        for (BatchVersionEnum version : versions) {
            BatchWrapper wrapper = BatchWrapper.createBatch(
                    version,
                    testBatchIndex,
                    testParentBatchWrapper,
                    testPostStateRoot,
                    testL1MsgRollingHash,
                    testL2MsgRoot,
                    testFinalizedL1MsgIndex,
                    testChunks
            );

            assertNotNull(wrapper);
            assertNotNull(wrapper.getBatch());
        }
    }
}
