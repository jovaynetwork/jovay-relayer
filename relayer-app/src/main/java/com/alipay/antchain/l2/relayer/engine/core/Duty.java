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
 * Duty会定时轮询任务表，读取当前时间片属于本节点的任务，交给系列职能线程池处理
 */
@Component
@Slf4j
public class Duty {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private ScheduleContext scheduleContext;

    @Getter
    @Value("${l2-relayer.engine.schedule.duty.dt_task.time_slice:180000}")
    private long timeSliceLength;

    @Resource
    private Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

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
            task.setTimeSliceLength(timeSliceLength);
            scheduleTaskExecutorMap.get(task.getTaskType()).execute(task);
        }
    }
}
