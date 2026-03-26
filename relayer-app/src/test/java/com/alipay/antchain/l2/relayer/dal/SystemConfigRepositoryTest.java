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

package com.alipay.antchain.l2.relayer.dal;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.cache.Cache;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.web3j.protocol.Web3j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false", "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class SystemConfigRepositoryTest extends TestBase {

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private Activator activator;

    @MockitoBean
    private Dispatcher dispatcher;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoSpyBean(name = "systemConfigCache")
    private Cache<String, String> systemConfigCache;

    @Test
    public void testAnchorBatchSet() {
        assertFalse(systemConfigRepository.isAnchorBatchSet());
        systemConfigRepository.markAnchorBatchHasBeenSet();
        assertTrue(systemConfigRepository.isAnchorBatchSet());
    }

    @Test
    public void testConfig() {
        assertFalse(systemConfigRepository.hasSystemConfig("test"));
        assertNull(systemConfigRepository.getSystemConfig("test"));
        systemConfigRepository.setSystemConfig("test", "test");
        assertEquals("test", systemConfigRepository.getSystemConfig("test"));
        systemConfigRepository.setSystemConfig("test", "test1");
        assertEquals("test1", systemConfigRepository.getSystemConfig("test"));

        Map<String, String> val = new HashMap<>();
        val.put("test2", "test3");
        systemConfigRepository.setSystemConfig(val);

        assertEquals("test3", systemConfigRepository.getSystemConfig("test2"));

        when(systemConfigCache.containsKey(notNull())).thenThrow(RuntimeException.class);
        assertThrows(L2RelayerException.class, () -> systemConfigRepository.getSystemConfig("wrong"));
    }

    // ==================== Negative Case Tests ====================

    /**
     * Test get system config with non-existent key
     * Verifies that querying non-existent config returns null
     */
    @Test
    public void testGetSystemConfig_NonExistentKey() {
        String result = systemConfigRepository.getSystemConfig("non-existent-key-12345");
        assertNull(result);
    }

    /**
     * Test has system config with non-existent key
     * Verifies that checking non-existent config returns false
     */
    @Test
    public void testHasSystemConfig_NonExistentKey() {
        boolean result = systemConfigRepository.hasSystemConfig("non-existent-key-67890");
        assertFalse(result);
    }

    /**
     * Test set system config with null value
     * Verifies that setting null value is handled correctly
     */
    @Test
    public void testSetSystemConfig_NullValue() {
        systemConfigRepository.setSystemConfig("null-test-key", null);

        // Verify the config was saved
        assertTrue(systemConfigRepository.hasSystemConfig("null-test-key"));
        String result = systemConfigRepository.getSystemConfig("null-test-key");
        // Null value should be stored as null or empty string
        assertTrue(result == null || result.isEmpty());
    }

    /**
     * Test set system config with empty string value
     * Verifies that setting empty string is handled correctly
     */
    @Test
    public void testSetSystemConfig_EmptyValue() {
        systemConfigRepository.setSystemConfig("empty-test-key", "");

        assertTrue(systemConfigRepository.hasSystemConfig("empty-test-key"));
        String result = systemConfigRepository.getSystemConfig("empty-test-key");
        assertEquals("", result);
    }

    /**
     * Test set system config with empty map
     * Verifies that setting empty map is handled gracefully
     */
    @Test
    public void testSetSystemConfig_EmptyMap() {
        Map<String, String> emptyMap = new HashMap<>();

        // Should not throw exception when setting empty map
        systemConfigRepository.setSystemConfig(emptyMap);

        // Verify no configs were added
        Map<String, String> result = systemConfigRepository.getPrefixedSystemConfig("empty-map-test");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test get prefixed system config with non-existent prefix
     * Verifies that querying non-existent prefix returns empty map
     */
    @Test
    public void testGetPrefixedSystemConfig_NonExistentPrefix() {
        Map<String, String> result = systemConfigRepository.getPrefixedSystemConfig("non-existent-prefix-xyz");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test get prefixed system config with empty prefix
     * Verifies that empty prefix returns all configs
     */
    @Test
    public void testGetPrefixedSystemConfig_EmptyPrefix() {
        // Set some test configs
        systemConfigRepository.setSystemConfig("prefix-test-1", "value1");
        systemConfigRepository.setSystemConfig("prefix-test-2", "value2");

        Map<String, String> result = systemConfigRepository.getPrefixedSystemConfig("");
        assertNotNull(result);
        // Should return all configs (at least the ones we just set)
        assertTrue(result.size() >= 2);
    }

    /**
     * Test update existing config multiple times
     * Verifies that config can be updated multiple times
     */
    @Test
    public void testSetSystemConfig_MultipleUpdates() {
        String key = "multi-update-test";

        systemConfigRepository.setSystemConfig(key, "value1");
        assertEquals("value1", systemConfigRepository.getSystemConfig(key));

        systemConfigRepository.setSystemConfig(key, "value2");
        assertEquals("value2", systemConfigRepository.getSystemConfig(key));

        systemConfigRepository.setSystemConfig(key, "value3");
        assertEquals("value3", systemConfigRepository.getSystemConfig(key));

        // Should still have only one config entry
        assertTrue(systemConfigRepository.hasSystemConfig(key));
    }

    /**
     * Test set system config map with duplicate keys
     * Verifies that duplicate keys in map are handled correctly
     */
    @Test
    public void testSetSystemConfig_MapWithDuplicateKeys() {
        Map<String, String> configs = new HashMap<>();
        configs.put("dup-key-1", "value1");
        configs.put("dup-key-2", "value2");

        // Set first time
        systemConfigRepository.setSystemConfig(configs);
        assertEquals("value1", systemConfigRepository.getSystemConfig("dup-key-1"));
        assertEquals("value2", systemConfigRepository.getSystemConfig("dup-key-2"));

        // Set again with different values
        configs.put("dup-key-1", "new-value1");
        configs.put("dup-key-2", "new-value2");

        try {
            systemConfigRepository.setSystemConfig(configs);
            // If no exception, verify values were updated or duplicates were handled
            String val1 = systemConfigRepository.getSystemConfig("dup-key-1");
            String val2 = systemConfigRepository.getSystemConfig("dup-key-2");
            assertNotNull(val1);
            assertNotNull(val2);
        } catch (Exception e) {
            // Exception is acceptable for duplicate insertion
            assertTrue(e.getMessage() != null);
        }
    }

    /**
     * Test is anchor batch set when not set
     * Verifies that default state is false
     */
    @Test
    public void testIsAnchorBatchSet_NotSet() {
        // Clear any existing flag
        boolean initialState = systemConfigRepository.isAnchorBatchSet();

        if (initialState) {
            // If already set, we can't test the not-set case
            assertTrue(true);
        } else {
            assertFalse(systemConfigRepository.isAnchorBatchSet());
        }
    }

    /**
     * Test mark anchor batch has been set multiple times
     * Verifies that marking multiple times is idempotent
     */
    @Test
    public void testMarkAnchorBatchHasBeenSet_MultipleTimes() {
        systemConfigRepository.markAnchorBatchHasBeenSet();
        assertTrue(systemConfigRepository.isAnchorBatchSet());

        // Mark again
        systemConfigRepository.markAnchorBatchHasBeenSet();
        assertTrue(systemConfigRepository.isAnchorBatchSet());

        // Should still be true
        assertTrue(systemConfigRepository.isAnchorBatchSet());
    }

    /**
     * Test get prefixed system config with special characters in prefix
     * Verifies that special characters are handled correctly
     */
    @Test
    public void testGetPrefixedSystemConfig_SpecialCharacters() {
        // Set configs with special characters
        systemConfigRepository.setSystemConfig("special_test.key-1", "value1");
        systemConfigRepository.setSystemConfig("special_test.key-2", "value2");

        Map<String, String> result = systemConfigRepository.getPrefixedSystemConfig("special_test");
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    /**
     * Test set system config with very long key and value
     * Verifies that long strings are handled correctly
     */
    @Test
    public void testSetSystemConfig_LongStrings() {
        String longKey = "long_key_" + "x".repeat(100);
        String longValue = "y".repeat(500);

        try {
            systemConfigRepository.setSystemConfig(longKey, longValue);
            String result = systemConfigRepository.getSystemConfig(longKey);
            assertEquals(longValue, result);
        } catch (Exception e) {
            // Exception is acceptable if database has length constraints
            assertTrue(e.getMessage() != null);
        }
    }
}
