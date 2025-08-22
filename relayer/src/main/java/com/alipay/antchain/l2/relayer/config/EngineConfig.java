package com.alipay.antchain.l2.relayer.config;

import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.l2.relayer.engine.executor.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@Getter
@EnableRetry
public class EngineConfig {

    @Value("${l2-relayer.engine.node_id_mode:IP}")
    private String nodeIdMode;

    @Bean
    public ScheduleContext scheduleContext() {
        return new ScheduleContext(nodeIdMode);
    }

    @Bean
    public Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap(
            BlockPollingTaskExecutor blockPollingTaskExecutor,
            BatchProveTaskExecutor batchProveTaskExecutor,
            BatchCommitTaskExecutor batchCommitTaskExecutor,
            ProofCommitTaskExecutor proofCommitTaskExecutor,
            ReliableTxTaskExecutor reliableTxTaskExecutor,
            L1MsgPollingTaskExecutor l1MsgPollingTaskExecutor,
            L1MsgProcessTaskExecutor l1MsgProcessTaskExecutor,
            ProveL2MsgTaskExecutor proveL2MsgTaskExecutor
    ) {
        Map<BizTaskTypeEnum, BaseScheduleTaskExecutor> res = new HashMap<>();
        res.put(BizTaskTypeEnum.BLOCK_POLLING_TASK, blockPollingTaskExecutor);
        res.put(BizTaskTypeEnum.BATCH_PROVE_TASK, batchProveTaskExecutor);
        res.put(BizTaskTypeEnum.BATCH_COMMIT_TASK, batchCommitTaskExecutor);
        res.put(BizTaskTypeEnum.PROOF_COMMIT_TASK, proofCommitTaskExecutor);
        res.put(BizTaskTypeEnum.RELIABLE_TX_TASK, reliableTxTaskExecutor);
        res.put(BizTaskTypeEnum.L1MSG_POLLING_TASK, l1MsgPollingTaskExecutor);
        res.put(BizTaskTypeEnum.L1MSG_PROCESS_TASK, l1MsgProcessTaskExecutor);
        res.put(BizTaskTypeEnum.L2MSG_PROVE_TASK, proveL2MsgTaskExecutor);
        return res;
    }
}
