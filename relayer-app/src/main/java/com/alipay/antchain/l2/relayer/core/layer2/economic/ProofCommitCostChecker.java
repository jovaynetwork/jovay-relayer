package com.alipay.antchain.l2.relayer.core.layer2.economic;

import com.alipay.antchain.l2.relayer.commons.exceptions.RollupEconomicStrategyNotAllowedException;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ProofCommitCostChecker implements IRollupCostChecker {

    private BatchProveRequestDO currProveReq;

    private int currPendingProveReqCount;

    private RollupEconomicStrategyConfig strategyConfig;

    @Override
    public void check(@NonNull IGasPrice gasPrice) {
        var currPrice = gasPrice.baseFee().add(gasPrice.maxPriorityFeePerGas());
        // green range
        if (currPrice.compareTo(strategyConfig.getMidEip1559PriceLimit()) <= 0) {
            log.debug("currPrice of committing proof is in green range, currPrice: {}", currPrice);
            return;
        }
        // yellow range
        if (currPrice.compareTo(strategyConfig.getHighEip1559PriceLimit()) <= 0) {
            if (currPendingProveReqCount >= strategyConfig.getMaxPendingProofCount()
                || currProveReq.getGmtModified().getTime() + strategyConfig.getMaxProofWaitingTime() * 1000 < System.currentTimeMillis()) {
                // let the proof commit tx pass
                log.info("currPrice of committing proof is in yellow range but pass the economic check, currPrice: {}", currPrice);
                return;
            }
            throw new RollupEconomicStrategyNotAllowedException(gasPrice.baseFee(), gasPrice.maxPriorityFeePerGas());
        }
        // red range
        if (currPendingProveReqCount >= strategyConfig.getMaxPendingProofCount()
            && currProveReq.getGmtModified().getTime() + strategyConfig.getMaxProofWaitingTime() * 1000 < System.currentTimeMillis()) {
            // let the proof commit tx pass
            log.info("currPrice of committing proof is in red range but pass the economic check, currPrice: {}", currPrice);
            return;
        }
        throw new RollupEconomicStrategyNotAllowedException(gasPrice.baseFee(), gasPrice.maxPriorityFeePerGas());
    }
}
