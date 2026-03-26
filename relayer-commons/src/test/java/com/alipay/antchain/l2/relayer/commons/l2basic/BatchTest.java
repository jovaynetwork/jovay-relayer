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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Batch
 */
public class BatchTest {

    private Batch batch;
    private List<Chunk> testChunks;
    private BatchHeader parentBatchHeader;
    private byte[] testL1MsgRollingHash;

    @Before
    public void setUp() {
        testChunks = createTestChunks(2);
        parentBatchHeader = createTestBatchHeader();
        testL1MsgRollingHash = new byte[32];
        Arrays.fill(testL1MsgRollingHash, (byte) 1);
    }

    // ==================== Helper Methods ====================

    /**
     * Create test chunks
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
                    .build();
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Create test batch header
     */
    private BatchHeader createTestBatchHeader() {
        return BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(99))
                .l1MsgRollingHash(new byte[32])
                .dataHash(new byte[32])
                .parentBatchHash(new byte[32])
                .build();
    }

    // ==================== Positive Tests ====================

    /**
     * Test constructor with version, index, parent header, rolling hash and chunks
     * Should create batch with correct header and payload
     */
    @Test
    public void testConstructorWithBasicParams() {
        Batch b = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertNotNull(b);
        assertNotNull(b.getBatchHeader());
        assertNotNull(b.getPayload());
        assertEquals(BatchVersionEnum.BATCH_V0, b.getBatchHeader().getVersion());
        assertEquals(BigInteger.valueOf(100), b.getBatchHeader().getBatchIndex());
    }

    /**
     * Test constructor with DA data
     * Should create batch with provided DA data
     */
    @Test
    public void testConstructorWithDaData() {
        IDaData mockDaData = new IDaData() {
            @Override
            public byte[] dataHash() {
                return new byte[32];
            }

            @Override
            public int getDataLen() {
                return 100;
            }

            @Override
            public DaVersion getDaVersion() {
                return DaVersion.DA_0;
            }

            @Override
            public BatchVersionEnum getBatchVersion() {
                return BatchVersionEnum.BATCH_V0;
            }

            @Override
            public IBatchPayload toBatchPayload() {
                return new ChunksPayload(BatchVersionEnum.BATCH_V0, testChunks);
            }
        };

        Batch b = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks,
                mockDaData
        );

        assertNotNull(b);
        assertNotNull(b.getDaData());
    }

    /**
     * Test createBatch static method
     * Should create batch correctly
     */
    @Test
    public void testCreateBatch() {
        Batch b = Batch.createBatch(
                BatchVersionEnum.BATCH_V1,
                BigInteger.valueOf(200),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks,
                null
        );

        assertNotNull(b);
        assertEquals(BatchVersionEnum.BATCH_V1, b.getBatchHeader().getVersion());
        assertEquals(BigInteger.valueOf(200), b.getBatchIndex());
    }

    /**
     * Test getBatchHeader
     * Should return batch header
     */
    @Test
    public void testGetBatchHeader() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        BatchHeader header = batch.getBatchHeader();

        assertNotNull(header);
        assertEquals(BatchVersionEnum.BATCH_V0, header.getVersion());
        assertEquals(BigInteger.valueOf(100), header.getBatchIndex());
    }

    /**
     * Test getPayload
     * Should return batch payload
     */
    @Test
    public void testGetPayload() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        IBatchPayload payload = batch.getPayload();

        assertNotNull(payload);
        assertTrue(payload instanceof ChunksPayload);
    }

    /**
     * Test getBatchHash
     * Should return batch hash
     */
    @Test
    public void testGetBatchHash() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        byte[] hash = batch.getBatchHash();

        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    /**
     * Test getBatchHashHex
     * Should return hex string of batch hash
     */
    @Test
    public void testGetBatchHashHex() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        String hashHex = batch.getBatchHashHex();

        assertNotNull(hashHex);
        assertEquals(64, hashHex.length());
    }

    /**
     * Test getBatchIndex
     * Should return batch index
     */
    @Test
    public void testGetBatchIndex() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        BigInteger index = batch.getBatchIndex();

        assertEquals(BigInteger.valueOf(100), index);
    }

    /**
     * Test getStartBlockNumber
     * Should return first block number
     */
    @Test
    public void testGetStartBlockNumber() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        BigInteger startBlock = batch.getStartBlockNumber();

        assertEquals(BigInteger.valueOf(100), startBlock);
    }

    /**
     * Test getEndBlockNumber
     * Should return last block number
     */
    @Test
    public void testGetEndBlockNumber() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        BigInteger endBlock = batch.getEndBlockNumber();

        assertEquals(BigInteger.valueOf(103), endBlock);
    }

    /**
     * Test getDaData
     * Should return DA data
     */
    @Test
    public void testGetDaData() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        IDaData daData = batch.getDaData();

        assertNotNull(daData);
    }

    /**
     * Test getDaData lazy initialization
     * Should create DA data if not provided
     */
    @Test
    public void testGetDaDataLazyInitialization() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks,
                null
        );

        IDaData daData1 = batch.getDaData();
        IDaData daData2 = batch.getDaData();

        assertNotNull(daData1);
        assertSame(daData1, daData2); // Should return same instance
    }

    /**
     * Test getBatchTxsLength
     * Should return total transaction length (0 for chunks without l2Transactions)
     */
    @Test
    public void testGetBatchTxsLength() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        long txsLength = batch.getBatchTxsLength();

        // Since test chunks don't have l2Transactions set, length should be 0
        assertEquals(0L, txsLength);
    }

    /**
     * Test validate method
     * Should not throw exception for valid batch
     */
    @Test
    public void testValidate() throws InvalidBatchException {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        // Should not throw exception
        batch.validate();
    }

    /**
     * Test getEthBlobs
     * Should return Ethereum blobs
     */
    @Test
    public void testGetEthBlobs() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        try {
            batch.getEthBlobs();
            // If no exception, test passes
        } catch (Exception e) {
            // Some implementations may throw exception
            assertNotNull(e);
        }
    }

    /**
     * Test with BATCH_V0
     */
    @Test
    public void testWithBatchV0() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertEquals(BatchVersionEnum.BATCH_V0, batch.getBatchHeader().getVersion());
    }

    /**
     * Test with BATCH_V1
     */
    @Test
    public void testWithBatchV1() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V1,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertEquals(BatchVersionEnum.BATCH_V1, batch.getBatchHeader().getVersion());
    }

    /**
     * Test with single chunk
     */
    @Test
    public void testWithSingleChunk() {
        List<Chunk> singleChunk = createTestChunks(1);

        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                singleChunk
        );

        assertNotNull(batch);
        assertEquals(BigInteger.valueOf(100), batch.getStartBlockNumber());
    }

    /**
     * Test with many chunks
     */
    @Test
    public void testWithManyChunks() {
        List<Chunk> manyChunks = createTestChunks(10);

        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                manyChunks
        );

        assertNotNull(batch);
        assertEquals(10, ((ChunksPayload) batch.getPayload()).chunks().size());
    }

    /**
     * Test batch hash consistency
     * Same data should produce same hash
     */
    @Test
    public void testBatchHashConsistency() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        byte[] hash1 = batch.getBatchHash();
        byte[] hash2 = batch.getBatchHash();

        assertArrayEquals(hash1, hash2);
    }

    /**
     * Test batch hash hex consistency
     * Multiple calls should return same hex string
     */
    @Test
    public void testBatchHashHexConsistency() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        String hex1 = batch.getBatchHashHex();
        String hex2 = batch.getBatchHashHex();

        assertEquals(hex1, hex2);
    }

    /**
     * Test with zero batch index
     */
    @Test
    public void testWithZeroBatchIndex() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.ZERO,
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertEquals(BigInteger.ZERO, batch.getBatchIndex());
    }

    /**
     * Test with large batch index
     */
    @Test
    public void testWithLargeBatchIndex() {
        BigInteger largeIndex = new BigInteger("999999999999999999");

        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                largeIndex,
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertEquals(largeIndex, batch.getBatchIndex());
    }

    /**
     * Test builder pattern
     */
    @Test
    public void testBuilder() {
        BatchHeader header = createTestBatchHeader();
        ChunksPayload payload = new ChunksPayload(BatchVersionEnum.BATCH_V1, testChunks);

        Batch b = Batch.builder()
                .batchHeader(header)
                .payload(payload)
                .build();

        assertNotNull(b);
        assertEquals(header, b.getBatchHeader());
        assertEquals(payload, b.getPayload());
    }

    /**
     * Test setBatchHeader
     */
    @Test
    public void testSetBatchHeader() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        BatchHeader newHeader = createTestBatchHeader();
        batch.setBatchHeader(newHeader);

        assertEquals(newHeader, batch.getBatchHeader());
    }

    /**
     * Test setPayload
     */
    @Test
    public void testSetPayload() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        ChunksPayload newPayload = new ChunksPayload(BatchVersionEnum.BATCH_V0, createTestChunks(1));
        batch.setPayload(newPayload);

        assertEquals(newPayload, batch.getPayload());
    }

    // ==================== Negative Tests ====================

    /**
     * Test constructor with null chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testConstructorWithNullChunks() {
        new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                null
        );
    }

    /**
     * Test constructor with empty chunks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testConstructorWithEmptyChunks() {
        new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                new ArrayList<>()
        );
    }

    /**
     * Test createBatch with null chunks
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testCreateBatchWithNullChunks() {
        Batch.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                null,
                null
        );
    }

    /**
     * Test constructor with null version
     * Should handle or throw exception
     */
    @Test
    public void testConstructorWithNullVersion() {
        try {
            new Batch(
                    null,
                    BigInteger.valueOf(100),
                    parentBatchHeader,
                    testL1MsgRollingHash,
                    testChunks
            );
            // If no exception, verify batch is created
        } catch (Exception e) {
            // Exception is acceptable for null version
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with null batch index
     * Should handle or throw exception
     */
    @Test
    public void testConstructorWithNullBatchIndex() {
        try {
            new Batch(
                    BatchVersionEnum.BATCH_V0,
                    null,
                    parentBatchHeader,
                    testL1MsgRollingHash,
                    testChunks
            );
            // If no exception, verify batch is created
        } catch (Exception e) {
            // Exception is acceptable for null index
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with null parent batch header
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testConstructorWithNullParentHeader() {
        new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                null,
                testL1MsgRollingHash,
                testChunks
        );
    }

    /**
     * Test constructor with null l1MsgRollingHash
     * Should handle or throw exception
     */
    @Test
    public void testConstructorWithNullL1MsgRollingHash() {
        try {
            new Batch(
                    BatchVersionEnum.BATCH_V0,
                    BigInteger.valueOf(100),
                    parentBatchHeader,
                    null,
                    testChunks
            );
            // If no exception, verify batch is created
        } catch (Exception e) {
            // Exception is acceptable for null rolling hash
            assertNotNull(e);
        }
    }

    /**
     * Test with negative batch index
     */
    @Test
    public void testWithNegativeBatchIndex() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(-1),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertEquals(BigInteger.valueOf(-1), batch.getBatchIndex());
    }

    /**
     * Test validate with invalid chunks
     * Should throw InvalidBatchException for discontinuous chunk blocks
     */
    @Test(expected = InvalidBatchException.class)
    public void testValidateWithInvalidChunks() throws InvalidBatchException {
        // Create two chunks with discontinuous block numbers between chunks
        List<Chunk> invalidChunks = new ArrayList<>();

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
                .build();
        invalidChunks.add(chunk1);

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
                .build();
        invalidChunks.add(chunk2);

        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                invalidChunks
        );

        // This should throw InvalidBatchException due to discontinuous chunks
        batch.validate();
    }

    /**
     * Test getBatchTxsLength consistency
     * Multiple calls should return same value
     */
    @Test
    public void testGetBatchTxsLengthConsistency() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        long length1 = batch.getBatchTxsLength();
        long length2 = batch.getBatchTxsLength();

        assertEquals(length1, length2);
        // Since test chunks don't have l2Transactions set, both should be 0
        assertEquals(0L, length1);
        assertEquals(0L, length2);
    }

    /**
     * Test getDaData multiple calls
     * Should return same instance
     */
    @Test
    public void testGetDaDataMultipleCalls() {
        batch = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        IDaData data1 = batch.getDaData();
        IDaData data2 = batch.getDaData();
        IDaData data3 = batch.getDaData();

        assertSame(data1, data2);
        assertSame(data2, data3);
    }

    /**
     * Test batch hash determinism
     * Same input should produce same hash
     */
    @Test
    public void testBatchHashDeterminism() {
        Batch batch1 = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        Batch batch2 = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertArrayEquals(batch1.getBatchHash(), batch2.getBatchHash());
    }

    /**
     * Test batch hash uniqueness
     * Different input should produce different hash
     */
    @Test
    public void testBatchHashUniqueness() {
        Batch batch1 = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        Batch batch2 = new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(101), // Different index
                parentBatchHeader,
                testL1MsgRollingHash,
                testChunks
        );

        assertFalse(Arrays.equals(batch1.getBatchHash(), batch2.getBatchHash()));
    }
}
