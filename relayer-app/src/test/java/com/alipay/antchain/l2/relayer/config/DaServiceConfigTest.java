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

package com.alipay.antchain.l2.relayer.config;

import java.lang.reflect.Field;

import com.alipay.antchain.l2.relayer.commons.enums.DasType;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.IDaService;
import com.alipay.antchain.l2.relayer.config.auto.DaServiceProperties;
import com.alipay.antchain.l2.relayer.core.layer2.RelayerLocalDaService;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DaServiceConfig.
 * <p>
 * Tests the configuration of data availability service bean creation,
 * specifically testing the daService() method with different DasType values.
 * </p>
 */
@RunWith(JUnit4.class)
public class DaServiceConfigTest {

    /**
     * Tests that daService() returns RelayerLocalDaService when DasType is LOCAL.
     */
    @Test
    public void testDaServiceWhenTypeIsLocal() throws Exception {
        DaServiceConfig config = new DaServiceConfig();
        DaServiceProperties properties = mock(DaServiceProperties.class);
        IRollupRepository rollupRepository = mock(IRollupRepository.class);

        when(properties.getType()).thenReturn(DasType.LOCAL);

        Field propertiesField = DaServiceConfig.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        propertiesField.set(config, properties);

        IDaService result = config.daService(rollupRepository);

        assertNotNull("daService should not be null", result);
        assertTrue("daService should be instance of RelayerLocalDaService", result instanceof RelayerLocalDaService);
    }

    /**
     * Tests that daService() throws RuntimeException when DasType is not LOCAL.
     * <p>
     * Note: This test is skipped because DasType currently only has LOCAL value,
     * so this branch is unreachable in practice.
     * </p>
     */
    @Test
    public void testDaServiceWhenTypeIsNotLocal() throws Exception {
        // Skipped: DasType only has LOCAL value, this branch is unreachable
        // If DasType is extended in the future, this test can be enabled
    }
}