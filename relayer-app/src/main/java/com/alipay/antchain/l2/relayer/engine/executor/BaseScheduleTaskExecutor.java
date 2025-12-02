package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式任务基类，传入具体分布式任务，使用线程池异步执行
 */
@Slf4j
@Getter
public abstract class BaseScheduleTaskExecutor {

    private final ExecutorService executor;

    private final IDistributedTaskChecker distributedTaskChecker;

    private final long awaitTerminateTime;

    public BaseScheduleTaskExecutor(ExecutorService executor, IDistributedTaskChecker distributedTaskChecker, long awaitTerminateTime) {
        Assert.notNull(executor);
        this.executor = executor;
        this.distributedTaskChecker = distributedTaskChecker;
        this.awaitTerminateTime = awaitTerminateTime;
    }

    /**
     * 分布式任务执行基类
     *
     * @param task
     */
    @Synchronized
    public void execute(IDistributedTask task) {

        BizTaskTypeEnum taskType = task.getTaskType();
        // 判断时间片是否结束
        if (task.ifFinish()) {
            log.debug("task out of time slice : {}", taskType);
            return;
        }

        // 该任务是否已经在执行
        if (!distributedTaskChecker.checkIfContinue(taskType)) {
            log.debug("task {} is running locally or on other node remotely", taskType);
            return;
        }

        // 触发执行
        log.debug("execute task : {}", taskType);
        distributedTaskChecker.addLocalFuture(taskType, CompletableFuture.runAsync(genTask(task), executor));
    }

    //*******************************************
    // 子类实现
    //*******************************************

    public abstract Runnable genTask(IDistributedTask task);

    public void shutdown() throws InterruptedException {
        if (getExecutor().isShutdown()) {
            return;
        }
        getExecutor().shutdown();
        getExecutor().awaitTermination(awaitTerminateTime, TimeUnit.MILLISECONDS);
    }
}
