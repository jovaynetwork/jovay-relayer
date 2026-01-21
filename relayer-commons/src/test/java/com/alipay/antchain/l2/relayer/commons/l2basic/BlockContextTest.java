package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BlockContext
 */
public class BlockContextTest {

    private BlockContext blockContext;

    @Before
    public void setUp() {
        blockContext = new BlockContext();
    }

    // ==================== Positive Tests ====================

    /**
     * Test default constructor
     * Should create instance with null/zero values
     */
    @Test
    public void testDefaultConstructor() {
        BlockContext context = new BlockContext();

        assertNotNull(context);
    }

    /**
     * Test all args constructor
     * Should create instance with specified values
     */
    @Test
    public void testAllArgsConstructor() {
        BlockContext context = new BlockContext(
                1L,
                BigInteger.valueOf(100),
                System.currentTimeMillis(),
                BigInteger.valueOf(1000000000L),
                BigInteger.valueOf(30000000L),
                10,
                5
        );

        assertNotNull(context);
        assertEquals(1L, context.getSpecVersion());
        assertEquals(BigInteger.valueOf(100), context.getBlockNumber());
        assertEquals(BigInteger.valueOf(1000000000L), context.getBaseFee());
        assertEquals(BigInteger.valueOf(30000000L), context.getGasLimit());
        assertEquals(10, context.getNumTransactions());
        assertEquals(5, context.getNumL1Messages());
    }

    /**
     * Test builder pattern
     * Should create instance with specified values
     */
    @Test
    public void testBuilder() {
        BlockContext context = BlockContext.builder()
                .specVersion(2L)
                .blockNumber(BigInteger.valueOf(200))
                .timestamp(1234567890L)
                .baseFee(BigInteger.valueOf(2000000000L))
                .gasLimit(BigInteger.valueOf(40000000L))
                .numTransactions(20)
                .numL1Messages(10)
                .build();

        assertNotNull(context);
        assertEquals(2L, context.getSpecVersion());
        assertEquals(BigInteger.valueOf(200), context.getBlockNumber());
        assertEquals(1234567890L, context.getTimestamp());
        assertEquals(BigInteger.valueOf(2000000000L), context.getBaseFee());
        assertEquals(BigInteger.valueOf(40000000L), context.getGasLimit());
        assertEquals(20, context.getNumTransactions());
        assertEquals(10, context.getNumL1Messages());
    }

    /**
     * Test serialize method
     * Should return byte array of correct size
     */
    @Test
    public void testSerialize() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        byte[] serialized = context.serialize();

