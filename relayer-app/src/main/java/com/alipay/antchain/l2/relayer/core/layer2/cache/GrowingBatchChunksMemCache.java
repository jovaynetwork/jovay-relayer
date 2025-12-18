package com.alipay.antchain.l2.relayer.core.layer2.cache;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockPollingException;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrowingBatchChunksMemCache {

    private byte[] growingBatchSerializedChunks = new byte[0];

    private final List<ChunkPointer> growingBatchChunkPointers = new ArrayList<>();

    private final ThreadPoolExecutor chunkReadingExecutor;

    public GrowingBatchChunksMemCache(
            @Value("${l2-relayer.tasks.block-polling.l2.growing-batch-chunks-mem-cache.async-core-size:4}") int asyncCoreSize
    ) {
        this.chunkReadingExecutor = new ThreadPoolExecutor(
                asyncCoreSize,
                asyncCoreSize * 2,
                5000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("growing-batch-chunks-mem-cache-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void reset() {
        growingBatchChunkPointers.clear();
        growingBatchSerializedChunks = new byte[0];
    }

    public void add(ChunkWrapper chunk) {
        if (!growingBatchChunkPointers.isEmpty()) {
            if (!growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex().equals(chunk.getBatchIndex())) {
                // add chunk from different batch is not allowed
                // supposed to reset the cache by calling checkAndFill when block processing start
                throw new RuntimeException(StrUtil.format("batch index not match: {} vs {}",
                        growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex(), chunk.getBatchIndex()));
            }
            if (growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() == chunk.getChunkIndex()) {
                // if already added once, just remove the old one
                var pointerToDel = growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1);
                growingBatchChunkPointers.remove(growingBatchChunkPointers.size() - 1);
                growingBatchSerializedChunks = ArrayUtil.sub(growingBatchSerializedChunks, 0, pointerToDel.offset());
            }
        }
        // do some checks
        Assert.isTrue(chunk.getChunkIndex() == growingBatchChunkPointers.size());
        if (chunk.getChunkIndex() != 0) {
            Assert.equals(
                    growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() + 1,
                    chunk.getChunkIndex()
            );
            Assert.equals(
                    growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).endBlockNumber().add(BigInteger.ONE),
                    chunk.getChunk().getStartBlockNumber()
            );
        }
        var offset = growingBatchSerializedChunks.length;
        growingBatchSerializedChunks = RollupUtils.appendToRawChunks(growingBatchSerializedChunks, chunk.getChunk());
        growingBatchChunkPointers.add(ChunkPointer.from(chunk, offset, growingBatchSerializedChunks.length - offset));
    }

    @SneakyThrows
    public void checkAndFill(final ChunkWrapper currChunk, IRollupRepository rollupRepository) {
        if (!growingBatchChunkPointers.isEmpty()) {
            if (!growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex().equals(currChunk.getBatchIndex())) {
                // if new batch has been born, reset the growing chunks memory cache
                // When task switched from other relayer node, this situation could be arrived.
                log.info("Clean up the growing batch chunks memory cache after new batch born");
                reset();
            } else if (growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() >= currChunk.getChunkIndex()) {
                // if already added once, just reset the cache
                log.warn("Re-added the chunk {} for batch {}, just reset the cache", currChunk.getChunkIndex(), currChunk.getBatchIndex());
                reset();
            }
        }
        var nextChunkIndex = growingBatchChunkPointers.size();
        if (nextChunkIndex < currChunk.getChunkIndex()) {
            // case 1: create the growing chunks memory cache when the program runs here first time.
            // case 2: fill and catch up the growing chunks memory cache after task switched from other relayer node.
            List<CompletableFuture<ChunkWrapper>> futures = new ArrayList<>();
            for (var i = nextChunkIndex; i < currChunk.getChunkIndex(); i++) {
                int finalI = i;
                futures.add(CompletableFuture.supplyAsync(
                        () -> rollupRepository.getChunk(currChunk.getBatchIndex(), finalI),
                        chunkReadingExecutor
                ));
            }
            for (CompletableFuture<ChunkWrapper> future : futures) {
                var chunkWrapper = future.get(10, TimeUnit.SECONDS);
                if (ObjectUtil.isNull(chunkWrapper)) {
                    throw new BlockPollingException("get null prev chunk for batch {} and chunk {}", currChunk.getBatchIndex(), currChunk.getChunkIndex());
                }
                if (nextChunkIndex != 0 && chunkWrapper.getChunkIndex() == nextChunkIndex) {
                    // check the first added
                    if (!chunkWrapper.getStartBlockNumber().equals(growingBatchChunkPointers.get(nextChunkIndex - 1).endBlockNumber().add(BigInteger.ONE))) {
                        log.warn("fill the growing batch chunks cache failed that discontinuous blocks between chunk {} and chunk {}, " +
                                 "start height of next is {}, end height of previous is {}. Gonna to reload the cache",
                                nextChunkIndex - 1, nextChunkIndex, chunkWrapper.getStartBlockNumber(), growingBatchChunkPointers.get(nextChunkIndex - 1).endBlockNumber());
                        reset();
                        checkAndFill(currChunk, rollupRepository);
                        return;
                    }
                }
                log.info("read chunk {} for batch {} asyncly from repo and add it into cache", chunkWrapper.getChunkIndex(), currChunk.getBatchIndex());
                var offset = growingBatchSerializedChunks.length;
                growingBatchSerializedChunks = RollupUtils.appendToRawChunks(growingBatchSerializedChunks, chunkWrapper.getChunk());
                growingBatchChunkPointers.add(ChunkPointer.from(chunkWrapper, offset, growingBatchSerializedChunks.length - offset));
            }
        }
    }

    public byte[] copyAndAppend(ChunkWrapper currChunk) {
        return RollupUtils.appendToRawChunks(growingBatchSerializedChunks, currChunk.getChunk());
    }
}
