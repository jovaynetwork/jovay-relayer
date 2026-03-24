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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Getter
@Component("localDistributedTaskChecker")
@Slf4j
public class LocalDistributedTaskChecker implements IDistributedTaskChecker {

    private final Map<BizTaskTypeEnum, CompletableFuture> localRunningTasks = Maps.newConcurrentMap();

    @Override
    public void addLocalFuture(BizTaskTypeEnum taskType, CompletableFuture<Void> future) {
        localRunningTasks.put(taskType, future);
    }

    @Override
    public boolean checkIfContinue(BizTaskTypeEnum taskType) {
        if (localRunningTasks.containsKey(taskType)) {
            if (!localRunningTasks.get(taskType).isDone()) {
                log.debug("local task is running : {}", taskType);
                return false;
            }
            log.debug("local task finish : {}", taskType);
            localRunningTasks.remove(taskType);
        }
        return true;
    }
}
