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

import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import org.springframework.stereotype.Component;

/**
 * Activator负责分布式节点的定时心跳，往全局DB定时登记心跳，表示节点活性
 */
@Component
public class Activator {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private ScheduleContext scheduleContext;

    /**
     * 往全局DB定时登记心跳，表示节点活性
     */
    public void activate() {
        scheduleRepository.activate(
                scheduleContext.getNodeId(),
                scheduleContext.getNodeIp()
        );
    }
}
