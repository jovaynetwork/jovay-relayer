package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

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
import com.alipay.antchain.l2.relayer.commons.l2basic.Batch;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
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
import com.google.protobuf.ByteString;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

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
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(10_000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000L);
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
        var rawChunk0 = RandomUtil.randomBytes(100 * 1024);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(100 * 1024);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var rawChunk2 = RandomUtil.randomBytes(500 * 1024);
        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(rawChunk2);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

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
        when(currBlock.getZkCycles()).thenReturn(940_000L);
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currHeader.getNumber()).thenReturn(32L);
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

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31L)), eq(BigInteger.valueOf(32L)));
        verify(proverControllerClient, never()).proveBatch(notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).createBatchProveRequest(notNull(), notNull());
        verify(rollupRepository, never()).saveBatch(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.valueOf(4)));

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(
                16 + rawChunk0.length + rawChunk1.length + rawChunk2.length +
                            new Chunk(ListUtil.toList(lastBlock, currBlock), rollupConfig.getMaxTxsInChunks()).serialize().length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );

        growingBatchChunks.reset();
    }

    @Test
    @SneakyThrows
    public void testProcessDiscontinueBlocksBetweenBatches() {
        var rawChunk0 = RandomUtil.randomBytes(100 * 1024);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(100 * 1024);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var rawChunk2 = RandomUtil.randomBytes(500 * 1024);
        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(rawChunk2);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

        var rawChunk3 = RandomUtil.randomBytes(41);
        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(rawChunk3);
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

        Assert.assertThrows(InvalidBatchException.class, () -> rollupAggregator.process(currBlock));

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31L)), eq(BigInteger.valueOf(31L)));
        verify(proverControllerClient, never()).proveBatch(any());

        var growingBatchChunkPointersF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchChunkPointers");
        growingBatchChunkPointersF.setAccessible(true);
        Assert.assertEquals(4, ((List<ChunkPointer>) growingBatchChunkPointersF.get(growingBatchChunks)).size());

        var growingBatchSerializedChunksF = ReflectUtil.getField(GrowingBatchChunksMemCache.class, "growingBatchSerializedChunks");
        growingBatchSerializedChunksF.setAccessible(true);
        Assert.assertEquals(
                16 + rawChunk0.length + rawChunk1.length + rawChunk2.length +
                new Chunk(ListUtil.toList(lastBlock), rollupConfig.getMaxTxsInChunks()).serialize().length,
                ((byte[]) growingBatchSerializedChunksF.get(growingBatchChunks)).length
        );

        growingBatchChunks.reset();
    }

    @Test
    @SneakyThrows
    public void testProcessWithPreviousBatchMemCache() {
        var rawChunk0 = RandomUtil.randomBytes(100 * 1024);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(1000 * 1024);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

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
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

        var chunkOfNewBatch = mock(Chunk.class);
        when(chunkOfNewBatch.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunkOfNewBatch.getEndBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunkOfNewBatch.serialize()).thenReturn(RandomUtil.randomBytes(41));
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
    }

    @Test
    @SneakyThrows
    public void testProcessBlockBelongsToNextChunk() {
        var rawChunk0 = RandomUtil.randomBytes(100 * 1024);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(100 * 1024);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var rawChunk2 = RandomUtil.randomBytes(500 * 1024);
        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(rawChunk2);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

        var rawChunk3 = RandomUtil.randomBytes(41);
        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(rawChunk3);
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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
        when(currHeader.getNumber()).thenReturn(32L);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

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
    }

    @Test
    @SneakyThrows
    public void testOverOrEqualMaxChunksMemoryUsed() {
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));

        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));

        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(RandomUtil.randomBytes(500 * 1024));
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));

        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(RandomUtil.randomBytes(41));
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

        rollupAggregator.process(currBlock);

        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(32)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.valueOf(1)), eq(3L), eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)));
        verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
        verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));

        Assert.assertEquals(0, growingBatchChunks.copy().size());

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
                                                .setData(ByteString.copyFrom(new byte[1380241 - 6]))
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

        Assert.assertEquals(0, growingBatchChunks.copy().size());
    }

    @Test
    @SneakyThrows
    public void testProcessCurrChunkAbandoned() {
        var rawChunk0 = RandomUtil.randomBytes(100 * 1024);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(100 * 1024);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var rawChunk2 = RandomUtil.randomBytes(500 * 1024);
        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(rawChunk2);
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

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
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

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
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimit() {
        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(RandomUtil.randomBytes(500 * 1024));
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(RandomUtil.randomBytes(41));
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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
                                                .setData(ByteString.copyFrom(RandomUtil.randomBytes(44981)))
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
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

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
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimit_GrowingMemLoadedWithGap() {

        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunkWrapper0.getStartBlockNumber()).thenReturn(BigInteger.ONE);

        // loaded and with gap
        growingBatchChunks.add(chunkWrapper0);

        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));

        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(RandomUtil.randomBytes(500 * 1024));
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunkWrapper2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));

        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(RandomUtil.randomBytes(41));
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunkWrapper3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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
                                                .setData(ByteString.copyFrom(RandomUtil.randomBytes(44981)))
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
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ZERO);

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
    }

    @Test
    @SneakyThrows
    public void testProcessCurrBlockEqualBlobLimitAndOverChunkLimit() {
        var forkInfo = mock(ForkInfo.class);
        when(forkInfo.getBatchVersion()).thenReturn(BatchVersionEnum.BATCH_V0);
        when(rollupSpecs.getFork(anyLong())).thenReturn(forkInfo);

        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(RandomUtil.randomBytes(100 * 1024));
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));

        var chunk2 = mock(Chunk.class);
        when(chunk2.getStartBlockNumber()).thenReturn(BigInteger.valueOf(21));
        when(chunk2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk2.serialize()).thenReturn(RandomUtil.randomBytes(500 * 1024));
        var chunkWrapper2 = mock(ChunkWrapper.class);
        when(chunkWrapper2.getChunkIndex()).thenReturn(2L);
        when(chunkWrapper2.getChunk()).thenReturn(chunk2);
        when(chunkWrapper2.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper2.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

        var chunk3 = mock(Chunk.class);
        when(chunk3.getStartBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));
        when(chunk3.serialize()).thenReturn(RandomUtil.randomBytes(41));
        var chunkWrapper3 = mock(ChunkWrapper.class);
        when(chunkWrapper3.getChunkIndex()).thenReturn(3L);
        when(chunkWrapper3.getChunk()).thenReturn(chunk3);
        when(chunkWrapper3.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper3.getEndBlockNumber()).thenReturn(BigInteger.valueOf(31));

        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(0L))).thenReturn(chunkWrapper0);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(1L))).thenReturn(chunkWrapper1);
        when(rollupRepository.getChunk(eq(BigInteger.ONE), eq(2L))).thenReturn(chunkWrapper2);
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(ListUtil.toList(chunkWrapper0, chunkWrapper1, chunkWrapper2, chunkWrapper3));

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
                                                .setData(ByteString.copyFrom(RandomUtil.randomBytes(44941)))
                                ).build()
                )
        );
        when(currBlock.getHeader()).thenReturn(currHeader);
        when(currBlock.getZkCycles()).thenReturn(940_000L);

        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(31)))).thenReturn(lastBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(31)))).thenReturn(ListUtil.toList(lastBlock));
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(32)))).thenReturn(currBlock);
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(31)), eq(BigInteger.valueOf(32)))).thenReturn(ListUtil.toList(lastBlock, currBlock));

        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK))).thenReturn(BigInteger.valueOf(3));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH))).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT))).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR))).thenReturn(BigInteger.ONE);

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
    }

    @Test
    @SneakyThrows
    public void testGetCommitBatchBlobSizeWithExistedChunksMethod() {
        var getGrowingBatchInfoM = ReflectUtil.getMethod(RollupAggregator.class, "getGrowingBatchInfo", ChunkWrapper.class);
        getGrowingBatchInfoM.setAccessible(true);

        // 1025284;
        var batchHeader154 = BatchHeader.deserializeFrom(HexUtil.decodeHex(FileUtil.readString("data/batch/batch-header-154", Charset.defaultCharset())));
        List<ChunkWrapper> chunks154 = JSON.parseArray(FileUtil.readString("data/batch/batch-154-chunks", Charset.defaultCharset())).stream()
                .map(x -> ChunkWrapper.decodeFromJson(x.toString())).toList();

        for (ChunkWrapper wrapper : chunks154) {
            when(rollupRepository.getChunk(eq(wrapper.getBatchIndex()), eq(wrapper.getChunkIndex()))).thenReturn(wrapper);
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
        var info = (RollupAggregator.GrowingBatchInfo) getGrowingBatchInfoM.invoke(rollupAggregator, chunks154.get(13));

        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected, 1073741824) < 0);
        Assert.assertTrue(info.getOverBatchBlobLimitFlag(expected - 1, 1073741824) > 0);

        cleanUpGrowingBatchChunksMemCache();

        info = (RollupAggregator.GrowingBatchInfo) getGrowingBatchInfoM.invoke(rollupAggregator, chunks154.get(0));
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

        var rawChunk0 = RandomUtil.randomBytes(100);
        var chunk0 = mock(Chunk.class);
        when(chunk0.getStartBlockNumber()).thenReturn(BigInteger.ONE);
        when(chunk0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));
        when(chunk0.serialize()).thenReturn(rawChunk0);
        var chunkWrapper0 = mock(ChunkWrapper.class);
        when(chunkWrapper0.getChunkIndex()).thenReturn(0L);
        when(chunkWrapper0.getChunk()).thenReturn(chunk0);
        when(chunkWrapper0.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper0.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10));

        var rawChunk1 = RandomUtil.randomBytes(100);
        var chunk1 = mock(Chunk.class);
        when(chunk1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunk1.serialize()).thenReturn(rawChunk1);
        var chunkWrapper1 = mock(ChunkWrapper.class);
        when(chunkWrapper1.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1.getChunk()).thenReturn(chunk1);
        when(chunkWrapper1.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1.getEndBlockNumber()).thenReturn(BigInteger.valueOf(20));
        when(chunkWrapper1.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));

        var rawChunk1x = RandomUtil.randomBytes(100);
        var chunk1x = mock(Chunk.class);
        when(chunk1x.getStartBlockNumber()).thenReturn(BigInteger.valueOf(11));
        when(chunk1x.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));
        when(chunk1x.serialize()).thenReturn(rawChunk1x);
        var chunkWrapper1x = mock(ChunkWrapper.class);
        when(chunkWrapper1x.getChunkIndex()).thenReturn(1L);
        when(chunkWrapper1x.getChunk()).thenReturn(chunk1x);
        when(chunkWrapper1x.getBatchIndex()).thenReturn(BigInteger.valueOf(1));
        when(chunkWrapper1x.getEndBlockNumber()).thenReturn(BigInteger.valueOf(30));

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
}
