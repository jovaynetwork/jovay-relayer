package com.alipay.antchain.l2.relayer.metrics.alarm;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.RollupNumberInfo;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "l2-relayer.alarm.rollup.switch", havingValue = "true")
@Slf4j
public class RollupAlarm {

    @Value("${l2-relayer.tasks.batch-prove.prove-req-number-per-batch-limit:10}")
    private int proveReqNumberPerBatchLimit;

    @Value("${l2-relayer.tasks.batch-prove.req-types:ALL}")
    private String batchProveReqTypes;

    @Value("${l2-relayer.alarm.rollup.proof-delayed-threshold:300000}")
    private long proofDelayedThreshold;

    @Value("${l2-relayer.alarm.rollup.l2-block-delayed-threshold:300000}")
    private long l2BlockDelayedThreshold;

    @Value("${l2-relayer.alarm.rollup.chunk-delayed-threshold:3600000}")
    private long chunkDelayedThreshold;

    @Value("${l2-relayer.alarm.rollup.batch-delayed-threshold:10800000}")
    private long batchDelayedThreshold;

    @Value("${l2-relayer.alarm.rollup.tx-over-pending-threshold:1800000}")
    private long txDelayedThreshold;

    @Value("${l2-relayer.alarm.rollup.max-gap-between-batch-and-proof-commit:20}")
    private long maxGapBetweenBatchAndProofCommit;

    @Value("${l2-relayer.alarm.rollup.circuit-breaker-threshold:18000000}")
    private long circuitBreakerThreshold;

    @Resource
    private IRollupRepository rollupRepository;

    @Scheduled(fixedRateString = "${l2-relayer.alarm.rollup.interval:10000}")
    public void process() {
        log.debug("try to process rollup alarm");
        checkProveReqs();
        checkProcessedL2Block();
        checkDelayedChunk();
        checkDelayedBatch();
        checkOverPendingTransactions();
        checkGapBetweenBatchAndProofCommit();
        circuitBreakerWarning();
    }

    private void checkProveReqs() {
        List<BatchProveRequestDO> requestDOS = new ArrayList<>();
        if (RollupUtils.isProveReqTypeToProcess(batchProveReqTypes, ProveTypeEnum.TEE_PROOF)) {
            requestDOS.addAll(rollupRepository.peekPendingBatchProveRequest(proveReqNumberPerBatchLimit, ProveTypeEnum.TEE_PROOF));
        }
        if (RollupUtils.isProveReqTypeToProcess(batchProveReqTypes, ProveTypeEnum.ZK_PROOF)) {
            requestDOS.addAll(rollupRepository.peekPendingBatchProveRequest(proveReqNumberPerBatchLimit, ProveTypeEnum.ZK_PROOF));
        }
        requestDOS.stream()
                .filter(req -> isTimeOverThreshold(req.getGmtModified(), proofDelayedThreshold))
                .forEach(req -> {
                    log.error("🚔 rollup alarm: delayed prove request {}-{} is pending since {}",
                            req.getProveType(), req.getBatchIndex(), DateUtil.format(req.getGmtModified(), DatePattern.NORM_DATETIME_MS_PATTERN));
                });
    }

    private void checkProcessedL2Block() {
        RollupNumberInfo info = rollupRepository.getRollupNumberInfo(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED);
        if (info != null && isTimeOverThreshold(info.getGmtModified(), l2BlockDelayedThreshold)) {
            log.error("🚔 rollup alarm: l2 next block {} has been delayed since {}",
                    info.getNumber().add(BigInteger.ONE), DateUtil.format(info.getGmtModified(), DatePattern.NORM_DATETIME_MS_PATTERN));
        }
    }

    private void checkDelayedChunk() {
        RollupNumberInfo info = rollupRepository.getRollupNumberInfo(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK);
        if (info != null && isTimeOverThreshold(info.getGmtModified(), chunkDelayedThreshold)) {
            log.error("🚔 rollup alarm: chunk {} has been delayed since {}",
                    info.getNumber(), DateUtil.format(info.getGmtModified(), DatePattern.NORM_DATETIME_MS_PATTERN));
        }
    }

    private void checkDelayedBatch() {
        RollupNumberInfo info = rollupRepository.getRollupNumberInfo(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH);
        if (info != null && isTimeOverThreshold(info.getGmtModified(), batchDelayedThreshold)) {
            log.error("🚔 rollup alarm: batch {} has been delayed since {}",
                    info.getNumber(), DateUtil.format(info.getGmtModified(), DatePattern.NORM_DATETIME_MS_PATTERN));
        }
    }

    private void checkOverPendingTransactions() {
        List<ReliableTransactionDO> txs = rollupRepository.getTxPendingReliableTransactions(proveReqNumberPerBatchLimit);
        if (ObjectUtil.isEmpty(txs)) {
            return;
        }
        txs.stream()
                .filter(tx -> isTimeOverThreshold(tx.getGmtCreate(), txDelayedThreshold))
                .forEach(tx -> {
                    log.error("🚔 rollup alarm: delayed tx {} is pending since {}",
                            tx.getOriginalTxHash(), DateUtil.format(tx.getGmtCreate(), DatePattern.NORM_DATETIME_MS_PATTERN));
                });
    }

    private void checkGapBetweenBatchAndProofCommit() {
        var batchCommittedIndex = rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED);
        if (batchCommittedIndex == null) {
            return;
        }
        var batchIndex = batchCommittedIndex.subtract(BigInteger.valueOf(maxGapBetweenBatchAndProofCommit));
        if (batchIndex.compareTo(BigInteger.ZERO) <= 0) {
            return;
        }
        if (ObjectUtil.isNull(rollupRepository.getReliableTransaction(
                ChainTypeEnum.LAYER_ONE,
                batchIndex,
                TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX
        ))) {
            log.error("🚔 rollup alarm: gap between batch and proof committed is over limit, latest batch committed is {}",
                    batchCommittedIndex);
        }
    }

    private void circuitBreakerWarning() {
        try {
            var batchCommittedNumInfo = rollupRepository.getRollupNumberInfo(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED);
            if (batchCommittedNumInfo == null) {
                return;
            }
            if (isTimeOverThreshold(batchCommittedNumInfo.getGmtModified(), circuitBreakerThreshold)) {
                var gap = rollupRepository.calcWaitingBatchCountBeyondIndex(batchCommittedNumInfo.getNumber());
                log.warn("🚔 rollup alarm - circuit breaker warning that last batch committed is {}, waiting batch size: {}",
                        batchCommittedNumInfo.getNumber(), gap);
            }
        } catch (Throwable t) {
            log.warn("circuitBreakerWarning execute failed: ", t);
        }
    }

    private boolean isTimeOverThreshold(Date lastModified, long threshold) {
        return System.currentTimeMillis() - lastModified.getTime() > threshold;
    }
}
