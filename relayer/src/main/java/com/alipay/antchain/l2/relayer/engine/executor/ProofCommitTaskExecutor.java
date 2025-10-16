package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IRollupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProofCommitTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IRollupService rollupService;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Autowired
    public ProofCommitTaskExecutor(
            @Qualifier("processScheduleTaskExecutorThreadsPool") ExecutorService executor,
            @Qualifier("defaultDistributedTaskChecker") IDistributedTaskChecker distributedTaskChecker,
            @Value("${l2-relayer.engine.shutdown.await-terminate-time:10000}") long awaitTerminateTime
    ) {
        super(executor, distributedTaskChecker, awaitTerminateTime);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            if (!systemConfigRepository.isAnchorBatchSet()) {
                log.debug("anchor batch not set, please set it first! ");
                return;
            }
            try {
                rollupService.commitL2TeeProof();
            } catch (Throwable t) {
                log.error("failed to commit l2 tee proof: ", t);
            }
            try {
                rollupService.commitL2ZkProof();
            } catch (Throwable t) {
                log.error("failed to commit l2 zk proof: ", t);
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown proof commit executor...");
        try {
            super.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown proof commit threads pool: ", e);
        }
        log.info("successful to shutdown proof commit executor");
    }
}
