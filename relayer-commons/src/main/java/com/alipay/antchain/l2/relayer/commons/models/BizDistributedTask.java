

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

package com.alipay.antchain.l2.relayer.commons.models;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Business distributed task model representing a task in the distributed scheduling system.
 * <p>
 * This class encapsulates information about a distributed task including:
 * <ul>
 *   <li>Node assignment: which node is responsible for executing the task</li>
 *   <li>Task type: the specific type of business task</li>
 *   <li>Time slice: the time window allocated for task execution</li>
 *   <li>Extension data: additional task-specific information</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class BizDistributedTask implements IDistributedTask {

    /**
     * The ID of the node assigned to execute this task.
     */
    private String nodeId = StrUtil.EMPTY;

    /**
     * The type of this business task.
     */
    private BizTaskTypeEnum taskType;

    /**
     * Extension data for task-specific information.
     */
    private String ext = StrUtil.EMPTY;

    /**
     * The start time of the task execution (in milliseconds).
     */
    private long startTime = 0;

    /**
     * The length of the time slice allocated for this task (in milliseconds).
     */
    private long timeSliceLength = 0;

    /**
     * Constructs a BizDistributedTask with only the task type.
     *
     * @param taskType the type of the business task
     */
    public BizDistributedTask(BizTaskTypeEnum taskType) {
        this.taskType = taskType;
    }

    /**
     * Constructs a BizDistributedTask with full parameters.
     *
     * @param nodeId    the ID of the node assigned to execute this task
     * @param taskType  the type of the business task
     * @param ext       extension data for task-specific information
     * @param startTime the start time of the task execution
     */
    public BizDistributedTask(
            String nodeId,
            BizTaskTypeEnum taskType,
            String ext,
            long startTime
    ) {
        this.nodeId = nodeId;
        this.taskType = taskType;
        this.ext = ext;
        this.startTime = startTime;
    }

    /**
     * Checks if the task has finished its time slice.
     *
     * @return {@code true} if the current time exceeds the task's time slice,
     *         {@code false} otherwise
     */
    public boolean ifFinish() {
        return (System.currentTimeMillis() - this.startTime) > timeSliceLength;
    }

    /**
     * Gets the end timestamp of the task's time slice.
     *
     * @return the end timestamp in milliseconds
     */
    public long getEndTimestamp() {
        return this.startTime + this.timeSliceLength;
    }
}
