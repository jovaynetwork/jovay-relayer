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

package com.alipay.antchain.l2.relayer.metrics;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.monitor.AccountBalanceMonitor;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountBalanceMonitorTest extends TestBase {

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private AccountBalanceMonitor monitor;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    @SneakyThrows
    public void testMonitorL1() {
        var legacyTxManager = mock(BaseRawTransactionManager.class);
        var blobTxManager = mock(BaseRawTransactionManager.class);
        when(legacyTxManager.getAddress()).thenReturn("0x123");
        when(blobTxManager.getAddress()).thenReturn("0x456");
        when(l1Client.getLegacyPoolTxManager()).thenReturn(legacyTxManager);
        when(l1Client.getBlobPoolTxManager()).thenReturn(blobTxManager);
        when(l1Client.queryAccountBalance(anyString(), notNull())).thenReturn(
                Convert.toWei(BigDecimal.valueOf(3.1), Convert.Unit.ETHER).toBigInteger(),
                Convert.toWei(BigDecimal.valueOf(2.1), Convert.Unit.ETHER).toBigInteger()
        );
        monitor.monitorL1Acc();
        monitor.monitorL1Acc();
    }
}
