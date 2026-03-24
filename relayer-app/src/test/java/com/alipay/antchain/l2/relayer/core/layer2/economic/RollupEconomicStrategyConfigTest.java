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

package com.alipay.antchain.l2.relayer.core.layer2.economic;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.BlockchainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.alarm.RollupAlarm;
import com.alipay.antchain.l2.relayer.metrics.monitor.AccountBalanceMonitor;
import jakarta.annotation.Resource;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RollupEconomicStrategyConfigTest extends TestBase {

    private static final String L1_ECONOMIC_CONF_MAP_KEY = "relayer-dynamic-config@rollup-economic-strategy-conf";

    @Resource
    private RollupEconomicStrategyConfig strategyConfig;

    @MockitoBean
    private BlockchainConfig blockchainConfig;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupAlarm rollupAlarm;

    @MockitoBean
    private AccountBalanceMonitor accountBalanceMonitor;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private RedissonClient redisson;

    @Test
    public void testDefaultValues() {
        // clean the cache
        redisson.getMap(L1_ECONOMIC_CONF_MAP_KEY).clear();

        Assert.assertEquals(12, strategyConfig.getMaxPendingBatchCount());
        Assert.assertEquals(12, strategyConfig.getMaxPendingProofCount());
        Assert.assertEquals(43200L, strategyConfig.getMaxBatchWaitingTime());
        Assert.assertEquals(43200L, strategyConfig.getMaxProofWaitingTime());
        Assert.assertEquals(BigInteger.valueOf(3000000000L), strategyConfig.getMidEip1559PriceLimit());
        Assert.assertEquals(BigInteger.valueOf(8000000000L), strategyConfig.getHighEip1559PriceLimit());
    }

    @Test
    public void testSetterAndGetters() {
        strategyConfig.setMaxPendingBatchCount(200);
        Assert.assertEquals(200, strategyConfig.getMaxPendingBatchCount());

        strategyConfig.setMaxPendingProofCount(200);
        Assert.assertEquals(200, strategyConfig.getMaxPendingProofCount());

        strategyConfig.setMaxBatchWaitingTime(100L);
        Assert.assertEquals(100L, strategyConfig.getMaxBatchWaitingTime());

        strategyConfig.setMaxProofWaitingTime(100L);
        Assert.assertEquals(100L, strategyConfig.getMaxProofWaitingTime());

        strategyConfig.setMidEip1559PriceLimit(BigInteger.valueOf(100L));
        Assert.assertEquals(BigInteger.valueOf(100L), strategyConfig.getMidEip1559PriceLimit());

        strategyConfig.setHighEip1559PriceLimit(BigInteger.valueOf(100L));
        Assert.assertEquals(BigInteger.valueOf(100L), strategyConfig.getHighEip1559PriceLimit());

        // clean the cache
        redisson.getMap(L1_ECONOMIC_CONF_MAP_KEY).clear();
    }
}