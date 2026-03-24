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

package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ClassUtil;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Base executor for distributed scheduled tasks.
 * <p>
 * This abstract class provides a framework for executing distributed tasks asynchronously
 * using a thread pool. It handles task scheduling, execution control, and graceful shutdown.
 * <p>
 * Key features:
 * <ul>
 *   <li>Asynchronous task execution using ExecutorService</li>
 *   <li>Distributed task coordination and checking</li>
 *   <li>Time slice management for task execution</li>
 *   <li>Graceful shutdown with configurable timeout</li>
 * </ul>
 */
@Slf4j
@Getter
public abstract class BaseScheduleTaskExecutor {

    /**
     * The executor service for running tasks asynchronously.
     */
    private final ExecutorService executor;

    /**
     * The checker for coordinating distributed task execution.
     */
    private final IDistributedTaskChecker distributedTaskChecker;

    /**
     * The maximum time in milliseconds to wait for task termination during shutdown.
     * Use -1 to wait indefinitely.
     */
    private final long awaitTerminateTime;

    /**
     * Constructs a new BaseScheduleTaskExecutor.
     *
     * @param executor                  the executor service for running tasks
     * @param distributedTaskChecker    the checker for distributed task coordination
     * @param awaitTerminateTime        the maximum time in milliseconds to wait during shutdown,
     *                                  or -1 to wait indefinitely
     * @throws IllegalArgumentException if executor is null
     */
    public BaseScheduleTaskExecutor(ExecutorService executor, IDistributedTaskChecker distributedTaskChecker, long awaitTerminateTime) {
        Assert.notNull(executor);
        this.executor = executor;
        this.distributedTaskChecker = distributedTaskChecker;
        this.awaitTerminateTime = awaitTerminateTime;
    }

    /**
     * Executes a distributed task asynchronously.
     * <p>
     * This method performs the following checks before execution:
     * <ul>
     *   <li>Verifies the task's time slice has not expired</li>
     *   <li>Checks if the task is already running locally or remotely</li>
     * </ul>
     * If all checks pass, the task is submitted to the executor service.
     *
     * @param task the distributed task to execute
     */
    @Synchronized
    public void execute(IDistributedTask task) {

        BizTaskTypeEnum taskType = task.getTaskType();
        // 判断时间片是否结束
        if (task.ifFinish()) {
            log.debug("task out of time slice : {}", taskType);
            return;
        }

        // 该任务是否已经在执行
        if (!distributedTaskChecker.checkIfContinue(taskType)) {
            log.debug("task {} is running locally or on other node remotely", taskType);
            return;
        }

        // 触发执行
        log.debug("execute task : {}", taskType);
        distributedTaskChecker.addLocalFuture(taskType, CompletableFuture.runAsync(genTask(task), executor));
    }

    //*******************************************
    // Abstract methods for subclass implementation
    //*******************************************

    /**
     * Generates a runnable task from the distributed task.
     * <p>
     * Subclasses must implement this method to define the actual task execution logic.
     *
     * @param task the distributed task
     * @return a runnable that executes the task logic
     */
    public abstract Runnable genTask(IDistributedTask task);

    /**
     * Shuts down the executor service and waits for task completion.
     * <p>
     * If awaitTerminateTime is -1, waits indefinitely for all tasks to complete.
     * Otherwise, waits for the specified time in milliseconds.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void shutdown() throws InterruptedException {
        if (getExecutor().isShutdown()) {
            return;
        }
        getExecutor().shutdown();
        if (awaitTerminateTime == -1) {
            // Wait indefinitely for all tasks to complete
            getExecutor().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } else {
            if (!getExecutor().awaitTermination(awaitTerminateTime, TimeUnit.MILLISECONDS)) {
                log.error("shutdown {} executor service timeout", ClassUtil.getClassName(this, true));
            }
        }
    }
}
