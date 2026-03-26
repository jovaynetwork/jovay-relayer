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
import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.StaticGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPricePolicyEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.web3j.protocol.Web3j;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for SubChainConfig.
 * <p>
 * Tests the configuration of Layer 2 blockchain client components,
 * including Web3j client creation and gas price provider configuration.
 * </p>
 */
@RunWith(JUnit4.class)
public class SubChainConfigTest {

    /**
     * Tests that l2Web3j() returns non-null Web3j client when l2RpcUrl is set.
     */
    @Test
    public void testL2Web3jReturnsNonNull() throws Exception {
        SubChainConfig config = new SubChainConfig();

        Field l2RpcUrlField = SubChainConfig.class.getDeclaredField("l2RpcUrl");
        l2RpcUrlField.setAccessible(true);
        l2RpcUrlField.set(config, "http://localhost:8545");

        Web3j result = config.l2Web3j();

        assertNotNull("l2Web3j should not be null", result);
    }

    /**
     * Tests that l2GasPriceProvider() returns StaticGasPriceProvider when policy is STATIC.
     */
    @Test
    public void testL2GasPriceProviderWhenStaticPolicy() throws Exception {
        SubChainConfig config = new SubChainConfig();
        Web3j mockWeb3j = mock(Web3j.class);
        IGasPriceProviderConfig mockConfig = mock(IGasPriceProviderConfig.class);

        Field l2GasPricePolicyField = SubChainConfig.class.getDeclaredField("l2GasPricePolicy");
        l2GasPricePolicyField.setAccessible(true);
        l2GasPricePolicyField.set(config, GasPricePolicyEnum.STATIC);

        Field l2StaticGasPriceField = SubChainConfig.class.getDeclaredField("l2StaticGasPrice");
        l2StaticGasPriceField.setAccessible(true);
        l2StaticGasPriceField.set(config, BigInteger.valueOf(4100000000L));

        Field l2StaticMaxFeePerGasField = SubChainConfig.class.getDeclaredField("l2StaticMaxFeePerGas");
        l2StaticMaxFeePerGasField.setAccessible(true);
        l2StaticMaxFeePerGasField.set(config, BigInteger.valueOf(50000000000L));

        Field l2StaticMaxPriorityFeePerGasField = SubChainConfig.class.getDeclaredField("l2StaticMaxPriorityFeePerGas");
        l2StaticMaxPriorityFeePerGasField.setAccessible(true);
        l2StaticMaxPriorityFeePerGasField.set(config, BigInteger.valueOf(2000000000L));

        IGasPriceProvider result = config.l2GasPriceProvider(mockWeb3j, mockConfig);

        assertNotNull("l2GasPriceProvider should not be null", result);
        assertTrue("l2GasPriceProvider should be instance of StaticGasPriceProvider", result instanceof StaticGasPriceProvider);
    }
}