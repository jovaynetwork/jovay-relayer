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

import java.math.BigInteger;

import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecsNetwork;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.alarm.RollupAlarm;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

public class RollupConfigTest extends TestBase {

    @MockitoBean
    private BlockchainConfig blockchainConfig;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupAlarm rollupAlarm;

    @jakarta.annotation.Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Test
    @SneakyThrows
    public void testRollupSpecsMainnet() {
        var rollupConfig = new RollupConfig();
        var rollupSpecsNetworkField = ReflectUtil.getField(RollupConfig.class, "rollupSpecsNetwork");
        rollupSpecsNetworkField.setAccessible(true);
        rollupSpecsNetworkField.set(rollupConfig, RollupSpecsNetwork.MAINNET);
        var l1ChainIdField = ReflectUtil.getField(RollupConfig.class, "l1ChainId");
        l1ChainIdField.setAccessible(true);
        l1ChainIdField.set(rollupConfig, BigInteger.ONE);
        var l2ChainIdField = ReflectUtil.getField(RollupConfig.class, "l2ChainId");
        l2ChainIdField.setAccessible(true);
        l2ChainIdField.set(rollupConfig, BigInteger.valueOf(5734951));

        var rollupSpecs = rollupConfig.rollupSpecs();
        Assert.assertEquals(RollupSpecsNetwork.MAINNET, rollupSpecs.getNetwork());
        Assert.assertEquals(BigInteger.ONE, rollupSpecs.getLayer1ChainId());
        Assert.assertEquals(BigInteger.valueOf(5734951), rollupSpecs.getLayer2ChainId());
        var forkInfo = rollupSpecs.getFork(1);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V1, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(System.currentTimeMillis());
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V2, forkInfo.getBatchVersion());
    }

    @Test
    @SneakyThrows
    public void testRollupSpecsTestnet() {
        var rollupConfig = new RollupConfig();
        var rollupSpecsNetworkField = ReflectUtil.getField(RollupConfig.class, "rollupSpecsNetwork");
        rollupSpecsNetworkField.setAccessible(true);
        rollupSpecsNetworkField.set(rollupConfig, RollupSpecsNetwork.TESTNET);
        var l1ChainIdField = ReflectUtil.getField(RollupConfig.class, "l1ChainId");
        l1ChainIdField.setAccessible(true);
        l1ChainIdField.set(rollupConfig, BigInteger.valueOf(11155111));
        var l2ChainIdField = ReflectUtil.getField(RollupConfig.class, "l2ChainId");
        l2ChainIdField.setAccessible(true);
        l2ChainIdField.set(rollupConfig, BigInteger.valueOf(2019775));

        var rollupSpecs = rollupConfig.rollupSpecs();
        Assert.assertEquals(RollupSpecsNetwork.TESTNET, rollupSpecs.getNetwork());
        Assert.assertEquals(BigInteger.valueOf(11155111), rollupSpecs.getLayer1ChainId());
        Assert.assertEquals(BigInteger.valueOf(2019775), rollupSpecs.getLayer2ChainId());
        var forkInfo = rollupSpecs.getFork(1);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V0, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(1727170712000L);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V0, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(1758327170000L);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V1, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(1758328170000L);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V1, forkInfo.getBatchVersion());
    }

    @Test
    @SneakyThrows
    public void testRollupSpecsPrivateNet() {
        var rollupConfig = new RollupConfig();
        var rollupSpecsNetworkField = ReflectUtil.getField(RollupConfig.class, "rollupSpecsNetwork");
        rollupSpecsNetworkField.setAccessible(true);
        rollupSpecsNetworkField.set(rollupConfig, RollupSpecsNetwork.PRIVATE_NET);
        var rollupPrivateNetFile = ReflectUtil.getField(RollupConfig.class, "privateNetFile");
        rollupPrivateNetFile.setAccessible(true);
        var privateNetFileResourceMock = Mockito.mock(Resource.class);
        Mockito.when(privateNetFileResourceMock.getContentAsString(ArgumentMatchers.notNull())).thenReturn("""
                {
                  "network": "private_net",
                  "layer2_chain_id": "2019775",
                  "layer1_chain_id": "11155111",
                  "forks": {
                    "1": {
                      "batch_version": 0
                    },
                    "14400": {
                      "batch_version": 1
                    }
                  }
                }""");
        rollupPrivateNetFile.set(rollupConfig, privateNetFileResourceMock);
        var l1ChainIdField = ReflectUtil.getField(RollupConfig.class, "l1ChainId");
        l1ChainIdField.setAccessible(true);
        l1ChainIdField.set(rollupConfig, BigInteger.valueOf(11155111));
        var l2ChainIdField = ReflectUtil.getField(RollupConfig.class, "l2ChainId");
        l2ChainIdField.setAccessible(true);
        l2ChainIdField.set(rollupConfig, BigInteger.valueOf(2019775));

        var rollupSpecs = rollupConfig.rollupSpecs();
        Assert.assertEquals(RollupSpecsNetwork.PRIVATE_NET, rollupSpecs.getNetwork());
        Assert.assertEquals(BigInteger.valueOf(11155111), rollupSpecs.getLayer1ChainId());
        Assert.assertEquals(BigInteger.valueOf(2019775), rollupSpecs.getLayer2ChainId());
        var forkInfo = rollupSpecs.getFork(1);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V0, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(1000);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V0, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(14400);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V1, forkInfo.getBatchVersion());
        forkInfo = rollupSpecs.getFork(24400);
        Assert.assertNotNull(forkInfo);
        Assert.assertEquals(BatchVersionEnum.BATCH_V1, forkInfo.getBatchVersion());
    }

    @Test
    @SneakyThrows
    public void testContractErrorParser() {
        var contractConfig = new ContractConfig();
        var resourcePatternResolverField = ReflectUtil.getField(ContractConfig.class, "resourcePatternResolver");
        resourcePatternResolverField.setAccessible(true);
        resourcePatternResolverField.set(contractConfig, resourcePatternResolver);
        var contractErrorParser = contractConfig.contractErrorParser();

        var res = contractErrorParser.parse("0xff04ba37");
        Assert.assertNotNull(res);
        Assert.assertEquals("Rollup", res.contractName());
        Assert.assertEquals("NotSupportZkProof:[]", res.getReason());

        res = contractErrorParser.parse("0xff04ba38");
        Assert.assertNull(res);

        res = contractErrorParser.parse("0x6dfcc65000000000000000000000000000000000000000000000000000000000000000f8000000000000000000000000000000000000000000000000000000000000007b");
        Assert.assertNotNull(res);
        Assert.assertEquals("DcapAttestationRouter", res.contractName());
        Assert.assertEquals("SafeCastOverflowedUintDowncast:[248, 123]", res.getReason());

        res = contractErrorParser.parse("0x0a2a914200000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000005");
        Assert.assertNotNull(res);
        Assert.assertEquals("PCCSRouter", res.contractName());
        Assert.assertEquals("QEIdentityExpiredOrNotFound:[2, 5]", res.getReason());

        res = contractErrorParser.parse("\"0x0a2a914200000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000005\"");
        Assert.assertNotNull(res);
        Assert.assertEquals("PCCSRouter", res.contractName());
        Assert.assertEquals("QEIdentityExpiredOrNotFound:[2, 5]", res.getReason());
    }
}
