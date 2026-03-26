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

package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ChunkWrapper class
 * Tests chunk wrapper functionality
 */
public class ChunkWrapperTest {

    private BigInteger testBatchIndex;
    private long testChunkIndex;
    private byte[] testChunkHash;
    private long testGasSum;
    private byte[] testRawChunk;

    @Before
    public void setUp() {
        testBatchIndex = BigInteger.valueOf(1);
        testChunkIndex = 0L;
        testChunkHash = new byte[32];
        Arrays.fill(testChunkHash, (byte) 0x01);
        testGasSum = 1000L;

        // Create a simple raw chunk with minimal data
        testRawChunk = new byte[100];
        testRawChunk[0] = 1; // numBlocks = 1
        // Fill with block context data (58 bytes per block)
        Arrays.fill(testRawChunk, 1, 59, (byte) 0x00);
    }

    // ==================== Helper Methods ====================

    /**
     * Create a test BasicBlockTrace
     */
    private BasicBlockTrace createTestTrace(long blockNumber, long zkCycles) {
        return BasicBlockTrace.newBuilder()
                .setChainId(1L)
                .setHeader(com.alipay.antchain.l2.trace.BlockHeader.newBuilder()
                        .setNumber(blockNumber)
                        .setTimestamp(System.currentTimeMillis())
                        .setGasUsed(zkCycles)
                        .build())
                .setZkCycles(zkCycles)
                .build();
    }

