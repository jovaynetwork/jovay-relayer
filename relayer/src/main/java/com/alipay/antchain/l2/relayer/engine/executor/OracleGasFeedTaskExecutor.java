package com.alipay.antchain.l2.relayer.engine.executor;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IOracleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class OracleGasFeedTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IOracleService oracleService;

    @Autowired
    public OracleGasFeedTaskExecutor(
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
                oracleService.processBlockOracle();
                oracleService.processBatchOracle();
            } catch (Throwable t) {
                log.error("failed to feed latest gas to l2: ", t);
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown oracle gas feed executor...");
        try {
            super.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown oracle gas feed threads pool: ", e);
        }
        log.info("successful to shutdown oracle gas feed executor");
    }
}
