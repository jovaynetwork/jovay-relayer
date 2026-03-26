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

package com.alipay.antchain.l2.relayer.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.l2.relayer.engine.executor.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration class for the L2 Relayer engine.
 * <p>
 * This class configures the scheduling engine components including:
 * <ul>
 *   <li>Schedule context for managing node identification</li>
 *   <li>Task executor mapping for different task types</li>
 *   <li>Running task list for distributed task management</li>
 * </ul>
 * </p>
 */
@Configuration
@Getter
@EnableRetry
public class EngineConfig {

    /**
     * The mode for generating node ID.
     * <p>
     * Supported modes:
     * <ul>
     *   <li>IP: Use IP address as node ID</li>
     *   <li>Other custom modes as configured</li>
     * </ul>
     * </p>
     */
    @Value("${l2-relayer.engine.node_id_mode:IP}")
    private String nodeIdMode;

    /**
     * Creates the schedule context bean for managing scheduling operations.
     *
     * @return the configured schedule context instance
     */
    @Bean
    public ScheduleContext scheduleContext() {
        return new ScheduleContext(nodeIdMode);
    }

    /**
     * Creates a mapping of task types to their corresponding executors.
     * <p>
     * This bean configures all available task executors for the relayer engine.
     * The oracle gas feed task executor is conditionally included based on
     * whether the parent chain type requires rollup fee feed.
     * </p>
     *
     * @param blockPollingTaskExecutor    executor for L2 block polling tasks
     * @param batchProveTaskExecutor      executor for batch proof generation tasks
     * @param batchCommitTaskExecutor     executor for batch commit tasks
     * @param proofCommitTaskExecutor     executor for proof commit tasks
     * @param reliableTxTaskExecutor      executor for reliable transaction tasks
     * @param l1BlockPollingTaskExecutor  executor for L1 block polling tasks
     * @param l1MsgProcessTaskExecutor    executor for L1 message processing tasks
     * @param proveL2MsgTaskExecutor      executor for L2 message proof tasks
     * @param oracleGasFeedTaskExecutor   executor for oracle gas feed tasks
     * @param needRollupFeeFeed           whether rollup fee feed is required
     * @return a map of task types to their executors
     */
    @Bean
    public Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap(
            BlockPollingTaskExecutor blockPollingTaskExecutor,
            BatchProveTaskExecutor batchProveTaskExecutor,
            BatchCommitTaskExecutor batchCommitTaskExecutor,
            ProofCommitTaskExecutor proofCommitTaskExecutor,
            ReliableTxTaskExecutor reliableTxTaskExecutor,
            L1BlockPollingTaskExecutor l1BlockPollingTaskExecutor,
            L1MsgProcessTaskExecutor l1MsgProcessTaskExecutor,
            ProveL2MsgTaskExecutor proveL2MsgTaskExecutor,
            OracleGasFeedTaskExecutor oracleGasFeedTaskExecutor,
            @Value("#{rollupConfig.getParentChainType().needRollupFeeFeed()}") boolean needRollupFeeFeed
    ) {
        Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> res = new HashMap<>();
        res.put(BizTaskTypeEnum.BLOCK_POLLING_TASK, blockPollingTaskExecutor);
        res.put(BizTaskTypeEnum.BATCH_PROVE_TASK, batchProveTaskExecutor);
        res.put(BizTaskTypeEnum.BATCH_COMMIT_TASK, batchCommitTaskExecutor);
        res.put(BizTaskTypeEnum.PROOF_COMMIT_TASK, proofCommitTaskExecutor);
        res.put(BizTaskTypeEnum.RELIABLE_TX_TASK, reliableTxTaskExecutor);
        res.put(BizTaskTypeEnum.L1_BLOCK_POLLING_TASK, l1BlockPollingTaskExecutor);
        res.put(BizTaskTypeEnum.L1MSG_PROCESS_TASK, l1MsgProcessTaskExecutor);
        res.put(BizTaskTypeEnum.L2MSG_PROVE_TASK, proveL2MsgTaskExecutor);
        if (needRollupFeeFeed) {
            res.put(BizTaskTypeEnum.ORACLE_GAS_FEED_TASK, oracleGasFeedTaskExecutor);
        }
        return res;
    }

    /**
     * Creates the list of running distributed tasks.
     * <p>
     * This bean generates a list of distributed tasks based on the available
     * task executors. Each task type from the executor map is converted into
     * a BizDistributedTask instance.
     * </p>
     *
     * @param scheduleTaskExecutorMap the map of task executors
     * @return a list of distributed tasks to be executed
     */
    @Bean
    public List<IDistributedTask> runningTaskList(Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap) {
        return scheduleTaskExecutorMap.keySet().stream().map(BizDistributedTask::new).collect(Collectors.toList());
    }
}
