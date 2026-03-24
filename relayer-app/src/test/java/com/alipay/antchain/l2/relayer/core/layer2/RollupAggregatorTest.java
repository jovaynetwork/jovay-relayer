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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.l2basic.*;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.layer2.cache.ChunkPointer;
import com.alipay.antchain.l2.relayer.core.layer2.cache.GrowingBatchChunksMemCache;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.core.tracer.TraceServiceClient;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.trace.*;
import com.github.luben.zstd.Zstd;
import com.google.protobuf.ByteString;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RollupAggregatorTest extends TestBase {

    @Resource
    private IRollupAggregator rollupAggregator;

    @Resource
    private GrowingBatchChunksMemCache growingBatchChunks;

    @MockitoBean
    private ISystemConfigRepository systemConfigRepository;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private ProverControllerClient proverControllerClient;

    @MockitoBean
    private TraceServiceClient traceServiceClient;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(6);
        when(rollupConfig.getMaxTimeIntervalBetweenBatches()).thenReturn(3600_000L);
        when(rollupConfig.getZkVerificationStartBatch()).thenReturn(BigInteger.ONE);
        when(rollupConfig.getMaxChunksMemoryUsed()).thenReturn(2 * 1024 * 1024);

        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);
    }

    @Test
    @SneakyThrows
    public void testProcessOnlyNewChunk() {
        var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 100 * 1024);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

        var lastHeader = mock(BlockHeader.class);
        var lastBlock = mock(BasicBlockTrace.class);
        when(lastBlock.getHeader()).thenReturn(lastHeader);
        when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastHeader.getNumber()).thenReturn(31L);

        var currHeader = mock(BlockHeader.class);
        var currBlock = mock(BasicBlockTrace.class);
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currHeader.getNumber()).thenReturn(32L);
        when(currHeader.getGasUsed()).thenReturn(3000_0001L);
        when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

        var lastBatchHeader = mock(BatchHeader.class);
        var lastBatchWrapper = mock(BatchWrapper.class);
        when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
        when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

        when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                BasicBlockTrace.newBuilder()
                        .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                        .build()
        );

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(32L)), eq(BigInteger.valueOf(32L)))).thenReturn(ListUtil.toList(currBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO, BigInteger.valueOf(3000_0000 - 1));

        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31L)), eq(BigInteger.valueOf(32L)));
        verify(proverControllerClient, never()).proveBatch(notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).saveBatch(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR), eq(BigInteger.ZERO));

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(
                16 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length +
                new Chunk(ListUtil.toList(lastBlock, currBlock)).serialize(false).length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );
        var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
        latestSealedChunkF.setAccessible(true);
        var latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
        Assert.assertEquals(3L, latestSealedChunk.getChunkIndex());
        Assert.assertEquals(BigInteger.valueOf(32), latestSealedChunk.getEndBlockNumber());
        Assert.assertEquals(2, latestSealedChunk.getChunk().getBlocks().size());
        Assert.assertArrayEquals(
                new Chunk(ListUtil.toList(lastBlock, currBlock)).serialize(false),
                latestSealedChunk.getChunk().serialize(false)
        );
        growingBatchChunks.reset();

        // pick the left not the right
        clearInvocations(rollupRepository);
        when(currHeader.getGasUsed()).thenReturn(3L);

        rollupAggregator.process(currBlock);
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(4L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31L)), eq(BigInteger.valueOf(31L)));
        verify(proverControllerClient, never()).proveBatch(notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).saveBatch(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR), eq(BigInteger.valueOf(3)));

        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
        Assert.assertEquals(
                16 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length +
                new Chunk(ListUtil.toList(lastBlock)).serialize(false).length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );
        latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
        Assert.assertEquals(3L, latestSealedChunk.getChunkIndex());
        Assert.assertEquals(BigInteger.valueOf(31), latestSealedChunk.getEndBlockNumber());
        Assert.assertEquals(1, latestSealedChunk.getChunk().getBlocks().size());
        Assert.assertArrayEquals(
                new Chunk(ListUtil.toList(lastBlock)).serialize(false),
                latestSealedChunk.getChunk().serialize(false)
        );
        growingBatchChunks.reset();

        // no chunk born if gas accumulator not over recommended value
        clearInvocations(rollupRepository, proverControllerClient);
        when(currHeader.getGasUsed()).thenReturn(1L);
        rollupAggregator.process(currBlock);
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, never()).notifyChunk(notNull(), anyLong(), notNull(), notNull());

        Assert.assertEquals(3, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
        Assert.assertEquals(
                12 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );
        latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
        Assert.assertEquals(2L, latestSealedChunk.getChunkIndex());
        Assert.assertEquals(BigInteger.valueOf(30), latestSealedChunk.getEndBlockNumber());
        Assert.assertEquals(10, latestSealedChunk.getChunk().getBlocks().size());
        Assert.assertArrayEquals(
                chunk2.serialize(false),
                latestSealedChunk.getChunk().serialize(false)
        );
        growingBatchChunks.reset();
    }

    @Test
    @SneakyThrows
    public void testProcessNewChunkWithMaxBlockSizeMet() {
        var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 100 * 1024);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

        var lastHeader = mock(BlockHeader.class);
        var lastBlock = mock(BasicBlockTrace.class);
        when(lastBlock.getHeader()).thenReturn(lastHeader);
        when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastHeader.getNumber()).thenReturn(284L);

        var currHeader = mock(BlockHeader.class);
        var currBlock = mock(BasicBlockTrace.class);
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currHeader.getNumber()).thenReturn(285L);
        when(currHeader.getGasUsed()).thenReturn(100L);
        when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

        var lastBatchHeader = mock(BatchHeader.class);
        var lastBatchWrapper = mock(BatchWrapper.class);
        when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
        when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

        when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                BasicBlockTrace.newBuilder()
                        .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                        .build()
        );

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(285)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(284)))).thenReturn(lastBlock);
        var mockBlocksFrom31To283 = IntStream.range(31, 284).mapToObj(n -> {
            var tmpHeader = mock(BlockHeader.class);
            var tmpBlock = mock(BasicBlockTrace.class);
            when(tmpBlock.getHeader()).thenReturn(tmpHeader);
            when(tmpHeader.getNumber()).thenReturn((long) n);
            return tmpBlock;
        }).toList();

        var mockBlocksTo285 = new ArrayList<>(mockBlocksFrom31To283);
        mockBlocksTo285.add(lastBlock);
        mockBlocksTo285.add(currBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(285)))).thenReturn(mockBlocksTo285);
        var mockBlocksTo284 = new ArrayList<>(mockBlocksFrom31To283);
        mockBlocksTo284.add(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(284)))).thenReturn(mockBlocksTo284);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(285L)), eq(BigInteger.valueOf(285L)))).thenReturn(ListUtil.toList(currBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO, BigInteger.valueOf(3000_0000 - 1));

        // only block size meet the new chunk seal condition
        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(285)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(285)));
        verify(proverControllerClient, never()).proveBatch(notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).saveBatch(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR), eq(BigInteger.ZERO));

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(
                16 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length +
                new Chunk(mockBlocksTo285).serialize(false).length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );

        var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
        latestSealedChunkF.setAccessible(true);
        var latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
        Assert.assertEquals(3L, latestSealedChunk.getChunkIndex());
        Assert.assertEquals(BigInteger.valueOf(285), latestSealedChunk.getEndBlockNumber());
        Assert.assertEquals(255, latestSealedChunk.getChunk().getBlocks().size());
        Assert.assertArrayEquals(
                new Chunk(mockBlocksTo285).serialize(false),
                latestSealedChunk.getChunk().serialize(false)
        );

        growingBatchChunks.reset();

        clearInvocations(rollupRepository, proverControllerClient);
        // The chunk gas sum and the block size all meet the new chunk seal condition
        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(4L), eq(BigInteger.valueOf(285)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(284)));
        verify(proverControllerClient, never()).proveBatch(notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).saveBatch(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR), eq(BigInteger.valueOf(100)));

        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
        Assert.assertEquals(
                16 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length +
                new Chunk(mockBlocksTo284).serialize(false).length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );
        latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
        Assert.assertEquals(3L, latestSealedChunk.getChunkIndex());
        Assert.assertEquals(BigInteger.valueOf(284), latestSealedChunk.getEndBlockNumber());
        Assert.assertEquals(254, latestSealedChunk.getChunk().getBlocks().size());
        Assert.assertArrayEquals(
                new Chunk(mockBlocksTo284).serialize(false),
                latestSealedChunk.getChunk().serialize(false)
        );
        growingBatchChunks.reset();

        var currHeader256 = mock(BlockHeader.class);
        var currBlock256 = mock(BasicBlockTrace.class);
        when(currBlock256.getHeader()).thenReturn(currHeader256);
        when(currHeader256.getNumber()).thenReturn(286L);
        when(currHeader256.getGasUsed()).thenReturn(0L);
        when(currHeader256.getTimestamp()).thenReturn(System.currentTimeMillis());
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(286)))).thenReturn(currBlock256);

        var mockBlocksTo286 = new ArrayList<>(mockBlocksFrom31To283);
        mockBlocksTo286.add(lastBlock);
        mockBlocksTo286.add(currBlock);
        mockBlocksTo286.add(currBlock256);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(286)))).thenReturn(mockBlocksTo286);

        Assert.assertThrows("block size over limit inside building chunk 1-3 with batch version BATCH_V1: (from: 31, to: 286)",
                IllegalArgumentException.class, () -> rollupAggregator.process(currBlock256));
    }

    @Test
    @SneakyThrows
    public void testProcessDiscontinueBlocksBetweenBatches() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO, BigInteger.valueOf(-1));
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(31L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(100 * 1024)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getNumber()).thenReturn(32L);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            Assert.assertThrows(InvalidBatchException.class, () -> rollupAggregator.process(currBlock));

            verify(proverControllerClient, never()).notifyBlock(any(), anyLong(), any());
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31L)), eq(BigInteger.valueOf(31L)));
            verify(proverControllerClient, never()).proveBatch(any());

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(
                    16 + BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk0).length +
                    BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk1).length +
                    BatchVersionEnum.BATCH_V1.getChunkCodec().serialize(chunk2).length +
                    new Chunk(ListUtil.toList(lastBlock)).serialize(false).length,
                    ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
            );

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            var latestSealedChunk = (ChunkWrapper) latestSealedChunkF.get(growingBatchChunks);
            Assert.assertEquals(3L, latestSealedChunk.getChunkIndex());
            Assert.assertEquals(BigInteger.valueOf(31), latestSealedChunk.getEndBlockNumber());
            Assert.assertEquals(1, latestSealedChunk.getChunk().getBlocks().size());
            Assert.assertArrayEquals(
                    new Chunk(ListUtil.toList(lastBlock)).serialize(false),
                    latestSealedChunk.getChunk().serialize(false)
            );

            growingBatchChunks.reset();
        }
    }

    @Test
    @SneakyThrows
    public void testProcessWithPreviousBatchMemCache() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);

            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 1000 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            growingBatchChunks.add(chunkWrapper0);
            growingBatchChunks.add(chunkWrapper1);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(1)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(21)))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(21L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(800 * 1024)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getNumber()).thenReturn(22L);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(21)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(22)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(21)), eq(BigInteger.valueOf(22)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(21)), eq(BigInteger.valueOf(21)))).thenReturn(ListUtil.toList(lastBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(0));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(2));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            var chunkOfNewBatch = mock(Chunk.class);
            when(chunkOfNewBatch.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
            when(chunkOfNewBatch.getEndBlockNumber()).thenReturn(BigInteger.valueOf(21));
            when(chunkOfNewBatch.serialize(false)).thenReturn(RandomUtil.randomBytes(41));
            var chunkWrapperOfNewBatch = mock(ChunkWrapper.class);
            when(chunkWrapperOfNewBatch.getChunkIndex()).thenReturn(0L);
            when(chunkWrapperOfNewBatch.getChunk()).thenReturn(chunkOfNewBatch);
            when(chunkWrapperOfNewBatch.getBatchIndex()).thenReturn(BigInteger.valueOf(2));
            when(chunkWrapperOfNewBatch.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
            when(chunkWrapperOfNewBatch.getEndBlockNumber()).thenReturn(BigInteger.valueOf(21));

            when(rollupRepository.getChunks(eq(BigInteger.TWO))).thenReturn(ListUtil.toList(chunkWrapperOfNewBatch));

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(3)), eq(0L), eq(BigInteger.valueOf(22)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(21)), eq(BigInteger.valueOf(21)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.TWO)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.TWO), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.TWO), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.TWO)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testProcessNewBatchAndBlockBelongsToNextChunk() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk3 = mock(Chunk.class);
            when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunk3.serialize(false)).thenReturn(RandomUtil.randomBytes(41));
            var chunkWrapper3 = mock(ChunkWrapper.class);
            when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
            when(chunkWrapper3.getChunk()).thenReturn(chunk3);
            when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunkWrapper3.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
            when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));
            growingBatchChunks.checkAndFill(chunkWrapper3, rollupRepository);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(31L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(100 * 1024)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis());
            when(currHeader.getNumber()).thenReturn(32L);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));

            // let the batch time condition met
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis() - 61 * 60_000).build())
                            .build()
            );
            clearInvocations(rollupRepository, proverControllerClient);
            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testProcessBlockOverChunkGasLimitAndNewBatchSealedAfterRecalcDaPayload() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);

            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk3 = mock(Chunk.class);
            when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunk3.serialize(false)).thenReturn(RandomUtil.randomBytes(41));
            var chunkWrapper3 = mock(ChunkWrapper.class);
            when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
            when(chunkWrapper3.getChunk()).thenReturn(chunk3);
            when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
            when(chunkWrapper3.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
            growingBatchChunks.checkAndFill(chunkWrapper3, rollupRepository);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(31L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(100 * 1024 - 57466)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getGasUsed()).thenReturn(10L);
            when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis());
            when(currHeader.getNumber()).thenReturn(32L);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(32)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(currBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.valueOf(29999999));

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testOverOrEqualMaxChunksMemoryUsed() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(17);

            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(31L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(2 * 1024 * 1024)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getNumber()).thenReturn(32L);
            when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis());

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));

            when(currHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(currBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(new byte[1380241 - 6 - 1203]))
                                    ).build()
                    )
            );

            clearInvocations(proverControllerClient, rollupRepository);
            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testProcessCurrChunkAbandoned() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
            when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2));

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currHeader.getNumber()).thenReturn(31L);
            when(currHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(100 * 1024)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());

            var currHeader30 = mock(BlockHeader.class);
            var currBlock30 = mock(BasicBlockTrace.class);
            when(currHeader30.getNumber()).thenReturn(30L);
            when(currHeader30.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(currBlock30.getHeader()).thenReturn(currHeader);
            when(currBlock30.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock30.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(30)))).thenReturn(currBlock30);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(currBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, never()).notifyChunk(any(), anyLong(), notNull(), notNull());
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimit() {
        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

        var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

        var lastBatchHeader = mock(BatchHeader.class);
        when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
        var lastBatchWrapper = mock(BatchWrapper.class);
        when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
        when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

        when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                BasicBlockTrace.newBuilder()
                        .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                        .build()
        );

        var currHeader = mock(BlockHeader.class);
        var currBlock = mock(BasicBlockTrace.class);
        when(currHeader.getNumber()).thenReturn(31L);
        when(currHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
        when(currBlock.getTransactionsList()).thenReturn(
                ListUtil.toList(
                        Transaction.newBuilder()
                                .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                .setLegacyTx(
                                        LegacyTransaction.newBuilder()
                                                .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                .setData(ByteString.copyFrom(RandomUtil.randomBytes(43778)))
                                ).build()
                )
        );
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(currBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(currBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
        verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
        verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

        var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
        latestSealedChunkF.setAccessible(true);
        Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimit_GrowingMemLoadedWithGap() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);

            var forkInfo = mock(ForkInfo.class);
            when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
            when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            // loaded and with gap
            growingBatchChunks.add(chunkWrapper0);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                            .build()
            );

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currHeader.getNumber()).thenReturn(31L);
            when(currHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(43778)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(currBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimitAndOverChunkLimit() {
        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

        var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 500 * 1024);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

        var lastBatchHeader = mock(BatchHeader.class);
        when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
        var lastBatchWrapper = mock(BatchWrapper.class);
        when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
        when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

        when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                BasicBlockTrace.newBuilder()
                        .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                        .build()
        );

        var lastHeader = mock(BlockHeader.class);
        var lastBlock = mock(BasicBlockTrace.class);
        when(lastBlock.getHeader()).thenReturn(lastHeader);
        when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastHeader.getNumber()).thenReturn(31L);
        when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
        when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);
        when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
        when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));

        var currHeader = mock(BlockHeader.class);
        var currBlock = mock(BasicBlockTrace.class);
        when(currHeader.getNumber()).thenReturn(32L);
        when(currBlock.getTransactionsList()).thenReturn(
                ListUtil.toList(
                        Transaction.newBuilder()
                                .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                .setLegacyTx(
                                        LegacyTransaction.newBuilder()
                                                .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                .setData(ByteString.copyFrom(RandomUtil.randomBytes(43778)))
                                ).build()
                )
        );
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currHeader.getGasUsed()).thenReturn(1000L);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.valueOf(3000_0000 - 1));

        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
        verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
        verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

        var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
        latestSealedChunkF.setAccessible(true);
        Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockMeetBatchTimeInterval_OverEqualAndLessChunkGasRecommendation() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            var forkInfo = mock(ForkInfo.class);
            when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
            when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

            var chunk0 = mockChunk(BigInteger.ONE, 10, 100 * 1024);
            var chunkWrapper0 = mock(ChunkWrapper.class);
            when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
            when(chunkWrapper0.getChunk()).thenReturn(chunk0);
            when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
            when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
            when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk1 = mockChunk(BigInteger.valueOf(11), 10, 100 * 1024);
            var chunkWrapper1 = mock(ChunkWrapper.class);
            when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
            when(chunkWrapper1.getChunk()).thenReturn(chunk1);
            when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
            when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
            when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            var chunk2 = mockChunk(BigInteger.valueOf(21), 10, 400 * 1024);
            var chunkWrapper2 = mock(ChunkWrapper.class);
            when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
            when(chunkWrapper2.getChunk()).thenReturn(chunk2);
            when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
            when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
            when(chunkWrapper2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
            when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
            when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.ZERO);
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(0)), eq(false))).thenReturn(lastBatchWrapper);

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.ONE))).thenReturn(
                    BasicBlockTrace.newBuilder()
                            .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis() - 61 * 60_000).build())
                            .build()
            );

            var lastHeader = mock(BlockHeader.class);
            var lastBlock = mock(BasicBlockTrace.class);
            when(lastBlock.getHeader()).thenReturn(lastHeader);
            when(lastBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader.getNumber()).thenReturn(31L);
            when(lastHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);

            var currHeader = mock(BlockHeader.class);
            var currBlock = mock(BasicBlockTrace.class);
            when(currHeader.getNumber()).thenReturn(32L);
            when(currHeader.getTimestamp()).thenReturn(System.currentTimeMillis());
            when(currHeader.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(currBlock.getTransactionsList()).thenReturn(
                    ListUtil.toList(
                            Transaction.newBuilder()
                                    .setType(TransactionType.TRANSACTION_TYPE_LEGACY)
                                    .setLegacyTx(
                                            LegacyTransaction.newBuilder()
                                                    .setTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                                    .setData(ByteString.copyFrom(RandomUtil.randomBytes(44941)))
                                    ).build()
                    )
            );
            when(currBlock.getHeader()).thenReturn(currHeader);
            when(currHeader.getGasUsed()).thenReturn(1000L);
            when(currBlock.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(currBlock.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());

            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));

            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.valueOf(3000_0000 - 1));
            // over chunk recommended gas sum
            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
            growingBatchChunkPointersF.setAccessible(true);
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

            var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
            growingBatchSerializedChunksF.setAccessible(true);
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);

            var latestSealedChunkF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "latestSealedChunk");
            latestSealedChunkF.setAccessible(true);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));

            clearInvocations(proverControllerClient, rollupRepository);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.valueOf(3000_0000 - 1000));
            // equals chunk recommended gas sum
            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));

            clearInvocations(proverControllerClient, rollupRepository);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR))).thenReturn(BigInteger.valueOf(3000_0000 - 2000));
            // equals chunk recommended gas sum
            rollupAggregator.process(currBlock);

            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            Assert.assertEquals(0, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());
            Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length);
            Assert.assertNull(latestSealedChunkF.get(growingBatchChunks));
        }
    }

    @Test
    @SneakyThrows
    public void testGetCommitBatchBlobSizeWithExistedChunksMethod() {
        var getGrowingBatchInfoM = ReflectUtil.getMethod(RollupAggregator.class, "getGrowingBatchInfo",
                ChunkWrapper.class, BasicBlockTrace.class, long.class, BatchVersionEnum.class);
        getGrowingBatchInfoM.setAccessible(true);

        // 1025284;
        var batchHeader154 = BatchHeader.deserializeFrom(HexUtil.decodeHex(FileUtil.readString("data/batch/batch-header-154", Charset.defaultCharset())));
        List<ChunkWrapper> chunks154 = JSON.parseArray(FileUtil.readString("data/batch/batch-154-chunks", Charset.defaultCharset())).stream()
                .map(x -> ChunkWrapper.decodeFromJson(x.toString())).toList();

        for (ChunkWrapper wrapper : chunks154) {
            when(rollupRepository.getChunk(eq(wrapper.getBatchIndex()), eq(wrapper.getChunkIndex()))).thenReturn(wrapper);
            wrapper.setBatchVersion(BatchVersionEnum.BATCH_V1);
        }

        var batch154 = Batch.createBatch(
                BatchVersionEnum.BATCH_V1,
                batchHeader154.getBatchIndex(),
                batchHeader154,
                batchHeader154.getL1MsgRollingHash(),
                chunks154.stream().map(ChunkWrapper::getChunk).toList(),
                null
        );
        var batchWrapper154 = new BatchWrapper();
        batchWrapper154.setBatch(batch154);
        when(rollupRepository.getBatch(eq(batchHeader154.getBatchIndex()))).thenReturn(batchWrapper154);

        var batchWrapper153 = new BatchWrapper();
        batchWrapper153.setEndBlockNumber(batch154.getStartBlockNumber().subtract(BigInteger.ONE));
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(153)), eq(false))).thenReturn(batchWrapper153);

        when(rollupRepository.getL2BlockTrace(eq(batchWrapper154.getStartBlockNumber()))).thenReturn(
                BasicBlockTrace.newBuilder()
                        .setHeader(BlockHeader.newBuilder().setTimestamp(System.currentTimeMillis()).build())
                        .build()
        );

        int expected = batch154.getEthBlobs().blobs().size();
        var header = mock(BlockHeader.class);
        var block = mock(BasicBlockTrace.class);
        when(block.getHeader()).thenReturn(header);
        when(header.getTimestamp()).thenReturn(System.currentTimeMillis());
        var info = (RollupAggregator.GrowingBatchInfo) getGrowingBatchInfoM.invoke(rollupAggregator, chunks154.get(13), block, header.getTimestamp(), BatchVersionEnum.BATCH_V1);

        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected, 1073741824) < 0);
        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected - 1, 1073741824) > 0);

        cleanUpGrowingBatchChunksMemCache();

        info = (RollupAggregator.GrowingBatchInfo) getGrowingBatchInfoM.invoke(rollupAggregator, chunks154.get(0), block, header.getTimestamp(), BatchVersionEnum.BATCH_V1);
        expected = Batch.createBatch(
                BatchVersionEnum.BATCH_V1, batchHeader154.getBatchIndex(), batchHeader154, batchHeader154.getL1MsgRollingHash(),
                ListUtil.toList(chunks154.get(0).getChunk()), null
        ).getEthBlobs().blobs().size();
        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected, 1073741824) < 0);
        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected + 1, 1073741824) < 0);
    }

    @Test
    @SneakyThrows
    public void testGrowingBatchChunksMemCache() {
        var mem = new GrowingBatchChunksMemCache(4);

        var rawChunk0 = Numeric.hexStringToByteArray("0x200000000900000000000000010000019AEDD9DD5200000000000F4240000000003B9ACA00000000000000000900000000000000020000019AEDD9E91100000000000F4240000000003B9ACA00000000000000000900000000000000030000019AEDD9F4D000000000000F4240000000003B9ACA00000000000000000900000000000000040000019AEDDA008F00000000000F4240000000003B9ACA00000000000000000900000000000000050000019AEDDA0C4E00000000000F4240000000003B9ACA00000000000000000900000000000000060000019AEDDA180D00000000000F4240000000003B9ACA00000000000000000900000000000000070000019AEDDA23CB00000000000F4240000000003B9ACA00000000000000000900000000000000080000019AEDDA2F8A00000000000F4240000000003B9ACA00000000000000000900000000000000090000019AEDDA3B4900000000000F4240000000003B9ACA000000000000000009000000000000000A0000019AEDDA470700000000000F4240000000003B9ACA000000000000000009000000000000000B0000019AEDDA52C600000000000F4240000000003B9ACA000000000000000009000000000000000C0000019AEDDA5E8500000000000F4240000000003B9ACA000000000000000009000000000000000D0000019AEDDA6A4400000000000F4240000000003B9ACA000000000000000009000000000000000E0000019AEDDA760200000000000F4240000000003B9ACA000000000000000009000000000000000F0000019AEDDA81C100000000000F4240000000003B9ACA00000000000000000900000000000000100000019AEDDA8D8000000000000F4240000000003B9ACA00000000000000000900000000000000110000019AEDDA993E00000000000F4240000000003B9ACA00000000000000000900000000000000120000019AEDDAA4FD00000000000F4240000000003B9ACA00000000000000000900000000000000130000019AEDDAB0BC00000000000F4240000000003B9ACA00000000000000000900000000000000140000019AEDDABC7B00000000000F4240000000003B9ACA00000000000000000900000000000000150000019AEDDAC83900000000000F4240000000003B9ACA00000000000000000900000000000000160000019AEDDAD3F800000000000F4240000000003B9ACA00000000000000000900000000000000170000019AEDDADFB600000000000F4240000000003B9ACA00000000000000000900000000000000180000019AEDDAEB7500000000000F4240000000003B9ACA00000000000000000900000000000000190000019AEDDAF73400000000000F4240000000003B9ACA000000000000000009000000000000001A0000019AEDDB02F300000000000F4240000000003B9ACA000000000000000009000000000000001B0000019AEDDB0EB100000000000F4240000000003B9ACA000000000000000009000000000000001C0000019AEDDB1A7000000000000F4240000000003B9ACA000000000000000009000000000000001D0000019AEDDB262F00000000000F4240000000003B9ACA000000000000000009000000000000001E0000019AEDDB31ED00000000000F4240000000003B9ACA000000000000000009000000000000001F0000019AEDDB3DAC00000000000F4240000000003B9ACA00000000000000000900000000000000200000019AEDDB496B00000000000F4240000000003B9ACA0000000000");
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize(eq(false))).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var rawChunk1 = RandomUtil.randomBytes(RandomUtil.randomInt(10, 100));
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize(eq(false))).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunkWrapper1.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var rawChunk1x = RandomUtil.randomBytes(RandomUtil.randomInt(10, 100));
        var chunk1x = mock(Chunk.class);
        when(chunk1x.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1x.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk1x.serialize(eq(false))).thenReturn(rawChunk1x);
        var chunkWrapper1x = mock(ChunkWrapper.class);
        when(chunkWrapper1x.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1x.getChunk()).thenReturn(chunk1x);
        when(chunkWrapper1x.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1x.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper1x.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        // add
        mem.add(chunkWrapper0);
        mem.add(chunkWrapper1);
        mem.add(chunkWrapper1x);

        // copy
        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        var list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(chunkWrapper0.getChunk().getEndBlockNumber(), list.get(0).endBlockNumber());
        Assert.assertEquals(chunkWrapper1x.getChunk().getEndBlockNumber(), list.get(1).endBlockNumber());
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(8 + rawChunk0.length + rawChunk1x.length, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // reset
        mem.reset();
        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(0, list.size());
        Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // checkAndFill
        // from zero
        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);

        mem.checkAndFill(chunkWrapper2, rollupRepository);
        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(chunkWrapper0.getChunk().getEndBlockNumber(), list.get(0).endBlockNumber());
        Assert.assertEquals(chunkWrapper1.getChunk().getEndBlockNumber(), list.get(1).endBlockNumber());
        Assert.assertEquals(8 + rawChunk0.length + rawChunk1.length, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // from non-zero
        mem.reset();
        mem.add(chunkWrapper0);

        mem.checkAndFill(chunkWrapper2, rollupRepository);
        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(chunkWrapper0.getChunk().getEndBlockNumber(), list.get(0).endBlockNumber());
        Assert.assertEquals(chunkWrapper1.getChunk().getEndBlockNumber(), list.get(1).endBlockNumber());
        Assert.assertEquals(8 + rawChunk0.length + rawChunk1.length, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // new batch
        var chunkNewBatch = mock(Chunk.class);
        when(chunkNewBatch.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunkNewBatch.getEndBlockNumber()).thenReturn(BigInteger.valueOf(40));
        var chunkWrapperNewBatch = mock(ChunkWrapper.class);
        when(chunkWrapperNewBatch.getChunkIndex()).thenReturn(0L);
        when(chunkWrapperNewBatch.getChunk()).thenReturn(chunk2);
        when(chunkWrapperNewBatch.getBatchIndex()).thenReturn(BigInteger.valueOf(2));
        when(chunkWrapperNewBatch.getEndBlockNumber()).thenReturn(BigInteger.valueOf(40));
        when(chunkWrapperNewBatch.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        mem.checkAndFill(chunkWrapperNewBatch, rollupRepository);
        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(0, list.size());
        Assert.assertEquals(0, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // reprocess the block and constructing chunk already added
        mem.reset();
        mem.add(chunkWrapper0);
        mem.add(chunkWrapper1);

        mem.checkAndFill(chunkWrapper1x, rollupRepository);
        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(4 + rawChunk0.length, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);

        // getByIndex
        mem.reset();
        mem.add(chunkWrapper0);
        Assert.assertNull(mem.getByIndex(1));
        Assert.assertEquals(chunkWrapper0, mem.getByIndex(0));
        mem.add(chunkWrapper1);
        Assert.assertNull(mem.getByIndex(2));
        Assert.assertNotEquals(chunkWrapper0, mem.getByIndex(0));
        Assert.assertArrayEquals(chunkWrapper0.getChunk().serialize(false), mem.getByIndex(0).serializeChunk());
        Assert.assertEquals(chunkWrapper1, mem.getByIndex(1));

        // discontinuous blocks between chunk
        mem.reset();
        mem.add(chunkWrapper0);

        var rawChunk0y = RandomUtil.randomBytes(RandomUtil.randomInt(10, 100));
        var chunk0y = mock(Chunk.class);
        when(chunk0y.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0y.getEndBlockNumber()).thenReturn(BigInteger.valueOf(9));
        when(chunk0y.serialize(eq(false))).thenReturn(rawChunk0y);
        var chunkWrapper0y = mock(ChunkWrapper.class);
        when(chunkWrapper0y.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0y.getChunk()).thenReturn(chunk0y);
        when(chunkWrapper0y.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0y.getEndBlockNumber()).thenReturn(BigInteger.valueOf(9));
        when(chunkWrapper0y.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);

        var rawChunk1y = RandomUtil.randomBytes(RandomUtil.randomInt(10, 100));
        var chunk1y = mock(Chunk.class);
        when(chunk1y.getStartBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk1y.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1y.serialize(eq(false))).thenReturn(rawChunk1y);
        var chunkWrapper1y = mock(ChunkWrapper.class);
        when(chunkWrapper1y.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1y.getChunk()).thenReturn(chunk1y);
        when(chunkWrapper1y.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1y.getStartBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper1y.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V1);
        when(chunkWrapper1y.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1y);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0y);

        mem.checkAndFill(chunkWrapper2, rollupRepository);

        list = (List<ChunkPointer>) growingBatchChunkPointersF.get(mem);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(BigInteger.valueOf(9), list.get(0).endBlockNumber());
        Assert.assertEquals(BigInteger.valueOf(20), list.get(1).endBlockNumber());
        Assert.assertEquals(8 + rawChunk0y.length + rawChunk1y.length, ((byte[]) growingBatchSerializedChunksF.get(mem)).length);
    }

    @SneakyThrows
    private void cleanUpGrowingBatchChunksMemCache() {
        var growingBatchChunksField = ReflectUtil.getField(RollupAggregator.class, "growingBatchChunks");
        growingBatchChunksField.setAccessible(true);
        var mem = growingBatchChunksField.get(rollupAggregator);

        var m = ReflectUtil.getMethod(mem.getClass(), "reset");
        m.setAccessible(true);
        m.invoke(mem);
    }

    private Chunk mockChunk(BigInteger startNumber, int blockSize, int txBytesSize) {
        var result = new Chunk();
        result.setNumBlocks(blockSize);
        result.setL2Transactions(RandomUtil.randomBytes(txBytesSize));
        result.setBlocks(new ArrayList<>());
        IntStream.range(startNumber.intValue(), startNumber.intValue() + blockSize).forEach(i -> {
            var block = new BlockContext();
            block.setBlockNumber(BigInteger.valueOf(i));
            block.setTimestamp(System.currentTimeMillis());
            block.setBaseFee(BigInteger.ZERO);
            block.setGasLimit(BigInteger.ZERO);
            result.getBlocks().add(block);
        });
        return result;
    }
}
