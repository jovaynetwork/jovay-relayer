package com.alipay.antchain.l2.relayer.engine;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.relayer.engine.core.Duty;
import com.alipay.antchain.l2.relayer.engine.executor.BaseScheduleTaskExecutor;
import jakarta.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
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

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    @Value("${l2-relayer.engine.schedule.activate.ttl:5000}")
    private long nodeTimeToLive;

    @Before
    public void initMock() {
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000L);
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
        doNothing().when(executor).execute(notNull());

        duty.duty();

        verify(executor, times(1)).execute(notNull());
    }
}
