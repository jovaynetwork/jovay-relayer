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
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BatchHeader
 */
public class BatchHeaderTest {

    private BatchHeader batchHeader;
    private byte[] testHash32;
    private byte[] testHash32_2;

    @Before
    public void setUp() {
        batchHeader = new BatchHeader();
        testHash32 = new byte[32];
        Arrays.fill(testHash32, (byte) 1);
        testHash32_2 = new byte[32];
        Arrays.fill(testHash32_2, (byte) 2);
    }

    // ==================== Positive Tests ====================

    /**
     * Test default constructor
     * Should create instance with null values
     */
    @Test
    public void testDefaultConstructor() {
        BatchHeader header = new BatchHeader();

        assertNotNull(header);
    }

    /**
     * Test all args constructor
     * Should create instance with specified values
     */
    @Test
    public void testAllArgsConstructor() {
        BatchHeader header = new BatchHeader(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(100),
                testHash32,
                testHash32_2,
                new byte[32],
                null
        );

        assertNotNull(header);
        assertEquals(BatchVersionEnum.BATCH_V0, header.getVersion());
        assertEquals(BigInteger.valueOf(100), header.getBatchIndex());
        assertArrayEquals(testHash32, header.getL1MsgRollingHash());
        assertArrayEquals(testHash32_2, header.getDataHash());
    }

