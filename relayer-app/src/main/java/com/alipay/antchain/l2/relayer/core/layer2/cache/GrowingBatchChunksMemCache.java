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

/**
 * In-memory cache for managing growing batch chunks in the L2 relayer.
 * <p>
 * This cache maintains serialized chunks for the current growing batch and provides
 * efficient access to chunk data without repeatedly querying the repository.
 * It supports automatic filling of missing chunks and ensures data consistency
 * across batch boundaries.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Stores serialized chunk data in a continuous byte array for efficient memory usage</li>
 *   <li>Maintains chunk pointers for quick index-based access</li>
 *   <li>Asynchronously loads missing chunks from repository when needed</li>
 *   <li>Automatically resets when a new batch is detected</li>
 *   <li>Thread-safe chunk reading with configurable thread pool</li>
 * </ul>
 * </p>
 *
 * @author Aone Copilot
 * @since 1.0
 */
@Slf4j
@Component
public class GrowingBatchChunksMemCache {

    /**
     * Serialized chunks data stored as a continuous byte array.
     * <p>
     * Each chunk's data is appended sequentially to this array.
     * </p>
     */
    private byte[] growingBatchSerializedChunks = new byte[0];

    /**
     * List of chunk pointers that track the location and metadata of each chunk
     * within the serialized chunks byte array.
     */
    private final List<ChunkPointer> growingBatchChunkPointers = new ArrayList<>();

    /**
     * The most recently sealed (completed) chunk.
     * <p>
     * Cached separately for quick access to the latest chunk.
     * </p>
     */
    private ChunkWrapper latestSealedChunk;

    /**
     * Thread pool executor for asynchronously reading chunks from repository.
     * <p>
     * Used when filling missing chunks to avoid blocking the main thread.
     * </p>
     */
    private final ThreadPoolExecutor chunkReadingExecutor;

    /**
     * Constructs a new GrowingBatchChunksMemCache with configurable thread pool size.
     *
     * @param asyncCoreSize the core size of the thread pool for asynchronous chunk reading,
     *                      defaults to 4 if not configured
     */
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

    /**
     * Resets the cache by clearing all stored chunks and pointers.
     * <p>
     * This method is called when:
     * <ul>
     *   <li>A new batch is detected (batch index changes)</li>
     *   <li>A chunk is being re-added (potential data inconsistency)</li>
     *   <li>The cache needs to be rebuilt from scratch</li>
     * </ul>
     * </p>
     */
    public void reset() {
        growingBatchChunkPointers.clear();
        growingBatchSerializedChunks = new byte[0];
        latestSealedChunk = null;
    }

