package com.alipay.antchain.l2.relayer.core.layer2.economic;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.RollupEconomicStrategyNotAllowedException;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RetryTxCostChecker implements IRollupCostChecker {

    private ReliableTransactionDO reliableTx;

    private IRollupRepository rollupRepository;

    private RollupEconomicStrategyConfig strategyConfig;

    @Override
    public void check(IGasPrice gasPrice) {
        var currPrice = gasPrice.baseFee().add(gasPrice.maxPriorityFeePerGas());
        // green range & yellow range
        if (currPrice.compareTo(strategyConfig.getHighEip1559PriceLimit()) <= 0) {
            log.debug("currPrice of retry tx is in green range, currPrice: {}", currPrice);
            return;
        }

        // red range
        if (reliableTx.getTransactionType() == TransactionTypeEnum.BATCH_COMMIT_TX) {
            var count = rollupRepository.calcWaitingBatchCountBeyondIndex(reliableTx.getBatchIndex());
            if (count >= strategyConfig.getMaxPendingBatchCount()) {
                log.info("currPrice of batch retry tx is in red range but pass the economic check, currPrice: {}", currPrice);
                return;
            }
        } else if (reliableTx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX
                   || reliableTx.getTransactionType() == TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX) {
            var proveType = reliableTx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX ? ProveTypeEnum.TEE_PROOF : ProveTypeEnum.ZK_PROOF;
            var count = rollupRepository.calcWaitingProofCountBeyondIndex(proveType, reliableTx.getBatchIndex().subtract(BigInteger.ONE));
            if (count >= strategyConfig.getMaxPendingProofCount()) {
                log.info("currPrice of proof retry tx is in red range but pass the economic check, currPrice: {}", currPrice);
                return;
            }
        }

        throw new RollupEconomicStrategyNotAllowedException(gasPrice.baseFee(), gasPrice.maxPriorityFeePerGas());
    }
}
