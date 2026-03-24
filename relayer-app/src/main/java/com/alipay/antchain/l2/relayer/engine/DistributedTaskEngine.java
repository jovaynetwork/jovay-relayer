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

package com.alipay.antchain.l2.relayer.engine;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.relayer.engine.core.Duty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Getter
@Order(1)
public class DistributedTaskEngine implements ApplicationRunner {

    @Resource
    private Activator activator;

    @Resource
    private Dispatcher dispatcher;

    @Resource
    private Duty duty;

    @Resource(name = "distributedTaskEngineScheduleThreadsPool")
    private ScheduledExecutorService distributedTaskEngineScheduleThreadsPool;

    @Value("${l2-relayer.engine.schedule.activate.period:1000}")
    private long activatePeriod;

    @Value("${l2-relayer.engine.schedule.dispatcher.period:1000}")
    private long dispatchPeriod;

    @Value("${l2-relayer.engine.schedule.duty.period:100}")
    private long dutyPeriod;

    @Override
    public void run(ApplicationArguments args) {

        log.info("Starting DistributedTask Engine Now");

        // schedule activator
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        activator.activate();
                    } catch (Throwable e) {
                        log.error("schedule activator failed.", e);
                    }
                },
                0,
                activatePeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule dispatcher
        distributedTaskEngineScheduleThreadsPool.scheduleAtFixedRate(
                () -> {
                    try {
                        dispatcher.dispatch();
                    } catch (Throwable e) {
                        log.error("schedule dispatch failed.", e);
                    }
                },
                0,
                dispatchPeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule duty
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        duty.duty();
                    } catch (Throwable e) {
                        log.error("schedule duty failed.", e);
                    }
                },
                0,
                dutyPeriod,
                TimeUnit.MILLISECONDS
        );
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown schedule engine...");
        if (distributedTaskEngineScheduleThreadsPool.isShutdown()) {
            return;
        }
        try {
            distributedTaskEngineScheduleThreadsPool.shutdown();
            distributedTaskEngineScheduleThreadsPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("failed to shutdown schedule threads pool: ", e);
        }
        log.info("shutdown schedule engine successfully.");
    }
}
