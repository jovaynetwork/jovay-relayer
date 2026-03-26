
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

package com.alipay.antchain.l2.relayer.utils;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbFastRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.CachedNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.RemoteNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.StaticGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPricePolicyEnum;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.web3j.protocol.Web3j;
import org.web3j.service.TxSignService;

import static org.mockito.Mockito.mock;

public class BlockchainUtilsTest {

    @Test
    public void testCreateTransactionManagerWithFastPolicy() {
        BigInteger chainId = BigInteger.valueOf(1);
        EthNoncePolicyEnum noncePolicy = EthNoncePolicyEnum.FAST;
        Web3j web3j = mock(Web3j.class);
        TxSignService txSignService = mock(TxSignService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        EthBlobForkConfig ethBlobForkConfig = mock(EthBlobForkConfig.class);
        CachedNonceManager nonceManager = mock(CachedNonceManager.class);

        BaseRawTransactionManager result = BlockchainUtils.createTransactionManager(
                chainId, noncePolicy, web3j, txSignService, redisson, ethBlobForkConfig, nonceManager
        );

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof AcbFastRawTransactionManager);
    }

    @Test
    public void testCreateTransactionManagerWithNormalPolicy() {
        BigInteger chainId = BigInteger.valueOf(1);
        EthNoncePolicyEnum noncePolicy = EthNoncePolicyEnum.NORMAL;
        Web3j web3j = mock(Web3j.class);
        TxSignService txSignService = mock(TxSignService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        EthBlobForkConfig ethBlobForkConfig = mock(EthBlobForkConfig.class);
        RemoteNonceManager nonceManager = mock(RemoteNonceManager.class);

        BaseRawTransactionManager result = BlockchainUtils.createTransactionManager(
                chainId, noncePolicy, web3j, txSignService, redisson, ethBlobForkConfig, nonceManager
        );

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof AcbRawTransactionManager);
    }

    @Test
    public void testCreateGasPriceProviderWithStaticPolicy() {
        Web3j web3j = mock(Web3j.class);
        GasPricePolicyEnum gasPricePolicy = GasPricePolicyEnum.STATIC;
        BigInteger staticGasPrice = BigInteger.valueOf(1000000000L);
        BigInteger staticMaxFeePerGas = BigInteger.valueOf(2000000000L);
        BigInteger staticMaxPriorityFeePerGas = BigInteger.valueOf(1500000000L);

        IGasPriceProvider result = BlockchainUtils.createGasPriceProvider(
                web3j, gasPricePolicy, staticGasPrice, staticMaxFeePerGas, staticMaxPriorityFeePerGas,
                null, null
        );

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof StaticGasPriceProvider);
    }
}
