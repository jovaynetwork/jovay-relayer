package com.alipay.antchain.l2.relayer.core.layer2.economic;

import com.alipay.antchain.l2.relayer.commons.exceptions.RollupEconomicStrategyNotAllowedException;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Getter
public class BatchCommitCostChecker implements IRollupCostChecker {

    private BatchWrapper currBatch;

    private int currPendingBatchCount;

    private RollupEconomicStrategyConfig strategyConfig;

    @Override
    public void check(@NonNull IGasPrice gasPrice) {
        var currPrice = gasPrice.baseFee().add(gasPrice.maxPriorityFeePerGas());
        // green range
        if (currPrice.compareTo(strategyConfig.getMidEip1559PriceLimit()) <= 0) {
            log.debug("currPrice of committing batch is in green range, currPrice: {}", currPrice);
            return;
        }
        // yellow range
        if (currPrice.compareTo(strategyConfig.getHighEip1559PriceLimit()) <= 0) {
            if (currPendingBatchCount >= strategyConfig.getMaxPendingBatchCount()
                || currBatch.getGmtCreate() + strategyConfig.getMaxBatchWaitingTime() * 1000 < System.currentTimeMillis()) {
                // let the batch commit tx pass
                log.info("currPrice of committing batch is in yellow range but pass the economic check, currPrice: {}", currPrice);
                return;
            }
            throw new RollupEconomicStrategyNotAllowedException(gasPrice.baseFee(), gasPrice.maxPriorityFeePerGas());
        }
        // red range
        if (currPendingBatchCount >= strategyConfig.getMaxPendingBatchCount()
            && currBatch.getGmtCreate() + strategyConfig.getMaxBatchWaitingTime() * 1000 < System.currentTimeMillis()) {
            // let the batch commit tx pass
            log.info("currPrice of committing batch is in red range but pass the economic check, currPrice: {}", currPrice);
            return;
        }
        throw new RollupEconomicStrategyNotAllowedException(gasPrice.baseFee(), gasPrice.maxPriorityFeePerGas());
    }
}
