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

package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import cn.hutool.core.lang.Assert;
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
import com.alipay.antchain.l2.relayer.commons.l2basic.da.IDaService;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.specs.IRollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.layer2.cache.GrowingBatchChunksMemCache;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.metrics.otel.IOtelMetric;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Resource
    private IDaService daService;

    @Override
    public BigInteger process(BasicBlockTrace blockTrace) {
        final var currHeight = BigInteger.valueOf(blockTrace.getHeader().getNumber());
        // Current building chunk index.
        var nextChunkIndex = getNextChunkIndex();
        var currChunkIndex = nextChunkIndex;
        // Current building batch index.
        final var nextBatchIndex = getNextBatchIndex();
        // Gas accumulator of this building chunk.
        final var chunkGasAccumulator = getChunkGasAccumulator();

        final var currChunkStartBlockNumber = getCurrChunkStartBlockNumber(nextChunkIndex, nextBatchIndex);
        var isFirstBlockOfCurrChunk = ObjectUtil.equal(currChunkStartBlockNumber, currHeight);

        // if we add this block to the building chunk, let's see if that over batch tx data size limit.
        var batchStartTimestamp = getStartBlockTimestampForBatch(nextBatchIndex);
        var currBatchVersion = getCurrBatchVersion(batchStartTimestamp);
        var buildingChunk = buildL2NextChunk(currBatchVersion, nextBatchIndex, nextChunkIndex, currChunkStartBlockNumber, currHeight);
        // Growing batch info contains the uncompressed batch data.
        // And capable to calc the actual eip4844 blobs size if rollup the building batch now.
        var growingBatchInfo = getGrowingBatchInfo(buildingChunk, blockTrace, batchStartTimestamp, currBatchVersion);
        var overBatchBlobLimitFlag = growingBatchInfo.getOverBatchBlobLimitFlag(
                rollupConfig.getBatchCommitBlobSizeLimit(),
                rollupConfig.getMaxChunksMemoryUsed()
        );
        // if we add this block to the building chunk, let's see if that over batch duration limit.
        var overBatchDurationFlag = growingBatchInfo.batchDurationOverLimit(rollupConfig.getMaxTimeIntervalBetweenBatches());
        log.debug("add this block and then commit batch, the growing batch info is {}", growingBatchInfo.toJson());

        var chunkGasWithCurrBlock = chunkGasAccumulator + blockTrace.getHeader().getGasUsed();
        var chunkGasRightGap = chunkGasWithCurrBlock - rollupConfig.getGasPerChunk();
        var newBatchSealed = overBatchBlobLimitFlag >= 0 || overBatchDurationFlag;
        // if next chunk ready
        var newChunkSealed = chunkGasRightGap > 0
                             // If building chunk meets the max block size condition, we need born a new chunk.
                             || buildingChunk.getChunk().getNumBlocks() == growingBatchInfo.getBatchVersion().getMaxBlockSizeSingleChunk()
                             || newBatchSealed;

        var chunkGasLeftGap = Math.abs(rollupConfig.getGasPerChunk() - chunkGasAccumulator);
        log.info("curr building chunk gas status: (accumulator: {}, curr_block_gas_used: {}, right_gap: {}, left_gap: {})",
                chunkGasAccumulator, blockTrace.getHeader().getGasUsed(), chunkGasRightGap, chunkGasLeftGap);

        // below here, last block of this chunk is `currHeight`
        // Check if this block is gonna to be included inside the newborn chunk
        // If the newChunkSealed is false, this variable means nothing
        var newChunkIncludesCurrBlock = overBatchBlobLimitFlag <= 0
                                        && (isFirstBlockOfCurrChunk || chunkGasRightGap <= chunkGasLeftGap);

        if (newChunkSealed) {
            ChunkWrapper currChunk;
            BigInteger lastBlockHeightOfCurrChunk;
            if (newChunkIncludesCurrBlock) {
                // reuse the building chunk as current chunk
                currChunk = buildingChunk;
                lastBlockHeightOfCurrChunk = currHeight;
            } else if (isFirstBlockOfCurrChunk) {
                // already over the blob data capacity from here
                if (nextChunkIndex == 0) {
                    throw new InvalidBatchException("The first chunk of batch#{} only contains one block#{}, and already over the blobs capacity",
                            nextBatchIndex, currChunkStartBlockNumber);
                }
                // Not the first chunk of curr batch, but still a chunk contains
                // only one block and over blob capacity with this chunk,
                // we need abandon this chunk and start a new batch next round.
                currChunk = null;
                lastBlockHeightOfCurrChunk = currChunkStartBlockNumber.subtract(BigInteger.ONE);
                log.info("chunk {}-{} only has one block inside and cause that batch over the blobs capacity, so abandon it!", nextBatchIndex, nextChunkIndex);
            } else {
                // 1. The gap-bigger block than previous one
                // 2. Already over the blob data capacity
                lastBlockHeightOfCurrChunk = currHeight.subtract(BigInteger.ONE);
                currChunk = buildL2NextChunk(growingBatchInfo.getBatchVersion(), nextBatchIndex, nextChunkIndex, currChunkStartBlockNumber, lastBlockHeightOfCurrChunk);
                if (!newBatchSealed) {
                    // curr block is not included into curr born chunk and no batch sealed,
                    // so we need to re-calc the blob bytes size with new sealed chunk
                    var singleCurrBlockChunk = buildL2NextChunk(currBatchVersion, nextBatchIndex, nextChunkIndex + 1, currHeight, currHeight);

                    log.info("prev growing batch info: {}", growingBatchInfo.toJson());
                    growingBatchInfo = getGrowingBatchInfo(currChunk, singleCurrBlockChunk, blockTrace, batchStartTimestamp, currBatchVersion);
                    overBatchBlobLimitFlag = growingBatchInfo.getOverBatchBlobLimitFlag(
                            rollupConfig.getBatchCommitBlobSizeLimit(),
                            rollupConfig.getMaxChunksMemoryUsed()
                    );
                    newBatchSealed = overBatchBlobLimitFlag >= 0;
                    log.info("recalculate the result of batch sealed: ( result: {}, over_size: {} ) ", newBatchSealed, overBatchBlobLimitFlag);
                }
            }

            if (ObjectUtil.isNotNull(currChunk)) {
                rollupRepository.saveChunk(validateChunk(currChunk));
                log.info("👏 chunk {} for batch {} is ready on block height {}", nextChunkIndex, nextBatchIndex, lastBlockHeightOfCurrChunk);
                log.info("current chunk parameters: (chunk_gas_accumulator: {} + {}, chunk_start_height: {}, chunk_block_size: {}, growing_batch_info: {})",
                        chunkGasAccumulator, blockTrace.getHeader().getGasUsed(), currChunkStartBlockNumber, currChunk.getChunk().getNumBlocks(), growingBatchInfo.toJson());

                proverControllerClient.notifyChunk(nextBatchIndex, nextChunkIndex, currChunk.getStartBlockNumber(), currChunk.getEndBlockNumber());
                growingBatchChunks.add(currChunk);
                log.info("add the next chunk into memory cache: (batch {}, chunk {})", currChunk.getBatchIndex(), currChunk.getChunkIndex());
                rollupRepository.clearL2BlockTracesCacheForCurrChunk();
            }

            if (newBatchSealed) {
                var lastBlockTrace = currHeight.compareTo(lastBlockHeightOfCurrChunk) == 0 ? blockTrace : rollupRepository.getL2BlockTrace(lastBlockHeightOfCurrChunk);
                var parentBatch = rollupRepository.getBatch(nextBatchIndex.subtract(BigInteger.ONE), false);
                var nextBatch = validateBatch(BatchWrapper.createBatch(
                        growingBatchInfo.getBatchVersion(),
                        nextBatchIndex,
                        parentBatch,
                        lastBlockTrace.getHeader().getStateRoot().toByteArray(),
                        lastBlockTrace.getL1MsgRollingHash().getValue().toByteArray(),
                        lastBlockTrace.getL2MsgRoot().getValue().toByteArray(),
                        lastBlockTrace.getFinalizeL1MsgIndex(),
                        growingBatchChunks.getCurrBatchChunks(nextBatchIndex)
                ), parentBatch);

                log.info("🧱 build next batch (index: {}, hash: {}) with {} chunks from block height {} to {} included",
                        nextBatch.getBatch().getBatchIndex(), nextBatch.getBatch().getBatchHashHex(),
                        ((ChunksPayload) nextBatch.getBatch().getPayload()).chunks().size(),
                        nextBatch.getBatch().getStartBlockNumber(), nextBatch.getBatch().getEndBlockNumber());

                proverControllerClient.proveBatch(nextBatch);
                rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.TEE_PROOF);
                if (nextBatch.getBatchIndex().compareTo(rollupConfig.getZkVerificationStartBatch()) >= 0) {
                    rollupRepository.createBatchProveRequest(nextBatch.getBatch().getBatchIndex(), ProveTypeEnum.ZK_PROOF);
                }

                switch (rollupConfig.getDaType()) {
                    case BLOBS -> rollupRepository.saveBatch(nextBatch);
                    // Ideally, the batch data should be uploaded to the DAS asynchronously,
                    // and more design work will be introduced in the future.
                    // Currently, a local dummy DA service is used in the Relayer,
                    // so performance and latency are not considered for now. 🤪
                    case DAS -> daService.uploadBatch(nextBatch);
                }
                rollupRepository.updateRollupNumberRecord(
                        ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH,
                        nextBatchIndex.add(BigInteger.ONE)
                );
                // reset the chunk index
                nextChunkIndex = 0;
                // reset the growing chunks memory cache
                growingBatchChunks.reset();
                otelMetric.recordBatchConstructedEvent(nextBatch);
                cleanUpCache(nextBatch);

                log.info("🏅 successful to process batch-v{} {} with {} chunks",
                        nextBatch.getBatchHeader().getVersion().getValueAsUint8(),
                        nextBatch.getBatch().getBatchIndex(),
                        ((ChunksPayload) nextBatch.getBatch().getPayload()).chunks().size()
                );
            } else {
                log.info("🏆 successful to process chunk {}@{}", nextBatchIndex, nextChunkIndex);
                // batch not ready, so accumulate chunk index
                nextChunkIndex++;
            }

            updateNewChunkNumbers(newChunkIncludesCurrBlock, blockTrace, nextChunkIndex);
        } else {
            updateChunkNumbers(chunkGasWithCurrBlock);
        }

        // if over blob data limit, this block belongs to next batch
        // if new batch sealed and this block not included, this block belongs to next batch
        var batchThisBlockBelongsTo = newBatchSealed && !newChunkIncludesCurrBlock ?
                nextBatchIndex.add(BigInteger.ONE) : nextBatchIndex;
        // if over blob data limit, this block belongs to the zero chunk of next batch
        // if new batch sealed and still not included inside curr chunk, this block belongs to the zero chunk of next batch
        // if not meet the upper condition, and new chunk born but curr block not included, this block belongs to next chunk
        var chunkThisBlockBelongsTo = newBatchSealed && !newChunkIncludesCurrBlock ?
                0 : (newChunkSealed && !newChunkIncludesCurrBlock ? currChunkIndex + 1 : currChunkIndex);
        proverControllerClient.notifyBlock(batchThisBlockBelongsTo, chunkThisBlockBelongsTo, currHeight);

        return batchThisBlockBelongsTo;
    }

    private GrowingBatchInfo getGrowingBatchInfo(ChunkWrapper currChunk, BasicBlockTrace currBlock, long batchStartTimestamp, BatchVersionEnum currBatchVersion) {
        growingBatchChunks.checkAndFill(currChunk, rollupRepository);
        return new GrowingBatchInfo(
                currBatchVersion,
                batchStartTimestamp,
                currBlock.getHeader().getTimestamp(),
                growingBatchChunks.copyAndAppend(currChunk)
        );
    }

    private GrowingBatchInfo getGrowingBatchInfo(ChunkWrapper currChunkExcludesCurrBlock, ChunkWrapper chunkSingleBlock,
                                                 BasicBlockTrace currBlock, long batchStartTimestamp, BatchVersionEnum currBatchVersion) {
        growingBatchChunks.checkAndFill(currChunkExcludesCurrBlock, rollupRepository);
        return new GrowingBatchInfo(
                currBatchVersion,
                batchStartTimestamp,
                currBlock.getHeader().getTimestamp(),
                growingBatchChunks.copyAndAppend(currChunkExcludesCurrBlock, chunkSingleBlock)
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
            if (batchWrapper.getChunks().size() == 1 && batchWrapper.getChunks().get(0).getNumBlocks() == 1) {
                throw new InvalidBatchException("The first chunk of batch#{} only contains one block#{}, and already over the blobs capacity",
                        batchWrapper.getBatchIndex(), batchWrapper.getChunks().get(0).getStartBlockNumber());
            }
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
        var latestChunk = growingBatchChunks.getByIndex((int) (nextChunkIndex - 1));
        if (ObjectUtil.isNull(latestChunk) || !latestChunk.getBatchIndex().equals(nextBatchIndex)) {
            latestChunk = rollupRepository.getChunk(nextBatchIndex, nextChunkIndex - 1);
        }
        return latestChunk.getEndBlockNumber().add(BigInteger.ONE);
    }

    private ChunkWrapper buildL2NextChunk(BatchVersionEnum batchVersion, BigInteger nextBatchIndex, Long nextChunkIndex, BigInteger startBlockNumber, BigInteger endBlockNumber) {
        Assert.isTrue(
                endBlockNumber.subtract(startBlockNumber).add(BigInteger.ONE).longValue() <= batchVersion.getMaxBlockSizeSingleChunk(),
                "block size over limit inside building chunk {}-{} with batch version {}: (from: {}, to: {})",
                nextBatchIndex, nextChunkIndex, batchVersion, startBlockNumber, endBlockNumber
        );
        var blockTraces = rollupRepository.getL2BlockTraceRange(startBlockNumber, endBlockNumber);
        if (ObjectUtil.isEmpty(blockTraces)) {
            throw new BlockPollingException("no block trace found for chunk {} in batch {}", nextChunkIndex, nextBatchIndex);
        }
        return new ChunkWrapper(batchVersion, nextBatchIndex, nextChunkIndex, blockTraces);
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

    private void updateNewChunkNumbers(boolean newChunkIncludesCurrBlock, BasicBlockTrace blockTrace, long nextChunkIndex) {
        // reset zk accumulator to curr block's zk-cycles or zero
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR,
                newChunkIncludesCurrBlock ? BigInteger.ZERO : BigInteger.valueOf(blockTrace.getHeader().getGasUsed())
        );
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK,
                BigInteger.valueOf(nextChunkIndex)
        );
    }

    private void updateChunkNumbers(long chunkGasWithCurrBlock) {
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR,
                BigInteger.valueOf(chunkGasWithCurrBlock)
        );
    }

    private long getNextChunkIndex() {
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

    private long getChunkGasAccumulator() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR).intValue();
    }

    private BatchVersionEnum getCurrBatchVersion(long currTimestamp) {
        return getCurrFork(currTimestamp).getBatchVersion();
    }

    private ForkInfo getCurrFork(long currTimestamp) {
        return this.rollupSpecs.getFork(currTimestamp);
    }

    private void cleanUpCache(BatchWrapper nextBatch) {
        // delete all useless cached data after batch sealed
        CompletableFuture.runAsync(() -> {
            rollupRepository.deleteL2BlockTracesFromCache(nextBatch.getStartBlockNumber(), nextBatch.getEndBlockNumber());
            rollupRepository.deleteChunksFromCache(nextBatch.getBatchIndex(), 0, nextBatch.getChunks().size() - 1);
        }).exceptionally(throwable -> {
            log.error("failed to delete useless cached data after batch sealed", throwable);
            return null;
        });
    }

    @Getter
    static class GrowingBatchInfo {
        private final BatchVersionEnum batchVersion;
        private final byte[] rawPayload;
        private int compressedDataSize = -1;
        private final long batchDurationInMs;

        public GrowingBatchInfo(BatchVersionEnum batchVersion, long currBatchStartTimestamp, long currBlockTimestamp, byte[] rawPayload) {
            this.batchVersion = batchVersion;
            this.rawPayload = rawPayload;
            this.batchDurationInMs = currBlockTimestamp - currBatchStartTimestamp;
        }

        public int getPayloadSize() {
            return rawPayload.length;
        }

        public int getOverBatchBlobLimitFlag(int batchCommitBlobSizeLimit, int maxChunksMemoryUsed) {
            var res = getPayloadSize() - maxChunksMemoryUsed;
            if (res >= 0) {
                log.warn("raw chunks is over or equal to the max memory used");
                return res;
            }
            // Only batch version ge 1 use new layout for DA data
            if (batchVersion.isBatchDataCompressionSupport()) {
                res = getPayloadSize() + BlobsDaData.DA_DATA_META_LEN_SIZE - batchCommitBlobSizeLimit * BlobsDaData.CAPACITY_BYTE_PER_BLOB;
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

        public boolean batchDurationOverLimit(long maxTimeIntervalBetweenBatches) {
            return batchDurationInMs >= maxTimeIntervalBetweenBatches;
        }

        public String toJson() {
            var obj = new JSONObject();
            obj.put("payloadSize", getPayloadSize());
            obj.put("compressedDataSize", compressedDataSize);
            obj.put("batchDurationInMs", batchDurationInMs);
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
