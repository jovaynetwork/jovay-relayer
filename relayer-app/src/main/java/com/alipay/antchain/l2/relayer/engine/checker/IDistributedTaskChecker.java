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

import java.util.concurrent.CompletableFuture;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;

/**
 * Distributed task checker interface for coordinating task execution across multiple instances.
 * <p>
 * This interface provides methods to manage and check the execution status of distributed tasks,
 * ensuring that only one instance executes a specific task type at a time in a distributed environment.
 * It uses CompletableFuture to track local task execution and distributed locks to coordinate
 * across instances.
 * </p>
 */
public interface IDistributedTaskChecker {

    /**
     * Add a local task future for tracking.
     * <p>
     * This registers a CompletableFuture for a specific task type, allowing the checker
     * to monitor the task's completion status and coordinate with other instances.
     * </p>
     *
     * @param taskType the business task type
     * @param future   the CompletableFuture representing the task execution
     */
    void addLocalFuture(BizTaskTypeEnum taskType, CompletableFuture<Void> future);

    /**
     * Check if the task should continue execution.
     * <p>
     * This method checks whether the current instance should continue executing the specified
     * task type. It considers factors such as:
     * <ul>
     *     <li>Whether another instance is already executing this task</li>
     *     <li>Whether the local task is still running</li>
     *     <li>Distributed lock status</li>
     * </ul>
     * </p>
     *
     * @param taskType the business task type to check
     * @return true if the task should continue, false otherwise
     */
    boolean checkIfContinue(BizTaskTypeEnum taskType);
}
