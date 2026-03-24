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

package com.alipay.antchain.l2.relayer.engine;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.checker.DefaultDistributedTaskChecker;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.Mockito.when;

public class CheckerTest extends TestBase {

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource(name = "defaultDistributedTaskChecker")
    private DefaultDistributedTaskChecker checker;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    @SneakyThrows
    public void testDefaultDistributedTaskChecker() {
        var future = CompletableFuture.runAsync(() -> {
            System.out.println("test");
            throw new RuntimeException();
        });
        checker.addLocalFuture(BizTaskTypeEnum.RELIABLE_TX_TASK, future);

        Assert.assertThrows(RuntimeException.class, future::join);

        Assert.assertTrue(checker.checkIfContinue(BizTaskTypeEnum.RELIABLE_TX_TASK));
    }
}
