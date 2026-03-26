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

import com.alipay.antchain.l2.relayer.commons.enums.DaType;
import com.alipay.antchain.l2.relayer.commons.enums.ParentChainType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for RollupConfig validation logic.
 * <p>
 * Tests the validate() method that ensures compatibility between parent chain type
 * and data availability type combinations. This test class is separate from
 * RollupConfigTest to avoid inheriting TestBase and starting Spring container.
 * </p>
 */
@RunWith(JUnit4.class)
public class RollupConfigValidateTest {

    /**
     * Tests that validate() does not throw exception for ETHEREUM + BLOBS combination.
     */
    @Test
    public void testValidateWhenEthereumAndBlobs() throws Exception {
        RollupConfig config = new RollupConfig();

        Field parentChainTypeField = RollupConfig.class.getDeclaredField("parentChainType");
        parentChainTypeField.setAccessible(true);
        parentChainTypeField.set(config, ParentChainType.ETHEREUM);

        Field daTypeField = RollupConfig.class.getDeclaredField("daType");
        daTypeField.setAccessible(true);
        daTypeField.set(config, DaType.BLOBS);

        config.validate();
    }

    /**
     * Tests that validate() does not throw exception for JOVAY + DAS combination.
     */
    @Test
    public void testValidateWhenJovayAndDas() throws Exception {
        RollupConfig config = new RollupConfig();

        Field parentChainTypeField = RollupConfig.class.getDeclaredField("parentChainType");
        parentChainTypeField.setAccessible(true);
        parentChainTypeField.set(config, ParentChainType.JOVAY);

        Field daTypeField = RollupConfig.class.getDeclaredField("daType");
        daTypeField.setAccessible(true);
        daTypeField.set(config, DaType.DAS);

        config.validate();
    }

    /**
     * Tests that validate() throws IllegalStateException for ETHEREUM + DAS combination.
     */
    @Test
    public void testValidateWhenEthereumAndDasThrowsException() throws Exception {
        RollupConfig config = new RollupConfig();

        Field parentChainTypeField = RollupConfig.class.getDeclaredField("parentChainType");
        parentChainTypeField.setAccessible(true);
        parentChainTypeField.set(config, ParentChainType.ETHEREUM);

        Field daTypeField = RollupConfig.class.getDeclaredField("daType");
        daTypeField.setAccessible(true);
        daTypeField.set(config, DaType.DAS);

        try {
            config.validate();
            fail("validate() should throw IllegalStateException for ETHEREUM + DAS");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should contain DA type", e.getMessage().contains("DA type"));
            assertTrue("Exception message should contain parent chain", e.getMessage().contains("parent chain"));
        }
    }

    /**
     * Tests that validate() throws IllegalStateException for JOVAY + BLOBS combination.
     */
    @Test
    public void testValidateWhenJovayAndBlobsThrowsException() throws Exception {
        RollupConfig config = new RollupConfig();

        Field parentChainTypeField = RollupConfig.class.getDeclaredField("parentChainType");
        parentChainTypeField.setAccessible(true);
        parentChainTypeField.set(config, ParentChainType.JOVAY);

        Field daTypeField = RollupConfig.class.getDeclaredField("daType");
        daTypeField.setAccessible(true);
        daTypeField.set(config, DaType.BLOBS);

        try {
            config.validate();
            fail("validate() should throw IllegalStateException for JOVAY + BLOBS");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should contain DA type", e.getMessage().contains("DA type"));
            assertTrue("Exception message should contain parent chain", e.getMessage().contains("parent chain"));
        }
    }
}