    /**
     * Adds a new chunk to the cache.
     * <p>
     * This method performs several validations:
     * <ul>
     *   <li>Ensures the chunk belongs to the same batch as existing chunks</li>
     *   <li>Verifies chunk index continuity</li>
     *   <li>Checks block number continuity between consecutive chunks</li>
     *   <li>Handles duplicate chunk additions by replacing the old one</li>
     * </ul>
     * </p>
     *
     * @param chunk the chunk to add to the cache
     * @throws RuntimeException         if the chunk belongs to a different batch
     * @throws IllegalArgumentException if chunk index or block number continuity is violated
     */
    public void add(ChunkWrapper chunk) {
        if (!growingBatchChunkPointers.isEmpty()) {
            if (!growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex().equals(chunk.getBatchIndex())) {
                // Add chunk from different batch is not allowed
                // Supposed to reset the cache by calling checkAndFill when block processing starts
                throw new RuntimeException(StrUtil.format("batch index not match: {} vs {}",
                        growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex(), chunk.getBatchIndex()));
            }
            if (growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() == chunk.getChunkIndex()) {
                // If already added once, just remove the old one
                var pointerToDel = growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1);
                growingBatchChunkPointers.remove(growingBatchChunkPointers.size() - 1);
                growingBatchSerializedChunks = ArrayUtil.sub(growingBatchSerializedChunks, 0, pointerToDel.offset());
                latestSealedChunk = null;
            }
        }
        // Validate chunk index matches the expected position
        Assert.isTrue(chunk.getChunkIndex() == growingBatchChunkPointers.size());
        if (chunk.getChunkIndex() != 0) {
            // Ensure chunk index continuity
            Assert.equals(
                    growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() + 1,
                    chunk.getChunkIndex()
            );
            // Ensure block number continuity between consecutive chunks
            Assert.equals(
                    growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).endBlockNumber().add(BigInteger.ONE),
                    chunk.getChunk().getStartBlockNumber()
            );
        }
        var offset = growingBatchSerializedChunks.length;
        growingBatchSerializedChunks = RollupUtils.appendToRawChunks(growingBatchSerializedChunks, chunk.getBatchVersion(), chunk.getChunk());
        growingBatchChunkPointers.add(ChunkPointer.from(chunk, offset, growingBatchSerializedChunks.length - offset));
        latestSealedChunk = chunk;
    }

    /**
     * Checks the cache state and fills missing chunks if necessary.
     * <p>
     * This method handles several scenarios:
     * <ol>
     *   <li>New batch detection: Resets the cache when a new batch is detected</li>
     *   <li>Duplicate chunk: Resets the cache if the current chunk was already added</li>
     *   <li>Missing chunks: Asynchronously loads missing chunks from the repository</li>
     * </ol>
     * </p>
     * <p>
     * Missing chunks can occur in two cases:
     * <ul>
     *   <li>Initial cache creation when the program starts</li>
     *   <li>Cache recovery after task switches from another relayer node</li>
     * </ul>
     * </p>
     * <p>
     * The method uses parallel chunk loading for efficiency and validates block
     * number continuity to ensure data consistency.
     * </p>
     *
     * @param currChunk        the current chunk being processed
     * @param rollupRepository the repository to load missing chunks from
     * @throws BlockPollingException if a required chunk cannot be loaded from repository
     */
    @SneakyThrows
    public void checkAndFill(final ChunkWrapper currChunk, IRollupRepository rollupRepository) {
        if (!growingBatchChunkPointers.isEmpty()) {
            if (!growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).batchIndex().equals(currChunk.getBatchIndex())) {
                // If new batch has been born, reset the growing chunks memory cache
                // This situation can occur when task switches from another relayer node
                log.info("Clean up the growing batch chunks memory cache after new batch born");
                reset();
            } else if (growingBatchChunkPointers.get(growingBatchChunkPointers.size() - 1).chunkIndex() >= currChunk.getChunkIndex()) {
                // If already added once, just reset the cache
                log.warn("Re-added the chunk {} for batch {}, just reset the cache", currChunk.getChunkIndex(), currChunk.getBatchIndex());
                reset();
            }
        }
        var nextChunkIndex = growingBatchChunkPointers.size();
        if (nextChunkIndex < currChunk.getChunkIndex()) {
            // Case 1: Create the growing chunks memory cache when the program runs here first time
            // Case 2: Fill and catch up the growing chunks memory cache after task switched from other relayer node
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
                    // Check the first added chunk for block number continuity
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
                growingBatchSerializedChunks = RollupUtils.appendToRawChunks(growingBatchSerializedChunks, chunkWrapper.getBatchVersion(), chunkWrapper.getChunk());
                growingBatchChunkPointers.add(ChunkPointer.from(chunkWrapper, offset, growingBatchSerializedChunks.length - offset));
                latestSealedChunk = chunkWrapper;
            }
        }
    }

    /**
     * Creates a copy of the cached serialized chunks and appends additional chunks.
     * <p>
     * This method is useful for creating a complete batch by combining cached chunks
     * with newly created chunks without modifying the cache itself.
     * </p>
     *
     * @param chunks the chunks to append to the cached data
     * @return a new byte array containing both cached and appended chunks
     * @throws IllegalArgumentException if the chunks parameter is empty
     */
    public byte[] copyAndAppend(ChunkWrapper... chunks) {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new IllegalArgumentException("chunks is empty");
        }
        var result = new byte[growingBatchSerializedChunks.length];
        ArrayUtil.copy(growingBatchSerializedChunks, result, result.length);
        for (var currChunk : chunks) {
            result = RollupUtils.appendToRawChunks(result, currChunk.getBatchVersion(), currChunk.getChunk());
        }
        return result;
    }

    /**
     * Retrieves a chunk by its index from the cache.
     * <p>
     * This method provides efficient access to cached chunks:
     * <ul>
     *   <li>Returns null if the requested index is out of bounds</li>
     *   <li>Returns the cached latestSealedChunk directly if it matches the requested index</li>
     *   <li>Otherwise, reconstructs the chunk from the serialized data using chunk pointers</li>
     * </ul>
     * </p>
     *
     * @param chunkIndex the index of the chunk to retrieve
     * @return the chunk at the specified index, or {@code null} if not found
     * @throws IllegalArgumentException if the chunk pointer's index doesn't match the expected index
     */
    public ChunkWrapper getByIndex(int chunkIndex) {
        if (chunkIndex >= growingBatchChunkPointers.size()) {
            return null;
        }
        if (ObjectUtil.isNotNull(latestSealedChunk) && latestSealedChunk.getChunkIndex() == chunkIndex) {
            return latestSealedChunk;
        }
        var chunkPointer = growingBatchChunkPointers.get(chunkIndex);
        Assert.equals((long) chunkIndex, chunkPointer.chunkIndex(),
                "GrowingBatchChunksMemCache#getByIndex: element's index incorrect, expected {} but {}",
                chunkIndex, chunkPointer.chunkIndex());
        return new ChunkWrapper(
                chunkPointer.batchVersion(),
                chunkPointer.batchIndex(),
                chunkPointer.chunkIndex(),
                chunkPointer.gasSum(),
                ArrayUtil.sub(growingBatchSerializedChunks, chunkPointer.chunkStartOffset(), chunkPointer.chunkEndOffset())
        );
    }

    /**
     * Retrieves all chunks for the current batch from the cache.
     * <p>
     * This method reconstructs all cached chunks and validates that they all belong
     * to the expected batch. It's useful for batch finalization or verification.
     * </p>
     *
     * @param expectedBatchIndex the expected batch index for all chunks
     * @return a list of all chunks in the cache
     * @throws IllegalArgumentException if any chunk doesn't belong to the expected batch
     */
    public List<ChunkWrapper> getCurrBatchChunks(BigInteger expectedBatchIndex) {
        var chunks = new ArrayList<ChunkWrapper>();
        for (var chunkPointer : growingBatchChunkPointers) {
            Assert.isTrue(chunkPointer.batchIndex().equals(expectedBatchIndex), "incorrect batch index for chunk {} from growing cache", chunkPointer.chunkIndex());
            chunks.add(new ChunkWrapper(
                    chunkPointer.batchVersion(),
                    chunkPointer.batchIndex(),
                    chunkPointer.chunkIndex(),
                    chunkPointer.gasSum(),
                    ArrayUtil.sub(growingBatchSerializedChunks, chunkPointer.chunkStartOffset(), chunkPointer.chunkEndOffset())
            ));
        }
        return chunks;
    }
}
