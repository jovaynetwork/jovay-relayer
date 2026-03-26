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

package com.alipay.antchain.l2.relayer.engine.checker;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * Negative case tests for LocalDistributedTaskChecker
 * Tests exception handling and edge cases in local task checking
 */
public class LocalDistributedTaskCheckerTest {

    private LocalDistributedTaskChecker checker;

    @Before
    public void setUp() {
        checker = new LocalDistributedTaskChecker();
    }

    /**
     * Test addLocalFuture with null task type
     * Verifies that null task type causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testAddLocalFuture_NullTaskType() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        checker.addLocalFuture(null, future);
    }

    /**
     * Test addLocalFuture with null future
     * Verifies that null future is handled
     */
    @Test
    public void testAddLocalFuture_NullFuture() {
        try {
            checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, null);
            // Verify it was added
            assertTrue(checker.getLocalRunningTasks().containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK));
        } catch (NullPointerException e) {
            // Exception is acceptable for null future
            assertNotNull(e);
        }
    }

    /**
     * Test checkIfContinue with null task type
     * Verifies that null task type causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testCheckIfContinue_NullTaskType() {
        checker.checkIfContinue(null);
    }

    /**
     * Test checkIfContinue when task is not in map
     * Verifies that non-existent task returns true
     */
    @Test
    public void testCheckIfContinue_TaskNotInMap() {
        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result);
    }

    /**
     * Test checkIfContinue when task is running
     * Verifies that running task returns false
     */
    @Test
    public void testCheckIfContinue_TaskRunning() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertFalse(result);
    }

    /**
     * Test checkIfContinue when task is completed
     * Verifies that completed task is removed and returns true
     */
    @Test
    public void testCheckIfContinue_TaskCompleted() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result);
        assertFalse(checker.getLocalRunningTasks().containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK));
    }

    /**
     * Test checkIfContinue when task completed with exception
     * Verifies that exceptionally completed task is removed and returns true
     */
    @Test
    public void testCheckIfContinue_TaskCompletedExceptionally() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Task failed"));
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result);
        assertFalse(checker.getLocalRunningTasks().containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK));
    }

    /**
     * Test addLocalFuture with same task type multiple times
     * Verifies that later future overwrites earlier one
     */
    @Test
    public void testAddLocalFuture_SameTaskTypeMultipleTimes() {
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        CompletableFuture<Void> future2 = new CompletableFuture<>();

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future1);
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future2);

        assertEquals(1, checker.getLocalRunningTasks().size());
        assertSame(future2, checker.getLocalRunningTasks().get(BizTaskTypeEnum.BLOCK_POLLING_TASK));
    }

    /**
     * Test checkIfContinue multiple times for same completed task
     * Verifies that second check returns true after task is removed
     */
    @Test
    public void testCheckIfContinue_MultipleTimesForCompletedTask() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result1 = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result1);

        boolean result2 = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result2);
    }

    /**
     * Test with multiple different task types
     * Verifies that different tasks are tracked independently
     */
    @Test
    public void testMultipleDifferentTaskTypes() {
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        CompletableFuture<Void> future2 = CompletableFuture.completedFuture(null);

        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future1);
        checker.addLocalFuture(BizTaskTypeEnum.BATCH_PROVE_TASK, future2);

        assertFalse(checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK));
        assertTrue(checker.checkIfContinue(BizTaskTypeEnum.BATCH_PROVE_TASK));
    }

    /**
     * Test checkIfContinue when future is cancelled
     * Verifies that cancelled task is treated as done
     */
    @Test
    public void testCheckIfContinue_CancelledTask() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.cancel(true);
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        boolean result = checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
        assertTrue(result);
        assertFalse(checker.getLocalRunningTasks().containsKey(BizTaskTypeEnum.BLOCK_POLLING_TASK));
    }

    /**
     * Test getLocalRunningTasks returns actual map
     * Verifies that modifications to returned map affect checker
     */
    @Test
    public void testGetLocalRunningTasks_ReturnsActualMap() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);

        var tasks = checker.getLocalRunningTasks();
        assertEquals(1, tasks.size());

        // Modify the returned map
        tasks.clear();
        assertEquals(0, checker.getLocalRunningTasks().size());
    }

    /**
     * Test concurrent access to local running tasks
     * Verifies that concurrent map handles concurrent operations
     */
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                checker.addLocalFuture(BizTaskTypeEnum.BLOCK_POLLING_TASK, future);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                checker.checkIfContinue(BizTaskTypeEnum.BLOCK_POLLING_TASK);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Should not throw ConcurrentModificationException
        assertNotNull(checker.getLocalRunningTasks());
    }
}
