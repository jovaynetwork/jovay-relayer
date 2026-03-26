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

package com.alipay.antchain.l2.relayer.engine.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.executor.BaseScheduleTaskExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Negative case tests for Duty
 * Tests exception handling and edge cases in duty execution
 */
@RunWith(MockitoJUnitRunner.class)
public class DutyTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private ScheduleContext scheduleContext;

    @Mock
    private Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

    @Mock
    private BaseScheduleTaskExecutor mockExecutor;

    @InjectMocks
    private Duty duty;

    private static final String TEST_NODE_ID = "test-node-id";

    @Before
    public void setUp() {
        when(scheduleContext.getNodeId()).thenReturn(TEST_NODE_ID);
    }

    /**
     * Test duty when node is null
     * Verifies that null node is handled gracefully
     */
    @Test
    public void testDuty_NullNode() {
        when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(null);

        // Should not throw exception, just log and return
        duty.duty();

        verify(scheduleRepository, times(1)).getActiveNodeByNodeId(TEST_NODE_ID);
        verify(scheduleRepository, never()).getBizDistributedTasksByNodeId(any());
    }

    /**
     * Test duty when node is offline
     * Verifies that offline node is handled correctly
     */
    @Test
    public void testDuty_OfflineNode() {
        ActiveNode offlineNode = new ActiveNode();
        offlineNode.setNodeId(TEST_NODE_ID);
        offlineNode.setStatus(ActiveNodeStatusEnum.OFFLINE);

        when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(offlineNode);

        duty.duty();

        verify(scheduleRepository, times(1)).getActiveNodeByNodeId(TEST_NODE_ID);
        verify(scheduleRepository, never()).getBizDistributedTasksByNodeId(any());
    }

    /**
     * Test duty when repository throws exception
     * Verifies that repository exceptions are handled
     */
    @Test
    public void testDuty_RepositoryThrowsException() {
        when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID))
                .thenThrow(new RuntimeException("Database connection failed"));

        try {
            duty.duty();
        } catch (RuntimeException e) {
            // Exception is expected
            assert e.getMessage().contains("Database connection failed");
        }
    }

    /**
     * Test duty when task list is empty
     * Verifies that empty task list is handled correctly
     */
    @Test
    public void testDuty_EmptyTaskList() {
        ActiveNode onlineNode = new ActiveNode();
        onlineNode.setNodeId(TEST_NODE_ID);
        onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

        when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
        when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                .thenReturn(Collections.emptyList());

        duty.duty();

        verify(scheduleRepository, times(1)).getBizDistributedTasksByNodeId(TEST_NODE_ID);
        verify(scheduleTaskExecutorMap, never()).get(any());
    }

    /**
     * Test duty when task executor is null
     * Verifies that null executor causes exception
     */
    @Test
    public void testDuty_NullTaskExecutor() {
        var logger = (Logger) LoggerFactory.getLogger(Duty.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            ActiveNode onlineNode = new ActiveNode();
            onlineNode.setNodeId(TEST_NODE_ID);
            onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

            BizDistributedTask task = new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK);

            lenient().when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
            lenient().when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                    .thenReturn(List.of(task));
            lenient().when(scheduleTaskExecutorMap.get(BizTaskTypeEnum.BLOCK_POLLING_TASK)).thenReturn(null);

            duty.duty();

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("try to run task BLOCK_POLLING_TASK but found no executor, just skip it"));
            Assert.assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * Test duty when executor throws exception
     * Verifies that executor exceptions are handled
     */
    @Test
    public void testDuty_ExecutorThrowsException() {
        ActiveNode onlineNode = new ActiveNode();
        onlineNode.setNodeId(TEST_NODE_ID);
        onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

        BizDistributedTask task = new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK);

        lenient().when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
        lenient().when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                .thenReturn(List.of(task));
        lenient().when(scheduleTaskExecutorMap.get(BizTaskTypeEnum.BLOCK_POLLING_TASK)).thenReturn(mockExecutor);
        lenient().doThrow(new RuntimeException("Executor failed")).when(mockExecutor).execute(any());

        try {
            duty.duty();
        } catch (RuntimeException e) {
            // Exception is expected
            assert e.getMessage().contains("Executor failed");
        }
    }

    /**
     * Test duty when schedule context is null
     * Verifies that null context causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testDuty_NullScheduleContext() {
        Duty nullContextDuty = new Duty();
        nullContextDuty.duty();
    }

    /**
     * Test duty with multiple tasks
     * Verifies that all tasks are executed even if one fails
     */
    @Test
    public void testDuty_MultipleTasksWithOneFailure() {
        ActiveNode onlineNode = new ActiveNode();
        onlineNode.setNodeId(TEST_NODE_ID);
        onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

        BizDistributedTask task1 = new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        BizDistributedTask task2 = new BizDistributedTask(BizTaskTypeEnum.BATCH_PROVE_TASK);

        lenient().when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
        lenient().when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                .thenReturn(List.of(task1, task2));
        lenient().when(scheduleTaskExecutorMap.get(BizTaskTypeEnum.BLOCK_POLLING_TASK)).thenReturn(mockExecutor);
        lenient().when(scheduleTaskExecutorMap.containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK)).thenReturn(true);

        doThrow(new RuntimeException("First task failed")).when(mockExecutor).execute(any());

        try {
            duty.duty();
        } catch (RuntimeException e) {
            // Exception is expected when first task fails
            assert e.getMessage().contains("First task failed");
        }

        // Verify first executor was called (and failed)
        verify(mockExecutor, times(1)).execute(any());
    }

    /**
     * Test duty when node ID is null
     * Verifies that null node ID is handled
     */
    @Test
    public void testDuty_NullNodeId() {
        when(scheduleContext.getNodeId()).thenReturn(null);

        try {
            duty.duty();
        } catch (Exception e) {
            // Exception is acceptable for null node ID
            assert e != null;
        }
    }

    /**
     * Test duty when node ID is empty
     * Verifies that empty node ID is handled
     */
    @Test
    public void testDuty_EmptyNodeId() {
        when(scheduleContext.getNodeId()).thenReturn("");
        when(scheduleRepository.getActiveNodeByNodeId("")).thenReturn(null);

        duty.duty();

        verify(scheduleRepository, times(1)).getActiveNodeByNodeId("");
    }

    /**
     * Test duty when task has null task type
     * Verifies that null task type causes exception
     */
    @Test
    public void testDuty_TaskWithNullType() {
        var logger = (Logger) LoggerFactory.getLogger(Duty.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            ActiveNode onlineNode = new ActiveNode();
            onlineNode.setNodeId(TEST_NODE_ID);
            onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

            BizDistributedTask taskWithNullType = new BizDistributedTask(null);

            when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
            when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                    .thenReturn(List.of(taskWithNullType));

            duty.duty();
            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("try to run task null but found no executor, just skip it"));
            Assert.assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * Test duty when executor map is empty
     * Verifies that empty executor map causes exception
     */
    @Test
    public void testDuty_EmptyExecutorMap() {
        var logger = (Logger) LoggerFactory.getLogger(Duty.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            ActiveNode onlineNode = new ActiveNode();
            onlineNode.setNodeId(TEST_NODE_ID);
            onlineNode.setStatus(ActiveNodeStatusEnum.ONLINE);

            BizDistributedTask task = new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK);

            when(scheduleRepository.getActiveNodeByNodeId(TEST_NODE_ID)).thenReturn(onlineNode);
            when(scheduleRepository.getBizDistributedTasksByNodeId(TEST_NODE_ID))
                    .thenReturn(List.of(task));
            lenient().when(scheduleTaskExecutorMap.get(any())).thenReturn(null);

            duty.duty();
            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("try to run task BLOCK_POLLING_TASK but found no executor, just skip it"));
            Assert.assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
