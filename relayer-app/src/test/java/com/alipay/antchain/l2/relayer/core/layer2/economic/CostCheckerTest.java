package com.alipay.antchain.l2.relayer.core.layer2.economic;

import java.math.BigInteger;
import java.util.Date;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.RollupEconomicStrategyNotAllowedException;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.dal.repository.RollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.Mockito.*;

public class CostCheckerTest extends TestBase {

    private static BatchWrapper batchCurrent;

    private static BatchWrapper batchOneDayAgo;

    // 添加 proof 相关的对象
    private static BatchProveRequestDO proofCurrent;

    private static BatchProveRequestDO proofOneDayAgo;

    static {
        batchCurrent = new BatchWrapper();
        batchCurrent.setGmtCreate(System.currentTimeMillis());

        batchOneDayAgo = new BatchWrapper();
        batchOneDayAgo.setGmtCreate(System.currentTimeMillis() - 12 * 60 * 60 * 1000 - 1);

        // 创建 proof 相关的测试对象
        proofCurrent = new BatchProveRequestDO();
        proofCurrent.setGmtModified(new Date());

        proofOneDayAgo = new BatchProveRequestDO();
        proofOneDayAgo.setGmtModified(new Date(System.currentTimeMillis() - 12 * 60 * 60 * 1000 - 1));
    }

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @Resource
    private RollupEconomicStrategyConfig strategyConfig;

    @Test
    public void testBatchCommitCostChecker() {
        // green range
        var batchCommitCostChecker = new BatchCommitCostChecker(batchCurrent, 0, strategyConfig);
        var price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
        batchCommitCostChecker.check(price);

        // yellow range
        batchCommitCostChecker = new BatchCommitCostChecker(batchCurrent, strategyConfig.getMaxPendingBatchCount() + 1, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        batchCommitCostChecker.check(price);

        batchCommitCostChecker = new BatchCommitCostChecker(batchOneDayAgo, 0, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        batchCommitCostChecker.check(price);

        var batchCommitCostChecker1 = new BatchCommitCostChecker(batchCurrent, 1, strategyConfig);
        var price1 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> batchCommitCostChecker1.check(price1));

        // red range
        batchCommitCostChecker = new BatchCommitCostChecker(batchOneDayAgo, strategyConfig.getMaxPendingBatchCount() + 1, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        batchCommitCostChecker.check(price);

        var batchCommitCostChecker2 = new BatchCommitCostChecker(batchOneDayAgo, 1, strategyConfig);
        var price2 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> batchCommitCostChecker2.check(price2));

        var batchCommitCostChecker3 = new BatchCommitCostChecker(batchCurrent, strategyConfig.getMaxPendingBatchCount() + 1, strategyConfig);
        var price3 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> batchCommitCostChecker3.check(price3));
    }

    @Test
    public void testProofCommitCostChecker() {
        // green range - 绿色范围：低成本，应该允许提交
        var proofCommitCostChecker = new ProofCommitCostChecker(proofCurrent, 0, strategyConfig);
        var price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
        proofCommitCostChecker.check(price);

        // yellow range - 黄色范围：中等成本，在特定条件下允许
        proofCommitCostChecker = new ProofCommitCostChecker(proofCurrent, strategyConfig.getMaxPendingProofCount() + 1, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        proofCommitCostChecker.check(price);

        proofCommitCostChecker = new ProofCommitCostChecker(proofOneDayAgo, 0, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        proofCommitCostChecker.check(price);

        var proofCommitCostChecker1 = new ProofCommitCostChecker(proofCurrent, 1, strategyConfig);
        var price1 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getMidEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> proofCommitCostChecker1.check(price1));

        // red range - 红色范围：高成本，只在紧急情况下允许
        proofCommitCostChecker = new ProofCommitCostChecker(proofOneDayAgo, strategyConfig.getMaxPendingProofCount() + 1, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        proofCommitCostChecker.check(price);

        var proofCommitCostChecker2 = new ProofCommitCostChecker(proofOneDayAgo, 1, strategyConfig);
        var price2 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> proofCommitCostChecker2.check(price2));

        var proofCommitCostChecker3 = new ProofCommitCostChecker(proofCurrent, strategyConfig.getMaxPendingProofCount() + 1, strategyConfig);
        var price3 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> proofCommitCostChecker3.check(price3));
    }

