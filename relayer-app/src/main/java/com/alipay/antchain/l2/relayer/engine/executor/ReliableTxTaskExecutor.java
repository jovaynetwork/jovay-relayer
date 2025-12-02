package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IReliableTxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReliableTxTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IReliableTxService reliableTxService;

    public ReliableTxTaskExecutor(
            @Qualifier("reliableProcessTaskExecutorThreadsPool") ExecutorService executor,
            @Qualifier("defaultDistributedTaskChecker") IDistributedTaskChecker distributedTaskChecker,
            @Value("${l2-relayer.engine.shutdown.await-terminate-time:10000}") long awaitTerminateTime
    ) {
        super(executor, distributedTaskChecker, awaitTerminateTime);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            try {
                // consider to do the jobs async
                reliableTxService.processL1NotFinalizedTx();
                reliableTxService.processL2NotFinalizedTx();
                reliableTxService.retryFailedTx();
            } catch (Exception e) {
                log.error("reliable tx process failed: ", e);
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown reliable tx executor...");
        try {
            super.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown reliable tx threads pool: ", e);
        }
        log.info("successful to shutdown reliable tx executor");
    }
}
