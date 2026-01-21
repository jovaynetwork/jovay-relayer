package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecs;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.layer2.IRollupAggregator;
import com.alipay.antchain.l2.relayer.core.layer2.RollupAggregator;
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
import org.slf4j.LoggerFactory;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RollupServiceTest extends TestBase {

    @Resource
    private IRollupService rollupService;

    @Resource
    private IRollupAggregator rollupAggregator;

    @MockitoBean(reset = MockReset.BEFORE)
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
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000L);
        when(rollupConfig.getMaxTimeIntervalBetweenBatches()).thenReturn(3600_000L);
        when(rollupConfig.getZkVerificationStartBatch()).thenReturn(BigInteger.valueOf(0));
        when(rollupConfig.getMaxChunksMemoryUsed()).thenReturn(1073741824);

        when(l1Client.queryTxCount(notNull(), notNull())).thenReturn(BigInteger.ONE);
        var blobTxManager = mock(BaseRawTransactionManager.class);
        when(l1Client.getBlobPoolTxManager()).thenReturn(blobTxManager);
        when(l1Client.getLegacyPoolTxManager()).thenReturn(blobTxManager);
        when(blobTxManager.getAddress()).thenReturn("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981");

        cleanUpGrowingBatchChunksMemCache();
    }

    @Test
    public void testSetAnchorBatchWithHeader() {
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(false);
        rollupService.setAnchorBatch(ZERO_BATCH_HEADER, null);

        verify(rollupRepository).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ZERO));
        verify(rollupRepository).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.ZERO));
        verify(rollupRepository).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH), eq(BigInteger.ONE));
        verify(rollupRepository).savePartialBatchHeader(notNull(), eq(0L), eq(0L), notNull(), notNull());

        clearInvocations(rollupRepository);
        when(systemConfigRepository.isAnchorBatchSet()).thenReturn(true);
        Assert.assertThrows(RuntimeException.class, () -> rollupService.setAnchorBatch(ZERO_BATCH_HEADER, null));
        verify(rollupRepository, never()).updateRollupNumberRecord(any(), any(), any());
    }

    @Test
    public void testSetAnchorBatchWithIndex() {
        Assert.assertThrows(RuntimeException.class, () -> rollupService.setAnchorBatch(BigInteger.ZERO));
    }

    @Test
    public void testPollL2BlocksOnlyBlock() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000_000L);
        rollupService.pollL2Blocks();
        verify(rollupRepository).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(proverControllerClient).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, never()).notifyChunk(notNull(), anyLong(), notNull(), notNull());
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_OverZkLimit() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithZkCycles = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addAllTransactions(BASIC_BLOCK_TRACE2.getTransactionsList())
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(2).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithZkCycles
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithZkCycles)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(BASIC_BLOCK_TRACE1.getZkCycles() + 1);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(1L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.ONE));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_OverMaxTxInChunk() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2With2Tx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2With2Tx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2With2Tx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L * 2);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(2);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(8)));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(1L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.ONE));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_OverMaxCallDataSizeInChunk() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L * 2);
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(6L);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(2)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(2)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(1L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.ONE));
    }

    @Test
    public void testPollL2BlocksWithNewBatchBorn_OverMaxTimeIntervalBetweenBatches() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getChunks(eq(BigInteger.ONE))).thenReturn(
                ListUtil.toList(
                        new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx), 4)
                )
        );
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L * 2);
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(8L);
        when(rollupConfig.getMaxTimeIntervalBetweenBatches()).thenReturn(3008L);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.valueOf(trace2WithTx.getTransactionsCount())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0));
        verify(rollupRepository).saveBatch(argThat(argument -> argument.getBatchIndex().equals(BigInteger.ONE)));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
        verify(proverControllerClient, times(1)).proveBatch(notNull());
    }

    @Test
    @SneakyThrows
    public void testPollL2BlocksWithNewChunkBorn_OverBatchBlobsLimit() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                    .thenReturn(BigInteger.ZERO, BigInteger.ONE);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                    .thenReturn(BigInteger.ZERO);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
            int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
            when(rollupRepository.getChunks(eq(BigInteger.ONE)))
                    .thenReturn(ListUtil.toList(
                            new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(BASIC_BLOCK_TRACE1), maxTxsInChunks)
                    ));

            when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                    BASIC_BLOCK_TRACE1
            );
            BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                    .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                    .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                    .addTransactions(Transaction.newBuilder().setLegacyTx(
                            LegacyTransaction.newBuilder().setData(ByteString.copyFrom(new byte[EthBlobs.BLOB_SIZE * 4]))
                    )).addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                    .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                    .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                    .setZkCycles(0).build();
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                    trace2WithTx
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                    ListUtil.toList(BASIC_BLOCK_TRACE1)
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                    ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                    ListUtil.toList(BASIC_BLOCK_TRACE1)
            );
            when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
            when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
            when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L * 2);

            rollupService.pollL2Blocks();
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            verify(rollupRepository, times(2)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4 * EthBlobs.BLOB_SIZE)));
            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(2)), eq(0L), eq(BigInteger.valueOf(2)));
            verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.ONE));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE) && argument.getBatch().getEndBlockNumber().equals(BigInteger.valueOf(1))));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH), eq(BigInteger.valueOf(2)));
        }
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_OverBatchBlobsLimit_NextChunkOnlyHasOneBlock() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);

            List<ChunkWrapper> chunks154 = JSON.parseArray(FileUtil.readString("data/batch/batch-154-chunks", Charset.defaultCharset())).stream()
                    .map(x -> ChunkWrapper.decodeFromJson(x.toString())).collect(Collectors.toList());
            chunks154.get(0).getChunk().setL2Transactions(new byte[0]);

            when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(11022));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                    .thenReturn(BigInteger.valueOf(11021));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                    .thenReturn(BigInteger.valueOf(13));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH)))
                    .thenReturn(BigInteger.valueOf(154));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                    .thenReturn(BigInteger.ZERO);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                    .thenReturn(BigInteger.ZERO);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                    .thenReturn(BigInteger.ZERO);

            for (ChunkWrapper wrapper : chunks154) {
                when(rollupRepository.getChunk(eq(wrapper.getBatchIndex()), eq(wrapper.getChunkIndex()))).thenReturn(wrapper);
            }

            var tempBasicTrace11022 = BasicBlockTrace.newBuilder()
                    .setChainId(BASIC_BLOCK_TRACE_11022.getChainId())
                    .setHeader(BASIC_BLOCK_TRACE_11022.getHeader())
                    .addTransactions(
                            Transaction.newBuilder().setLegacyTx(
                                    LegacyTransaction.newBuilder().setData(ByteString.copyFrom(new byte[EthBlobs.BLOB_SIZE]))
                            )
                    ).addAllGroups(BASIC_BLOCK_TRACE_11022.getGroupsList())
                    .setStorageTrace(BASIC_BLOCK_TRACE_11022.getStorageTrace())
                    .setStartL1QueueIndex(BASIC_BLOCK_TRACE_11022.getStartL1QueueIndex())
                    .setZkCycles(0).build();

            when(rollupRepository.getChunks(eq(BigInteger.valueOf(154))))
                    .thenReturn(ListUtil.sub(chunks154, 0, 13));
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                    tempBasicTrace11022
            );
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(11022)))).thenReturn(
                    tempBasicTrace11022
            );
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(11021)))).thenReturn(
                    BASIC_BLOCK_TRACE_11021
            );

            var lastHeader10910 = mock(BlockHeader.class);
            var lastBlock10910 = mock(BasicBlockTrace.class);
            when(lastBlock10910.getHeader()).thenReturn(lastHeader10910);
            when(lastBlock10910.getL1MsgRollingHash()).thenReturn(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastBlock10910.getL2MsgRoot()).thenReturn(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))).build());
            when(lastHeader10910.getNumber()).thenReturn(10910L);
            when(lastHeader10910.getStateRoot()).thenReturn(ByteString.copyFrom(RandomUtil.randomBytes(32)));
            when(lastHeader10910.getTimestamp()).thenReturn(System.currentTimeMillis() - 10 * 60_000L);
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(10910)))).thenReturn(lastBlock10910);

            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(11021)), eq(BigInteger.valueOf(11021)))).thenReturn(
                    ListUtil.toList(BASIC_BLOCK_TRACE_11021)
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(11022)), eq(BigInteger.valueOf(11022)))).thenReturn(
                    ListUtil.toList(tempBasicTrace11022)
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(11021)), eq(BigInteger.valueOf(11022)))).thenReturn(
                    ListUtil.toList(BASIC_BLOCK_TRACE_11021, tempBasicTrace11022)
            );

            var lastBatchHeader = mock(BatchHeader.class);
            when(lastBatchHeader.getHash()).thenReturn(RandomUtil.randomBytes(32));
            var lastBatchWrapper = mock(BatchWrapper.class);
            when(lastBatchWrapper.getEndBlockNumber()).thenReturn(BigInteger.valueOf(10910 - 1));
            when(lastBatchWrapper.getBatchHeader()).thenReturn(lastBatchHeader);

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(153)), eq(false))).thenReturn(lastBatchWrapper);
            when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(8000000L);

            Assert.assertThrows(InvalidBatchException.class, () -> rollupService.pollL2Blocks());

            clearInvocations(rollupRepository);
            clearInvocations(proverControllerClient);
            when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(8);

            rollupService.pollL2Blocks();
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(11022)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.valueOf(155)), eq(0L), eq(BigInteger.valueOf(11022)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.valueOf(154))));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.valueOf(154)), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.valueOf(154)), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.valueOf(154)) && argument.getBatch().getEndBlockNumber().equals(BigInteger.valueOf(11021))));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH), eq(BigInteger.valueOf(155)));
        }
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_ZkEquals() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(1).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(BASIC_BLOCK_TRACE1.getZkCycles() + 1);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_TxCountEquals() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(2);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_CallDataSizeEquals() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L);
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(8L);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_BlockNumSatisfied() {
        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                BASIC_BLOCK_TRACE1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(BASIC_BLOCK_TRACE1.getTransactionsList().get(0))
                .addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(2L);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_BatchBlobsLimitEquals_BatchV1() {
        try (var mockedZstd = mockStatic(Zstd.class)) {
            mockedZstd.when(() -> Zstd.compress(any()))
                    .then(invocationOnMock -> invocationOnMock.getArguments()[0]);
            when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                    .thenReturn(BigInteger.ZERO, BigInteger.ONE);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                    .thenReturn(BigInteger.ZERO);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                    .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
            when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
            when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
            var trace1 = BasicBlockTrace.newBuilder()
                    .setChainId(BASIC_BLOCK_TRACE1.getChainId())
                    .setHeader(BASIC_BLOCK_TRACE1.getHeader())
                    .addTransactions(Transaction.newBuilder().setLegacyTx(
                            LegacyTransaction.newBuilder().setData(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData())
                    )).addAllGroups(BASIC_BLOCK_TRACE1.getGroupsList())
                    .setStorageTrace(BASIC_BLOCK_TRACE1.getStorageTrace())
                    .setStartL1QueueIndex(BASIC_BLOCK_TRACE1.getStartL1QueueIndex())
                    .setZkCycles(BASIC_BLOCK_TRACE1.getZkCycles()).build();
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                    trace1
            );
            BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                    .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                    .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                    .addTransactions(Transaction.newBuilder().setLegacyTx(
                            LegacyTransaction.newBuilder().setData(ByteString.copyFrom(new byte[507787 - 10]))
                    )).addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                    .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                    .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                    .setZkCycles(0).build();
            when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                    trace2WithTx
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                    ListUtil.toList(trace1, trace2WithTx)
            );
            when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                    ListUtil.toList(trace1)
            );
            int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
            when(rollupRepository.getChunks(eq(BigInteger.ONE)))
                    .thenReturn(ListUtil.toList(
                            new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(trace1, trace2WithTx), maxTxsInChunks)
                    ));
            when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
            when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L);

            rollupService.pollL2Blocks();
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(trace1.getZkCycles())));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
            verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
            verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
            verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
            verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE)));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
            verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
            verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE) && argument.getBatch().getEndBlockNumber().equals(BigInteger.valueOf(2))));
            verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH), eq(BigInteger.valueOf(2)));
        }
    }

    @Test
    public void testPollL2BlocksWithNewChunkBorn_BatchBlobsLimitEquals_BatchV0() {
        when(getRollupSpecs().getFork(anyLong())).then(invocationOnMock -> {
            var curr = (long) invocationOnMock.getArguments()[0];
            RollupSpecs specs = JSON.parseObject(FileUtil.readBytes("specs/testnet.json"), RollupSpecs.class);
            return specs.getFork(curr);
        });

        when(l2Client.queryLatestBlockNumber(any())).thenReturn(BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED)))
                .thenReturn(BigInteger.ZERO, BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK)))
                .thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactionsCount()));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData().toByteArray().length));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR)))
                .thenReturn(BigInteger.ZERO, BigInteger.valueOf(BASIC_BLOCK_TRACE1.getZkCycles()));
        when(rollupRepository.getBatch(eq(BigInteger.ZERO), eq(false))).thenReturn(ZERO_BATCH_WRAPPER);
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        var trace1 = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE1.getChainId())
                .setHeader(BASIC_BLOCK_TRACE1.getHeader())
                .addTransactions(Transaction.newBuilder().setLegacyTx(
                        LegacyTransaction.newBuilder().setData(BASIC_BLOCK_TRACE1.getTransactions(0).getLegacyTx().getData())
                )).addAllGroups(BASIC_BLOCK_TRACE1.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE1.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE1.getStartL1QueueIndex())
                .setZkCycles(BASIC_BLOCK_TRACE1.getZkCycles()).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(1)))).thenReturn(
                trace1
        );
        BasicBlockTrace trace2WithTx = BasicBlockTrace.newBuilder()
                .setChainId(BASIC_BLOCK_TRACE2.getChainId())
                .setHeader(BASIC_BLOCK_TRACE2.getHeader())
                .addTransactions(Transaction.newBuilder().setLegacyTx(
                        LegacyTransaction.newBuilder().setData(ByteString.copyFrom(new byte[507787 - 6]))
                )).addAllGroups(BASIC_BLOCK_TRACE2.getGroupsList())
                .setStorageTrace(BASIC_BLOCK_TRACE2.getStorageTrace())
                .setStartL1QueueIndex(BASIC_BLOCK_TRACE2.getStartL1QueueIndex())
                .setZkCycles(0).build();
        when(rollupRepository.getL2BlockTrace(eq(BigInteger.valueOf(2)))).thenReturn(
                trace2WithTx
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(2)))).thenReturn(
                ListUtil.toList(trace1, trace2WithTx)
        );
        when(rollupRepository.getL2BlockTraceRange(eq(BigInteger.valueOf(1)), eq(BigInteger.valueOf(1)))).thenReturn(
                ListUtil.toList(trace1)
        );
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getChunks(eq(BigInteger.ONE)))
                .thenReturn(ListUtil.toList(
                        new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(trace1, trace2WithTx), maxTxsInChunks)
                ));
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(31140325L);

        rollupService.pollL2Blocks();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.valueOf(trace1.getZkCycles())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.ZK_ROWS_ACCUMULATOR), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_TX_COUNT), eq(BigInteger.ZERO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.valueOf(4)));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_CALL_DATA_COUNT), eq(BigInteger.ZERO));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE));
        verify(proverControllerClient, times(1)).notifyBlock(eq(BigInteger.ONE), eq(0L), eq(BigInteger.valueOf(2)));
        verify(rollupRepository).saveChunk(argThat(argument -> argument.getChunkIndex() == 0 && argument.getChunk().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(proverControllerClient, times(1)).notifyChunk(eq(BigInteger.ONE), eq(0L), eq(BigInteger.ONE), eq(BigInteger.valueOf(2)));
        verify(proverControllerClient, times(1)).proveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE)));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF));
        verify(rollupRepository, times(1)).createBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF));
        verify(rollupRepository, times(1)).saveBatch(argThat(argument -> argument.getBatch().getBatchHeader().getBatchIndex().equals(BigInteger.ONE) && argument.getBatch().getEndBlockNumber().equals(BigInteger.valueOf(2))));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH), eq(BigInteger.valueOf(2)));
    }

    @Test
    public void testProveL2Batch() {
        when(rollupRepository.peekPendingBatchProveRequest(anyInt(), notNull()))
                .thenReturn(ListUtil.toList(
                        BatchProveRequestDO.builder()
                                .proveType(ProveTypeEnum.TEE_PROOF)
                                .batchIndex(BigInteger.ONE)
                                .state(BatchProveRequestStateEnum.PENDING)
                                .build()
                ));
        when(proverControllerClient.getBatchProof(eq(ProveTypeEnum.TEE_PROOF), eq(BigInteger.ONE))).thenReturn("a".getBytes());

        rollupService.proveTeeL2Batch();

        verify(rollupRepository).saveBatchProofAndUpdateReqState(notNull(), notNull(), argThat(argument -> new String(argument).equals("a")));
    }

    @Test
    public void testCommitL2BatchNormalCase() {
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ZERO);
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        var batchOne = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        var batchTwo = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.TWO, batchOne, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.TWO, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        var batchThree = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(3), batchTwo, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(3), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(batchOne);
        when(rollupRepository.getBatch(eq(BigInteger.TWO)))
                .thenReturn(batchTwo);
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(3))))
                .thenReturn(batchThree);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ONE))).thenReturn(batchOne.getBatchHeader());
        when(rollupRepository.getBatchHeader(eq(BigInteger.TWO))).thenReturn(batchTwo.getBatchHeader());

        when(l1Client.commitBatch(notNull(), notNull())).thenReturn(transactionInfo);
        rollupService.commitL2Batch();

        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.TWO));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.valueOf(3)));
        verify(rollupRepository, times(3)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
    }

    @Test
    public void testCommitL2BatchAlreadyCommitOnce() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        Assert.assertThrows(CommitL2BatchException.class, () -> rollupService.commitL2Batch());

        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(ReliableTransactionDO.builder().build(), null);
        rollupService.commitL2Batch();

        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,         BigInteger.ONE, ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(),
                        0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(l1Client.commitBatch(notNull(), notNull())).thenReturn(transactionInfo);

        rollupService.commitL2Batch();

        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
    }

    @Test
    public void testCommitL2BatchMissedHistoricalCommit() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.valueOf(100));
        Assert.assertThrows(CommitL2BatchException.class, () -> rollupService.commitL2Batch());

        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_FAILED).build());
        rollupService.commitL2Batch();
        verify(rollupRepository, times(0)).updateRollupNumberRecord(notNull(), notNull(), notNull());

        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_SUCCESS).build());
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        var batchOne = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        var batchTwo = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.TWO, batchOne, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.TWO, 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(batchOne);
        when(rollupRepository.getBatch(eq(BigInteger.TWO)))
                .thenReturn(batchTwo);
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(l1Client.commitBatch(notNull(), notNull())).thenReturn(transactionInfo);

        Assert.assertThrows(CommitL2BatchException.class, () -> rollupService.commitL2Batch());

        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
    }

    @Test
    public void testCommitL2BatchLocalIsLate() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.valueOf(1));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.valueOf(0));
        rollupService.commitL2Batch();

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(2))))
                .thenReturn(BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,         BigInteger.valueOf(2), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(),
                        0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(2), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(rollupRepository.getBatchHeader(eq(BigInteger.ONE))).thenReturn(ZERO_BATCH_HEADER);
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        when(l1Client.commitBatch(notNull(), notNull())).thenReturn(transactionInfo);

        rollupService.commitL2Batch();

        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED), eq(BigInteger.valueOf(2)));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
    }

    @Test
    public void testCommitL2TeeProof() {
        Assert.assertThrows(CommitL2BatchTeeProofException.class, () -> rollupService.commitL2TeeProof());

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.valueOf(0));
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.valueOf(0), BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED))).thenReturn(BigInteger.ZERO);
        rollupService.commitL2TeeProof();

        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("a".getBytes()).state(BatchProveRequestStateEnum.PROVE_READY).build()
        );
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        var batchOne = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        var batchTwo = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), batchOne, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(2), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(1))))
                .thenReturn(batchOne);
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(2))))
                .thenReturn(batchTwo);
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        when(l1Client.verifyBatch(notNull(), notNull())).thenReturn(transactionInfo);
        rollupService.commitL2TeeProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.TWO), eq(ProveTypeEnum.TEE_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(2)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));

        clearInvocations(rollupRepository);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.TWO), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("b".getBytes()).state(BatchProveRequestStateEnum.COMMITTED).build()
        );
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.TWO), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)))
                .thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_SUCCESS).build());
        rollupService.commitL2TeeProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.TWO), eq(ProveTypeEnum.TEE_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));

        clearInvocations(rollupRepository);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.TWO), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)))
                .thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_FAILED).build());
        rollupService.commitL2TeeProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
    }

    @Test
    public void testCommitL2ZkProof() {
        Assert.assertThrows(CommitL2BatchZkProofException.class, () -> rollupService.commitL2ZkProof());

        when(l1Client.lastZkVerifiedBatch()).thenReturn(BigInteger.valueOf(0));
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.valueOf(0), BigInteger.valueOf(2));
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED))).thenReturn(BigInteger.ZERO);
        rollupService.commitL2ZkProof();

        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("a".getBytes()).state(BatchProveRequestStateEnum.PROVE_READY).build()
        );
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        var batchOne = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        var batchTwo = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), batchOne, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(2), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(1))))
                .thenReturn(batchOne);
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(2))))
                .thenReturn(batchTwo);
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        when(l1Client.verifyBatch(notNull(), notNull())).thenReturn(transactionInfo);
        rollupService.commitL2ZkProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.TWO), eq(ProveTypeEnum.ZK_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(2)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));

        clearInvocations(rollupRepository);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.TWO), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("b".getBytes()).state(BatchProveRequestStateEnum.COMMITTED).build()
        );
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.TWO), eq(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX)))
                .thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_SUCCESS).build());
        rollupService.commitL2ZkProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.TWO), eq(ProveTypeEnum.ZK_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));

        clearInvocations(rollupRepository);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.TWO), eq(TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX)))
                .thenReturn(ReliableTransactionDO.builder().state(ReliableTransactionStateEnum.TX_FAILED).build());
        rollupService.commitL2ZkProof();
        verify(rollupRepository, times(1)).updateBatchProveRequestState(eq(BigInteger.ONE), eq(ProveTypeEnum.ZK_PROOF), eq(BatchProveRequestStateEnum.COMMITTED));
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(argument -> argument.getOriginalTxHash().equals(transactionInfo.getTxHash())));
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

    // ==================== Negative Test Cases: Exception Handling ====================

    /**
     * Test commit L2 batch when L1 client throws network exception
     */
    @Test
    public void testCommitL2Batch_NetworkException() {
        when(l1Client.lastCommittedBatch()).thenThrow(new RuntimeException("Network timeout"));

        try {
            rollupService.commitL2Batch();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Network timeout"));
        }
    }

    /**
     * Test commit L2 batch when batch retrieval fails
     */
    @Test
    public void testCommitL2Batch_BatchRetrievalFailure() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(null);
        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(null);

        try {
            rollupService.commitL2Batch();
            Assert.fail("Expected CommitL2BatchException not thrown");
        } catch (CommitL2BatchException e) {
            Assert.assertTrue(e.getMessage().contains("null batch for 1"));
        }
    }

    /**
     * Test commit L2 batch when commit transaction fails
     */
    @Test
    public void testCommitL2Batch_CommitTransactionFailure() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(null);

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(BatchWrapper.createBatch(
                        BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER,
                        BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(), 0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0,
                                ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);
        when(l1Client.commitBatch(notNull(), notNull())).thenThrow(new RuntimeException("Transaction failed"));

        try {
            rollupService.commitL2Batch();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Transaction failed"));
        }
    }

    /**
     * Test commit TEE proof when L1 client query fails
     */
    @Test
    public void testCommitL2TeeProof_QueryFailure() {
        when(l1Client.lastTeeVerifiedBatch()).thenThrow(new RuntimeException("RPC error"));

        try {
            rollupService.commitL2TeeProof();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("RPC error"));
        }
    }

    /**
     * Test commit TEE proof when proof is not ready
     */
    @Test
    public void testCommitL2TeeProof_ProofNotReady() {
        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().state(BatchProveRequestStateEnum.PENDING).build()
        );

        rollupService.commitL2TeeProof();

        // Should not attempt to verify when proof is not ready
        verify(l1Client, never()).verifyBatch(any(), any());
    }

    /**
     * Test commit TEE proof when verify batch throws exception
     */
    @Test
    public void testCommitL2TeeProof_VerifyBatchException() {
        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("a".getBytes()).state(BatchProveRequestStateEnum.PROVE_READY).build()
        );

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(BatchWrapper.createBatch(
                        BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER,
                        BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(), 0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0,
                                ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(l1Client.verifyBatch(notNull(), notNull())).thenThrow(new RuntimeException("Gas estimation failed"));

        try {
            rollupService.commitL2TeeProof();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Gas estimation failed"));
        }
    }

    /**
     * Test commit ZK proof when L1 client query fails
     */
    @Test
    public void testCommitL2ZkProof_QueryFailure() {
        when(l1Client.lastZkVerifiedBatch()).thenThrow(new RuntimeException("Connection refused"));

        try {
            rollupService.commitL2ZkProof();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Connection refused"));
        }
    }

    /**
     * Test commit ZK proof when proof is not ready
     */
    @Test
    public void testCommitL2ZkProof_ProofNotReady() {
        when(l1Client.lastZkVerifiedBatch()).thenReturn(BigInteger.ZERO);
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().state(BatchProveRequestStateEnum.PENDING).build()
        );

        rollupService.commitL2ZkProof();

        // Should not attempt to verify when proof is not ready
        verify(l1Client, never()).verifyBatch(any(), any());
    }

    /**
     * Test commit ZK proof when verify batch throws exception
     */
    @Test
    public void testCommitL2ZkProof_VerifyBatchException() {
        when(l1Client.lastZkVerifiedBatch()).thenReturn(BigInteger.ZERO);
        when(l1Client.lastCommittedBatch(notNull())).thenReturn(BigInteger.ONE);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getBatchProveRequest(notNull(), notNull())).thenReturn(
                BatchProveRequestDO.builder().proof("a".getBytes()).state(BatchProveRequestStateEnum.PROVE_READY).build()
        );

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(BatchWrapper.createBatch(
                        BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER,
                        BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(), 0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0,
                                ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(l1Client.verifyBatch(notNull(), notNull())).thenThrow(new RuntimeException("Invalid proof"));

        try {
            rollupService.commitL2ZkProof();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid proof"));
        }
    }

    /**
     * Test poll L2 blocks when L2 client throws exception
     */
    @Test
    public void testPollL2Blocks_L2ClientException() {
        when(l2Client.queryLatestBlockNumber(notNull())).thenThrow(new RuntimeException("L2 node unavailable"));

        try {
            rollupService.pollL2Blocks();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("L2 node unavailable"));
        }
    }

    /**
     * Test prove TEE L2 batch when batch retrieval fails
     */
    @Test
    public void testProveTeeL2Batch_BatchRetrievalFailure() {
        Logger logger = (Logger) LoggerFactory.getLogger(RollupServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_BATCH)))
                    .thenReturn(BigInteger.ONE);
            when(rollupRepository.peekPendingBatchProveRequest(anyInt(), eq(ProveTypeEnum.TEE_PROOF)))
                    .thenReturn(ListUtil.toList(BatchProveRequestDO.builder()
                            .proveType(ProveTypeEnum.TEE_PROOF)
                            .batchIndex(BigInteger.ONE)
                            .state(BatchProveRequestStateEnum.PENDING).build()));
            when(proverControllerClient.getBatchProof(eq(ProveTypeEnum.TEE_PROOF), eq(BigInteger.ONE)))
                    .thenThrow(new ProofNotReadyException(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));

            rollupService.proveTeeL2Batch();

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("proof is not ready"));
            Assert.assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }

    }

    /**
     * Test repository update failure during commit
     */
    @Test
    public void testCommitL2Batch_RepositoryUpdateFailure() {
        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(null);

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        when(rollupRepository.getBatch(eq(BigInteger.ONE)))
                .thenReturn(BatchWrapper.createBatch(
                        BatchVersionEnum.BATCH_V0, BigInteger.ONE, ZERO_BATCH_WRAPPER,
                        BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                        BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                        Bytes32.DEFAULT.getValue(), 0,
                        ListUtil.toList(new ChunkWrapper(BigInteger.ONE, 0,
                                ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
                ));
        when(rollupRepository.getBatchHeader(eq(BigInteger.ZERO))).thenReturn(ZERO_BATCH_HEADER);

        TransactionInfo transactionInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .rawTx("tx".getBytes())
                .senderAccount("0x5c02cAeB692Bf1b667D20d2B95c49B9DB1583981")
                .sendTxTime(new Date())
                .build();
        when(l1Client.commitBatch(notNull(), notNull())).thenReturn(transactionInfo);
        doThrow(new RuntimeException("Database error")).when(rollupRepository).updateRollupNumberRecord(any(), any(), any());

        try {
            rollupService.commitL2Batch();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Database error"));
        }
    }
}
