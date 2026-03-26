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

package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.tracer.TraceServiceClient;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.ISelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.MetricUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private IOracleRepository oracleRepository;

    @Resource
    private L1Client l1Client;

    @Resource
    private L2Client l2Client;

    @Resource
    private TraceServiceClient traceServiceClient;

    @Value("${l2-relayer.tasks.reliable-tx.process-batch-size:10}")
    private int processBatchSize;

    @Value("${l2-relayer.tasks.block-polling.l1.policy:FINALIZED}")
    private DefaultBlockParameterName l1BlockPollingPolicy;

    @Value("${l2-relayer.tasks.block-polling.l2.policy:LATEST}")
    private DefaultBlockParameterName l2BlockPollingPolicy;

    @Value("${l2-relayer.tasks.reliable-tx.retry-limit:0}")
    private int retryCountLimit;

    @Value("${l2-relayer.tasks.reliable-tx.tx-timeout-limit:600}")
    private int txTimeOutLimitSec;

    @Value("${l2-relayer.tasks.reliable-tx.parent-chain-tx-missed-tolerant-time:5}")
    private int parentChainTxMissedTolerantTimeSec;

    @Value("${l2-relayer.tasks.reliable-tx.subchain-tx-missed-tolerant-time:5}")
    private int subChainTxMissedTolerantTimeSec;

    @Value("${l2-relayer.l1-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l1NoncePolicy;

    @Resource
    private ISelfReportMetric selfReportMetric;

    @Resource
    private IOtelMetric otelMetric;

    @Resource
    private RollupConfig rollupConfig;

    @Value("${l2-relayer.tasks.oracle-gas-feed.base-proof:TEE_PROOF}")
    private OracleBaseProofTypeEnum baseProofType;

    @Override
    public void processL1NotFinalizedTx() {
        // Process TX_PACKAGED first, then TX_PENDING.
        // This order prevents duplicate processing since TX_PENDING transactions
        // may transition to TX_PACKAGED during processing.
        processL1PackagedStateTx();
        processL1PendingStateTx();
    }

    /**
     * Processes L1 transactions in TX_PACKAGED state.
     *
     * <p>TX_PACKAGED transactions are processed using the same logic as TX_PENDING
     * because block reorganization may cause packaged transactions to become lost
     * and require re-submission.</p>
     */
    private void processL1PackagedStateTx() {
        var packagedTxList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PACKAGED,
                processBatchSize
        );
        if (ObjectUtil.isEmpty(packagedTxList)) {
            log.debug("No L1 TX_PACKAGED reliable tx found, skip it...");
            return;
        }

        for (ReliableTransactionDO tx : packagedTxList) {
            try {
                processL1UnfinalizedTx(tx);
            } catch (RollupEconomicStrategyNotAllowedException e) {
                log.warn("L1 gas price too high, skip packaged tx {}-{}-{}: {}",
                        tx.getChainType(), tx.getTransactionType(), tx.getBatchIndex(), e.getMessage());
            } catch (Exception e) {
                log.error("Process L1 packaged tx for batch {}-{} failed: ",
                        tx.getTransactionType(), tx.getBatchIndex(), e);
            }
        }
    }

    /**
     * Processes L1 transactions in TX_PENDING state.
     *
     * <p>TX_PENDING transactions are checked for:
     * <ul>
     *   <li>Transaction loss - resend if transaction is not found on chain</li>
     *   <li>Timeout - speed up transaction with higher gas price if not packaged within timeout</li>
     *   <li>Confirmation - update state based on transaction receipt</li>
     * </ul>
     * </p>
     */
    private void processL1PendingStateTx() {
        var pendingTxList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PENDING,
                processBatchSize
        );
        if (ObjectUtil.isEmpty(pendingTxList)) {
            log.debug("No L1 TX_PENDING reliable tx found, skip it...");
            return;
        }

        for (ReliableTransactionDO tx : pendingTxList) {
            try {
                processL1UnfinalizedTx(tx);
            } catch (RollupEconomicStrategyNotAllowedException e) {
                log.warn("L1 gas price too high, skip pending tx {}-{}-{}: {}",
                        tx.getChainType(), tx.getTransactionType(), tx.getBatchIndex(), e.getMessage());
            } catch (Exception e) {
                log.error("Process L1 pending tx for batch {}-{} failed: ",
                        tx.getTransactionType(), tx.getBatchIndex(), e);
            }
        }
    }

    @Override
    public void processL2NotFinalizedTx() {
        var reliableTransactions = rollupRepository.getNotFinalizedReliableTransactions(ChainTypeEnum.LAYER_TWO, processBatchSize);
        if (ObjectUtil.isEmpty(reliableTransactions)) {
            log.debug("No L2 pending reliable tx found, skip it...");
            return;
        }

        for (ReliableTransactionDO tx : reliableTransactions) {
            try {
                processL2PendingTx(tx);
            } catch (RollupEconomicStrategyNotAllowedException e) {
                log.warn("gas price too high, skip this tx {}-{}-{}: {}", tx.getChainType(), tx.getTransactionType(), tx.getBatchIndex(), e.getMessage());
            } catch (Exception e) {
                log.error("process L2 reliable tx for batch {}-{} failed: ", tx.getTransactionType(), tx.getBatchIndex(), e);
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
        var l1TxList = reliableTransactions.stream().filter(x -> x.getChainType() == ChainTypeEnum.LAYER_ONE).collect(Collectors.toList());
        var l2TxList = reliableTransactions.stream().filter(x -> x.getChainType() == ChainTypeEnum.LAYER_TWO).collect(Collectors.toList());

        processL1FailedTx(l1TxList);
        processL2FailedTx(l2TxList);
    }

    private void processL1FailedTx(List<ReliableTransactionDO> reliableTransactions) {
        if (reliableTransactions.isEmpty()) {
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
        reliableTransactions.forEach(this::retryL1Tx);
    }

    private void processL2FailedTx(List<ReliableTransactionDO> reliableTransactions) {
        if (reliableTransactions.isEmpty()) {
            return;
        }
        ListUtil.sort(reliableTransactions, Comparator.comparing(ReliableTransactionDO::getBatchIndex));
        reliableTransactions.sort(Comparator.comparing(ReliableTransactionDO::getBatchIndex));
        reliableTransactions.forEach(this::retryL2Tx);
    }

    /**
     * Processes a single L1 reliable transaction that is not yet finalized.
     *
     * <p>This method handles transactions in both TX_PENDING and TX_PACKAGED states,
     * as block reorganization may cause packaged transactions to become lost.</p>
     *
     * <p>The processing flow:
     * <ol>
     *   <li>Check if transaction exists on chain - if not, handle missing transaction</li>
     *   <li>Check if transaction has receipt - if not, handle unpackaged transaction</li>
     *   <li>Process transaction based on receipt status</li>
     * </ol>
     * </p>
     *
     * @param tx the reliable transaction to process
     */
    private void processL1UnfinalizedTx(ReliableTransactionDO tx) {
        var transaction = l1Client.queryTx(tx.getLatestTxHash());

        // Scenario 1: Transaction not found on blockchain (possibly lost)
        if (ObjectUtil.isNull(transaction)) {
            handleL1MissingTx(tx);
            return;
        }

        // Scenario 2: Transaction exists, check receipt
        TransactionReceipt receipt = l1Client.queryTxReceipt(tx.getLatestTxHash());
        if (ObjectUtil.isNull(receipt)) {
            handleL1UnpackagedTx(tx);
            return;
        }

        // Scenario 3: Transaction has receipt, process based on status
        handleL1TxWithReceipt(tx, receipt);
    }

    /**
     * Handles L1 transaction that is missing from the blockchain.
     *
     * <p>This may occur due to:
     * <ul>
     *   <li>Transaction was dropped from mempool</li>
     *   <li>Block reorganization caused transaction to be reverted</li>
     *   <li>Network issues causing transaction to not propagate</li>
     * </ul>
     * </p>
     *
     * @param tx the missing transaction to handle
     */
    private void handleL1MissingTx(ReliableTransactionDO tx) {
        // Check if still within tolerant duration
        if (parentChainTxMissedTolerantTimeSec > 0
                && tx.getLatestTxSendTime().getTime() + parentChainTxMissedTolerantTimeSec * 1000L >= System.currentTimeMillis()) {
            log.info("Tx {} not found on parent-chain node, but still in tolerant duration, so we just continue...", tx.getLatestTxHash());
            return;
        }

        log.info("nonce {} missed for {} tx {} of batch {}, resend the raw signed tx...",
                tx.getNonce(), tx.getTransactionType(), tx.getLatestTxHash(), tx.getBatchIndex());

        if (l1NoncePolicy == EthNoncePolicyEnum.NORMAL) {
            retryL1Tx(tx, false);
        } else {
            handleL1MissingTxWithNonceCheck(tx);
        }
    }

    /**
     * Handles missing L1 transaction with nonce validation.
     *
     * <p>If the finalized nonce of sender account is greater than this transaction's nonce,
     * it means the transaction has already been processed and no resend is needed.</p>
     *
     * @param tx the missing transaction to handle
     */
    private void handleL1MissingTxWithNonceCheck(ReliableTransactionDO tx) {
        // Check if nonce has already been finalized
        if (l1Client.queryTxCount(tx.getSenderAccount(), l1BlockPollingPolicy).longValue() > tx.getNonce()) {
            log.info("Nonce {} of {} tx {} for batch {} has been finalized, update its state to success",
                    tx.getNonce(), tx.getTransactionType(), tx.getLatestTxHash(), tx.getBatchIndex());
            tx.setState(ReliableTransactionStateEnum.TX_SUCCESS);
            rollupRepository.updateReliableTransaction(tx);

            // WARN: The intermediate transactions of retry cannot be found,
            // and the txHash does not exist during the retry process.
            TransactionReceipt originalTxReceipt = l1Client.queryTxReceipt(tx.getOriginalTxHash());
            if (rollupConfig.getParentChainType().needRollupFeeFeed()
                    && ObjectUtil.isNotNull(originalTxReceipt)
                    && originalTxReceipt.isStatusOK()) {
                createL2GasFeedRequest(tx, originalTxReceipt);
            }
            return;
        }

        // Resend the raw transaction
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

    /**
     * Handles L1 transaction that exists on chain but has no receipt yet.
     *
     * <p>If the transaction has been pending for longer than the timeout limit,
     * it will be sped up with a higher gas price.</p>
     *
     * @param tx the unpackaged transaction to handle
     */
    private void handleL1UnpackagedTx(ReliableTransactionDO tx) {
        // Check if transaction has timed out
        if (tx.getLatestTxSendTime().getTime() + txTimeOutLimitSec * 1000L < System.currentTimeMillis()) {
            speedUpL1Tx(tx);
        } else {
            log.debug("tx {} not timeout yet, wait for block packaging...", tx.getLatestTxHash());
        }
    }

    /**
     * Speeds up an L1 transaction by resubmitting with higher gas price.
     *
     * @param tx the transaction to speed up
     */
    private void speedUpL1Tx(ReliableTransactionDO tx) {
        log.info("{}-{} tx {} timeout from {}, update the gas price and speed it up...",
                tx.getTransactionType(), tx.getBatchIndex(), tx.getLatestTxHash(),
                DateUtil.format(tx.getLatestTxSendTime(), DatePattern.NORM_DATETIME_MS_PATTERN));

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
        } catch (RollupEconomicStrategyNotAllowedException e) {
            // Going to be handled outside this method
            throw e;
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
    }

    /**
     * Handles L1 transaction that has a receipt.
     *
     * @param tx the transaction to handle
     * @param receipt the transaction receipt
     */
    private void handleL1TxWithReceipt(ReliableTransactionDO tx, TransactionReceipt receipt) {
        if (receipt.isStatusOK()) {
            handleL1SuccessfulTx(tx, receipt);
        } else {
            handleL1FailedTx(tx, receipt);
        }
    }

    /**
     * Handles L1 transaction with successful receipt.
     *
     * <p>Checks if the transaction has been finalized (block confirmed) or just packaged.</p>
     *
     * @param tx the successful transaction
     * @param receipt the transaction receipt
     */
    private void handleL1SuccessfulTx(ReliableTransactionDO tx, TransactionReceipt receipt) {
        boolean isFinalized = receipt.getBlockNumber().compareTo(l1Client.queryLatestBlockNumber(l1BlockPollingPolicy)) <= 0
                && receipt.getBlockNumber().compareTo(BigInteger.ZERO) > 0;

        if (isFinalized) {
            // Transaction has been confirmed
            doMetrics(tx);
            doNotify(tx);
            log.info("🎉 tx {} for batch {}-{} has been finalized and success",
                    tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex());
            rollupRepository.updateReliableTransactionState(
                    ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType(), ReliableTransactionStateEnum.TX_SUCCESS);

            if (rollupConfig.getParentChainType().needRollupFeeFeed()) {
                createL2GasFeedRequest(tx, receipt);
            }
            removeFinalizedTxDataAsync(tx);
        } else {
            // Transaction has been packaged but not yet finalized
            if (tx.getState() != ReliableTransactionStateEnum.TX_PACKAGED) {
                rollupRepository.updateReliableTransactionState(
                        ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType(), ReliableTransactionStateEnum.TX_PACKAGED);
                log.info("update tx's state to packaged...");
            }
            log.info("tx {} already been packaged into block {}-{}, please wait for block confirmation...",
                    tx.getLatestTxHash(), receipt.getBlockNumber(), receipt.getBlockHash());
        }
    }

    /**
     * Handles L1 transaction with failed receipt.
     *
     * @param tx the failed transaction
     * @param receipt the transaction receipt
     */
    private void handleL1FailedTx(ReliableTransactionDO tx, TransactionReceipt receipt) {
        if (StrUtil.isEmpty(tx.getRevertReason())) {
            tx.setRevertReason("receipt shows: " + receipt.getRevertReason());
        }
        tx.setState(ReliableTransactionStateEnum.TX_FAILED);
        rollupRepository.updateReliableTransaction(tx);
        log.error("tx {} failed of batch {}-{}: {}",
                tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex(), tx.getRevertReason());
    }

    private void processL2PendingTx(ReliableTransactionDO tx) {
        // only oracle feeding tx need to check
        // if tx is lost
        if ((tx.getTransactionType() == TransactionTypeEnum.L2_ORACLE_BASE_FEE_FEED_TX || tx.getTransactionType() == TransactionTypeEnum.L2_ORACLE_BATCH_FEE_FEED_TX)
                && ObjectUtil.isNull(l2Client.queryTxWithRetry(tx.getSenderAccount(), tx.getLatestTxHash(), BigInteger.valueOf(tx.getNonce())))) {
            if (subChainTxMissedTolerantTimeSec > 0
                    && tx.getLatestTxSendTime().getTime() + subChainTxMissedTolerantTimeSec * 1000L >= System.currentTimeMillis()) {
                log.info("Tx {} not found on subchain node, but still in tolerant duration, so we just continue...", tx.getLatestTxHash());
                return;
            }
            // tx not found on blockchain
            log.warn("🚨 oracle feeding tx is lost, oracle index: {}, oracle type: {}, txHash: {}. try to resend it...",
                    tx.getBatchIndex(), tx.getTransactionType(), tx.getLatestTxHash());
            switch (tx.getTransactionType()) {
                case L2_ORACLE_BATCH_FEE_FEED_TX:
                    if (tx.getBatchIndex().compareTo(oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE)) < 0) {
                        log.warn("already have bigger batch-fee feeding oracle request, just skip current lost one.");
                        tx.setState(ReliableTransactionStateEnum.BIZ_SUCCESS);
                        rollupRepository.updateReliableTransaction(tx);
                        return;
                    }
                    break;
                case L2_ORACLE_BASE_FEE_FEED_TX:
                    if (tx.getBatchIndex().compareTo(oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE)) < 0) {
                        log.warn("already have bigger block-fee feeding oracle request, just skip current lost one.");
                        tx.setState(ReliableTransactionStateEnum.BIZ_SUCCESS);
                        rollupRepository.updateReliableTransaction(tx);
                        return;
                    }
                    break;
            }
            var txObj = EthTxDecoder.decode(Numeric.toHexString(tx.getRawTx()));
            TransactionInfo transactionInfo = null;
            try {
                transactionInfo = l2Client.resendGasFeedTx(txObj.getData());
            } catch (L2RelayerException e) {
                dealWithFailedTxReason(tx, e);
                tx.setState(ReliableTransactionStateEnum.TX_FAILED);
            }
            if (ObjectUtil.isNotNull(transactionInfo)) {
                log.info("resend the lost l2 tx {} with nonce {} for {}-{}", transactionInfo.getTxHash(), transactionInfo.getNonce(), tx.getTransactionType(), tx.getBatchIndex());
                tx.setLatestTxHash(transactionInfo.getTxHash());
                tx.setLatestTxSendTime(transactionInfo.getSendTxTime());
                tx.setNonce(transactionInfo.getNonce().longValue());
                tx.setRawTx(transactionInfo.getRawTx());
                tx.setSenderAccount(transactionInfo.getSenderAccount());
            }
            rollupRepository.updateReliableTransaction(tx);
            return;
        }

        if (tx.getTransactionType() == TransactionTypeEnum.L1_MSG_TX && ObjectUtil.isNull(l2Client.queryTxWithRetry(tx.getSenderAccount(), tx.getLatestTxHash(), BigInteger.valueOf(tx.getNonce())))) {
            retryL1MsgTx(tx);
            return;
        }
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

    private void removeFinalizedTxDataAsync(ReliableTransactionDO tx) {
        CompletableFuture.runAsync(() -> {
            log.info("try to remove raw tx data for {}-{}-{} because that tx has been confirmed success", ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType());
            rollupRepository.removeRawTx(ChainTypeEnum.LAYER_ONE, tx.getBatchIndex(), tx.getTransactionType());
        }).exceptionally(throwable -> {
            log.error("remove raw tx data for {}-{} from db failed: ", tx.getTransactionType(), tx.getBatchIndex(), throwable);
            return null;
        });
    }

    private void retryL1Tx(ReliableTransactionDO tx) {
        if (!checkIfRetry(tx)) {
            return;
        }
        try {
            retryL1Tx(tx, true);
        } catch (RollupEconomicStrategyNotAllowedException e) {
            log.warn("gas price too high when retry tx {}-{}: {}", tx.getTransactionType(), tx.getBatchIndex(), e.getMessage());
        }
    }

    private void retryL1MsgTx(ReliableTransactionDO tx) {
        // only resend origin tx without change nonce
        l2Client.resendL1MsgTx(L1MsgTransaction.decode(tx.getRawTx()));
    }

    private void retryL2Tx(ReliableTransactionDO tx) {
        if (tx.getRetryCount() >= retryCountLimit) {
            log.info("retry {} tx for oracle request Index {} excess retry limit: {}", tx.getTransactionType(), tx.getBatchIndex(), retryCountLimit);
            return;
        }

        log.info("retry {} l2 tx for oracle request Index {}", tx.getTransactionType(), tx.getBatchIndex());
        TransactionInfo transactionInfo = null;
        if (tx.getTransactionType() != TransactionTypeEnum.L1_MSG_TX) {
            try {
                var txObj = EthTxDecoder.decode(Numeric.toHexString(tx.getRawTx()));
                transactionInfo = l2Client.resendGasFeedTx(txObj.getData());
            } catch (L2RelayerException e) {
                dealWithFailedTxReason(tx, e);
            }
        }

        if (ObjectUtil.isNotNull(transactionInfo)) {
            log.info("resend l2 tx {} with nonce {} for {}-{}", transactionInfo.getTxHash(), transactionInfo.getNonce(), tx.getTransactionType(), tx.getBatchIndex());
            tx.setLatestTxHash(transactionInfo.getTxHash());
            tx.setLatestTxSendTime(transactionInfo.getSendTxTime());
            tx.setNonce(transactionInfo.getNonce().longValue());
            tx.setRawTx(transactionInfo.getRawTx());
            tx.setSenderAccount(transactionInfo.getSenderAccount());
            tx.setState(ReliableTransactionStateEnum.TX_PENDING);
        }

        tx.setRetryCount(tx.getRetryCount() + 1);

        rollupRepository.updateReliableTransaction(tx);
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
            if (tx.getTransactionType() == TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX && rollupConfig.getZkVerificationStartBatch().equals(tx.getBatchIndex())) {
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

    private void retryL1Tx(ReliableTransactionDO tx, boolean addCountOrNot) {
        log.info("retry {} tx for batch {}", tx.getTransactionType(), tx.getBatchIndex());
        TransactionInfo transactionInfo = null;
        try {
            var txObj = EthTxDecoder.decode(Numeric.toHexString(tx.getRawTx()));
            if (txObj.getType() == TransactionType.EIP4844) {
                transactionInfo = l1Client.resendRollupTx(tx, (Transaction4844) txObj.getTransaction());
            } else {
                transactionInfo = l1Client.resendRollupTx(tx, txObj.getData());
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
        } catch (RollupEconomicStrategyNotAllowedException e) {
            // catch it and throw seems strange, but this e is gonna to be caught outside.
            throw e;
        } catch (L2RelayerException e) {
            dealWithFailedTxReason(tx, e);
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

    private void createL2GasFeedRequest(ReliableTransactionDO tx, TransactionReceipt txReceipt) {
        try {
            if (tx.getTransactionType().equals(TransactionTypeEnum.BATCH_COMMIT_TX)) {
                oracleRepository.saveRollupTxReceipt(
                        tx.getBatchIndex(),
                        OracleTypeEnum.L2_GAS_ORACLE,
                        OracleRequestTypeEnum.L2_BATCH_COMMIT,
                        txReceipt
                );
                log.info("🎉 create L2 batch commit record success, batchIndex: {}", tx.getBatchIndex());
            } else if (
                    (baseProofType.equals(OracleBaseProofTypeEnum.TEE_PROOF) && tx.getTransactionType().equals(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX))
                            || (baseProofType.equals(OracleBaseProofTypeEnum.ZK_PROOF) && tx.getTransactionType().equals(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX))
            ) {
                oracleRepository.saveRollupTxReceipt(
                        tx.getBatchIndex(),
                        OracleTypeEnum.L2_GAS_ORACLE,
                        OracleRequestTypeEnum.L2_BATCH_PROVE,
                        txReceipt
                );
                log.info("🎉 create L2 batch prove record success, batchIndex: {}, proveType: {}", tx.getBatchIndex(), baseProofType);
            }
        } catch (Exception e) {
            log.error("🚨 create L2 gas feed Request failed, batchIndex: {}.", tx.getBatchIndex());
            throw new RuntimeException(e);
        }
    }

    private void dealWithFailedTxReason(ReliableTransactionDO tx, L2RelayerException e) {
        log.error("unexpect exception from eth call when resend tx {}-{}:", tx.getTransactionType(), tx.getBatchIndex(), e);
        var revertReason = "unexpected: ";
        tx.setRevertReason(
                e.getMessage().length() > 200 ?
                        revertReason + StrUtil.sub(e.getMessage(), e.getMessage().length() - 200, e.getMessage().length())
                        : revertReason + e.getMessage()
        );
    }
}
