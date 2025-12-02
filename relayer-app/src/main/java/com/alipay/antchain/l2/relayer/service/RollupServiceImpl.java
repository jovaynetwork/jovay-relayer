package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.alipay.antchain.l2.relayer.config.RollupConfig;
import jakarta.annotation.Resource;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.RollupThrottle;
import com.alipay.antchain.l2.relayer.core.layer2.IL2MsgFetcher;
import com.alipay.antchain.l2.relayer.core.layer2.IRollupAggregator;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.dal.repository.IL2MerkleTreeRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;

@Service
@Slf4j
public class RollupServiceImpl implements IRollupService {

    @Resource
    private ExecutorService blockPollingTaskExecutorThreadsPool;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private L2Client l2Client;

    @Resource
    private L1Client l1Client;

    @Resource
    private RollupThrottle rollupThrottle;

    @Resource
    private IRollupAggregator rollupAggregator;

    @Resource
    private IL2MsgFetcher l2MsgFetcher;

    @Resource
    private ProverControllerClient proverControllerClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Resource
    private IL2MerkleTreeRepository l2MerkleTreeRepository;

    @Resource
    private IOtelMetric otelMetric;

    @Resource
    private RollupConfig rollupConfig;

    @Value("${l2-relayer.tasks.block-polling.l2.policy:LATEST}")
    private DefaultBlockParameterName blockPollingPolicy;

    @Value("${l2-relayer.tasks.block-polling.l2.max-poling-block-size:32}")
    private int maxPollingBlockSize;

    @Value("${l2-relayer.tasks.batch-commit.batch-commit-windows-length:12}")
    private int batchCommitWindowsLength;

    @Value("${l2-relayer.tasks.batch-prove.prove-req-number-per-batch-limit:10}")
    private int proveReqNumberPerBatchLimit;

    @Value("${l2-relayer.tasks.batch-prove.req-types:ALL}")
    private String batchProveReqTypes;

    /**
     * Timeout for getting l2 block trace from future, in sec.
     */
    @Value("${l2-relayer.tasks.block-polling.l2.get-block-timeout:10}")
    private int getL2BlockTimeout;

    @Value("${l2-relayer.tasks.proof-commit.rollup-query-level:LATEST}")
    private DefaultBlockParameterName proofCommitRollupQueryLevel;