    /**
     * Create test traces
     */
    private List<BasicBlockTrace> createTestTraces(int count) {
        List<BasicBlockTrace> traces = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            traces.add(createTestTrace(i, 100L * (i + 1)));
        }
        return traces;
    }

    // ==================== Constructor Tests (from serialized data) ====================

    /**
     * Test constructor with serialized data
     * Should create chunk wrapper successfully
     */
    @Test
    public void testConstructorWithSerializedData() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        assertNotNull(wrapper);
        assertEquals(testBatchIndex, wrapper.getBatchIndex());
        assertEquals(testChunkIndex, wrapper.getChunkIndex());
        assertEquals(testGasSum, wrapper.getGasSum());
        assertNotNull(wrapper.getChunk());
    }

    /**
     * Test constructor with different batch indices
     * Should handle various batch indices
     */
    @Test
    public void testConstructorWithDifferentBatchIndices() {
        BigInteger[] batchIndices = {
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.TEN,
                BigInteger.valueOf(100),
                new BigInteger("999999999999999999")
        };

        for (BigInteger batchIndex : batchIndices) {
            ChunkWrapper wrapper = new ChunkWrapper(
                    BatchVersionEnum.BATCH_V1,
                    batchIndex,
                    testChunkIndex,
                    testGasSum,
                    testRawChunk
            );

            assertEquals(batchIndex, wrapper.getBatchIndex());
        }
    }

    /**
     * Test constructor with different chunk indices
     * Should handle various chunk indices
     */
    @Test
    public void testConstructorWithDifferentChunkIndices() {
        long[] chunkIndices = {0L, 1L, 10L, 100L, 1000L};

        for (long chunkIndex : chunkIndices) {
            ChunkWrapper wrapper = new ChunkWrapper(
                    BatchVersionEnum.BATCH_V1,
                    testBatchIndex,
                    chunkIndex,
                    testGasSum,
                    testRawChunk
            );

            assertEquals(chunkIndex, wrapper.getChunkIndex());
        }
    }

    /**
     * Test constructor with zero zkCycleSum
     * Should handle zero value
     */
    @Test
    public void testConstructorWithZeroZkCycleSum() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                0L,
                testRawChunk
        );

        assertEquals(0L, wrapper.getGasSum());
    }

    /**
     * Test constructor with large zkCycleSum
     * Should handle large values
     */
    @Test
    public void testConstructorWithLargeZkCycleSum() {
        long largeGasSum = Long.MAX_VALUE;

        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                largeGasSum,
                testRawChunk
        );

        assertEquals(largeGasSum, wrapper.getGasSum());
    }

    // ==================== Constructor Tests (from traces) ====================

    /**
     * Test constructor with traces
     * Should create chunk wrapper and calculate zkCycleSum
     */
    @Test
    public void testConstructorWithTraces() {
        List<BasicBlockTrace> traces = createTestTraces(3);

        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                traces
        );

        assertNotNull(wrapper);
        assertEquals(testBatchIndex, wrapper.getBatchIndex());
        assertEquals(testChunkIndex, wrapper.getChunkIndex());
        assertNotNull(wrapper.getChunk());
        // zkCycleSum should be sum of all traces: 100 + 200 + 300 = 600
        assertEquals(600L, wrapper.getGasSum());
    }

    /**
     * Test constructor with single trace
     * Should handle single trace correctly
     */
    @Test
    public void testConstructorWithSingleTrace() {
        List<BasicBlockTrace> traces = Collections.singletonList(createTestTrace(0, 500L));

        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                traces
        );

        assertNotNull(wrapper);
        assertEquals(500L, wrapper.getGasSum());
    }

    /**
     * Test constructor with multiple traces
     * Should sum up all zkCycles
     */
    @Test
    public void testConstructorWithMultipleTraces() {
        List<BasicBlockTrace> traces = createTestTraces(10);

        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                traces
        );

        // zkCycleSum should be: 100 + 200 + ... + 1000 = 5500
        assertEquals(5500L, wrapper.getGasSum());
    }

    /**
     * Test constructor with traces having zero zkCycles
     * Should handle zero zkCycles
     */
    @Test
    public void testConstructorWithZeroZkCyclesTraces() {
        List<BasicBlockTrace> traces = new ArrayList<>();
        traces.add(createTestTrace(0, 0L));
        traces.add(createTestTrace(1, 0L));

        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                traces
        );

        assertEquals(0L, wrapper.getGasSum());
    }

    // ==================== Getter Tests ====================

    /**
     * Test getBatchIndex
     * Should return batch index
     */
    @Test
    public void testGetBatchIndex() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        assertEquals(testBatchIndex, wrapper.getBatchIndex());
    }

    /**
     * Test getChunkIndex
     * Should return chunk index
     */
    @Test
    public void testGetChunkIndex() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        assertEquals(testChunkIndex, wrapper.getChunkIndex());
    }

    /**
     * Test getChunk
     * Should return chunk object
     */
    @Test
    public void testGetChunk() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        Chunk chunk = wrapper.getChunk();
        assertNotNull(chunk);
    }

    /**
     * Test getZkCycleSum
     * Should return zkCycleSum
     */
    @Test
    public void testGetZkCycleSum() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        assertEquals(testGasSum, wrapper.getGasSum());
    }

    /**
     * Test getStartBlockNumber
     * Should return start block number from chunk
     */
    @Test
    public void testGetStartBlockNumber() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        BigInteger startBlockNumber = wrapper.getStartBlockNumber();
        assertNotNull(startBlockNumber);
        assertEquals(wrapper.getChunk().getStartBlockNumber(), startBlockNumber);
    }

    /**
     * Test getEndBlockNumber
     * Should return end block number from chunk
     */
    @Test
    public void testGetEndBlockNumber() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        BigInteger endBlockNumber = wrapper.getEndBlockNumber();
        assertNotNull(endBlockNumber);
        assertEquals(wrapper.getChunk().getEndBlockNumber(), endBlockNumber);
    }

    /**
     * Test setters
     * Should update values correctly
     */
    @Test
    public void testSetters() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        BigInteger newBatchIndex = BigInteger.valueOf(999);
        wrapper.setBatchIndex(newBatchIndex);
        assertEquals(newBatchIndex, wrapper.getBatchIndex());

        long newChunkIndex = 100L;
        wrapper.setChunkIndex(newChunkIndex);
        assertEquals(newChunkIndex, wrapper.getChunkIndex());

        long newGasSum = 5000L;
        wrapper.setGasSum(newGasSum);
        assertEquals(newGasSum, wrapper.getGasSum());
    }

    // ==================== JSON Tests ====================

    /**
     * Test toJson
     * Should serialize to JSON correctly
     */
    @Test
    public void testToJson() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        String json = wrapper.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Verify JSON contains expected fields
        JSONObject jsonObject = JSONObject.parseObject(json);
        assertEquals(testBatchIndex.toString(), jsonObject.getString("batch_index"));
        assertEquals(testChunkIndex, jsonObject.getLongValue("chunk_index"));
        assertEquals(testGasSum, jsonObject.getLongValue("gas_sum"));
        assertNotNull(jsonObject.getString("raw_chunk"));
    }

    /**
     * Test decodeFromJson
     * Should deserialize from JSON correctly
     */
    @Test
    public void testDecodeFromJson() {
        ChunkWrapper original = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        String json = original.toJson();
        ChunkWrapper decoded = ChunkWrapper.decodeFromJson(json);

        assertNotNull(decoded);
        assertEquals(original.getBatchIndex(), decoded.getBatchIndex());
        assertEquals(original.getChunkIndex(), decoded.getChunkIndex());
        assertEquals(original.getGasSum(), decoded.getGasSum());
    }

    /**
     * Test JSON encode-decode round trip
     * Should maintain data integrity
     */
    @Test
    public void testJsonEncodeDecodeRoundTrip() {
        ChunkWrapper original = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        String json = original.toJson();
        ChunkWrapper decoded = ChunkWrapper.decodeFromJson(json);
        String json2 = decoded.toJson();

        // JSON should be consistent
        assertEquals(json, json2);
    }

    /**
     * Test toJson with different values
     * Should handle various values correctly
     */
    @Test
    public void testToJsonWithDifferentValues() {
        BigInteger[] batchIndices = {BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(999)};
        long[] chunkIndices = {0L, 1L, 100L};

        for (BigInteger batchIndex : batchIndices) {
            for (long chunkIndex : chunkIndices) {
                ChunkWrapper wrapper = new ChunkWrapper(
                        BatchVersionEnum.BATCH_V1,
                        batchIndex,
                        chunkIndex,
                        testGasSum,
                        testRawChunk
                );

                String json = wrapper.toJson();
                assertNotNull(json);

                JSONObject jsonObject = JSONObject.parseObject(json);
                assertEquals(batchIndex.toString(), jsonObject.getString("batch_index"));
                assertEquals(chunkIndex, jsonObject.getLongValue("chunk_index"));
            }
        }
    }

    // ==================== Negative Tests ====================

    /**
     * Test constructor with null raw chunk
     * Should throw exception during deserialization
     */
    @Test(expected = Exception.class)
    public void testConstructorWithNullRawChunk() {
        new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                null
        );
    }

    /**
     * Test constructor with empty raw chunk
     * Should throw exception during deserialization
     */
    @Test(expected = Exception.class)
    public void testConstructorWithEmptyRawChunk() {
        new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                new byte[0]
        );
    }


    /**
     * Test decodeFromJson with invalid JSON
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeFromJsonWithInvalidJson() {
        ChunkWrapper.decodeFromJson("invalid json");
    }

    /**
     * Test decodeFromJson with missing fields
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeFromJsonWithMissingFields() {
        String incompleteJson = "{\"batch_index\":\"1\"}";
        ChunkWrapper.decodeFromJson(incompleteJson);
    }

    /**
     * Test decodeFromJson with null JSON
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeFromJsonWithNull() {
        ChunkWrapper.decodeFromJson(null);
    }

    /**
     * Test decodeFromJson with empty JSON
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDecodeFromJsonWithEmptyJson() {
        ChunkWrapper.decodeFromJson("{}");
    }

    /**
     * Test constructor with negative chunk index
     * Should create wrapper (validation is caller's responsibility)
     */
    @Test
    public void testConstructorWithNegativeChunkIndex() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                -1L,
                testGasSum,
                testRawChunk
        );

        assertEquals(-1L, wrapper.getChunkIndex());
    }

    /**
     * Test constructor with negative zkCycleSum
     * Should create wrapper (validation is caller's responsibility)
     */
    @Test
    public void testConstructorWithNegativeZkCycleSum() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                -1L,
                testRawChunk
        );

        assertEquals(-1L, wrapper.getGasSum());
    }

    /**
     * Test JSON field names consistency
     * Should use snake_case field names
     */
    @Test
    public void testJsonFieldNamesConsistency() {
        ChunkWrapper wrapper = new ChunkWrapper(
                BatchVersionEnum.BATCH_V1,
                testBatchIndex,
                testChunkIndex,
                testGasSum,
                testRawChunk
        );

        String json = wrapper.toJson();
        JSONObject jsonObject = JSONObject.parseObject(json);

        // Verify field names are in snake_case
        assertTrue(jsonObject.containsKey("batch_index"));
        assertTrue(jsonObject.containsKey("chunk_index"));
        assertTrue(jsonObject.containsKey("raw_chunk"));
        assertTrue(jsonObject.containsKey("gas_sum"));
    }
}