        assertNotNull(serialized);
        assertEquals(BlockContext.BLOCK_CONTEXT_SIZE, serialized.length);
        assertEquals(40, serialized.length);
    }

    /**
     * Test deserializeFrom method
     * Should correctly deserialize from byte array
     */
    @Test
    public void testDeserializeFrom() {
        BlockContext original = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1234567890L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(10)
                .numL1Messages(5)
                .build();

        byte[] serialized = original.serialize();
        BlockContext deserialized = BlockContext.deserializeFrom(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getSpecVersion(), deserialized.getSpecVersion());
        assertEquals(original.getBlockNumber(), deserialized.getBlockNumber());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
        assertEquals(original.getBaseFee(), deserialized.getBaseFee());
        assertEquals(original.getGasLimit(), deserialized.getGasLimit());
        assertEquals(original.getNumTransactions(), deserialized.getNumTransactions());
        assertEquals(original.getNumL1Messages(), deserialized.getNumL1Messages());
    }

    /**
     * Test serialize and deserialize round trip
     * Should preserve all values
     */
    @Test
    public void testSerializeDeserializeRoundTrip() {
        BlockContext original = BlockContext.builder()
                .specVersion(3L)
                .blockNumber(BigInteger.valueOf(999))
                .timestamp(9876543210L)
                .baseFee(BigInteger.valueOf(5000000000L))
                .gasLimit(BigInteger.valueOf(50000000L))
                .numTransactions(100)
                .numL1Messages(50)
                .build();

        byte[] serialized = original.serialize();
        BlockContext deserialized = BlockContext.deserializeFrom(serialized);

        assertEquals(original.getSpecVersion(), deserialized.getSpecVersion());
        assertEquals(original.getBlockNumber(), deserialized.getBlockNumber());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
        assertEquals(original.getBaseFee(), deserialized.getBaseFee());
        assertEquals(original.getGasLimit(), deserialized.getGasLimit());
        assertEquals(original.getNumTransactions(), deserialized.getNumTransactions());
        assertEquals(original.getNumL1Messages(), deserialized.getNumL1Messages());
    }

    /**
     * Test BLOCK_CONTEXT_SIZE constant
     * Should be 40 bytes
     */
    @Test
    public void testBlockContextSize() {
        assertEquals(40, BlockContext.BLOCK_CONTEXT_SIZE);
    }

    /**
     * Test getter and setter for specVersion
     */
    @Test
    public void testSpecVersionGetterSetter() {
        blockContext.setSpecVersion(5L);
        assertEquals(5L, blockContext.getSpecVersion());
    }

    /**
     * Test getter and setter for blockNumber
     */
    @Test
    public void testBlockNumberGetterSetter() {
        BigInteger blockNumber = BigInteger.valueOf(12345);
        blockContext.setBlockNumber(blockNumber);
        assertEquals(blockNumber, blockContext.getBlockNumber());
    }

    /**
     * Test getter and setter for timestamp
     */
    @Test
    public void testTimestampGetterSetter() {
        long timestamp = System.currentTimeMillis();
        blockContext.setTimestamp(timestamp);
        assertEquals(timestamp, blockContext.getTimestamp());
    }

    /**
     * Test getter and setter for baseFee
     */
    @Test
    public void testBaseFeeGetterSetter() {
        BigInteger baseFee = BigInteger.valueOf(3000000000L);
        blockContext.setBaseFee(baseFee);
        assertEquals(baseFee, blockContext.getBaseFee());
    }

    /**
     * Test getter and setter for gasLimit
     */
    @Test
    public void testGasLimitGetterSetter() {
        BigInteger gasLimit = BigInteger.valueOf(60000000L);
        blockContext.setGasLimit(gasLimit);
        assertEquals(gasLimit, blockContext.getGasLimit());
    }

    /**
     * Test getter and setter for numTransactions
     */
    @Test
    public void testNumTransactionsGetterSetter() {
        blockContext.setNumTransactions(50);
        assertEquals(50, blockContext.getNumTransactions());
    }

    /**
     * Test getter and setter for numL1Messages
     */
    @Test
    public void testNumL1MessagesGetterSetter() {
        blockContext.setNumL1Messages(25);
        assertEquals(25, blockContext.getNumL1Messages());
    }

    /**
     * Test with zero values
     * Should handle zero values correctly
     */
    @Test
    public void testWithZeroValues() {
        BlockContext context = BlockContext.builder()
                .specVersion(0L)
                .blockNumber(BigInteger.ZERO)
                .timestamp(0L)
                .baseFee(BigInteger.ZERO)
                .gasLimit(BigInteger.ZERO)
                .numTransactions(0)
                .numL1Messages(0)
                .build();

        byte[] serialized = context.serialize();
        BlockContext deserialized = BlockContext.deserializeFrom(serialized);

        assertEquals(0L, deserialized.getSpecVersion());
        assertEquals(BigInteger.ZERO, deserialized.getBlockNumber());
        assertEquals(0L, deserialized.getTimestamp());
        assertEquals(BigInteger.ZERO, deserialized.getBaseFee());
        assertEquals(BigInteger.ZERO, deserialized.getGasLimit());
        assertEquals(0, deserialized.getNumTransactions());
        assertEquals(0, deserialized.getNumL1Messages());
    }

    /**
     * Test with maximum values
     * Should handle large values correctly
     */
    @Test
    public void testWithMaximumValues() {
        BlockContext context = BlockContext.builder()
                .specVersion(Integer.MAX_VALUE)
                .blockNumber(new BigInteger("18446744073709551615")) // Max uint64
                .timestamp(Long.MAX_VALUE)
                .baseFee(new BigInteger("18446744073709551615"))
                .gasLimit(new BigInteger("18446744073709551615"))
                .numTransactions(65535) // Max uint16
                .numL1Messages(65535)
                .build();

        byte[] serialized = context.serialize();

        assertNotNull(serialized);
        assertEquals(BlockContext.BLOCK_CONTEXT_SIZE, serialized.length);
    }

    /**
     * Test serialize consistency
     * Multiple serializations should produce same result
     */
    @Test
    public void testSerializeConsistency() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        byte[] serialized1 = context.serialize();
        byte[] serialized2 = context.serialize();

        assertArrayEquals(serialized1, serialized2);
    }

    /**
     * Test builder with partial fields
     */
    @Test
    public void testBuilderWithPartialFields() {
        BlockContext context = BlockContext.builder()
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .build();

        assertNotNull(context);
        assertEquals(BigInteger.valueOf(100), context.getBlockNumber());
        assertEquals(1000000L, context.getTimestamp());
    }

    // ==================== Negative Tests ====================

    /**
     * Test deserializeFrom with null array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromNull() {
        BlockContext.deserializeFrom(null);
    }

    /**
     * Test deserializeFrom with invalid length
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromInvalidLength() {
        byte[] invalidData = new byte[30]; // Less than BLOCK_CONTEXT_SIZE
        BlockContext.deserializeFrom(invalidData);
    }

    /**
     * Test deserializeFrom with empty array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromEmptyArray() {
        BlockContext.deserializeFrom(new byte[0]);
    }

    /**
     * Test deserializeFrom with too short array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromTooShort() {
        byte[] shortData = new byte[10];
        BlockContext.deserializeFrom(shortData);
    }

    /**
     * Test with null values in builder
     * Should handle null values
     */
    @Test
    public void testBuilderWithNullValues() {
        BlockContext context = BlockContext.builder()
                .blockNumber(null)
                .baseFee(null)
                .gasLimit(null)
                .build();

        assertNotNull(context);
        assertNull(context.getBlockNumber());
        assertNull(context.getBaseFee());
        assertNull(context.getGasLimit());
    }

    /**
     * Test serialize with null BigInteger fields
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullBigInteger() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(null) // Null BigInteger
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        context.serialize();
    }

    /**
     * Test with negative specVersion
     * Should handle negative values
     */
    @Test
    public void testWithNegativeSpecVersion() {
        BlockContext context = BlockContext.builder()
                .specVersion(-1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        assertEquals(-1L, context.getSpecVersion());
    }

    /**
     * Test with negative timestamp
     * Should handle negative values
     */
    @Test
    public void testWithNegativeTimestamp() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(-1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        assertEquals(-1000000L, context.getTimestamp());
    }

    /**
     * Test with negative numTransactions
     * Should handle negative values
     */
    @Test
    public void testWithNegativeNumTransactions() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(-1)
                .numL1Messages(2)
                .build();

        assertEquals(-1, context.getNumTransactions());
    }

    /**
     * Test with negative numL1Messages
     * Should handle negative values
     */
    @Test
    public void testWithNegativeNumL1Messages() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(-1)
                .build();

        assertEquals(-1, context.getNumL1Messages());
    }

    /**
     * Test deserializeFrom with extra bytes
     * Should only read BLOCK_CONTEXT_SIZE bytes
     */
    @Test
    public void testDeserializeFromExtraBytes() {
        BlockContext original = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        byte[] serialized = original.serialize();
        byte[] withExtra = Arrays.copyOf(serialized, serialized.length + 10);

        BlockContext deserialized = BlockContext.deserializeFrom(withExtra);

        assertNotNull(deserialized);
        assertEquals(original.getSpecVersion(), deserialized.getSpecVersion());
        assertEquals(original.getBlockNumber(), deserialized.getBlockNumber());
    }

    /**
     * Test multiple serialize calls
     * Should produce consistent results
     */
    @Test
    public void testMultipleSerializeCalls() {
        BlockContext context = BlockContext.builder()
                .specVersion(1L)
                .blockNumber(BigInteger.valueOf(100))
                .timestamp(1000000L)
                .baseFee(BigInteger.valueOf(1000000000L))
                .gasLimit(BigInteger.valueOf(30000000L))
                .numTransactions(5)
                .numL1Messages(2)
                .build();

        byte[] first = context.serialize();
        byte[] second = context.serialize();
        byte[] third = context.serialize();

        assertArrayEquals(first, second);
        assertArrayEquals(second, third);
    }
}
