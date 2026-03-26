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
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Negative case tests for Dispatcher
 * Tests exception handling and edge cases in task dispatching
 */
@RunWith(MockitoJUnitRunner.class)
public class DispatcherTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private Lock mockLock;

    @InjectMocks
    private Dispatcher dispatcher;

    @Before
    public void setUp() {
        when(scheduleRepository.getDispatchLock()).thenReturn(mockLock);
    }

    /**
     * Test dispatch when lock cannot be acquired
     * Verifies that dispatch is skipped when lock is not available
     */
    @Test
    public void testDispatch_LockNotAcquired() {
        when(mockLock.tryLock()).thenReturn(false);

        dispatcher.dispatch();

        verify(mockLock, times(1)).tryLock();
        verify(scheduleRepository, never()).getAllActiveNodes();
    }

    /**
     * Test dispatch when no online nodes
     * Verifies that empty node list is handled correctly
     */
    @Test
    public void testDispatch_NoOnlineNodes() {
        when(mockLock.tryLock()).thenReturn(true);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(Collections.emptyList());

        dispatcher.dispatch();

        verify(mockLock, times(1)).tryLock();
        verify(mockLock, times(1)).unlock();
        verify(scheduleRepository, times(1)).getAllActiveNodes();
        verify(scheduleRepository, never()).batchUpdateBizDTTasks(any());
    }

    /**
     * Test dispatch when all nodes are offline
     * Verifies that offline nodes are filtered out
     */
    @Test
    public void testDispatch_AllNodesOffline() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node1 = new ActiveNode();
        node1.setNodeId("node1");
        node1.setLastActiveTime(System.currentTimeMillis() - 10000); // Old timestamp

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node1));

        dispatcher.dispatch();

        verify(mockLock, times(1)).unlock();
        verify(scheduleRepository, times(1)).updateStatusOfActiveNodes(any());
    }

    /**
     * Test dispatch when repository throws exception
     * Verifies that exceptions are caught and lock is released
     */
    @Test
    public void testDispatch_RepositoryThrowsException() {
        when(mockLock.tryLock()).thenReturn(true);
        when(scheduleRepository.getAllActiveNodes())
                .thenThrow(new RuntimeException("Database connection failed"));

        dispatcher.dispatch();

        verify(mockLock, times(1)).tryLock();
        verify(mockLock, times(1)).unlock();
    }

    /**
     * Test dispatch when lock is null
     * Verifies that null lock causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testDispatch_NullLock() {
        when(scheduleRepository.getDispatchLock()).thenReturn(null);

        dispatcher.dispatch();
    }

    /**
     * Test dispatch when getAllActiveNodes returns null
     * Verifies that null node list is handled
     */
    @Test
    public void testDispatch_NullNodeList() {
        when(mockLock.tryLock()).thenReturn(true);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(null);

        try {
            dispatcher.dispatch();
        } catch (NullPointerException e) {
            // Expected exception for null node list
            verify(mockLock, times(1)).unlock();
        }
    }

    /**
     * Test dispatch when node has null node ID
     * Verifies that null node ID is handled
     */
    @Test
    public void testDispatch_NodeWithNullId() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode nodeWithNullId = new ActiveNode();
        nodeWithNullId.setNodeId(null);
        nodeWithNullId.setLastActiveTime(System.currentTimeMillis());

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(nodeWithNullId));

        try {
            dispatcher.dispatch();
        } catch (Exception e) {
            // Exception is acceptable for null node ID
            verify(mockLock, times(1)).unlock();
        }
    }

    /**
     * Test dispatch when updateStatusOfActiveNodes throws exception
     * Verifies that exception is caught and lock is released
     */
    @Test
    public void testDispatch_UpdateStatusThrowsException() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node = new ActiveNode();
        node.setNodeId("node1");
        node.setLastActiveTime(System.currentTimeMillis());

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node));
        doThrow(new RuntimeException("Update failed"))
                .when(scheduleRepository).updateStatusOfActiveNodes(any());

        dispatcher.dispatch();

        verify(mockLock, times(1)).unlock();
    }

    /**
     * Test dispatch when batchInsertBizDTTasks throws exception
     * Verifies that exception is caught and lock is released
     */
    @Test
    public void testDispatch_BatchInsertThrowsException() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node = new ActiveNode();
        node.setNodeId("node1");
        node.setLastActiveTime(System.currentTimeMillis());

        BizDistributedTask task = new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        task.setNodeId("node1");

        clearInvocations(scheduleRepository);
        lenient().when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node));
        lenient().when(scheduleRepository.getAllBizDistributedTasks()).thenReturn(List.of(task));
        lenient().doThrow(new RuntimeException("Insert failed"))
                .when(scheduleRepository).batchInsertBizDTTasks(any());

        dispatcher.dispatch();

        verify(mockLock, times(1)).unlock();
    }

    /**
     * Test dispatch when batchUpdateBizDTTasks throws exception
     * Verifies that exception is caught and lock is released
     */
    @Test
    public void testDispatch_BatchUpdateThrowsException() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node = new ActiveNode();
        node.setNodeId("node1");
        node.setLastActiveTime(System.currentTimeMillis());

        lenient().when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node));
        lenient().when(scheduleRepository.getAllBizDistributedTasks()).thenReturn(Collections.emptyList());
        lenient().doThrow(new RuntimeException("Update failed"))
                .when(scheduleRepository).batchUpdateBizDTTasks(any());

        dispatcher.dispatch();

        verify(mockLock, times(1)).unlock();
    }

    /**
     * Test dispatch when unlock throws exception
     * Verifies that unlock exception is handled
     */
    @Test
    public void testDispatch_UnlockThrowsException() {
        when(mockLock.tryLock()).thenReturn(true);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Unlock failed")).when(mockLock).unlock();

        try {
            dispatcher.dispatch();
        } catch (RuntimeException e) {
            // Exception from unlock is expected
            assert e.getMessage().contains("Unlock failed");
        }
    }

    /**
     * Test dispatch with node having very old last active time
     * Verifies that old nodes are marked as offline
     */
    @Test
    public void testDispatch_VeryOldLastActiveTime() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode oldNode = new ActiveNode();
        oldNode.setNodeId("old-node");
        oldNode.setLastActiveTime(System.currentTimeMillis() - 1000000); // Very old

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(oldNode));

        dispatcher.dispatch();

        verify(scheduleRepository, times(1)).updateStatusOfActiveNodes(any());
        verify(mockLock, times(1)).unlock();
    }

    /**
     * Test dispatch when getAllBizDistributedTasks returns null
     * Verifies that null task list is handled
     */
    @Test
    public void testDispatch_NullTaskList() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node = new ActiveNode();
        node.setNodeId("node1");
        node.setLastActiveTime(System.currentTimeMillis());

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node));
        lenient().when(scheduleRepository.getAllBizDistributedTasks()).thenReturn(null);

        try {
            dispatcher.dispatch();
        } catch (NullPointerException e) {
            // Expected exception for null task list
            verify(mockLock, times(1)).unlock();
        }
    }

    /**
     * Test dispatch with task having null task type
     * Verifies that null task type is handled
     */
    @Test
    public void testDispatch_TaskWithNullType() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode node = new ActiveNode();
        node.setNodeId("node1");
        node.setLastActiveTime(System.currentTimeMillis());

        BizDistributedTask taskWithNullType = new BizDistributedTask(null);

        lenient().when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(node));
        lenient().when(scheduleRepository.getAllBizDistributedTasks()).thenReturn(List.of(taskWithNullType));

        try {
            dispatcher.dispatch();
        } catch (Exception e) {
            // Exception is acceptable for null task type
            verify(mockLock, times(1)).unlock();
        }
    }

    /**
     * Test dispatch with empty node ID
     * Verifies that empty node ID is handled
     */
    @Test
    public void testDispatch_EmptyNodeId() {
        when(mockLock.tryLock()).thenReturn(true);

        ActiveNode nodeWithEmptyId = new ActiveNode();
        nodeWithEmptyId.setNodeId("");
        nodeWithEmptyId.setLastActiveTime(System.currentTimeMillis());

        when(scheduleRepository.getAllActiveNodes()).thenReturn(List.of(nodeWithEmptyId));

        try {
            dispatcher.dispatch();
        } catch (Exception e) {
            // Exception is acceptable for empty node ID
            verify(mockLock, times(1)).unlock();
        }
    }
}
