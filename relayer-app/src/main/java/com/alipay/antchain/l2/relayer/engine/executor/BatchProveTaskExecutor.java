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

import java.util.concurrent.ExecutorService;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IRollupService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BatchProveTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IRollupService rollupService;

    public BatchProveTaskExecutor(
            @Qualifier("processScheduleTaskExecutorThreadsPool") ExecutorService executor,
            @Qualifier("defaultDistributedTaskChecker") IDistributedTaskChecker distributedTaskChecker,
            @Value("${l2-relayer.engine.shutdown.await-terminate-time:60000}") long awaitTerminateTime
    ) {
        super(executor, distributedTaskChecker, awaitTerminateTime);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            try {
                rollupService.proveTeeL2Batch();
            } catch (Exception e) {
                log.error("prove batch from TEE failed: ", e);
            }
            try {
                rollupService.proveZkL2Batch();
            } catch (Exception e) {
                log.error("prove batch from ZK failed: ", e);
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown batch prove executor...");
        try {
            super.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown batch prove threads pool: ", e);
        }
        log.info("successful to shutdown batch prove executor");
    }
}