    @Test
    public void testBatchCommitCostCheckerNullSafety() {
        var checker = new BatchCommitCostChecker(batchCurrent, 0, strategyConfig);
        Assert.assertThrows(NullPointerException.class, () -> {
            checker.check(null);
        });
    }

    @Test
    public void testProofCommitCostCheckerNullSafety() {
        var checker = new ProofCommitCostChecker(proofCurrent, 0, strategyConfig);
        Assert.assertThrows(NullPointerException.class, () -> {
            checker.check(null);
        });
    }

    @Test
    public void testRetryTxCostChecker() {
        var rollupRepo = mock(RollupRepository.class);

        var batchCommitTx = new ReliableTransactionDO();
        batchCommitTx.setBatchIndex(BigInteger.ONE);
        batchCommitTx.setTransactionType(TransactionTypeEnum.BATCH_COMMIT_TX);

        // green range & yellow range
        var checker = new RetryTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        var price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        // red range
        when(rollupRepo.calcWaitingBatchCountBeyondIndex(notNull())).thenReturn(strategyConfig.getMaxPendingBatchCount());
        checker = new RetryTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingBatchCountBeyondIndex(notNull())).thenReturn(strategyConfig.getMaxPendingBatchCount() - 1);
        var checker1 = new RetryTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        var price1 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());

        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker1.check(price1));

        var teeCommitTx = new ReliableTransactionDO();
        teeCommitTx.setBatchIndex(BigInteger.ONE);
        teeCommitTx.setTransactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount());
        checker = new RetryTxCostChecker(teeCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount() - 1);
        var checker2 = new RetryTxCostChecker(teeCommitTx, rollupRepo, strategyConfig);
        var price2 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker2.check(price2));

        var zkCommitTx = new ReliableTransactionDO();
        zkCommitTx.setBatchIndex(BigInteger.ONE);
        zkCommitTx.setTransactionType(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount());
        checker = new RetryTxCostChecker(zkCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount() - 1);
        var checker3 = new RetryTxCostChecker(zkCommitTx, rollupRepo, strategyConfig);
        var price3 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker3.check(price3));
    }

    @Test
    public void testSpeedUpTxCostChecker() {
        var rollupRepo = mock(RollupRepository.class);

        var batchCommitTx = new ReliableTransactionDO();
        batchCommitTx.setBatchIndex(BigInteger.ONE);
        batchCommitTx.setTransactionType(TransactionTypeEnum.BATCH_COMMIT_TX);

        // green range & yellow range
        var checker = new SpeedUpTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        var price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        // red range
        when(rollupRepo.calcWaitingBatchCountBeyondIndex(notNull())).thenReturn(strategyConfig.getMaxPendingBatchCount());
        checker = new SpeedUpTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingBatchCountBeyondIndex(notNull())).thenReturn(strategyConfig.getMaxPendingBatchCount() - 1);
        var checker1 = new SpeedUpTxCostChecker(batchCommitTx, rollupRepo, strategyConfig);
        var price1 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());

        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker1.check(price1));

        var teeCommitTx = new ReliableTransactionDO();
        teeCommitTx.setBatchIndex(BigInteger.ONE);
        teeCommitTx.setTransactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount());
        checker = new SpeedUpTxCostChecker(teeCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount() - 1);
        var checker2 = new SpeedUpTxCostChecker(teeCommitTx, rollupRepo, strategyConfig);
        var price2 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker2.check(price2));

        var zkCommitTx = new ReliableTransactionDO();
        zkCommitTx.setBatchIndex(BigInteger.ONE);
        zkCommitTx.setTransactionType(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount());
        checker = new SpeedUpTxCostChecker(zkCommitTx, rollupRepo, strategyConfig);
        price = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ZERO, strategyConfig.getHighEip1559PriceLimit());
        checker.check(price);

        when(rollupRepo.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(strategyConfig.getMaxPendingProofCount() - 1);
        var checker3 = new SpeedUpTxCostChecker(zkCommitTx, rollupRepo, strategyConfig);
        var price3 = new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, strategyConfig.getHighEip1559PriceLimit());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> checker3.check(price3));
    }
}