    /**
     * When try to commit proof to rollup contract, relayer gonna to
     * query the last committed batch index from rollup contract.
     * The query would be based on number from the {@code proofCommitRollupQueryLevel} height
     * subtract {@code proofCommitRollupQueryHeightBackoff}.
     */
    @Value("${l2-relayer.tasks.proof-commit.rollup-query-height-backoff:0}")
    private int proofCommitRollupQueryHeightBackoff;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAnchorBatch(BatchHeader anchorBatch, AppendMerkleTree anchorBatchMerkleTree) {
        log.info("⚙️save anchor batch {}-{}...", anchorBatch.getBatchIndex(), HexUtil.encodeHexStr(anchorBatch.getHash()));
        if (systemConfigRepository.isAnchorBatchSet()) {
            throw new RuntimeException("anchor batch has been set!");
        }
        if (ObjectUtil.isNull(anchorBatchMerkleTree)) {
            log.info("no anchor batch merkle tree, use empty tree instead");
            anchorBatchMerkleTree = AppendMerkleTree.EMPTY_TREE;
        }

        if (!anchorBatch.getBatchIndex().equals(BigInteger.ZERO)) {
            throw new RuntimeException("only accept zero batch as anchor batch for now 🙇");
        }

        // For now, only batch zero accept here
        BigInteger endHeight = BigInteger.ZERO;
        BigInteger startHeight = BigInteger.ZERO;
        log.info("anchor batch from block {} to {} included", startHeight, endHeight);

        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH, anchorBatch.getBatchIndex().add(BigInteger.ONE));
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH, anchorBatch.getBatchIndex().add(BigInteger.ONE));
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED, anchorBatch.getBatchIndex());
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED, endHeight);
        rollupRepository.savePartialBatchHeader(anchorBatch, 0, 0, startHeight, endHeight);

        l2MerkleTreeRepository.saveMerkleTree(anchorBatchMerkleTree, anchorBatch.getBatchIndex());

        systemConfigRepository.markAnchorBatchHasBeenSet();

        log.info("success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAnchorBatch(BigInteger batchIndex) {
        throw new RuntimeException("This method has been deprecated, please use another func");
    }

    @Override
    public void pollL2Blocks() {
        BigInteger currL2Height = l2Client.queryLatestBlockNumber(blockPollingPolicy);
        BigInteger heightProcessed = getProcessedBlockHeight();
        if (currL2Height.compareTo(heightProcessed) <= 0) {
            log.debug("already processed the latest height {} on L2", currL2Height);
            return;
        }

        BigInteger maxPollingLimit = heightProcessed.add(BigInteger.valueOf(maxPollingBlockSize));
        maxPollingLimit = currL2Height.compareTo(maxPollingLimit) <= 0 ? currL2Height : maxPollingLimit;
        log.info("process blocks from {} to {} included and latest {} on l2 chain", heightProcessed.add(BigInteger.ONE), maxPollingLimit, currL2Height);

        Map<BigInteger, CompletableFuture<BasicBlockTrace>> blockFutureWrappers = new HashMap<>();
        for (BigInteger h = heightProcessed.add(BigInteger.ONE); h.compareTo(maxPollingLimit) <= 0; h = h.add(BigInteger.ONE)) {
            BigInteger currHeight = h;
            blockFutureWrappers.put(
                    currHeight,
                    CompletableFuture.supplyAsync(() -> rollupRepository.getL2BlockTrace(currHeight), blockPollingTaskExecutorThreadsPool)
            );
        }

        for (BigInteger h = heightProcessed.add(BigInteger.ONE); h.compareTo(maxPollingLimit) <= 0; h = h.add(BigInteger.ONE)) {
            log.info("👀 start to process l2 block {}", h);
            BigInteger currHeight = h;
            transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            // process the block
                            BasicBlockTrace blockTrace;
                            try {
                                blockTrace = blockFutureWrappers.get(currHeight).get(getL2BlockTimeout, TimeUnit.SECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                if (e instanceof ExecutionException && e.getCause() instanceof BlockTraceNotReadyException) {
                                    throw (BlockTraceNotReadyException) e.getCause();
                                }
                                throw new BlockPollingException(e, "failed to get {} block trace from future: ", currHeight);
                            }

                            // aggregate chunks and batch
                            var batchThisBlockBelongsTo = rollupAggregator.process(blockTrace);
                            l2MsgFetcher.process(blockTrace, batchThisBlockBelongsTo);

                            rollupRepository.updateRollupNumberRecord(
                                    ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED,
                                    currHeight
                            );
                            log.info("l2 block {} processed successfully", currHeight);
                        }
                    }
            );
        }
    }

    @Override
    public void proveTeeL2Batch() {
        proveL2Batch(ProveTypeEnum.TEE_PROOF);
    }

    @Override
    public void proveZkL2Batch() {
        proveL2Batch(ProveTypeEnum.ZK_PROOF);
    }

    @Override
    public void commitL2Batch() {
        if (!rollupThrottle.checkL1BlobPoolTraffic()) {
            log.warn("too many l1 txns is pending, so skip this committing process");
            return;
        }
        BigInteger lastCommittedBatchIdx = l1Client.lastCommittedBatch();
        if (ObjectUtil.isNull(lastCommittedBatchIdx)) {
            throw new CommitL2BatchException("null result from calling lastCommittedBatch of rollup contract");
        }
        var windowEnd = lastCommittedBatchIdx.add(BigInteger.valueOf(batchCommitWindowsLength));
        log.info("🧐 process batch committing window from {}+1 to {}", lastCommittedBatchIdx, windowEnd);
        var localBatchIndexCommitted = rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED);
        for (
                var nextBatchIndexExpectedOnChain = lastCommittedBatchIdx.add(BigInteger.ONE);
                nextBatchIndexExpectedOnChain.compareTo(windowEnd) <= 0;
                nextBatchIndexExpectedOnChain = nextBatchIndexExpectedOnChain.add(BigInteger.ONE)
        ) {
            log.debug("process batch {} committing", nextBatchIndexExpectedOnChain);
            var finalNextBatchIndexExpectedOnChain = nextBatchIndexExpectedOnChain;
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                commitNextL2Batch(finalNextBatchIndexExpectedOnChain, localBatchIndexCommitted);
                            }
                        }
                );
            } catch (BatchNotReadyException e) {
                // batch not ready, so no need to continue 😴
                log.info("batch {} not ready, so break the loop", finalNextBatchIndexExpectedOnChain);
                break;
            } catch (BatchCommitFailedException e) {
                // batch commit failed, so no need to continue 😈
                log.warn("batch {} commit failed, so break the loop, plz check the transaction fail reason", finalNextBatchIndexExpectedOnChain);
                break;
            }
        }
    }

    @Override
    public void commitL2TeeProof() {
        if (!RollupUtils.isProveReqTypeToProcess(batchProveReqTypes, ProveTypeEnum.TEE_PROOF)) {
            log.debug("skip tee proof commit now!");
            return;
        }
        if (!rollupThrottle.checkL1LegacyPoolTraffic()) {
            log.warn("too many l1 txns is pending, so skip this tee proof committing process");
            return;
        }
        var latestVerifiedBatchIdxOnChain = l1Client.lastTeeVerifiedBatch();
        if (ObjectUtil.isNull(latestVerifiedBatchIdxOnChain)) {
            throw new CommitL2BatchTeeProofException("null result from calling lastTeeVerifiedBatch of rollup contract");
        }

        var lastCommittedBatchIdx = queryLastCommittedBatchIndex();
        if (ObjectUtil.isNull(lastCommittedBatchIdx)) {
            throw new CommitL2BatchTeeProofException("null result from calling lastCommittedBatch of rollup contract");
        }
        if (lastCommittedBatchIdx.compareTo(latestVerifiedBatchIdxOnChain) <= 0) {
            log.debug("tee proof commit task waiting for batch#{} to be committed", latestVerifiedBatchIdxOnChain.add(BigInteger.ONE));
            return;
        }

        // pick the bigger one
        var windowEnd = latestVerifiedBatchIdxOnChain.add(BigInteger.valueOf(batchCommitWindowsLength));
        windowEnd = windowEnd.compareTo(lastCommittedBatchIdx) <= 0 ? windowEnd : lastCommittedBatchIdx;
        log.info("🤓 process tee proof committing window from {}+1 to {}", latestVerifiedBatchIdxOnChain, windowEnd);
        for (
                var nextBatchIdxToCommitTeeProof = latestVerifiedBatchIdxOnChain.add(BigInteger.ONE);
                nextBatchIdxToCommitTeeProof.compareTo(windowEnd) <= 0;
                nextBatchIdxToCommitTeeProof = nextBatchIdxToCommitTeeProof.add(BigInteger.ONE)
        ) {
            var finalNextBatchIdxToCommitTeeProof = nextBatchIdxToCommitTeeProof;
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                commitNextTeeProof(finalNextBatchIdxToCommitTeeProof);
                            }
                        }
                );
            } catch (ProofNotReadyException e) {
                // proof not ready, so no need to continue 😴
                log.info("tee proof for batch {} not ready, so break the loop", finalNextBatchIdxToCommitTeeProof);
                break;
            } catch (ProofCommitFailedException e) {
                // tx committed failed , so no need to continue 😈
                log.warn("tee proof for batch {} commit failed, so break the loop, plz check the transaction fail reason", finalNextBatchIdxToCommitTeeProof);
                break;
            } catch (RollupEconomicStrategyNotAllowedException e) {
                // economic strategy not satisfied, so no need to continue 😴
                log.warn("rollup economic strategy not satisfied, skip this task: {}", e.getMessage());
                break;
            }
        }
    }

    @Override
    public void commitL2ZkProof() {
        if (!RollupUtils.isProveReqTypeToProcess(batchProveReqTypes, ProveTypeEnum.ZK_PROOF)) {
            log.debug("skip zk proof commit now!");
            return;
        }
        if (!rollupThrottle.checkL1LegacyPoolTraffic()) {
            log.warn("too many l1 txns is pending, so skip this zk proof committing process");
            return;
        }
        var latestVerifiedBatchIdxOnChain = l1Client.lastZkVerifiedBatch();
        if (ObjectUtil.isNull(latestVerifiedBatchIdxOnChain)) {
            throw new CommitL2BatchZkProofException("null result from calling lastZkVerifiedBatch of rollup contract");
        }

        var lastCommittedBatchIdx = queryLastCommittedBatchIndex();
        if (ObjectUtil.isNull(lastCommittedBatchIdx)) {
            throw new CommitL2BatchZkProofException("null result from calling lastCommittedBatch of rollup contract");
        }
        if (lastCommittedBatchIdx.compareTo(latestVerifiedBatchIdxOnChain) <= 0) {
            log.debug("zk proof commit task waiting for batch#{} to be committed", latestVerifiedBatchIdxOnChain.add(BigInteger.ONE));
            return;
        }

        // pick the bigger one
        var windowEnd = latestVerifiedBatchIdxOnChain.add(BigInteger.valueOf(batchCommitWindowsLength));
        windowEnd = windowEnd.compareTo(lastCommittedBatchIdx) <= 0 ? windowEnd : lastCommittedBatchIdx;
        log.info("🤓 process zk proof committing window from {}+1 to {}", latestVerifiedBatchIdxOnChain, windowEnd);
        for (
                var nextBatchIdxToCommitZkProof = latestVerifiedBatchIdxOnChain.add(BigInteger.ONE);
                nextBatchIdxToCommitZkProof.compareTo(windowEnd) <= 0;
                nextBatchIdxToCommitZkProof = nextBatchIdxToCommitZkProof.add(BigInteger.ONE)
        ) {
            var finalNextBatchIdxToCommitZkProof = nextBatchIdxToCommitZkProof;
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                commitNextZkProof(finalNextBatchIdxToCommitZkProof);
                            }
                        }
                );
            } catch (ProofNotReadyException e) {
                // proof not ready, so no need to continue 😴
                log.info("zk proof for batch {} not ready, so break the loop", finalNextBatchIdxToCommitZkProof);
                break;
            } catch (ProofCommitFailedException e) {
                // tx committed failed , so no need to continue 😈
                log.warn("zk proof for batch {} commit failed, so break the loop, plz check the transaction fail reason", finalNextBatchIdxToCommitZkProof);
                break;
            }
        }
    }

    private void commitNextL2Batch(BigInteger nextBatchIndex, BigInteger lastProcessCommitted) {
        BatchWrapper nextBatch;
        ReliableTransactionDO txCommitted = null;
        if (lastProcessCommitted.compareTo(nextBatchIndex) < 0) {
            // commit the batch
            nextBatch = rollupRepository.getBatch(nextBatchIndex);
            if (ObjectUtil.isNull(nextBatch)) {
                throw new BatchNotReadyException(nextBatchIndex);
            }
        } else {
            // already been committed the batch
            txCommitted = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, nextBatchIndex, TransactionTypeEnum.BATCH_COMMIT_TX);
            if (ObjectUtil.isNull(txCommitted)) {
                log.error("🚨 has no tx info for batch {} but record its index as committed !!!", nextBatchIndex);
                nextBatch = rollupRepository.getBatch(nextBatchIndex);
                if (ObjectUtil.isNull(nextBatch)) {
                    throw new CommitL2BatchException("null batch for {}", nextBatchIndex);
                }
            } else {
                if (ReliableTransactionStateEnum.considerAsSuccess(txCommitted.getState())) {
                    log.error("🚨 tx {} shows batch {} commit success but not on contract, recommit it! ", txCommitted.getLatestTxHash(), txCommitted.getBatchIndex());
                    nextBatch = rollupRepository.getBatch(nextBatchIndex);
                    if (ObjectUtil.isNull(nextBatch)) {
                        throw new CommitL2BatchException("null batch for {}", nextBatchIndex);
                    }
                } else if (ReliableTransactionStateEnum.considerAsFailed(txCommitted.getState())) {
                    throw new BatchCommitFailedException(txCommitted.getBatchIndex());
                } else {
                    log.debug("batch index {} already commit and wait for tx confirmation", nextBatchIndex);
                    return;
                }
            }
        }

        BatchHeader parentHeader = getParentBatchHeader(nextBatch.getBatch().getBatchIndex().subtract(BigInteger.ONE));
        if (ObjectUtil.isNull(parentHeader)) {
            throw new CommitL2BatchException("null parent batch header for {}", nextBatch.getBatch().getBatchIndex());
        }

        log.info("try to commit next batch {}-{}", nextBatch.getBatch().getBatchIndex(), nextBatch.getBatch().getBatchHashHex());

        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED, nextBatch.getBatch().getBatchIndex());

        TransactionInfo transactionInfo;
        try {
            transactionInfo = l1Client.commitBatch(nextBatch, parentHeader);
        } catch (L1ContractWarnException e) {
            log.info("rollup contract shows that batch {} has been committed", nextBatch.getBatchHeader().getBatchIndex());
            return;
        }
        var reliableTx = ReliableTransactionDO.builder()
                .rawTx(transactionInfo.getRawTx())
                .latestTxHash(transactionInfo.getTxHash())
                .originalTxHash(transactionInfo.getTxHash())
                .nonce(transactionInfo.getNonce().longValue())
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .chainType(ChainTypeEnum.LAYER_ONE)
                .senderAccount(transactionInfo.getSenderAccount())
                .latestTxSendTime(transactionInfo.getSendTxTime())
                .batchIndex(nextBatch.getBatch().getBatchIndex())
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .build();
        saveTx(txCommitted, reliableTx);

        otelMetric.recordBatchCommitEvent(nextBatch.getBatch().getBatchIndex());
        log.info("commit batch {} with tx {}", nextBatch.getBatch().getBatchIndex(), transactionInfo.getTxHash());
    }

    private void commitNextTeeProof(BigInteger nextBatchIdxToCommitTeeProof) {
        log.debug("local tee proof is for batch#{}", nextBatchIdxToCommitTeeProof);

        var request = rollupRepository.getBatchProveRequest(nextBatchIdxToCommitTeeProof, ProveTypeEnum.TEE_PROOF);
        if (ObjectUtil.isNull(request) || ObjectUtil.isEmpty(request.getProof()) || request.getState() == BatchProveRequestStateEnum.PENDING) {
            log.debug("tee proof for next batch {} not ready, please wait...", nextBatchIdxToCommitTeeProof);
            throw new ProofNotReadyException(ProveTypeEnum.TEE_PROOF, nextBatchIdxToCommitTeeProof);
        }
        ReliableTransactionDO txCommitted = null;
        if (request.getState() == BatchProveRequestStateEnum.COMMITTED) {
            txCommitted = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, nextBatchIdxToCommitTeeProof, TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX);
            if (ObjectUtil.isNull(txCommitted)) {
                log.error("🚨 has no tx info for tee proof of batch {} but record its index as committed, recommit it !!!", nextBatchIdxToCommitTeeProof);
            } else {
                if (ReliableTransactionStateEnum.considerAsSuccess(txCommitted.getState())) {
                    log.error("🚨 tx {} shows tee proof of batch {} commit success but not on contract, recommit it! ",
                            txCommitted.getLatestTxHash(), txCommitted.getBatchIndex());
                } else if (ReliableTransactionStateEnum.considerAsFailed(txCommitted.getState())) {
                    throw new ProofCommitFailedException(txCommitted.getBatchIndex());
                } else {
                    log.debug("tee proof for batch {} has been committed, please wait tx confirmation...", nextBatchIdxToCommitTeeProof);
                    return;
                }
            }
        }

        log.info("try to commit next batch tee proof {}", nextBatchIdxToCommitTeeProof);

        var batchWrapper = rollupRepository.getBatch(nextBatchIdxToCommitTeeProof);
        if (ObjectUtil.isNull(batchWrapper)) {
            throw new CommitL2BatchTeeProofException("batch for proof {} not found", nextBatchIdxToCommitTeeProof);
        }

        rollupRepository.updateBatchProveRequestState(nextBatchIdxToCommitTeeProof, ProveTypeEnum.TEE_PROOF, BatchProveRequestStateEnum.COMMITTED);
        TransactionInfo transactionInfo;
        try {
            transactionInfo = l1Client.verifyBatch(batchWrapper, request);
        } catch (L1ContractWarnException e) {
            log.info("rollup contract shows that tee batch proof {} has been committed", nextBatchIdxToCommitTeeProof);
            return;
        }

        var reliableTx = ReliableTransactionDO.builder()
                .rawTx(transactionInfo.getRawTx())
                .latestTxHash(transactionInfo.getTxHash())
                .originalTxHash(transactionInfo.getTxHash())
                .nonce(transactionInfo.getNonce().longValue())
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .chainType(ChainTypeEnum.LAYER_ONE)
                .senderAccount(transactionInfo.getSenderAccount())
                .latestTxSendTime(transactionInfo.getSendTxTime())
                .batchIndex(nextBatchIdxToCommitTeeProof)
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .build();
        saveTx(txCommitted, reliableTx);

        otelMetric.recordBatchTeeVerifyEvent(nextBatchIdxToCommitTeeProof);
        log.info("commit tee proof for batch {} with tx {}", nextBatchIdxToCommitTeeProof, transactionInfo.getTxHash());
    }

    private void commitNextZkProof(BigInteger nextBatchIdxToCommitZkProof) {
        log.debug("local zk proof is for batch#{}", nextBatchIdxToCommitZkProof);

        var request = rollupRepository.getBatchProveRequest(nextBatchIdxToCommitZkProof, ProveTypeEnum.ZK_PROOF);
        if (ObjectUtil.isNull(request) || ObjectUtil.isEmpty(request.getProof()) || request.getState() == BatchProveRequestStateEnum.PENDING) {
            log.debug("zk proof for next batch {} not ready, please wait...", nextBatchIdxToCommitZkProof);
            throw new ProofNotReadyException(ProveTypeEnum.ZK_PROOF, nextBatchIdxToCommitZkProof);
        }
        ReliableTransactionDO txCommitted = null;
        if (request.getState() == BatchProveRequestStateEnum.COMMITTED) {
            txCommitted = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, nextBatchIdxToCommitZkProof, TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX);
            if (ObjectUtil.isNull(txCommitted)) {
                log.error("🚨 has no tx info for zk proof of batch {} but record its index as committed, recommit it !!!", nextBatchIdxToCommitZkProof);
            } else {
                if (ReliableTransactionStateEnum.considerAsSuccess(txCommitted.getState())) {
                    log.error("🚨 tx {} shows zk proof of batch {} commit success but not on contract, recommit it! ",
                            txCommitted.getLatestTxHash(), txCommitted.getBatchIndex());
                } else if (ReliableTransactionStateEnum.considerAsFailed(txCommitted.getState())) {
                    throw new ProofCommitFailedException(txCommitted.getBatchIndex());
                } else {
                    log.debug("zk proof for batch {} has been committed, please wait tx confirmation...", nextBatchIdxToCommitZkProof);
                    return;
                }
            }
        }

        log.info("try to commit next batch zk proof {}", nextBatchIdxToCommitZkProof);

        var batchWrapper = rollupRepository.getBatch(nextBatchIdxToCommitZkProof);
        if (ObjectUtil.isNull(batchWrapper)) {
            throw new CommitL2BatchZkProofException("batch for proof {} not found", nextBatchIdxToCommitZkProof);
        }

        rollupRepository.updateBatchProveRequestState(nextBatchIdxToCommitZkProof, ProveTypeEnum.ZK_PROOF, BatchProveRequestStateEnum.COMMITTED);
        TransactionInfo transactionInfo;
        try {
            transactionInfo = l1Client.verifyBatch(batchWrapper, request);
        } catch (L1ContractWarnException e) {
            log.info("rollup contract shows that zk batch proof {} has been committed", nextBatchIdxToCommitZkProof);
            return;
        }

        var reliableTx = ReliableTransactionDO.builder()
                .rawTx(transactionInfo.getRawTx())
                .latestTxHash(transactionInfo.getTxHash())
                .originalTxHash(transactionInfo.getTxHash())
                .nonce(transactionInfo.getNonce().longValue())
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .chainType(ChainTypeEnum.LAYER_ONE)
                .senderAccount(transactionInfo.getSenderAccount())
                .latestTxSendTime(transactionInfo.getSendTxTime())
                .batchIndex(nextBatchIdxToCommitZkProof)
                .transactionType(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX)
                .build();
        saveTx(txCommitted, reliableTx);

        log.info("commit zk proof for batch {} with tx {}", nextBatchIdxToCommitZkProof, transactionInfo.getTxHash());
    }

    private void saveTx(ReliableTransactionDO txCommitted, ReliableTransactionDO reliableTx) {
        if (ObjectUtil.isNull(txCommitted)) {
            try {
                rollupRepository.insertReliableTransaction(reliableTx);
            } catch (DuplicateKeyException e) {
                log.warn("Insert failed: tx for batch#{} already sent, so just update it", reliableTx.getBatchIndex());
                rollupRepository.updateReliableTransaction(reliableTx);
            }
        } else {
            rollupRepository.updateReliableTransaction(reliableTx);
        }
    }

    private void proveL2Batch(ProveTypeEnum proveType) {
        if (!RollupUtils.isProveReqTypeToProcess(batchProveReqTypes, proveType)) {
            log.debug("skip {} prove request now!", proveType);
            return;
        }
        log.debug("process {} prove request now!", proveType);
        List<BatchProveRequestDO> requests = rollupRepository.peekPendingBatchProveRequest(proveReqNumberPerBatchLimit, proveType);
        if (ObjectUtil.isEmpty(requests)) {
            log.debug("empty prove requests this time");
            return;
        }
        requests.sort(Comparator.comparing(BatchProveRequestDO::getBatchIndex));

        for (BatchProveRequestDO request : requests) {
            if (proveType == ProveTypeEnum.ZK_PROOF && request.getBatchIndex().compareTo(rollupConfig.getZkVerificationStartBatch()) < 0) {
                // abandon all the zk proof requests before zkVerificationStartBatch
                rollupRepository.updateBatchProveRequestState(request.getBatchIndex(), request.getProveType(), BatchProveRequestStateEnum.ABANDONED);
                continue;
            }

            log.info("try to get batch proof for index {} and type {}", request.getBatchIndex(), request.getProveType());
            byte[] proof;
            try {
                proof = proverControllerClient.getBatchProof(request.getProveType(), request.getBatchIndex());
            } catch (ProofNotReadyException e) {
                log.info("proof is not ready, so we skip it...: {}", e.getMessage());
                continue;
            }
            log.debug("raw proof: {}", HexUtil.encodeHexStr(proof));
            if (ObjectUtil.isEmpty(proof)) {
                log.error("get empty proof for batch {} and type {} !", request.getBatchIndex(), request.getProveType());
                continue;
            }

            rollupRepository.saveBatchProofAndUpdateReqState(request.getBatchIndex(), request.getProveType(), proof);
            log.info("⚓️ successful to get proof of batch {} and type {} !", request.getBatchIndex(), request.getProveType());
        }
    }

    private BigInteger queryLastCommittedBatchIndex() {
        if (proofCommitRollupQueryHeightBackoff == 0) {
            return l1Client.lastCommittedBatch(proofCommitRollupQueryLevel);
        }
        return l1Client.lastCommittedBatch(DefaultBlockParameter.valueOf(
                l1Client.queryLatestBlockNumber(proofCommitRollupQueryLevel)
                        .subtract(BigInteger.valueOf(proofCommitRollupQueryHeightBackoff))
        ));
    }

    private BigInteger getProcessedBlockHeight() {
        BigInteger height = rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED);
        if (ObjectUtil.isNull(height)) {
            throw new RuntimeException("no processed block height found");
        }

        return height;
    }

    private BatchHeader getParentBatchHeader(@NonNull BigInteger parentBatchIndex) {
        return rollupRepository.getBatchHeader(parentBatchIndex);
    }
}
