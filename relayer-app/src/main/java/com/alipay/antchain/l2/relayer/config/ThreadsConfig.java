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

import java.util.concurrent.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadsConfig {

    @Value("${l2-relayer.engine.duty.process.threads.core_size:8}")
    private int processScheduleTaskExecutorCoreSize;

    @Value("${l2-relayer.engine.duty.process.threads.total_size:8}")
    private int processScheduleTaskExecutorTotalSize;

    @Value("${l2-relayer.service.reliable_process.threads.core_size:1}")
    private int reliableProcessTaskExecutorCoreSize;

    @Value("${l2-relayer.service.reliable_process.threads.total_size:4}")
    private int reliableProcessTaskExecutorTotalSize;

    @Value("${l2-relayer.service.rollup_process.block_polling.threads.total_size:32}")
    private int blockPollingTaskExecutorTotalSize;

    @Value("${l2-relayer.service.rollup_process.block_polling.threads.core_size:4}")
    private int blockPollingTaskExecutorCoreSize;

    @Value("${l2-relayer.service.l1-client.l1-msg-fetcher-threads.total_size:8}")
    private int fetchingL1MsgTaskExecutorTotalSize;

    @Value("${l2-relayer.service.l1-client.l1-msg-fetcher-threads.core_size:4}")
    private int fetchingL1MsgTaskExecutorCoreSize;

    @Value("${l2-relayer.service.rollup_process.get-l2-block-trace-range-threads.core_size:4}")
    private int getL2BlockTraceRangeThreadsPoolCoreSize;

    @Value("${l2-relayer.service.rollup_process.get-l2-block-trace-range-threads.max_size:8}")
    private int getL2BlockTraceRangeThreadsPoolMaxSize;

    @Bean(name = "distributedTaskEngineScheduleThreadsPool")
    public ScheduledExecutorService distributedTaskEngineScheduleThreadsPool() {
        return new ScheduledThreadPoolExecutor(
                3,
                new ThreadFactoryBuilder().setNameFormat("ScheduleEngine-Executor-%d").build()
        );
    }

    @Bean(name = "processScheduleTaskExecutorThreadsPool")
    public ExecutorService processScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                processScheduleTaskExecutorCoreSize,
                processScheduleTaskExecutorTotalSize,
                5000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("process_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    /**
     * 可靠上链消息重试处理线程池
     *
     * @return
     */
    @Bean(name = "reliableProcessTaskExecutorThreadsPool")
    public ExecutorService reliableProcessTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                reliableProcessTaskExecutorCoreSize,
                reliableProcessTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("reliableProcess_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "blockPollingTaskExecutorThreadsPool")
    public ExecutorService blockPollingTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                blockPollingTaskExecutorCoreSize,
                blockPollingTaskExecutorTotalSize,
                3000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("blockPolling_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "fetchingL1MsgThreadsPool")
    public ExecutorService fetchingL1MsgThreadsPool() {
        return new ThreadPoolExecutor(
                fetchingL1MsgTaskExecutorCoreSize,
                fetchingL1MsgTaskExecutorTotalSize,
                3000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("fetchingL1Msg_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "dynamicPersisterScheduledExecutors")
    public ScheduledExecutorService dynamicPersisterScheduledExecutors() {
        return new ScheduledThreadPoolExecutor(
                1,
                new ThreadFactoryBuilder().setNameFormat("DynamicConfigPersister-%d").build()
        );
    }

    @Bean(name = "getL2BlockTraceRangeThreadsPool")
    public ExecutorService getL2BlockTraceRangeThreadsPool() {
        return new ThreadPoolExecutor(
                getL2BlockTraceRangeThreadsPoolCoreSize,
                getL2BlockTraceRangeThreadsPoolMaxSize,
                3000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("getL2BlockTraceRange_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
