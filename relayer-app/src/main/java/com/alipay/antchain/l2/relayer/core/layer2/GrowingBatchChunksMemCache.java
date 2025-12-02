package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockPollingException;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrowingBatchChunksMemCache {

    private final List<ChunkWrapper> growingBatchChunks = new ArrayList<>();

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

    @Synchronized
    public void reset() {
        growingBatchChunks.clear();
    }

    @Synchronized
    public void add(ChunkWrapper chunk) {
        if (!growingBatchChunks.isEmpty()) {
            if (!growingBatchChunks.get(growingBatchChunks.size() - 1).getBatchIndex().equals(chunk.getBatchIndex())) {
                // add chunk from different batch is not allowed
                // supposed to reset the cache by calling checkAndFill when block processing start
                throw new RuntimeException(StrUtil.format("batch index not match: {} vs {}",
                        growingBatchChunks.get(growingBatchChunks.size() - 1).getBatchIndex(), chunk.getBatchIndex()));
            }
            if (growingBatchChunks.get(growingBatchChunks.size() - 1).getChunkIndex() == chunk.getChunkIndex()) {
                // if already added once, just remove the old one
                growingBatchChunks.remove(growingBatchChunks.size() - 1);
            }
        }
        // do some checks
        Assert.isTrue(chunk.getChunkIndex() == growingBatchChunks.size());
        if (chunk.getChunkIndex() != 0) {
            Assert.equals(
                    growingBatchChunks.get(growingBatchChunks.size() - 1).getChunkIndex() + 1,
                    chunk.getChunkIndex()
            );
            Assert.equals(
                    growingBatchChunks.get(growingBatchChunks.size() - 1).getChunk().getEndBlockNumber().add(BigInteger.ONE),
                    chunk.getChunk().getStartBlockNumber()
            );
        }
        growingBatchChunks.add(chunk);
    }

    @Synchronized
    @SneakyThrows
    public void checkAndFill(ChunkWrapper currChunk, IRollupRepository rollupRepository) {
        if (!growingBatchChunks.isEmpty()) {
            if (!growingBatchChunks.get(growingBatchChunks.size() - 1).getBatchIndex().equals(currChunk.getBatchIndex())) {
                // if new batch has been born, reset the growing chunks memory cache
                // When task switched from other relayer node, this situation could be arrived.
                log.info("Clean up the growing batch chunks memory cache after new batch born");
                reset();
            } else if (growingBatchChunks.get(growingBatchChunks.size() - 1).getChunkIndex() >= currChunk.getChunkIndex()) {
                // if already added once, just reset the cache
                log.warn("Re-added the chunk {} for batch {}, just reset the cache", currChunk.getChunkIndex(), currChunk.getBatchIndex());
                reset();
            }
        }
        if (growingBatchChunks.size() < currChunk.getChunkIndex()) {
            // case 1: create the growing chunks memory cache when the program runs here first time.
            // case 2: fill and catch up the growing chunks memory cache after task switched from other relayer node.
            List<CompletableFuture<ChunkWrapper>> futures = new ArrayList<>();
            for (var i = growingBatchChunks.size(); i < currChunk.getChunkIndex(); i++) {
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
                log.info("read chunk {} for batch {} asyncly from repo and add it into cache", chunkWrapper.getChunkIndex(), currChunk.getBatchIndex());
                growingBatchChunks.add(chunkWrapper);
            }
        }
    }

    @Synchronized
    public List<Chunk> copy() {
        return growingBatchChunks.stream().map(ChunkWrapper::getChunk).collect(Collectors.toList());
    }
}
