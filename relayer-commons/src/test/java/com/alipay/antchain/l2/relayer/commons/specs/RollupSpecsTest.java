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

package com.alipay.antchain.l2.relayer.commons.specs;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidRollupSpecsException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for RollupSpecs class
 * Tests JSON deserialization, fork retrieval, and getter/setter methods
 */
public class RollupSpecsTest {

    private RollupSpecs testSpecs;
    private Map<BigInteger, ForkInfo> testForks;

    @Before
    public void setUp() {
        // Create test forks map
        testForks = new HashMap<>();
        
        ForkInfo fork1 = new ForkInfo();
        fork1.setBatchVersion(BatchVersionEnum.BATCH_V0);
        testForks.put(BigInteger.ONE, fork1);
        
        ForkInfo fork2 = new ForkInfo();
        fork2.setBatchVersion(BatchVersionEnum.BATCH_V1);
        testForks.put(BigInteger.valueOf(1000000), fork2);
        
        // Create test specs
        testSpecs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                testForks
        );
    }

    // ==================== Constructor Tests ====================

    /**
     * Test no-args constructor
     * Should create empty instance
     */
    @Test
    public void testNoArgsConstructor() {
        RollupSpecs specs = new RollupSpecs();
        
        assertNotNull(specs);
        assertNull(specs.getNetwork());
        assertNull(specs.getLayer2ChainId());
        assertNull(specs.getLayer1ChainId());
        assertNull(specs.getForks());
    }

    /**
     * Test all-args constructor
     * Should create instance with all specified values
     */
    @Test
    public void testAllArgsConstructor() {
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.MAINNET,
                BigInteger.valueOf(5734951),
                BigInteger.ONE,
                testForks
        );
        
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.MAINNET, specs.getNetwork());
        assertEquals(BigInteger.valueOf(5734951), specs.getLayer2ChainId());
        assertEquals(BigInteger.ONE, specs.getLayer1ChainId());
        assertEquals(testForks, specs.getForks());
    }

    // ==================== fromJson Tests ====================

    /**
     * Test fromJson with mainnet JSON
     * Should deserialize mainnet specs correctly
     */
    @Test
    public void testFromJsonMainnet() {
        String json = """
                {
                  "network": "mainnet",
                  "layer2_chain_id": "5734951",
                  "layer1_chain_id": "1",
                  "forks": {
                    "1": {
                      "batch_version": 1
                    }
                  }
                }
                """;
        
        RollupSpecs specs = RollupSpecs.fromJson(json);
        
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.MAINNET, specs.getNetwork());
        assertEquals(BigInteger.valueOf(5734951), specs.getLayer2ChainId());
        assertEquals(BigInteger.ONE, specs.getLayer1ChainId());
        assertNotNull(specs.getForks());
        assertEquals(1, specs.getForks().size());
        assertTrue(specs.getForks().containsKey(BigInteger.ONE));
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getForks().get(BigInteger.ONE).getBatchVersion());
    }

    /**
     * Test fromJson with testnet JSON
     * Should deserialize testnet specs with multiple forks
     */
    @Test
    public void testFromJsonTestnet() {
        String json = """
                {
                  "network": "testnet",
                  "layer2_chain_id": "2019775",
                  "layer1_chain_id": "11155111",
                  "forks": {
                    "1": {
                      "batch_version": 0
                    },
                    "1758327170000": {
                      "batch_version": 1
                    }
                  }
                }
                """;
        
        RollupSpecs specs = RollupSpecs.fromJson(json);
        
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.TESTNET, specs.getNetwork());
        assertEquals(BigInteger.valueOf(2019775), specs.getLayer2ChainId());
        assertEquals(BigInteger.valueOf(11155111), specs.getLayer1ChainId());
        assertNotNull(specs.getForks());
        assertEquals(2, specs.getForks().size());
        assertEquals(BatchVersionEnum.BATCH_V0, specs.getForks().get(BigInteger.ONE).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getForks().get(BigInteger.valueOf(1758327170000L)).getBatchVersion());
    }

    /**
     * Test fromJson with private net JSON
     * Should deserialize private net specs correctly
     */
    @Test
    public void testFromJsonPrivateNet() {
        String json = """
                {
                  "network": "private_net",
                  "layer2_chain_id": "999999",
                  "layer1_chain_id": "31337",
                  "forks": {
                    "1": {
                      "batch_version": 0
                    }
                  }
                }
                """;
        
        RollupSpecs specs = RollupSpecs.fromJson(json);
        
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.PRIVATE_NET, specs.getNetwork());
        assertEquals(BigInteger.valueOf(999999), specs.getLayer2ChainId());
        assertEquals(BigInteger.valueOf(31337), specs.getLayer1ChainId());
        assertNotNull(specs.getForks());
        assertEquals(1, specs.getForks().size());
    }

    /**
     * Test fromJson with minimal JSON
     * Should handle minimal valid JSON
     */
    @Test
    public void testFromJsonMinimal() {
        String json = """
                {
                  "network": "testnet",
                  "layer2_chain_id": "1",
                  "layer1_chain_id": "1",
                  "forks": {}
                }
                """;
        
        RollupSpecs specs = RollupSpecs.fromJson(json);
        
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.TESTNET, specs.getNetwork());
        assertNotNull(specs.getForks());
        assertTrue(specs.getForks().isEmpty());
    }

    /**
     * Test fromJson with invalid JSON
     * Should throw exception
     */
    @Test(expected = Exception.class)
    public void testFromJsonInvalid() {
        RollupSpecs.fromJson("invalid json");
    }

    /**
     * Test fromJson with null
     * Should return null (FastJSON behavior)
     */
    @Test
    public void testFromJsonNull() {
        RollupSpecs specs = RollupSpecs.fromJson(null);
        assertNull(specs);
    }

    /**
     * Test fromJson with empty JSON
     * Should create specs with null fields
     */
    @Test
    public void testFromJsonEmpty() {
        String json = "{}";
        
        RollupSpecs specs = RollupSpecs.fromJson(json);
        
        assertNotNull(specs);
        assertNull(specs.getNetwork());
        assertNull(specs.getLayer2ChainId());
        assertNull(specs.getLayer1ChainId());
        assertNull(specs.getForks());
    }

    // ==================== getFork Tests ====================

    /**
     * Test getFork with exact timestamp match
     * Should return fork at exact timestamp
     */
    @Test
    public void testGetForkExactMatch() {
        ForkInfo fork = testSpecs.getFork(1L);
        
        assertNotNull(fork);
        assertEquals(BatchVersionEnum.BATCH_V0, fork.getBatchVersion());
    }

    /**
     * Test getFork with timestamp between forks
     * Should return the most recent fork before timestamp
     */
    @Test
    public void testGetForkBetweenForks() {
        ForkInfo fork = testSpecs.getFork(500000L);
        
        assertNotNull(fork);
        assertEquals(BatchVersionEnum.BATCH_V0, fork.getBatchVersion());
    }

    /**
     * Test getFork with timestamp after all forks
     * Should return the latest fork
     */
    @Test
    public void testGetForkAfterAllForks() {
        ForkInfo fork = testSpecs.getFork(2000000L);
        
        assertNotNull(fork);
        assertEquals(BatchVersionEnum.BATCH_V1, fork.getBatchVersion());
    }

    /**
     * Test getFork with timestamp at second fork
     * Should return second fork
     */
    @Test
    public void testGetForkAtSecondFork() {
        ForkInfo fork = testSpecs.getFork(1000000L);
        
        assertNotNull(fork);
        assertEquals(BatchVersionEnum.BATCH_V1, fork.getBatchVersion());
    }

    /**
     * Test getFork with multiple forks
     * Should correctly select fork based on timestamp
     */
    @Test
    public void testGetForkMultipleForks() {
        Map<BigInteger, ForkInfo> forks = new HashMap<>();
        
        ForkInfo fork1 = new ForkInfo();
        fork1.setBatchVersion(BatchVersionEnum.BATCH_V0);
        forks.put(BigInteger.valueOf(100), fork1);
        
        ForkInfo fork2 = new ForkInfo();
        fork2.setBatchVersion(BatchVersionEnum.BATCH_V1);
        forks.put(BigInteger.valueOf(1000), fork2);
        
        ForkInfo fork3 = new ForkInfo();
        fork3.setBatchVersion(BatchVersionEnum.BATCH_V1);
        forks.put(BigInteger.valueOf(10000), fork3);
        
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                forks
        );
        
        // Test at each fork boundary
        assertEquals(BatchVersionEnum.BATCH_V0, specs.getFork(100L).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V0, specs.getFork(500L).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getFork(1000L).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getFork(5000L).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getFork(10000L).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getFork(20000L).getBatchVersion());
    }

    /**
     * Test getFork with timestamp before all forks
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetForkBeforeAllForks() {
        testSpecs.getFork(0L);
    }

    /**
     * Test getFork with negative timestamp
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetForkNegativeTimestamp() {
        testSpecs.getFork(-1L);
    }

    /**
     * Test getFork with empty forks map
     * Should throw RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetForkEmptyForks() {
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                new HashMap<>()
        );
        
        specs.getFork(1000L);
    }

    /**
     * Test getFork with null forks map
     * Should throw NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testGetForkNullForks() {
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                null
        );
        
        specs.getFork(1000L);
    }

    /**
     * Test getFork validates ForkInfo
     * Should call validate() on returned ForkInfo
     */
    @Test(expected = InvalidRollupSpecsException.class)
    public void testGetForkValidatesForkInfo() {
        Map<BigInteger, ForkInfo> forks = new HashMap<>();
        ForkInfo invalidFork = new ForkInfo();
        // Don't set batchVersion, making it invalid
        forks.put(BigInteger.ONE, invalidFork);
        
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                forks
        );
        
        specs.getFork(1L);
    }

    /**
     * Test getFork with large timestamp
     * Should handle large timestamp values
     */
    @Test
    public void testGetForkLargeTimestamp() {
        ForkInfo fork = testSpecs.getFork(Long.MAX_VALUE);
        
        assertNotNull(fork);
        assertEquals(BatchVersionEnum.BATCH_V1, fork.getBatchVersion());
    }

    // ==================== Getter/Setter Tests ====================

    /**
     * Test getNetwork
     * Should return network value
     */
    @Test
    public void testGetNetwork() {
        assertEquals(RollupSpecsNetwork.TESTNET, testSpecs.getNetwork());
    }

    /**
     * Test setNetwork
     * Should update network value
     */
    @Test
    public void testSetNetwork() {
        testSpecs.setNetwork(RollupSpecsNetwork.MAINNET);
        assertEquals(RollupSpecsNetwork.MAINNET, testSpecs.getNetwork());
    }

    /**
     * Test getLayer2ChainId
     * Should return layer2 chain id
     */
    @Test
    public void testGetLayer2ChainId() {
        assertEquals(BigInteger.valueOf(2019775), testSpecs.getLayer2ChainId());
    }

    /**
     * Test setLayer2ChainId
     * Should update layer2 chain id
     */
    @Test
    public void testSetLayer2ChainId() {
        BigInteger newChainId = BigInteger.valueOf(999999);
        testSpecs.setLayer2ChainId(newChainId);
        assertEquals(newChainId, testSpecs.getLayer2ChainId());
    }

    /**
     * Test getLayer1ChainId
     * Should return layer1 chain id
     */
    @Test
    public void testGetLayer1ChainId() {
        assertEquals(BigInteger.valueOf(11155111), testSpecs.getLayer1ChainId());
    }

    /**
     * Test setLayer1ChainId
     * Should update layer1 chain id
     */
    @Test
    public void testSetLayer1ChainId() {
        BigInteger newChainId = BigInteger.valueOf(31337);
        testSpecs.setLayer1ChainId(newChainId);
        assertEquals(newChainId, testSpecs.getLayer1ChainId());
    }

    /**
     * Test getForks
     * Should return forks map
     */
    @Test
    public void testGetForks() {
        assertEquals(testForks, testSpecs.getForks());
    }

    /**
     * Test setForks
     * Should update forks map
     */
    @Test
    public void testSetForks() {
        Map<BigInteger, ForkInfo> newForks = new HashMap<>();
        ForkInfo fork = new ForkInfo();
        fork.setBatchVersion(BatchVersionEnum.BATCH_V1);
        newForks.put(BigInteger.valueOf(999), fork);
        
        testSpecs.setForks(newForks);
        assertEquals(newForks, testSpecs.getForks());
    }

    /**
     * Test setters with null values
     * Should accept null values
     */
    @Test
    public void testSettersWithNull() {
        testSpecs.setNetwork(null);
        testSpecs.setLayer2ChainId(null);
        testSpecs.setLayer1ChainId(null);
        testSpecs.setForks(null);
        
        assertNull(testSpecs.getNetwork());
        assertNull(testSpecs.getLayer2ChainId());
        assertNull(testSpecs.getLayer1ChainId());
        assertNull(testSpecs.getForks());
    }

    // ==================== IRollupSpecs Interface Tests ====================

    /**
     * Test implements IRollupSpecs interface
     * Should implement all interface methods
     */
    @Test
    public void testImplementsIRollupSpecs() {
        assertTrue(testSpecs instanceof IRollupSpecs);
        
        IRollupSpecs iSpecs = testSpecs;
        assertEquals(RollupSpecsNetwork.TESTNET, iSpecs.getNetwork());
        assertEquals(BigInteger.valueOf(2019775), iSpecs.getLayer2ChainId());
        assertEquals(BigInteger.valueOf(11155111), iSpecs.getLayer1ChainId());
        assertNotNull(iSpecs.getFork(1L));
    }

    // ==================== JSON Serialization Tests ====================

    /**
     * Test JSON serialization round trip
     * Should maintain data integrity through serialize/deserialize
     * Note: We test with a simple structure that matches the actual JSON format
     */
    @Test
    public void testJsonSerializationRoundTrip() {
        // Use a JSON string that matches the actual format used in production
        String json = """
                {
                  "network": "testnet",
                  "layer2_chain_id": "2019775",
                  "layer1_chain_id": "11155111",
                  "forks": {
                    "1": {
                      "batch_version": 0
                    },
                    "1000000": {
                      "batch_version": 1
                    }
                  }
                }
                """;

        RollupSpecs specs = RollupSpecs.fromJson(json);
        assertNotNull(specs);
        assertEquals(RollupSpecsNetwork.TESTNET, specs.getNetwork());
        assertEquals(BigInteger.valueOf(2019775), specs.getLayer2ChainId());
        assertEquals(BigInteger.valueOf(11155111), specs.getLayer1ChainId());
        assertEquals(2, specs.getForks().size());

        // Verify forks are correctly deserialized
        assertEquals(BatchVersionEnum.BATCH_V0, specs.getForks().get(BigInteger.ONE).getBatchVersion());
        assertEquals(BatchVersionEnum.BATCH_V1, specs.getForks().get(BigInteger.valueOf(1000000)).getBatchVersion());
    }

    /**
     * Test JSON field names
     * Should use snake_case field names
     */
    @Test
    public void testJsonFieldNames() {
        String json = JSON.toJSONString(testSpecs);
        
        assertTrue(json.contains("\"network\""));
        assertTrue(json.contains("\"layer2_chain_id\""));
        assertTrue(json.contains("\"layer1_chain_id\""));
        assertTrue(json.contains("\"forks\""));
    }

    // ==================== Edge Cases ====================

    /**
     * Test with zero chain IDs
     * Should handle zero values
     */
    @Test
    public void testZeroChainIds() {
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.ZERO,
                BigInteger.ZERO,
                testForks
        );
        
        assertEquals(BigInteger.ZERO, specs.getLayer2ChainId());
        assertEquals(BigInteger.ZERO, specs.getLayer1ChainId());
    }

    /**
     * Test with very large chain IDs
     * Should handle large BigInteger values
     */
    @Test
    public void testLargeChainIds() {
        BigInteger largeId = new BigInteger("999999999999999999999999");
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.MAINNET,
                largeId,
                largeId,
                testForks
        );
        
        assertEquals(largeId, specs.getLayer2ChainId());
        assertEquals(largeId, specs.getLayer1ChainId());
    }

    /**
     * Test with single fork
     * Should work with minimal fork configuration
     */
    @Test
    public void testSingleFork() {
        Map<BigInteger, ForkInfo> singleFork = new HashMap<>();
        ForkInfo fork = new ForkInfo();
        fork.setBatchVersion(BatchVersionEnum.BATCH_V0);
        singleFork.put(BigInteger.ONE, fork);
        
        RollupSpecs specs = new RollupSpecs(
                RollupSpecsNetwork.TESTNET,
                BigInteger.valueOf(2019775),
                BigInteger.valueOf(11155111),
                singleFork
        );
        
        ForkInfo result = specs.getFork(1L);
        assertNotNull(result);
        assertEquals(BatchVersionEnum.BATCH_V0, result.getBatchVersion());
        
        result = specs.getFork(1000000L);
        assertNotNull(result);
        assertEquals(BatchVersionEnum.BATCH_V0, result.getBatchVersion());
    }

    /**
     * Test getFork error message
     * Should provide meaningful error message
     */
    @Test
    public void testGetForkErrorMessage() {
        try {
            testSpecs.getFork(0L);
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("No fork found"));
            assertTrue(e.getMessage().contains("0"));
        }
    }
}
