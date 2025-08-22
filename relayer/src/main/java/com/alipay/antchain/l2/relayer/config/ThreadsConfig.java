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
}
