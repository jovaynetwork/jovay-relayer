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

import java.util.List;
import java.util.Map;
import jakarta.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.executor.BaseScheduleTaskExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Duty periodically polls the task table, reads tasks belonging to the current node
 * in the current time slice, and hands them over to the corresponding executor thread pools.
 * <p>
 * The duty component is responsible for:
 * <ul>
 *   <li>Checking if the local node is active and online</li>
 *   <li>Retrieving tasks assigned to this node for the current time slice</li>
 *   <li>Dispatching tasks to their corresponding executors</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class Duty {

    /**
     * Repository for schedule-related data operations.
     */
    @Resource
    private IScheduleRepository scheduleRepository;

    /**
     * Context containing scheduling information for the current node.
     */
    @Resource
    private ScheduleContext scheduleContext;

    /**
     * The length of time slice for each task execution (in milliseconds).
     * <p>
     * Default value is 180,000 milliseconds (3 minutes).
     * </p>
     */
    @Getter
    @Value("${l2-relayer.engine.schedule.duty.dt_task.time_slice:180000}")
    private long timeSliceLength;

    /**
     * Map of task types to their corresponding executors.
     */
    @Resource
    private Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

    /**
     * Executes duty tasks assigned to this node.
     * <p>
     * This method:
     * <ol>
     *   <li>Checks if the local node is active and online</li>
     *   <li>Retrieves tasks assigned to this node for the current time slice</li>
     *   <li>Dispatches each task to its corresponding executor</li>
     * </ol>
     * </p>
     * <p>
     * If the node is offline or not yet activated, it will wait to be reactivated.
     * </p>
     */
    public void duty() {

        ActiveNode myselfNode = scheduleRepository.getActiveNodeByNodeId(this.scheduleContext.getNodeId());
        if (ObjectUtil.isNull(myselfNode) || myselfNode.getStatus() == ActiveNodeStatusEnum.OFFLINE) {
            log.info("⌛ local node waits to be reactivated...");
            return;
        }

        // 查询本节点的时间片任务
        List<BizDistributedTask> tasks = scheduleRepository.getBizDistributedTasksByNodeId(this.scheduleContext.getNodeId());
        if (tasks.isEmpty()) {
            log.debug("empty duty tasks");
        } else {
            log.debug("duty tasks size {}", tasks.size());
        }

        // 分配给各个职能线程池处理
        for (BizDistributedTask task : tasks) {
            if (!scheduleTaskExecutorMap.containsKey(task.getTaskType())) {
                log.warn("try to run task {} but found no executor, just skip it", task.getTaskType());
                continue;
            }
            task.setTimeSliceLength(timeSliceLength);
            scheduleTaskExecutorMap.get(task.getTaskType()).execute(task);
        }
    }
}
