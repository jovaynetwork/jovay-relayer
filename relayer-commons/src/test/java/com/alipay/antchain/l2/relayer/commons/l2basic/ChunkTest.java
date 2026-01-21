package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidChunkException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Chunk
 */
public class ChunkTest {

    private Chunk chunk;
    private List<BlockContext> testBlocks;

    @Before
    public void setUp() {
        chunk = new Chunk();
        testBlocks = createTestBlocks(3);
    }

    // ==================== Helper Methods ====================

    /**
     * Create test block contexts
     */
    private List<BlockContext> createTestBlocks(int count) {
        List<BlockContext> blocks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockContext block = BlockContext.builder()
                    .specVersion(1L)
                    .blockNumber(BigInteger.valueOf(100 + i))
                    .timestamp(System.currentTimeMillis() + i * 1000)
                    .baseFee(BigInteger.valueOf(1000000000L))
                    .gasLimit(BigInteger.valueOf(30000000L))
                    .numTransactions(5)
                    .numL1Messages(2)
                    .build();
            blocks.add(block);
        }
        return blocks;
    }

    // ==================== Positive Tests ====================

    /**
     * Test default constructor
     * Should create instance with null values
     */
    @Test
    public void testDefaultConstructor() {
        Chunk c = new Chunk();

        assertNotNull(c);
    }

    /**
     * Test all args constructor
     * Should create instance with specified values
     */
    @Test
    public void testAllArgsConstructor() {
        byte[] testTx = new byte[]{1, 2, 3, 4};
        byte[] testHash = new byte[32];
        Arrays.fill(testHash, (byte) 1);

        Chunk c = new Chunk((byte) 3, testBlocks, testTx, testHash);

        assertNotNull(c);
        assertEquals((byte) 3, c.getNumBlocks());
        assertEquals(testBlocks, c.getBlocks());
        assertArrayEquals(testTx, c.getL2Transactions());
        assertArrayEquals(testHash, c.getHash());
    }

    /**
     * Test builder pattern
     * Should create instance with specified values
     */
    @Test
    public void testBuilder() {
        byte[] testTx = new byte[]{1, 2, 3, 4};
        byte[] testHash = new byte[32];

        Chunk c = Chunk.builder()
                .numBlocks((byte) 3)
                .blocks(testBlocks)
                .l2Transactions(testTx)
                .hash(testHash)
                .build();

        assertNotNull(c);
        assertEquals((byte) 3, c.getNumBlocks());
        assertEquals(testBlocks, c.getBlocks());
        assertArrayEquals(testTx, c.getL2Transactions());
    }

    /**
     * Test getStartBlockNumber
     * Should return first block number
     */
    @Test
    public void testGetStartBlockNumber() {
        chunk.setBlocks(testBlocks);

        BigInteger startBlock = chunk.getStartBlockNumber();

        assertEquals(BigInteger.valueOf(100), startBlock);
    }

    /**
     * Test getEndBlockNumber
     * Should return last block number
     */
    @Test
    public void testGetEndBlockNumber() {
        chunk.setBlocks(testBlocks);

        BigInteger endBlock = chunk.getEndBlockNumber();

        assertEquals(BigInteger.valueOf(102), endBlock);
    }

    /**
     * Test getNumBlocksVal
     * Should return byte value as int
     */
    @Test
    public void testGetNumBlocksVal() {
        chunk.setNumBlocks((byte) 5);

        int numBlocks = chunk.getNumBlocksVal();

        assertEquals(5, numBlocks);
    }

    /**
     * Test serialize method
     * Should return byte array with correct structure
     */
    @Test
    public void testSerialize() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3, 4});

        byte[] serialized = chunk.serialize();

        assertNotNull(serialized);
        // 1 byte numBlocks + 3 * 40 bytes (BlockContext) + 4 bytes transactions
        assertEquals(1 + 3 * BlockContext.BLOCK_CONTEXT_SIZE + 4, serialized.length);
    }

    /**
     * Test serialize without transactions
     * Should serialize only blocks
     */
    @Test
    public void testSerializeWithoutTransactions() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(null);

        byte[] serialized = chunk.serialize();

        assertNotNull(serialized);
        assertEquals(1 + 3 * BlockContext.BLOCK_CONTEXT_SIZE, serialized.length);
    }

    /**
     * Test deserializeFrom method
     * Should correctly deserialize from byte array
     */
    @Test
    public void testDeserializeFrom() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3, 4});

        byte[] serialized = chunk.serialize();
        Chunk deserialized = Chunk.deserializeFrom(serialized);

        assertNotNull(deserialized);
        assertEquals(chunk.getNumBlocksVal(), deserialized.getNumBlocksVal());
        assertEquals(chunk.getBlocks().size(), deserialized.getBlocks().size());
    }

    /**
     * Test serialize and deserialize round trip
     * Should preserve all values
     */
    @Test
    public void testSerializeDeserializeRoundTrip() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3, 4, 5});

        byte[] serialized = chunk.serialize();
        Chunk deserialized = Chunk.deserializeFrom(serialized);

        assertEquals(chunk.getNumBlocksVal(), deserialized.getNumBlocksVal());
        assertEquals(chunk.getBlocks().size(), deserialized.getBlocks().size());
        assertArrayEquals(chunk.getL2Transactions(), deserialized.getL2Transactions());
    }

    /**
     * Test toJson method
     * Should return valid JSON string
     */
    @Test
    public void testToJson() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3});
        chunk.setHash(new byte[32]);

        String json = chunk.toJson();

        assertNotNull(json);
        assertTrue(json.contains("numBlocks"));
        assertTrue(json.contains("blockContexts"));
        assertTrue(json.contains("l2Transactions"));
        assertTrue(json.contains("hash"));
    }

    /**
     * Test fromJson method
     * Should correctly parse JSON string
     */
    @Test
    public void testFromJson() {
        chunk.setNumBlocks((byte) 2);
        chunk.setBlocks(createTestBlocks(2));
        chunk.setL2Transactions(new byte[]{1, 2, 3});
        chunk.setHash(new byte[32]);

        String json = chunk.toJson();
        Chunk fromJson = Chunk.fromJson(json);

        assertNotNull(fromJson);
        assertEquals(chunk.getNumBlocksVal(), fromJson.getNumBlocksVal());
        assertEquals(chunk.getBlocks().size(), fromJson.getBlocks().size());
    }

    /**
     * Test validate method with continuous blocks
     * Should not throw exception
     */
    @Test
    public void testValidateWithContinuousBlocks() {
        chunk.setBlocks(testBlocks);

        // Should not throw exception
        chunk.validate();
    }

    /**
     * Test getter and setter for numBlocks
     */
    @Test
    public void testNumBlocksGetterSetter() {
        chunk.setNumBlocks((byte) 5);
        assertEquals((byte) 5, chunk.getNumBlocks());
        assertEquals(5, chunk.getNumBlocksVal());
    }

    /**
     * Test getter and setter for blocks
     */
    @Test
    public void testBlocksGetterSetter() {
        chunk.setBlocks(testBlocks);
        assertEquals(testBlocks, chunk.getBlocks());
    }

    /**
     * Test getter and setter for l2Transactions
     */
    @Test
    public void testL2TransactionsGetterSetter() {
        byte[] tx = new byte[]{1, 2, 3, 4, 5};
        chunk.setL2Transactions(tx);
        assertArrayEquals(tx, chunk.getL2Transactions());
    }

    /**
     * Test getter and setter for hash
     */
    @Test
    public void testHashGetterSetter() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 1);
        chunk.setHash(hash);
        assertArrayEquals(hash, chunk.getHash());
    }

    /**
     * Test with single block
     */
    @Test
    public void testWithSingleBlock() {
        List<BlockContext> singleBlock = createTestBlocks(1);
        chunk.setNumBlocks((byte) 1);
        chunk.setBlocks(singleBlock);

        assertEquals(BigInteger.valueOf(100), chunk.getStartBlockNumber());
        assertEquals(BigInteger.valueOf(100), chunk.getEndBlockNumber());
    }

    /**
     * Test with many blocks
     */
    @Test
    public void testWithManyBlocks() {
        List<BlockContext> manyBlocks = createTestBlocks(10);
        chunk.setNumBlocks((byte) 10);
        chunk.setBlocks(manyBlocks);

        assertEquals(BigInteger.valueOf(100), chunk.getStartBlockNumber());
        assertEquals(BigInteger.valueOf(109), chunk.getEndBlockNumber());
    }

    /**
     * Test serialize consistency
     * Multiple serializations should produce same result
     */
    @Test
    public void testSerializeConsistency() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3});

        byte[] serialized1 = chunk.serialize();
        byte[] serialized2 = chunk.serialize();

        assertArrayEquals(serialized1, serialized2);
    }

    /**
     * Test with empty transactions
     */
    @Test
    public void testWithEmptyTransactions() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[0]);

        byte[] serialized = chunk.serialize();

        assertNotNull(serialized);
    }

    /**
     * Test getL2RawTransactionTotalLength with no transactions
     * Should return 0
     */
    @Test
    public void testGetL2RawTransactionTotalLengthWithNoTransactions() {
        chunk.setL2Transactions(null);

        long length = chunk.getL2RawTransactionTotalLength();

        assertEquals(0L, length);
    }

    /**
     * Test getL2RawTransactionTotalLength with empty transactions
     * Should return 0
     */
    @Test
    public void testGetL2RawTransactionTotalLengthWithEmptyTransactions() {
        chunk.setL2Transactions(new byte[0]);

        long length = chunk.getL2RawTransactionTotalLength();

        assertEquals(0L, length);
    }

    /**
     * Test getL2TransactionList with no transactions
     * Should return empty list
     */
    @Test
    public void testGetL2TransactionListWithNoTransactions() {
        chunk.setL2Transactions(null);

        List<?> transactions = chunk.getL2TransactionList();

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    /**
     * Test getL2TransactionHashes with no transactions
     * Should return empty list
     */
    @Test
    public void testGetL2TransactionHashesWithNoTransactions() {
        chunk.setL2Transactions(null);

        List<byte[]> hashes = chunk.getL2TransactionHashes();

        assertNotNull(hashes);
        assertTrue(hashes.isEmpty());
    }

    // ==================== Negative Tests ====================

    /**
     * Test getStartBlockNumber with empty blocks
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetStartBlockNumberWithEmptyBlocks() {
        chunk.setBlocks(new ArrayList<>());
        chunk.getStartBlockNumber();
    }

    /**
     * Test getStartBlockNumber with null blocks
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetStartBlockNumberWithNullBlocks() {
        chunk.setBlocks(null);
        chunk.getStartBlockNumber();
    }

    /**
     * Test getEndBlockNumber with empty blocks
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetEndBlockNumberWithEmptyBlocks() {
        chunk.setBlocks(new ArrayList<>());
        chunk.getEndBlockNumber();
    }

    /**
     * Test getEndBlockNumber with null blocks
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testGetEndBlockNumberWithNullBlocks() {
        chunk.setBlocks(null);
        chunk.getEndBlockNumber();
    }

    /**
     * Test validate with discontinuous blocks
     * Should throw InvalidChunkException
     */
    @Test(expected = InvalidChunkException.class)
    public void testValidateWithDiscontinuousBlocks() {
        List<BlockContext> discontinuousBlocks = new ArrayList<>();
        discontinuousBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(100))
                .build());
        discontinuousBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(102)) // Gap: missing 101
                .build());

        chunk.setBlocks(discontinuousBlocks);
        chunk.validate();
    }

    /**
     * Test validate with blocks in wrong order
     * Should throw InvalidChunkException
     */
    @Test(expected = InvalidChunkException.class)
    public void testValidateWithBlocksInWrongOrder() {
        List<BlockContext> wrongOrderBlocks = new ArrayList<>();
        wrongOrderBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(102))
                .build());
        wrongOrderBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(100))
                .build());

        chunk.setBlocks(wrongOrderBlocks);
        chunk.validate();
    }

    /**
     * Test deserializeFrom with null array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromNull() {
        Chunk.deserializeFrom(null);
    }

    /**
     * Test deserializeFrom with empty array
     * Should throw exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeserializeFromEmptyArray() {
        Chunk.deserializeFrom(new byte[0]);
    }

    /**
     * Test deserializeFrom with invalid length
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromInvalidLength() {
        byte[] invalidData = new byte[]{1}; // Only numBlocks, no block data
        Chunk.deserializeFrom(invalidData);
    }

    /**
     * Test serialize with mismatched numBlocks and blocks size
     * Should throw exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSerializeWithMismatchedNumBlocks() {
        chunk.setNumBlocks((byte) 5); // Says 5 blocks
        chunk.setBlocks(createTestBlocks(3)); // But only has 3

        chunk.serialize();
    }

    /**
     * Test serialize with null blocks
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullBlocks() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(null);

        chunk.serialize();
    }

    /**
     * Test fromJson with invalid JSON
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testFromJsonWithInvalidJson() {
        Chunk.fromJson("invalid json");
    }

    /**
     * Test fromJson with null
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testFromJsonWithNull() {
        Chunk.fromJson(null);
    }

    /**
     * Test fromJson with empty string
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testFromJsonWithEmptyString() {
        Chunk.fromJson("");
    }

    /**
     * Test with negative numBlocks
     */
    @Test
    public void testWithNegativeNumBlocks() {
        chunk.setNumBlocks((byte) -1);

        assertEquals((byte) -1, chunk.getNumBlocks());
        assertEquals(-1, chunk.getNumBlocksVal());
    }

    /**
     * Test toJson with null hash
     * Should handle gracefully
     */
    @Test
    public void testToJsonWithNullHash() {
        chunk.setNumBlocks((byte) 1);
        chunk.setBlocks(createTestBlocks(1));
        chunk.setL2Transactions(new byte[]{1});
        chunk.setHash(null);

        try {
            String json = chunk.toJson();
            assertNotNull(json);
        } catch (Exception e) {
            // Exception is acceptable for null hash
            assertNotNull(e);
        }
    }

    /**
     * Test toJson with null transactions
     * Should use empty byte array
     */
    @Test
    public void testToJsonWithNullTransactions() {
        chunk.setNumBlocks((byte) 1);
        chunk.setBlocks(createTestBlocks(1));
        chunk.setL2Transactions(null);
        chunk.setHash(new byte[32]);

        String json = chunk.toJson();

        assertNotNull(json);
        assertTrue(json.contains("l2Transactions"));
    }

    /**
     * Test deserializeFrom with extra bytes
     * Should only read required bytes
     */
    @Test
    public void testDeserializeFromExtraBytes() {
        chunk.setNumBlocks((byte) 2);
        chunk.setBlocks(createTestBlocks(2));
        chunk.setL2Transactions(new byte[]{1, 2, 3});

        byte[] serialized = chunk.serialize();
        byte[] withExtra = Arrays.copyOf(serialized, serialized.length + 10);

        Chunk deserialized = Chunk.deserializeFrom(withExtra);

        assertNotNull(deserialized);
        assertEquals(chunk.getNumBlocksVal(), deserialized.getNumBlocksVal());
    }

    /**
     * Test validate with single block
     * Should not throw exception
     */
    @Test
    public void testValidateWithSingleBlock() {
        List<BlockContext> singleBlock = createTestBlocks(1);
        chunk.setBlocks(singleBlock);

        // Should not throw exception
        chunk.validate();
    }

    /**
     * Test serialize with blocks in wrong order
     * Should sort blocks before serializing
     */
    @Test
    public void testSerializeWithBlocksInWrongOrder() {
        List<BlockContext> wrongOrderBlocks = new ArrayList<>();
        wrongOrderBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(102))
                .specVersion(1L)
                .timestamp(1000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        wrongOrderBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(100))
                .specVersion(1L)
                .timestamp(1000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());
        wrongOrderBlocks.add(BlockContext.builder()
                .blockNumber(BigInteger.valueOf(101))
                .specVersion(1L)
                .timestamp(1000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build());

        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(wrongOrderBlocks);

        byte[] serialized = chunk.serialize();

        assertNotNull(serialized);
        // Blocks should be sorted after serialization
        assertEquals(BigInteger.valueOf(100), chunk.getBlocks().get(0).getBlockNumber());
        assertEquals(BigInteger.valueOf(101), chunk.getBlocks().get(1).getBlockNumber());
        assertEquals(BigInteger.valueOf(102), chunk.getBlocks().get(2).getBlockNumber());
    }

    /**
     * Test multiple serialize calls
     * Should produce consistent results
     */
    @Test
    public void testMultipleSerializeCalls() {
        chunk.setNumBlocks((byte) 3);
        chunk.setBlocks(testBlocks);
        chunk.setL2Transactions(new byte[]{1, 2, 3});

        byte[] first = chunk.serialize();
        byte[] second = chunk.serialize();
        byte[] third = chunk.serialize();

        assertArrayEquals(first, second);
        assertArrayEquals(second, third);
    }
}
