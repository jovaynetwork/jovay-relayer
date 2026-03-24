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

import jakarta.annotation.Resource;

import cn.hutool.core.thread.ThreadUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.engine.executor.*;
import com.alipay.antchain.l2.relayer.service.IRollupService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

public class ExecutorTest extends TestBase {

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IRollupService rollupService;

    @MockitoBean
    private ISystemConfigRepository systemConfigRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private BlockPollingTaskExecutor blockPollingTaskExecutor;

    @Resource
    private BatchCommitTaskExecutor batchCommitTaskExecutor;

    @Resource
    private BatchProveTaskExecutor batchProveTaskExecutor;

    @Resource
    private ProofCommitTaskExecutor proofCommitTaskExecutor;

    @Resource
    private ReliableTxTaskExecutor reliableTxTaskExecutor;

    @Resource
    private L1BlockPollingTaskExecutor l1BlockPollingTaskExecutor;

    @Resource
    private OracleGasFeedTaskExecutor oracleGasFeedTaskExecutor;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    public void testBaseScheduleTaskExecutor() {
        IDistributedTaskChecker checker = mock(IDistributedTaskChecker.class);
        when(checker.checkIfContinue(notNull())).thenReturn(true);
        BaseScheduleTaskExecutor executor = new BaseScheduleTaskExecutor(ThreadUtil.newExecutor(), checker, 3000) {
            @Override
            public Runnable genTask(IDistributedTask task) {
                return () -> System.out.println("test");
            }
        };

        executor.execute(new BizDistributedTask("test", BizTaskTypeEnum.RELIABLE_TX_TASK, "", System.currentTimeMillis()));
        verify(checker, times(1)).addLocalFuture(any(), any());

        when(checker.checkIfContinue(notNull())).thenReturn(false);
        clearInvocations(checker);
        executor.execute(new BizDistributedTask("test", BizTaskTypeEnum.RELIABLE_TX_TASK, "", System.currentTimeMillis()));
        verify(checker, never()).addLocalFuture(any(), any());

        clearInvocations(checker);
        executor.execute(new BizDistributedTask("test", BizTaskTypeEnum.RELIABLE_TX_TASK, "", 0));
        verify(checker, never()).checkIfContinue(any());
    }

    @Test
    public void testBlockPollingTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        blockPollingTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        blockPollingTaskExecutor.genTask(null).run();

        blockPollingTaskExecutor.shutdown();
    }

    @Test
    public void testBatchCommitTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        batchCommitTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        batchCommitTaskExecutor.genTask(null).run();

        batchCommitTaskExecutor.shutdown();
    }

    @Test
    public void testBatchProveTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        batchProveTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        batchProveTaskExecutor.genTask(null).run();

        batchProveTaskExecutor.shutdown();
    }

    @Test
    public void testProofCommitTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        proofCommitTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        proofCommitTaskExecutor.genTask(null).run();

        proofCommitTaskExecutor.shutdown();
    }

    @Test
    public void testReliableTxTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        reliableTxTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        reliableTxTaskExecutor.genTask(null).run();

        reliableTxTaskExecutor.shutdown();
    }

    @Test
    public void testL1BlockPollingTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        l1BlockPollingTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        l1BlockPollingTaskExecutor.genTask(null).run();

        l1BlockPollingTaskExecutor.shutdown();
    }

    @Test
    public void testOracleGasFeedTaskExecutor() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        oracleGasFeedTaskExecutor.genTask(null).run();

        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        oracleGasFeedTaskExecutor.genTask(null).run();

        oracleGasFeedTaskExecutor.shutdown();
    }
}
