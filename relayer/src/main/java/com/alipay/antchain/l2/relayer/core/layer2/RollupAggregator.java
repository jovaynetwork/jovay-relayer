package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockPollingException;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidChunkException;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RollupAggregator implements IRollupAggregator {

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private L1Client l1Client;

    @Resource
    private RollupConfig rollupConfig;

    @Resource
    private ProverControllerClient proverControllerClient;

    @Resource
    private IOtelMetric otelMetric;

    @Override
    public BigInteger process(BasicBlockTrace blockTrace, BigInteger currHeight) {
        long nextChunkIndex = getNextChunkIndex();
        BigInteger nextBatchIndex = getNextBatchIndex();
        long currTxCntOfNextChunk = getChunkTxCount();
        long currCallDataSizeCountOfNextChunk = getChunkCallDataCount();
        long callDataSize = blockTrace.getTransactionsList().stream().map(Utils::getTxDataSize).reduce(Integer::sum).orElse(0);
        BigInteger currChunkStartBlockNumber = getCurrChunkStartBlockNumber(nextChunkIndex, nextBatchIndex);
        // if we add this block to the building chunk, let's see if that over batch tx data size limit.
        var buildingChunk = buildL2NextChunk(nextBatchIndex, nextChunkIndex, currChunkStartBlockNumber, currHeight);
        var growingBatchByteSize = getGrowingBatchByteSize(buildingChunk);
        log.debug("add this block and then commit batch, the blob byte size is {}", growingBatchByteSize);

        var overBatchBlobLimitFlag = growingBatchByteSize - rollupConfig.getBatchCommitBlobSizeLimit() * EthBlobs.CAPACITY_BYTE_PER_BLOB;

        long zkAccumulator = getChunkZkCycleAccumulator();
        // if next chunk ready
        // below here, last block of this chunk is `currHeight - 1`
        // By order : overZkLimitOneChunk, overMaxTxInChunk and overMaxCallDataSizeInChunk
        boolean overChunkLimit = isOverChunkLimit(
                zkAccumulator,
                callDataSize,
                blockTrace.getZkCycles(),
                blockTrace.getTransactionsCount(),
                currTxCntOfNextChunk,
                currCallDataSizeCountOfNextChunk
        ) || overBatchBlobLimitFlag > 0;

        // below here, last block of this chunk is `currHeight`
        // By order : zkEquals, txCountEquals, callDataSizeEquals and blockNumSatisfied
        boolean equalChunkLimit = zkAccumulator + blockTrace.getZkCycles() == rollupConfig.getChunkZkCycleSumLimit()
                                  || currTxCntOfNextChunk + blockTrace.getTransactionsCount() == rollupConfig.getMaxTxsInChunks()
                                  || currCallDataSizeCountOfNextChunk + callDataSize == rollupConfig.getMaxCallDataInChunk()
                                  || currHeight.subtract(currChunkStartBlockNumber).compareTo(BigInteger.valueOf(rollupConfig.getOneChunkBlocksLimit() - 1)) == 0
                                  || overBatchBlobLimitFlag == 0;

        var batchThisBlockBelongsTo = overBatchBlobLimitFlag > 0 ? nextBatchIndex.add(BigInteger.ONE) : nextBatchIndex;
        proverControllerClient.notifyBlock(
                // if over, this block belongs to next batch
                batchThisBlockBelongsTo,
                // if over tx data limit, this block belongs to the zero chunk of next batch
                // if not meet the upper condition, and then if over, this block belongs to next chunk
                overBatchBlobLimitFlag > 0 ? 0 : (overChunkLimit ? nextChunkIndex + 1 : nextChunkIndex),
                currHeight
        );

        if (overChunkLimit || equalChunkLimit) {
            // reuse the building chunk as current chunk
            ChunkWrapper currChunk;
            BigInteger lastBlockHeight;
            if (!overChunkLimit) {
                currChunk = buildingChunk;
                lastBlockHeight = currHeight;
            } else if (ObjectUtil.equal(currChunkStartBlockNumber, currHeight) && overBatchBlobLimitFlag > 0) {
                if (nextChunkIndex == 0) {
                    throw new InvalidBatchException("The first chunk of batch#{} only contains one block#{}, and already over the blobs capacity",
                            nextBatchIndex, currChunkStartBlockNumber);
                }
                currChunk = null;
                lastBlockHeight = currChunkStartBlockNumber.subtract(BigInteger.ONE);

                log.info("chunk {}-{} only has one block inside and cause that batch over the blobs capacity, so abandon it!", nextBatchIndex, nextChunkIndex);
            } else {
                lastBlockHeight = getLastBlockHeightOfNextChunk(true, currChunkStartBlockNumber, currHeight);
                if (ObjectUtil.equal(currChunkStartBlockNumber, lastBlockHeight)) {
                    if (ObjectUtil.equal(currHeight, lastBlockHeight)
                        || isOverChunkLimit(zkAccumulator, 0, 0, 0, currTxCntOfNextChunk, currCallDataSizeCountOfNextChunk)) {
                        throw new BlockPollingException("one block chunk over limit with L2 block height {}", currChunkStartBlockNumber);
                    }
                }
                currChunk = buildL2NextChunk(nextBatchIndex, nextChunkIndex, currChunkStartBlockNumber, lastBlockHeight);
            }

            if (ObjectUtil.isNotNull(currChunk)) {
                rollupRepository.saveChunk(validateChunk(currChunk));
                log.info("👏 chunk {} is ready on block height {}", nextChunkIndex, lastBlockHeight);
                log.info("current chunk parameters: (zk_sum: {} + {}, curr_tx_cnt: {} + {}, curr_call_data_size_cnt: {} + {}, chunk_start_height: {}, growing_batch_bit_size: {})",
                        zkAccumulator, blockTrace.getZkCycles(),
                        currTxCntOfNextChunk, blockTrace.getTransactionsCount(),
                        currCallDataSizeCountOfNextChunk, callDataSize,
                        currChunkStartBlockNumber,
                        growingBatchByteSize
                );

                proverControllerClient.notifyChunk(nextBatchIndex, nextChunkIndex);
                log.debug("next chunk in batch {} is {}", nextBatchIndex, nextChunkIndex);
            }

            // if next batch ready
            // Situations that over or equal to blobs capacity all lead to new batch building
            var lastBlockTrace = currHeight.compareTo(lastBlockHeight) == 0 ? blockTrace : rollupRepository.getL2BlockTrace(lastBlockHeight);
            if (overBatchBlobLimitFlag >= 0
                || rollupConfig.getMaxTimeIntervalBetweenBatches() <= lastBlockTrace.getHeader().getTimestamp() - getStartBlockTimestampForBatch(nextBatchIndex)) {
                var nextBatch = validateBatch(BatchWrapper.createBatchV0(
                        nextBatchIndex,
                        rollupRepository.getBatch(nextBatchIndex.subtract(BigInteger.ONE), false),
                        lastBlockTrace.getHeader().getStateRoot().toByteArray(),
                        lastBlockTrace.getL1MsgRollingHash().getValue().toByteArray(),
                        lastBlockTrace.getL2MsgRoot().getValue().toByteArray(),
                        rollupRepository.getChunks(nextBatchIndex)
                ));

                log.info("🧱 build next batch (index: {}, hash: {}) with {} chunks from block height {} to {} included",
                        nextBatch.getBatch().getBatchIndex(), nextBatch.getBatch().getBatchHashHex(), nextBatch.getBatch().getChunks().size(),
                        nextBatch.getBatch().getStartBlockNumber(), nextBatch.getBatch().getEndBlockNumber());

                proverControllerClient.proveBatch(nextBatch);
                rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.TEE_PROOF);
                rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.ZK_PROOF);

                rollupRepository.saveBatch(nextBatch);
                rollupRepository.updateRollupNumberRecord(
                        ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH,
                        nextBatchIndex.add(BigInteger.ONE)
                );
                // reset the chunk index
                nextChunkIndex = 0;
                otelMetric.recordBatchConstructedEvent(nextBatch);

                log.info("🏅 successful to process batch {} with {} chunks", nextBatch.getBatch().getBatchIndex(), nextBatch.getBatch().getChunks().size());
            } else {
                log.info("🏆 successful to process chunk {}@{}", nextBatchIndex, nextChunkIndex);
                // batch not ready, so accumulate chunk index
                nextChunkIndex++;
            }

            updateNewChunkNumbers(lastBlockHeight, blockTrace, nextChunkIndex, callDataSize, currHeight);
        } else {
            updateChunkNumbers(zkAccumulator, blockTrace, currTxCntOfNextChunk, currCallDataSizeCountOfNextChunk, callDataSize);
        }

        return batchThisBlockBelongsTo;
    }

    private boolean isOverChunkLimit(long zkAccumulator, long callDataSize, long currBlockZkCycles, int currBlockTxCnt,
                                     long currTxCntOfNextChunk, long currCallDataSizeCountOfNextChunk) {
        return zkAccumulator + currBlockZkCycles > rollupConfig.getChunkZkCycleSumLimit()
               || currTxCntOfNextChunk + currBlockTxCnt > rollupConfig.getMaxTxsInChunks()
               || currCallDataSizeCountOfNextChunk + callDataSize > rollupConfig.getMaxCallDataInChunk();
    }

    protected int getGrowingBatchByteSize(ChunkWrapper currChunk) {
        var chunkList = new ArrayList<Chunk>();
        for (int i = 0; i < currChunk.getChunkIndex(); i++) {
            var chunkWrapper = rollupRepository.getChunk(currChunk.getBatchIndex(), i);
            if (ObjectUtil.isNull(chunkWrapper)) {
                throw new BlockPollingException("get null prev chunk for batch {} and chunk {}", currChunk.getBatchIndex(), currChunk.getChunkIndex());
            }
            chunkList.add(chunkWrapper.getChunk());
        }
        chunkList.add(currChunk.getChunk());
        return RollupUtils.serializeChunks(chunkList).length;
    }

    private BatchWrapper validateBatch(BatchWrapper batchWrapper) {
        batchWrapper.getBatch().validate();
        var blobSize = batchWrapper.getBatch().getEthBlobs().blobs().size();
        if (blobSize > rollupConfig.getBatchCommitBlobSizeLimit()) {
            throw new InvalidBatchException("L1 tx size over limit if commit this batch {}: blob size {}", batchWrapper.getBatchIndex(), blobSize);
        }
        return batchWrapper;
    }

    private ChunkWrapper validateChunk(ChunkWrapper chunkWrapper) {
        try {
            chunkWrapper.getChunk().validate();
        } catch (InvalidChunkException e) {
            throw new InvalidChunkException("Invalid No.{} chunk for batch {}: {}", chunkWrapper.getChunkIndex(), chunkWrapper.getBatchIndex(), e.getMessage());
        }
        return chunkWrapper;
    }

    private long getChunkZkCycleAccumulator() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR).longValue();
    }

    private BigInteger getCurrChunkStartBlockNumber(long nextChunkIndex, BigInteger nextBatchIndex) {
        if (nextChunkIndex == 0) {
            // get it from last batch
            BigInteger lastBatchIndex = nextBatchIndex.subtract(BigInteger.ONE);
            BatchWrapper batch = rollupRepository.getBatch(lastBatchIndex, false);
            if (ObjectUtil.isNull(batch)) {
                throw new RuntimeException("none batch from local for " + lastBatchIndex);
            }
            return batch.getEndBlockNumber().add(BigInteger.ONE);
        }

        return rollupRepository.getChunk(nextBatchIndex, nextChunkIndex - 1).getChunk().getEndBlockNumber().add(BigInteger.ONE);
    }

    private ChunkWrapper buildL2NextChunk(BigInteger nextBatchIndex, Long nextChunkIndex, BigInteger startBlockNumber, BigInteger endBlockNumber) {
        List<BasicBlockTrace> blockTraces = rollupRepository.getL2BlockTraceRange(startBlockNumber, endBlockNumber);
        if (ObjectUtil.isEmpty(blockTraces)) {
            throw new BlockPollingException("no block trace found for chunk {} in batch {}", nextChunkIndex, nextBatchIndex);
        }
        return new ChunkWrapper(nextBatchIndex, nextChunkIndex, blockTraces, rollupConfig.getMaxTxsInChunks());
    }

    private BigInteger getStartBlockHeightForBatch(BigInteger batchIndex) {
        var batch = rollupRepository.getBatch(batchIndex.subtract(BigInteger.ONE), false);
        if (ObjectUtil.isNull(batch)) {
            throw new RuntimeException("none batch from local for " + batchIndex);
        }
        return batch.getEndBlockNumber().add(BigInteger.ONE);
    }

    private long getStartBlockTimestampForBatch(BigInteger batchIndex) {
        var trace = rollupRepository.getL2BlockTrace(getStartBlockHeightForBatch(batchIndex));
        if (ObjectUtil.isNull(trace)) {
            throw new RuntimeException("none first block trace found for batch " + batchIndex);
        }
        return trace.getHeader().getTimestamp();
    }

    private BigInteger getLastBlockHeightOfNextChunk(
            boolean overChunkLimit,
            final BigInteger currChunkStartBlockNumber,
            final BigInteger currHeight
    ) {
        if (currChunkStartBlockNumber.compareTo(currHeight) == 0) {
            return currHeight;
        }
        if (overChunkLimit) {
            return currHeight.subtract(BigInteger.ONE);
        }
        return currHeight;
    }

    private void updateNewChunkNumbers(BigInteger lastBlockHeight, BasicBlockTrace blockTrace, long nextChunkIndex, long callDataSize, BigInteger currHeight) {
        boolean isCurrBlockBelongsToNextChunk = lastBlockHeight.compareTo(currHeight) < 0;
        // reset zk accumulator to curr block's zk-cycles or zero
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR,
                isCurrBlockBelongsToNextChunk ? BigInteger.valueOf(blockTrace.getZkCycles()) : BigInteger.ZERO
        );
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK,
                BigInteger.valueOf(nextChunkIndex)
        );
        // reset tx count to curr block's tx number or zero
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT,
                isCurrBlockBelongsToNextChunk ? BigInteger.valueOf(blockTrace.getTransactionsCount()) : BigInteger.ZERO
        );
        // reset tx call data size counter to curr block's tx call data size or zero
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT,
                isCurrBlockBelongsToNextChunk ? BigInteger.valueOf(callDataSize) : BigInteger.ZERO
        );
    }

    private void updateChunkNumbers(long zkAccumulator, BasicBlockTrace blockTrace, long currTxCntOfNextChunk, long currCallDataSizeCountOfNextChunk, long callDataSize) {
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR,
                BigInteger.valueOf(zkAccumulator + blockTrace.getZkCycles())
        );
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT,
                BigInteger.valueOf(currTxCntOfNextChunk + blockTrace.getTransactionsCount())
        );
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT,
                BigInteger.valueOf(currCallDataSizeCountOfNextChunk + callDataSize)
        );
    }

    private Long getNextChunkIndex() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK).longValue();
    }

    private BigInteger getNextBatchIndex() {
        BigInteger batchIndex = rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH);
        if (ObjectUtil.isNull(batchIndex) || batchIndex.equals(BigInteger.ZERO)) {
            BigInteger nextBatchIndex = l1Client.lastCommittedBatch().add(BigInteger.ONE);
            // init it with the nextBatchIndex
            rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH, nextBatchIndex);
            return nextBatchIndex;
        }
        return batchIndex;
    }

    private long getChunkTxCount() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT).longValue();
    }

    private long getChunkCallDataCount() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT).longValue();
    }
}
