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
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.relayer.engine.core.Duty;
import com.alipay.antchain.l2.relayer.engine.executor.BaseScheduleTaskExecutor;
import com.alipay.antchain.l2.relayer.service.AdminGrpcService;
import io.grpc.Server;
import jakarta.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.Mockito.*;

public class CoreStructsTest extends TestBase {

    @Resource
    private Activator activator;

    @Resource
    private Dispatcher dispatcher;

    @Resource
    private Duty duty;

    @MockitoBean
    private IScheduleRepository scheduleRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

    @TestBean
    private List<IDistributedTask> runningTaskList;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private AdminGrpcService adminGrpcService;

    @MockitoBean
    private Server adminGrpcServer;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    @Value("${l2-relayer.engine.schedule.activate.ttl:5000}")
    private long nodeTimeToLive;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    public void testActivator() {
        activator.activate();
        verify(scheduleRepository, times(1)).activate(anyString(), anyString());
    }

    @Test
    public void testDispatcher() {
        when(scheduleRepository.getDispatchLock()).thenReturn(new ReentrantLock());
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(
                new ActiveNode("test", "test", System.currentTimeMillis()),
                new ActiveNode("test1", "test1", System.currentTimeMillis() - nodeTimeToLive - 100),
                new ActiveNode("test2", "test2", System.currentTimeMillis())
        ), ListUtil.toList(
                new ActiveNode("test", "test", System.currentTimeMillis() - nodeTimeToLive - 100),
                new ActiveNode("test1", "test1", System.currentTimeMillis() - nodeTimeToLive - 100),
                new ActiveNode("test2", "test2", System.currentTimeMillis())
        ));
        when(scheduleRepository.getAllBizDistributedTasks()).thenReturn(
                ListUtil.toList(
                        new BizDistributedTask("test", BizTaskTypeEnum.BATCH_COMMIT_TASK, "", System.currentTimeMillis()),
                        new BizDistributedTask("test", BizTaskTypeEnum.BLOCK_POLLING_TASK, "", System.currentTimeMillis() - timeSliceLength - 100),
                        new BizDistributedTask("test1", BizTaskTypeEnum.BATCH_PROVE_TASK, "", System.currentTimeMillis() - timeSliceLength - 100)
                )
        );
        dispatcher.dispatch();

        verify(scheduleRepository, times(1)).updateStatusOfActiveNodes(
                argThat(argument -> argument.size() == 3
                                    && (argument.get(0).getNodeId().equals("test") && argument.get(0).getStatus() == ActiveNodeStatusEnum.ONLINE)
                                    && (argument.get(1).getNodeId().equals("test1") && argument.get(1).getStatus() == ActiveNodeStatusEnum.OFFLINE)
                                    && (argument.get(2).getNodeId().equals("test2") && argument.get(2).getStatus() == ActiveNodeStatusEnum.ONLINE))
        );
        verify(scheduleRepository, times(1)).batchInsertBizDTTasks(argThat(argument -> argument.size() == 6));
        verify(scheduleRepository, times(1)).batchUpdateBizDTTasks(argThat(
                argument ->
                        argument.size() == 8 && argument.stream()
                                .filter(bizDistributedTask -> bizDistributedTask.getTaskType() == BizTaskTypeEnum.BLOCK_POLLING_TASK)
                                .allMatch(bizDistributedTask -> bizDistributedTask.getNodeId().equals("test"))));

        dispatcher.dispatch();

        verify(scheduleRepository, times(1)).updateStatusOfActiveNodes(
                argThat(argument -> argument.size() == 3
                                    && (argument.get(0).getNodeId().equals("test") && argument.get(0).getStatus() == ActiveNodeStatusEnum.OFFLINE)
                                    && (argument.get(1).getNodeId().equals("test1") && argument.get(1).getStatus() == ActiveNodeStatusEnum.OFFLINE)
                                    && (argument.get(2).getNodeId().equals("test2") && argument.get(2).getStatus() == ActiveNodeStatusEnum.ONLINE))
        );
        verify(scheduleRepository, times(1)).batchUpdateBizDTTasks(argThat(
                argument ->
                        argument.size() == 8 && argument.stream()
                                .filter(bizDistributedTask -> bizDistributedTask.getTaskType() == BizTaskTypeEnum.BLOCK_POLLING_TASK)
                                .allMatch(bizDistributedTask -> bizDistributedTask.getNodeId().equals("test2"))));
    }

    @Test
    public void testDuty() {
        when(scheduleRepository.getActiveNodeByNodeId(notNull())).thenReturn(new ActiveNode("test", "test", System.currentTimeMillis()));
        when(scheduleRepository.getBizDistributedTasksByNodeId(anyString())).thenReturn(
                ListUtil.toList(new BizDistributedTask("test", BizTaskTypeEnum.BATCH_COMMIT_TASK, "", System.currentTimeMillis()))
        );
        BaseScheduleTaskExecutor executor = mock(BaseScheduleTaskExecutor.class);
        when(scheduleTaskExecutorMap.get(notNull())).thenReturn(executor);
        when(scheduleTaskExecutorMap.containsKey(notNull())).thenReturn(true);
        doNothing().when(executor).execute(notNull());

        duty.duty();

        verify(executor, times(1)).execute(notNull());
    }
}
