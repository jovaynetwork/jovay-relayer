package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.ExecutorService;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IL1ListenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class L1BlockPollingTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IL1ListenService l1ListenService;

    @Autowired
    public L1BlockPollingTaskExecutor(
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
                l1ListenService.pollL1MsgBatch();
            } catch (Throwable t) {
                log.error("failed to poll l1Msgs: ", t);
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown l1 block polling executor...");
        try {
            super.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown l1 block polling threads pool: ", e);
        }
        log.info("successful to shutdown l1 block polling executor");
    }
}