    /**
     * Test builder pattern
     * Should create instance with specified values
     */
    @Test
    public void testBuilder() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V1)
                .batchIndex(BigInteger.valueOf(200))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertNotNull(header);
        assertEquals(BatchVersionEnum.BATCH_V1, header.getVersion());
        assertEquals(BigInteger.valueOf(200), header.getBatchIndex());
        assertArrayEquals(testHash32, header.getL1MsgRollingHash());
        assertArrayEquals(testHash32_2, header.getDataHash());
    }

    /**
     * Test serialize method
     * Should return byte array with correct structure
     */
    @Test
    public void testSerialize() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] serialized = header.serialize();

        assertNotNull(serialized);
        // 1 byte version + 8 bytes batchIndex + 32 bytes l1MsgRollingHash + 32 bytes dataHash + 32 bytes parentBatchHash
        assertEquals(105, serialized.length);
    }

    /**
     * Test deserializeFrom method
     * Should correctly deserialize from byte array
     */
    @Test
    public void testDeserializeFrom() {
        BatchHeader original = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] serialized = original.serialize();
        BatchHeader deserialized = BatchHeader.deserializeFrom(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getVersion(), deserialized.getVersion());
        assertEquals(original.getBatchIndex(), deserialized.getBatchIndex());
        assertArrayEquals(original.getL1MsgRollingHash(), deserialized.getL1MsgRollingHash());
        assertArrayEquals(original.getDataHash(), deserialized.getDataHash());
        assertArrayEquals(original.getParentBatchHash(), deserialized.getParentBatchHash());
    }

    /**
     * Test serialize and deserialize round trip
     * Should preserve all values
     */
    @Test
    public void testSerializeDeserializeRoundTrip() {
        BatchHeader original = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V1)
                .batchIndex(BigInteger.valueOf(999))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] serialized = original.serialize();
        BatchHeader deserialized = BatchHeader.deserializeFrom(serialized);

        assertEquals(original.getVersion(), deserialized.getVersion());
        assertEquals(original.getBatchIndex(), deserialized.getBatchIndex());
        assertArrayEquals(original.getL1MsgRollingHash(), deserialized.getL1MsgRollingHash());
        assertArrayEquals(original.getDataHash(), deserialized.getDataHash());
        assertArrayEquals(original.getParentBatchHash(), deserialized.getParentBatchHash());
    }

    /**
     * Test getHash method
     * Should compute and return hash
     */
    @Test
    public void testGetHash() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] hash = header.getHash();

        assertNotNull(hash);
        assertEquals(32, hash.length); // Keccak256 produces 32 bytes
    }

    /**
     * Test getHash caching
     * Should return same hash on multiple calls
     */
    @Test
    public void testGetHashCaching() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] hash1 = header.getHash();
        byte[] hash2 = header.getHash();

        assertSame(hash1, hash2); // Should return same instance
        assertArrayEquals(hash1, hash2);
    }

    /**
     * Test getHashHex method
     * Should return hex string representation of hash
     */
    @Test
    public void testGetHashHex() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        String hashHex = header.getHashHex();

        assertNotNull(hashHex);
        assertEquals(64, hashHex.length()); // 32 bytes = 64 hex chars
    }

    /**
     * Test toJson method
     * Should return valid JSON string
     */
    @Test
    public void testToJson() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        String json = header.toJson();

        assertNotNull(json);
        assertTrue(json.contains("version"));
        assertTrue(json.contains("batchIndex"));
        assertTrue(json.contains("dataHash"));
        assertTrue(json.contains("parentBatchHash"));
        assertTrue(json.contains("hash"));
    }

    /**
     * Test getter and setter for version
     */
    @Test
    public void testVersionGetterSetter() {
        batchHeader.setVersion(BatchVersionEnum.BATCH_V1);
        assertEquals(BatchVersionEnum.BATCH_V1, batchHeader.getVersion());
    }

    /**
     * Test getter and setter for batchIndex
     */
    @Test
    public void testBatchIndexGetterSetter() {
        BigInteger index = BigInteger.valueOf(12345);
        batchHeader.setBatchIndex(index);
        assertEquals(index, batchHeader.getBatchIndex());
    }

    /**
     * Test getter and setter for l1MsgRollingHash
     */
    @Test
    public void testL1MsgRollingHashGetterSetter() {
        batchHeader.setL1MsgRollingHash(testHash32);
        assertArrayEquals(testHash32, batchHeader.getL1MsgRollingHash());
    }

    /**
     * Test getter and setter for dataHash
     */
    @Test
    public void testDataHashGetterSetter() {
        batchHeader.setDataHash(testHash32_2);
        assertArrayEquals(testHash32_2, batchHeader.getDataHash());
    }

    /**
     * Test getter and setter for parentBatchHash
     */
    @Test
    public void testParentBatchHashGetterSetter() {
        byte[] parentHash = new byte[32];
        Arrays.fill(parentHash, (byte) 3);
        batchHeader.setParentBatchHash(parentHash);
        assertArrayEquals(parentHash, batchHeader.getParentBatchHash());
    }

    /**
     * Test getter and setter for hash
     */
    @Test
    public void testHashGetterSetter() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 4);
        batchHeader.setHash(hash);
        assertArrayEquals(hash, batchHeader.getHash());
    }

    /**
     * Test with BATCH_V0
     */
    @Test
    public void testWithBatchV0() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertEquals(BatchVersionEnum.BATCH_V0, header.getVersion());
    }

    /**
     * Test with BATCH_V1
     */
    @Test
    public void testWithBatchV1() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V1)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertEquals(BatchVersionEnum.BATCH_V1, header.getVersion());
    }

    /**
     * Test with zero batch index
     */
    @Test
    public void testWithZeroBatchIndex() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.ZERO)
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertEquals(BigInteger.ZERO, header.getBatchIndex());
    }

    /**
     * Test with large batch index
     */
    @Test
    public void testWithLargeBatchIndex() {
        BigInteger largeIndex = new BigInteger("999999999999999999");
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(largeIndex)
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertEquals(largeIndex, header.getBatchIndex());
    }

    /**
     * Test serialize consistency
     * Multiple serializations should produce same result
     */
    @Test
    public void testSerializeConsistency() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] serialized1 = header.serialize();
        byte[] serialized2 = header.serialize();

        assertArrayEquals(serialized1, serialized2);
    }

    /**
     * Test hash determinism
     * Same data should produce same hash
     */
    @Test
    public void testHashDeterminism() {
        BatchHeader header1 = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        BatchHeader header2 = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertArrayEquals(header1.getHash(), header2.getHash());
    }

    /**
     * Test hash uniqueness
     * Different data should produce different hash
     */
    @Test
    public void testHashUniqueness() {
        BatchHeader header1 = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        BatchHeader header2 = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(101)) // Different index
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        assertFalse(Arrays.equals(header1.getHash(), header2.getHash()));
    }

    /**
     * Test builder with partial fields
     */
    @Test
    public void testBuilderWithPartialFields() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .build();

        assertNotNull(header);
        assertEquals(BatchVersionEnum.BATCH_V0, header.getVersion());
        assertEquals(BigInteger.valueOf(100), header.getBatchIndex());
    }

    // ==================== Negative Tests ====================

    /**
     * Test deserializeFrom with null array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromNull() {
        BatchHeader.deserializeFrom(null);
    }

    /**
     * Test deserializeFrom with invalid length
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromInvalidLength() {
        byte[] invalidData = new byte[50]; // Less than required
        BatchHeader.deserializeFrom(invalidData);
    }

    /**
     * Test deserializeFrom with empty array
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testDeserializeFromEmptyArray() {
        BatchHeader.deserializeFrom(new byte[0]);
    }

    /**
     * Test serialize with null version
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullVersion() {
        BatchHeader header = BatchHeader.builder()
                .version(null)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        header.serialize();
    }

    /**
     * Test serialize with null batchIndex
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullBatchIndex() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(null)
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        header.serialize();
    }

    /**
     * Test serialize with null l1MsgRollingHash
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullL1MsgRollingHash() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(null)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        header.serialize();
    }

    /**
     * Test serialize with null dataHash
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullDataHash() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(null)
                .parentBatchHash(new byte[32])
                .build();

        header.serialize();
    }

    /**
     * Test serialize with null parentBatchHash
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testSerializeWithNullParentBatchHash() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(null)
                .build();

        header.serialize();
    }

    /**
     * Test with null values in builder
     */
    @Test
    public void testBuilderWithNullValues() {
        BatchHeader header = BatchHeader.builder()
                .version(null)
                .batchIndex(null)
                .l1MsgRollingHash(null)
                .dataHash(null)
                .parentBatchHash(null)
                .build();

        assertNotNull(header);
        assertNull(header.getVersion());
        assertNull(header.getBatchIndex());
        assertNull(header.getL1MsgRollingHash());
        assertNull(header.getDataHash());
        assertNull(header.getParentBatchHash());
    }

    /**
     * Test deserializeFrom with extra bytes
     * Should only read required bytes
     */
    @Test
    public void testDeserializeFromExtraBytes() {
        BatchHeader original = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] serialized = original.serialize();
        byte[] withExtra = Arrays.copyOf(serialized, serialized.length + 10);

        BatchHeader deserialized = BatchHeader.deserializeFrom(withExtra);

        assertNotNull(deserialized);
        assertEquals(original.getVersion(), deserialized.getVersion());
        assertEquals(original.getBatchIndex(), deserialized.getBatchIndex());
    }

    /**
     * Test toJson with null hash
     * Should compute hash before converting to JSON
     */
    @Test
    public void testToJsonWithNullHash() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .hash(null) // Explicitly set to null
                .build();

        String json = header.toJson();

        assertNotNull(json);
        assertTrue(json.contains("hash"));
    }

    /**
     * Test getHashHex consistency
     * Should return same hex string on multiple calls
     */
    @Test
    public void testGetHashHexConsistency() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        String hex1 = header.getHashHex();
        String hex2 = header.getHashHex();

        assertEquals(hex1, hex2);
    }

    /**
     * Test multiple serialize calls
     * Should produce consistent results
     */
    @Test
    public void testMultipleSerializeCalls() {
        BatchHeader header = BatchHeader.builder()
                .version(BatchVersionEnum.BATCH_V0)
                .batchIndex(BigInteger.valueOf(100))
                .l1MsgRollingHash(testHash32)
                .dataHash(testHash32_2)
                .parentBatchHash(new byte[32])
                .build();

        byte[] first = header.serialize();
        byte[] second = header.serialize();
        byte[] third = header.serialize();

        assertArrayEquals(first, second);
        assertArrayEquals(second, third);
    }
}
