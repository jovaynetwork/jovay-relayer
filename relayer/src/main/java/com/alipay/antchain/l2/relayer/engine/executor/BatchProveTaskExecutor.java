package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.ExecutorService;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.commons.exceptions.L1GasPriceTooHighException;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IRollupService;
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
            @Value("${l2-relayer.engine.shutdown.await-terminate-time:10000}") long awaitTerminateTime
    ) {
        super(executor, distributedTaskChecker, awaitTerminateTime);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            try {
                rollupService.proveTeeL2Batch();
                rollupService.proveZkL2Batch();
            } catch (L1GasPriceTooHighException e) {
                log.warn("l1 gas price too high, skip this batch prove: ", e);
            } catch (Exception e) {
                log.error("prove batch failed: ", e);
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
