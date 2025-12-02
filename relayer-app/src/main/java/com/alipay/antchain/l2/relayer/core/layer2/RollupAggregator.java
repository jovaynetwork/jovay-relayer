package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockPollingException;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidChunkException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlobsDaData;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.specs.IRollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import jakarta.annotation.Resource;
import lombok.Getter;
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
    private IRollupSpecs rollupSpecs;

    @Resource
    private IOtelMetric otelMetric;

    /**
     * Current batch chunks memory cache.
     * Going to upgrade the growing batch calculation logic to optimize these stuff.
     */
    @Resource
    private GrowingBatchChunksMemCache growingBatchChunks;

    @Override
    public BigInteger process(BasicBlockTrace blockTrace) {
        var currHeight = BigInteger.valueOf(blockTrace.getHeader().getNumber());
        // Current building chunk index.
        long nextChunkIndex = getNextChunkIndex();
        // Current building batch index.
        BigInteger nextBatchIndex = getNextBatchIndex();
        // Tx number already counted from this building chunk.
        long currTxCntOfNextChunk = getChunkTxCount();
        // Jovay Transaction Call data size already counted from this building chunk.
        long currCallDataSizeCountOfNextChunk = getChunkCallDataCount();
        // Calc the sum of transaction calldata size from curr block
        long callDataSize = blockTrace.getTransactionsList().stream().map(Utils::getTxDataSize).reduce(Integer::sum).orElse(0);
        BigInteger currChunkStartBlockNumber = getCurrChunkStartBlockNumber(nextChunkIndex, nextBatchIndex);
        // if we add this block to the building chunk, let's see if that over batch tx data size limit.
        var buildingChunk = buildL2NextChunk(nextBatchIndex, nextChunkIndex, currChunkStartBlockNumber, currHeight);
        // Growing batch info contains the uncompressed batch data.
        // And capable to calc the actual eip4844 blobs size if rollup the building batch now.
        var growingBatchInfo = getGrowingBatchInfo(buildingChunk);
        var overBatchBlobLimitFlag = growingBatchInfo.getOverBatchBlobLimitFlag(rollupConfig.getBatchCommitBlobSizeLimit());
        log.debug("add this block and then commit batch, the growing batch info is {}", growingBatchInfo.toJson());

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
        boolean equalChunkLimit = (zkAccumulator + blockTrace.getZkCycles() == rollupConfig.getChunkZkCycleSumLimit()
                                   || currTxCntOfNextChunk + blockTrace.getTransactionsCount() == rollupConfig.getMaxTxsInChunks()
                                   || currCallDataSizeCountOfNextChunk + callDataSize == rollupConfig.getMaxCallDataInChunk()
                                   || currHeight.subtract(currChunkStartBlockNumber).compareTo(BigInteger.valueOf(rollupConfig.getOneChunkBlocksLimit() - 1)) == 0
                                   || overBatchBlobLimitFlag == 0) && !overChunkLimit;

        // if over blob data limit, this block belongs to next batch
        // if equals blob data limit and still over chunk limit, this block belongs to next batch
        var batchThisBlockBelongsTo = overBatchBlobLimitFlag > 0 || (overBatchBlobLimitFlag == 0 && overChunkLimit) ?
                nextBatchIndex.add(BigInteger.ONE) : nextBatchIndex;
        // if over blob data limit, this block belongs to the zero chunk of next batch
        // if equals blob data limit and still over chunk limit, this block belongs to the zero chunk of next batch
        // if not meet the upper condition, and then if over, this block belongs to next chunk
        var chunkThisBlockBelongsTo = overBatchBlobLimitFlag > 0 || (overBatchBlobLimitFlag == 0 && overChunkLimit) ?
                0 : (overChunkLimit ? nextChunkIndex + 1 : nextChunkIndex);
        proverControllerClient.notifyBlock(
                // if over, this block belongs to next batch
                batchThisBlockBelongsTo,
                chunkThisBlockBelongsTo,
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
                log.info("current chunk parameters: (zk_sum: {} + {}, curr_tx_cnt: {} + {}, curr_call_data_size_cnt: {} + {}, chunk_start_height: {}, growing_batch_info: {})",
                        zkAccumulator, blockTrace.getZkCycles(),
                        currTxCntOfNextChunk, blockTrace.getTransactionsCount(),
                        currCallDataSizeCountOfNextChunk, callDataSize,
                        currChunkStartBlockNumber,
                        growingBatchInfo.toJson()
                );

                proverControllerClient.notifyChunk(nextBatchIndex, nextChunkIndex, currChunk.getStartBlockNumber(), currChunk.getEndBlockNumber());
                log.debug("next chunk in batch {} is {}", nextBatchIndex, nextChunkIndex);

                growingBatchChunks.add(currChunk);
                log.info("add the next chunk into memory cache: (batch {}, chunk {})", currChunk.getBatchIndex(), currChunk.getChunkIndex());
            }

            // if next batch ready
            // Situations that over or equal to blobs capacity all lead to new batch building
            var lastBlockTrace = currHeight.compareTo(lastBlockHeight) == 0 ? blockTrace : rollupRepository.getL2BlockTrace(lastBlockHeight);
            if (overBatchBlobLimitFlag >= 0
                || rollupConfig.getMaxTimeIntervalBetweenBatches() <= lastBlockTrace.getHeader().getTimestamp() - getStartBlockTimestampForBatch(nextBatchIndex)) {
                var parentBatch = rollupRepository.getBatch(nextBatchIndex.subtract(BigInteger.ONE), false);
                var nextBatch = validateBatch(BatchWrapper.createBatch(
                        growingBatchInfo.getBatchVersion(),
                        nextBatchIndex,
                        parentBatch,
                        lastBlockTrace.getHeader().getStateRoot().toByteArray(),
                        lastBlockTrace.getL1MsgRollingHash().getValue().toByteArray(),
                        lastBlockTrace.getL2MsgRoot().getValue().toByteArray(),
                        lastBlockTrace.getFinalizeL1MsgIndex(),
                        rollupRepository.getChunks(nextBatchIndex)
                ), parentBatch);

                log.info("🧱 build next batch (index: {}, hash: {}) with {} chunks from block height {} to {} included",
                        nextBatch.getBatch().getBatchIndex(), nextBatch.getBatch().getBatchHashHex(), ((ChunksPayload) nextBatch.getBatch().getPayload()).chunks().size(),
                        nextBatch.getBatch().getStartBlockNumber(), nextBatch.getBatch().getEndBlockNumber());

                proverControllerClient.proveBatch(nextBatch);
                rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.TEE_PROOF);
                if (nextBatch.getBatchIndex().compareTo(rollupConfig.getZkVerificationStartBatch()) >= 0) {
                    rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.ZK_PROOF);
                }

                rollupRepository.saveBatch(nextBatch);
                rollupRepository.updateRollupNumberRecord(
                        ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH,
                        nextBatchIndex.add(BigInteger.ONE)
                );
                // reset the chunk index
                nextChunkIndex = 0;
                // reset the growing chunks memory cache
                growingBatchChunks.reset();
                otelMetric.recordBatchConstructedEvent(nextBatch);

                log.info("🏅 successful to process batch-v{} {} with {} chunks",
                        nextBatch.getBatchHeader().getVersion().getValue(),
                        nextBatch.getBatch().getBatchIndex(),
                        ((ChunksPayload) nextBatch.getBatch().getPayload()).chunks().size()
                );
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

    private GrowingBatchInfo getGrowingBatchInfo(ChunkWrapper currChunk) {
        growingBatchChunks.checkAndFill(currChunk, rollupRepository);
        var chunkList = growingBatchChunks.copy();
        chunkList.add(currChunk.getChunk());
        return new GrowingBatchInfo(
                getCurrBatchVersion(getStartBlockTimestampForBatch(currChunk.getBatchIndex())),
                new ChunksPayload(chunkList).serialize()
        );
    }

    private BatchWrapper validateBatch(BatchWrapper batchWrapper, BatchWrapper parentBatch) {
        if (!parentBatch.getEndBlockNumber().add(BigInteger.ONE).equals(batchWrapper.getStartBlockNumber())) {
            throw new InvalidBatchException(
                    "discontinuous block numbers between batches: ( parent: {}, curr: {}, parent_end_block: {}, curr_start_block: {})",
                    parentBatch.getBatchIndex(), batchWrapper.getBatchIndex(), parentBatch.getEndBlockNumber(), batchWrapper.getStartBlockNumber()
            );
        }
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

    private BatchVersionEnum getCurrBatchVersion(long currTimestamp) {
        return getCurrFork(currTimestamp).getBatchVersion();
    }

    private ForkInfo getCurrFork(long currTimestamp) {
        return this.rollupSpecs.getFork(currTimestamp);
    }

    @Getter
    static class GrowingBatchInfo {
        private final BatchVersionEnum batchVersion;
        private final byte[] rawPayload;
        private int compressedDataSize = -1;

        public GrowingBatchInfo(BatchVersionEnum batchVersion, byte[] rawPayload) {
            this.batchVersion = batchVersion;
            this.rawPayload = rawPayload;
        }

        public int getPayloadSize() {
            return rawPayload.length;
        }

        public int getOverBatchBlobLimitFlag(int batchCommitBlobSizeLimit) {
            // Only batch version ge 1 use new layout for DA data
            if (batchVersion == BatchVersionEnum.BATCH_V1) {
                var res = getPayloadSize() + BlobsDaData.DA_DATA_META_LEN_SIZE - batchCommitBlobSizeLimit * BlobsDaData.CAPACITY_BYTE_PER_BLOB;
                if (res >= 0) {
                    // only when uncompressed data size over the limit, we do the compression
                    // to cut down the cost
                    res = getCompressedDataSize() - batchCommitBlobSizeLimit * BlobsDaData.CAPACITY_BYTE_PER_BLOB;
                }
                return res;
            }
            // for batch version 0, we use the old layout for DA data
            return getPayloadSize() - batchCommitBlobSizeLimit * BlobsDaData.CAPACITY_BYTE_PER_BLOB;
        }

        public String toJson() {
            var obj = new JSONObject();
            obj.put("payloadSize", getPayloadSize());
            obj.put("compressedDataSize", compressedDataSize);
            return obj.toJSONString();
        }

        private int getCompressedDataSize() {
            if (compressedDataSize == -1) {
                compressedDataSize = this.batchVersion.getDaCompressor().compress(rawPayload).length + BlobsDaData.DA_DATA_META_LEN_SIZE;
            }
            return compressedDataSize;
        }
    }
}
