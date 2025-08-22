package com.alipay.antchain.l2.relayer.engine.executor;

import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.engine.checker.IDistributedTaskChecker;
import com.alipay.antchain.l2.relayer.service.IMailboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class L1MsgPollingTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IMailboxService mailboxService;

    @Autowired
    public L1MsgPollingTaskExecutor(
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
                mailboxService.pollL1MsgBatch();
            } catch (Throwable t) {
                log.error("failed to poll l1Msgs: ", t);
            }
        };
    }
}
