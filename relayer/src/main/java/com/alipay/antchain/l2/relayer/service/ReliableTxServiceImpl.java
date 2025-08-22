package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Resource;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.tracer.TraceServiceClient;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.ISelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.MetricUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.crypto.transaction.type.TransactionType;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

@Service
@Slf4j
public class ReliableTxServiceImpl implements IReliableTxService {

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private L1Client l1Client;

    @Resource
    private L2Client l2Client;

    @Resource
    private TraceServiceClient traceServiceClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${l2-relayer.tasks.reliable-tx.process-batch-size:32}")
    private int processBatchSize;

    @Value("${l2-relayer.tasks.block-polling.l1.policy:FINALIZED}")
    private DefaultBlockParameterName l1BlockPollingPolicy;

    @Value("${l2-relayer.tasks.block-polling.l2.policy:LATEST}")
    private DefaultBlockParameterName l2BlockPollingPolicy;

    @Value("${l2-relayer.tasks.reliable-tx.retry-limit:0}")
    private int retryCountLimit;

    @Value("${l2-relayer.tasks.reliable-tx.tx-timeout-limit:600}")
    private int txTimeOutLimitSec;

    @Value("${l2-relayer.l1-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l1NoncePolicy;

    @Resource
    private ISelfReportMetric selfReportMetric;

    @Resource
    private IOtelMetric otelMetric;

    @Override
    public void processNotFinalizedTx() {
        var reliableTransactions = rollupRepository.getNotFinalizedReliableTransactions(processBatchSize);
        if (ObjectUtil.isEmpty(reliableTransactions)) {
            log.debug("No pending reliable tx found, skip it...");
            return;
        }

        for (ReliableTransactionDO tx : reliableTransactions) {
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                switch (tx.getChainType()) {
                                    case LAYER_ONE:
                                        processL1PendingTx(tx);
                                        break;
                                    case LAYER_TWO:
                                        processL2PendingTx(tx);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("unsupported chain type: " + tx.getChainType());
                                }
                            }
                        }
                );
            } catch (Exception e) {
                log.error("process reliable tx for batch {}-{} failed: ", tx.getTransactionType(), tx.getBatchIndex(), e);
            }
        }
    }

    @Override
    public void retryFailedTx() {
        if (retryCountLimit <= 0) {
            return;
        }
        var reliableTransactions = rollupRepository.getFailedReliableTransactions(processBatchSize, retryCountLimit);
        if (ObjectUtil.isEmpty(reliableTransactions)) {
            log.debug("No failed reliable tx found, skip it...");
            return;
        }
        reliableTransactions.sort((o1, o2) -> {
            if (o1.getBatchIndex().compareTo(o2.getBatchIndex()) < 0) {
                return -1;
            } else if (o1.getBatchIndex().compareTo(o2.getBatchIndex()) > 0) {
                return 1;
            }
            return Integer.compare(o1.getTransactionType().ordinal(), o2.getTransactionType().ordinal());
        });
        reliableTransactions.forEach(this::retryTx);
    }

    private void processL1PendingTx(ReliableTransactionDO tx) {
        var transaction = l1Client.queryTx(tx.getLatestTxHash());
        // tx not found on blockchain
        if (ObjectUtil.isNull(transaction)) {
            log.info("nonce {} missed for {} tx {} of batch {}, resend the raw signed tx...",
                    tx.getNonce(), tx.getTransactionType(), tx.getLatestTxHash(), tx.getBatchIndex());
            if (l1NoncePolicy == EthNoncePolicyEnum.NORMAL) {
                retryTx(tx, false);
            } else {
                // if the finalized nonce of sender account is greater than this one's nonce,
                // it means that no need to resend it.
                if (l1Client.queryTxCount(tx.getSenderAccount(), l1BlockPollingPolicy).longValue() >= tx.getNonce()) {
                    log.info("Nonce {} of {} tx {} for batch {} has been finalized, update its state to success",
                            tx.getNonce(), tx.getTransactionType(), tx.getLatestTxHash(), tx.getBatchIndex());
                    tx.setState(ReliableTransactionStateEnum.TX_SUCCESS);
                    rollupRepository.updateReliableTransaction(tx);
                    return;
                }
                EthSendTransaction sendResult = l1Client.sendRawTx(tx.getRawTx());
                if (sendResult.hasError()) {
                    if (StrUtil.containsAny(sendResult.getError().getMessage(), "already known", "nonce too low")) {
                        log.warn("resend missed tx {} failed: {}", tx.getLatestTxHash(), sendResult.getError().getMessage());
                    } else {
                        log.error("resend missed tx {} failed: {}", tx.getLatestTxHash(), sendResult.getError().getMessage());
                    }
                    return;
                }
                if (!StrUtil.equalsIgnoreCase(tx.getLatestTxHash(), sendResult.getTransactionHash())) {
                    log.error("resend tx hash {} not equals to last one {}", sendResult.getTransactionHash(), tx.getLatestTxHash());
                    return;
                }

                tx.setLatestTxSendTime(new Date());
                rollupRepository.updateReliableTransaction(tx);

                log.info("resend tx {} for batch {}-{}", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex());
            }
            return;
        }

        TransactionReceipt receipt = l1Client.queryTxReceipt(tx.getLatestTxHash());
        // tx not packaged
        if (ObjectUtil.isNull(receipt)) {
            // tx not packaged and has been timeout
            if (tx.getLatestTxSendTime().getTime() + txTimeOutLimitSec * 1000L < System.currentTimeMillis()) {
                // timeout the tx
                log.info("{}-{} tx {} timeout from {}, update the gas price and speed it up...",
                        tx.getTransactionType(), tx.getBatchIndex(), tx.getLatestTxHash(), DateUtil.format(tx.getLatestTxSendTime(), DatePattern.NORM_DATETIME_MS_PATTERN));
                TransactionInfo transactionInfo;
                try {
                    transactionInfo = l1Client.speedUpRollupTx(tx);
                } catch (L1ContractWarnException e) {
                    log.info("rollup contract shows that already committed for batch {}-{}", tx.getTransactionType(), tx.getBatchIndex());
                    tx.setRevertReason("repeat commit: " + tx.getTransactionType());
                    tx.setState(ReliableTransactionStateEnum.BIZ_SUCCESS);
                    rollupRepository.updateReliableTransaction(tx);
                    return;
                } catch (NoNeedToSpeedUpException e) {
                    log.warn("The {} tx for batch {} (latest: {}, original: {}) is no need to speed up: {}",
                            tx.getTransactionType(), tx.getBatchIndex(), tx.getOriginalTxHash(), tx.getLatestTxHash(), e.getMessage());
                    return;
                } catch (Exception e) {
                    log.error("failed to speed tx {} up for batch {}-{}", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex(), e);
                    return;
                }
                tx.setLatestTxHash(transactionInfo.getTxHash());
                tx.setLatestTxSendTime(transactionInfo.getSendTxTime());
                tx.setRawTx(transactionInfo.getRawTx());
                tx.setSenderAccount(transactionInfo.getSenderAccount());
                rollupRepository.updateReliableTransaction(tx);
                log.info("successful to speed up tx {} for batch {}-{}", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex());
            } else {
                // tx not packaged, just wait
                log.debug("tx {} not timeout yet, wait for block packaging...", tx.getLatestTxHash());
            }
            return;
        }

        if (receipt.isStatusOK()) {
            if (receipt.getBlockNumber().compareTo(l1Client.queryLatestBlockNumber(l1BlockPollingPolicy)) <= 0
                    && receipt.getBlockNumber().compareTo(BigInteger.ZERO) > 0) {
                // tx has been confirmed
                doMetrics(tx);
                doNotify(tx);
                log.info("🎉 tx {} for batch {}-{} has been finalized and success", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex());
                rollupRepository.updateReliableTransactionState(ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType(), ReliableTransactionStateEnum.TX_SUCCESS);
            } else {
                // tx has been packaged
                if (tx.getState() != ReliableTransactionStateEnum.TX_PACKAGED) {
                    rollupRepository.updateReliableTransactionState(ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType(), ReliableTransactionStateEnum.TX_PACKAGED);
                    log.info("update tx's state to packaged...");
                }
                log.info("tx {} already been packaged into block {}-{}, please wait for block confirmation...",
                        tx.getLatestTxHash(), receipt.getBlockNumber(), receipt.getBlockHash());
            }
        } else {
            // mark it failed
            if (StrUtil.isEmpty(tx.getRevertReason())) {
                tx.setRevertReason("receipt shows: " + receipt.getRevertReason());
            }
            tx.setState(ReliableTransactionStateEnum.TX_FAILED);
            rollupRepository.updateReliableTransaction(tx);
            log.error("tx {} failed of batch {}-{}: {}", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex(), tx.getRevertReason());
        }
    }

    public void processL2PendingTx(ReliableTransactionDO tx) {
        TransactionReceipt receipt = l2Client.queryTxReceipt(tx.getLatestTxHash());
        // tx not packaged
        if (ObjectUtil.isNull(receipt)) {
            // tx not packaged, just wait
            log.debug("tx {}-{} not packaged yet, wait for block packaging...", tx.getTransactionType(), tx.getLatestTxHash());
            return;
        }

        // tx has been confirmed
        if (receipt.getBlockNumber().compareTo(l2Client.queryLatestBlockNumber(l2BlockPollingPolicy)) <= 0
            && receipt.getBlockNumber().compareTo(BigInteger.ZERO) > 0) {
            if (receipt.isStatusOK()) {
                log.info("🎉 tx {}-{} success", tx.getTransactionType(), tx.getLatestTxHash());
                rollupRepository.updateReliableTransactionState(tx.getOriginalTxHash(), ReliableTransactionStateEnum.TX_SUCCESS);
            } else {
                tx.setRevertReason(receipt.getRevertReason());
                tx.setState(ReliableTransactionStateEnum.TX_FAILED);
                log.warn("tx {}-{} shows revert : {}", tx.getLatestTxHash(), tx.getTransactionType(), receipt.getRevertReason());
                rollupRepository.updateReliableTransaction(tx);
            }
        } else {
            // tx has been packaged
            log.info("tx {}-{} already been packaged into block {}-{}, please wait for block confirmation...",
                    tx.getLatestTxHash(), tx.getTransactionType(), receipt.getBlockNumber(), receipt.getBlockHash());
        }
    }

    private void doMetrics(ReliableTransactionDO tx) {
        switch (tx.getTransactionType()) {
            case BATCH_COMMIT_TX:
                selfReportMetric.recordEndAndReportAsync(MetricUtils.getBatchCommitMetricKey(tx.getBatchIndex()));
                break;
            case BATCH_TEE_PROOF_COMMIT_TX:
                otelMetric.recordBatchStableEvent(tx.getBatchIndex());
                selfReportMetric.recordEndAndReportAsync(MetricUtils.getProofCommitMetricKey(ProveTypeEnum.TEE_PROOF, tx.getBatchIndex()));
                break;
            case BATCH_ZK_PROOF_COMMIT_TX:
                selfReportMetric.recordEndAndReportAsync(MetricUtils.getProofCommitMetricKey(ProveTypeEnum.ZK_PROOF, tx.getBatchIndex()));
                break;
            default:
                log.warn("unexpected transaction type for metrics: {}", tx.getTransactionType());
        }
    }

    private void doNotify(ReliableTransactionDO tx) {
        CompletableFuture.runAsync(() -> {
            BatchWrapper batchWrapper = rollupRepository.getBatch(tx.getBatchIndex(), false);
            if (ObjectUtil.isNull(batchWrapper)) {
                log.error("batch {} not found when notify {} proof committed", tx.getBatchIndex(), tx.getTransactionType());
                return;
            }
            if (tx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX) {
                traceServiceClient.notifyProofCommitted(batchWrapper.getEndBlockNumber(), ProveTypeEnum.TEE_PROOF);
            } else if (tx.getTransactionType() == TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX) {
                traceServiceClient.notifyProofCommitted(batchWrapper.getEndBlockNumber(), ProveTypeEnum.ZK_PROOF);
            }
        }).exceptionally(throwable -> {
            log.error("notify {} proof committed for {} failed: ", tx.getTransactionType(), tx.getBatchIndex(), throwable);
            return null;
        });
    }

    private void retryTx(ReliableTransactionDO tx) {
        if (!checkIfRetry(tx)) {
            return;
        }
        retryTx(tx, true);
    }

    private boolean checkIfRetry(ReliableTransactionDO tx) {
        if (tx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX || tx.getTransactionType() == TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX) {
            // check if the previous batch commit settled on Rollup contract.
            var committedBatchIndex = l1Client.lastCommittedBatch(l1BlockPollingPolicy);
            if (committedBatchIndex.compareTo(tx.getBatchIndex()) < 0) {
                log.info("Current {} committed batch is {} and proof verify tx for batch {} has to wait the batch commit tx settled, so skip retry",
                        l1BlockPollingPolicy, committedBatchIndex, tx.getBatchIndex());
                return false;
            }
            if (tx.getBatchIndex().equals(BigInteger.ONE)) {
                return true;
            }
            // check if the previous proof verify tx settled on chain.
            var lastProofCommitTx = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, tx.getBatchIndex().subtract(BigInteger.ONE), tx.getTransactionType());
            if (ObjectUtil.isNull(lastProofCommitTx)) {
                throw new RuntimeException("last proof verify tx not found when retry proof tx of batch#" + tx.getBatchIndex());
            }
            if (lastProofCommitTx.getState() != ReliableTransactionStateEnum.TX_PACKAGED
                    && lastProofCommitTx.getState() != ReliableTransactionStateEnum.TX_SUCCESS) {
                log.info("proof verify tx for batch {} has to wait the previous proof verify tx packaged or success, so skip retry", tx.getBatchIndex());
                return false;
            }
        } else if (tx.getTransactionType() == TransactionTypeEnum.BATCH_COMMIT_TX && !tx.getBatchIndex().equals(BigInteger.ONE)) {
            var commitBatchTx = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, tx.getBatchIndex().subtract(BigInteger.ONE), TransactionTypeEnum.BATCH_COMMIT_TX);
            if (ObjectUtil.isNull(commitBatchTx)) {
                throw new RuntimeException("last commit batch tx not found when retry batch-commit tx of batch#" + tx.getBatchIndex());
            }
            if (commitBatchTx.getState() != ReliableTransactionStateEnum.TX_PACKAGED
                    && commitBatchTx.getState() != ReliableTransactionStateEnum.TX_SUCCESS) {
                log.info("batch commit tx for batch {} has to wait the previous batch commit tx packaged or success, so skip retry", tx.getBatchIndex());
                return false;
            }
        }
        return true;
    }

    private void retryTx(ReliableTransactionDO tx, boolean addCountOrNot) {
        log.info("retry {} tx for batch {}", tx.getTransactionType(), tx.getBatchIndex());
        TransactionInfo transactionInfo = null;
        try {
            var txObj = TransactionDecoder.decode(Numeric.toHexString(tx.getRawTx()));
            if (txObj.getType() == TransactionType.EIP4844) {
                transactionInfo = l1Client.resendRollupTx((Transaction4844) txObj.getTransaction());
            } else {
                transactionInfo = l1Client.resendRollupTx(txObj.getData());
            }
        } catch (L1ContractWarnException e) {
            log.info("rollup contract shows that no need to resend for batch {}-{}", tx.getTransactionType(), tx.getBatchIndex());
            tx.setRevertReason("no need to retry: " + tx.getTransactionType());
            tx.setState(ReliableTransactionStateEnum.BIZ_SUCCESS);
            rollupRepository.updateReliableTransaction(tx);
            return;
        } catch (L1ContractFatalException e) {
            log.error("resend for batch {}-{} failed when eth call contract: ", tx.getTransactionType(), tx.getBatchIndex(), e);
            tx.setRevertReason(e.getRevertReason());
        } catch (L2RelayerException e) {
            log.error("unexpect exception from eth call when resend tx {}-{}:", tx.getTransactionType(), tx.getBatchIndex(), e);
            tx.setRevertReason("unexpected exception, plz check log");
        }

        if (ObjectUtil.isNotNull(transactionInfo)) {
            log.info("resend tx {} with nonce {} for batch {}-{}", transactionInfo.getTxHash(), transactionInfo.getNonce(), tx.getTransactionType(), tx.getBatchIndex());
            tx.setLatestTxHash(transactionInfo.getTxHash());
            tx.setLatestTxSendTime(transactionInfo.getSendTxTime());
            tx.setNonce(transactionInfo.getNonce().longValue());
            tx.setRawTx(transactionInfo.getRawTx());
            tx.setSenderAccount(transactionInfo.getSenderAccount());
            tx.setState(ReliableTransactionStateEnum.TX_PENDING);
        }

        if (addCountOrNot) {
            tx.setRetryCount(tx.getRetryCount() + 1);
        }

        rollupRepository.updateReliableTransaction(tx);
    }
}
