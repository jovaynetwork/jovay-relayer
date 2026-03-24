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

package com.alipay.antchain.l2.relayer.core.layer2.economic;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RollupEconomicStrategy {

    @Value("${l2-relayer.rollup.economic-strategy-conf.switch:true}")
    private boolean strategySwitch;

    @Resource
    private RollupEconomicStrategyConfig strategyConfig;

    @Resource
    private IRollupRepository rollupRepository;

    public IRollupCostChecker createBatchCommitCostChecker(BatchWrapper currBatch) {
        return strategySwitch ? new BatchCommitCostChecker(
                currBatch,
                rollupRepository.calcWaitingBatchCountBeyondIndex(currBatch.getBatchIndex()),
                strategyConfig
        ) : NopeChecker.INSTANCE;
    }

    public IRollupCostChecker createProofCommitCostChecker(BatchProveRequestDO currProofReq) {
        return strategySwitch ? new ProofCommitCostChecker(
                currProofReq,
                rollupRepository.calcWaitingProofCountBeyondIndex(currProofReq.getProveType(), currProofReq.getBatchIndex().subtract(BigInteger.ONE)),
                strategyConfig
        ) : NopeChecker.INSTANCE;
    }

    public IRollupCostChecker createRetryTxCostChecker(ReliableTransactionDO reliableTx) {
        return strategySwitch ? new RetryTxCostChecker(reliableTx, rollupRepository, strategyConfig) : NopeChecker.INSTANCE;
    }

    public IRollupCostChecker createSpeedUpTxCostChecker(ReliableTransactionDO reliableTx) {
        return strategySwitch ? new SpeedUpTxCostChecker(reliableTx, rollupRepository, strategyConfig) : NopeChecker.INSTANCE;
    }
}
