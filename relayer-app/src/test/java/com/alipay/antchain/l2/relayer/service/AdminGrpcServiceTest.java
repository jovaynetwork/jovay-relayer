package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlobsDaData;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.DynamicGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import io.grpc.internal.testing.StreamRecorder;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

import static org.mockito.Mockito.*;

public class AdminGrpcServiceTest extends TestBase {

    @MockitoBean
    private IRollupService rollupService;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleService oracleService;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean("l1-gasprice-provider-conf")
    private DynamicGasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @Resource
    private AdminGrpcService adminGrpcService;

    @Before
    public void initMock() {
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000L);
    }

    @Test
    @SneakyThrows
    public void testInitAnchorBatch() {
        InitAnchorBatchReq req = InitAnchorBatchReq.newBuilder()
                .setBatchHeaderInfo(
                        BatchHeaderInfo.newBuilder()
                                .setBatchIndex(1)
                                .setHash("05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9")
                                .setDataHash("05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9")
                                .setVersion(0)
                                .setParentBatchHash("05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9")
                                .setTotalL1MessagePopped(0)
                                .setL1MessagePopped(0)
                                .build()
                ).build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();
        adminGrpcService.initAnchorBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        req = InitAnchorBatchReq.newBuilder().setAnchorBatchIndex(1).build();
        responseObserver = StreamRecorder.create();
        adminGrpcService.initAnchorBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        doThrow(new RuntimeException("test")).when(rollupService).setAnchorBatch(any(BigInteger.class));
        req = InitAnchorBatchReq.newBuilder().setAnchorBatchIndex(1).build();
        responseObserver = StreamRecorder.create();
        adminGrpcService.initAnchorBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testGetBatch() {
        GetBatchReq req = GetBatchReq.newBuilder().setBatchIndex("1").build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        BatchWrapper batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );

        when(rollupRepository.getBatch(notNull())).thenReturn(batchWrapper1);

        adminGrpcService.getBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        Assert.assertEquals(results.get(0).getGetBatchResp().getBatch().getHeader().getHash(), batchWrapper1.getBatchHeader().getHashHex());
    }

    @Test
    @SneakyThrows
    public void testGetRawBatch() {
        GetRawBatchReq req = GetRawBatchReq.newBuilder().setBatchIndex("1").build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        BatchWrapper batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );

        when(rollupRepository.getBatch(notNull())).thenReturn(batchWrapper1);

        adminGrpcService.getRawBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        Assert.assertArrayEquals(
                batchWrapper1.getBatchHeader().serialize(),
                results.get(0).getGetRawBatchResp().getBatchHeader().toByteArray()
        );
        Assert.assertEquals(
                HexUtil.encodeHexStr(batchWrapper1.getChunks().get(0).getHash()),
                results.get(0).getGetRawBatchResp().getChunks(0).getHash()
        );
    }

    @Test
    @SneakyThrows
    public void testRetryBatchTx() {
        var tx1 = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .batchIndex(BigInteger.ONE)
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .build();
        var tx2 = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .batchIndex(BigInteger.valueOf(2))
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_FAILED)
                .retryCount(3)
                .build();
        var tx3 = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .batchIndex(BigInteger.valueOf(3))
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_FAILED)
                .retryCount(2)
                .build();
        when(rollupRepository.getReliableTransaction(
                eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
        )).thenReturn(tx1);
        when(rollupRepository.getReliableTransaction(
                eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.valueOf(2)), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
        )).thenReturn(tx2);
        when(rollupRepository.getReliableTransaction(
                eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.valueOf(3)), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
        )).thenReturn(tx3);

        var req = RetryBatchTxReq.newBuilder()
                .setType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX.name())
                .setFromBatchIndex(1)
                .setToBatchIndex(3)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.retryBatchTx(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(rollupRepository, never()).updateReliableTransaction(
                argThat(tx -> tx.getBatchIndex().equals(BigInteger.ONE))
        );
        verify(rollupRepository, times(1)).updateReliableTransaction(
                argThat(tx -> tx.getBatchIndex().equals(BigInteger.valueOf(2)) && tx.getState() == ReliableTransactionStateEnum.TX_FAILED && tx.getRetryCount() == 0)
        );
        verify(rollupRepository, times(1)).updateReliableTransaction(
                argThat(tx -> tx.getBatchIndex().equals(BigInteger.valueOf(3)) && tx.getState() == ReliableTransactionStateEnum.TX_FAILED && tx.getRetryCount() == 0)
        );
    }

    @Test
    @SneakyThrows
    public void testQueryBatchTxInfo() {
        var tx1 = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .batchIndex(BigInteger.ONE)
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxSendTime(new Date())
                .retryCount(0)
                .build();
        when(rollupRepository.getReliableTransaction(
                eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
        )).thenReturn(tx1);

        var req = QueryBatchTxInfoReq.newBuilder()
                .setType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX.name())
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.queryBatchTxInfo(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        Assert.assertTrue(results.get(0).hasQueryBatchTxInfoResp());

        Assert.assertEquals(tx1.getOriginalTxHash(), results.get(0).getQueryBatchTxInfoResp().getTxInfo().getOriginalTx());
        Assert.assertEquals(tx1.getLatestTxHash(), results.get(0).getQueryBatchTxInfoResp().getTxInfo().getLatestTx());
        Assert.assertEquals(tx1.getLatestTxSendTime(), DateUtil.parse(results.get(0).getQueryBatchTxInfoResp().getTxInfo().getLatestSendDate()));
        Assert.assertEquals(tx1.getState().name(), results.get(0).getQueryBatchTxInfoResp().getTxInfo().getState());
    }

    @Test
    @SneakyThrows
    public void testQueryBatchDaInfo() {
        var req = QueryBatchDaInfoReq.newBuilder().setBatchIndex(1).build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        var maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        var batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V1, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );

        when(rollupRepository.getBatch(notNull())).thenReturn(batchWrapper1);

        adminGrpcService.queryBatchDaInfo(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        Assert.assertEquals(
                batchWrapper1.getBatchHeader().getBatchIndex().longValue(),
                results.get(0).getQueryBatchDaInfoResp().getBatchIndex()
        );
        Assert.assertTrue(
                results.get(0).getQueryBatchDaInfoResp().getDaInfo().getCompressed()
        );

        responseObserver = StreamRecorder.create();
        batchWrapper1.getBatch().setDaData(BlobsDaData.lazyBuildFrom(((BlobsDaData) batchWrapper1.getBatch().getDaData()).getBlobs()));
        adminGrpcService.queryBatchDaInfo(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());

        results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        Assert.assertEquals(
                batchWrapper1.getBatchHeader().getBatchIndex().longValue(),
                results.get(0).getQueryBatchDaInfoResp().getBatchIndex()
        );
        Assert.assertTrue(
                results.get(0).getQueryBatchDaInfoResp().getDaInfo().getCompressed()
        );
    }

    @Test
    @SneakyThrows
    public void testWithdrawFromVault() {
        String mockTo = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";

        var txInfo = TransactionInfo.builder()
                .senderAccount(mockTo)
                .nonce(BigInteger.valueOf(RandomUtil.randomInt()))
                .txHash(mockTxHash)
                .build();

        when(oracleService.withdrawVault(anyString(), any())).thenReturn(txInfo);

        var req = WithdrawFromVaultReq.newBuilder()
                .setTo(mockTo)
                .setAmount(100)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.withdrawFromVault(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertNotNull(results.get(0));
        Assert.assertTrue(results.get(0).hasWithdrawFromVaultResp());
        Assert.assertNotNull(results.get(0).getWithdrawFromVaultResp().getTxHash());
        Assert.assertEquals(mockTxHash, results.get(0).getWithdrawFromVaultResp().getTxHash());
    }

    @Test
    @SneakyThrows
    public void testUpdateFixedProfit() {
        String mockFixedProfit = "1000";
        String mockSender = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";

        var txInfo = TransactionInfo.builder()
                .senderAccount(mockSender)
                .nonce(BigInteger.valueOf(RandomUtil.randomInt()))
                .txHash(mockTxHash)
                .build();

        when(oracleService.updateFixedProfit(any())).thenReturn(txInfo);

        var req = UpdateFixedProfitReq.newBuilder()
                .setProfit(mockFixedProfit)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateFixedProfit(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertNotNull(results.get(0));
        Assert.assertTrue(results.get(0).hasUpdateFixedProfitResp());
        Assert.assertNotNull(results.get(0).getUpdateFixedProfitResp().getTxHash());
        Assert.assertEquals(mockTxHash, results.get(0).getUpdateFixedProfitResp().getTxHash());
    }

    @Test
    @SneakyThrows
    public void testUpdateTotalScala() {
        String mockTotalScala = "1";
        String mockSender = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";

        var txInfo = TransactionInfo.builder()
                .senderAccount(mockSender)
                .nonce(BigInteger.valueOf(RandomUtil.randomInt()))
                .txHash(mockTxHash)
                .build();

        when(oracleService.updateTotalScala(any())).thenReturn(txInfo);

        var req = UpdateTotalScalaReq.newBuilder()
                .setTotalScala(mockTotalScala)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateTotalScala(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertNotNull(results.get(0));
        Assert.assertTrue(results.get(0).hasUpdateTotalScalaResp());
        Assert.assertNotNull(results.get(0).getUpdateTotalScalaResp().getTxHash());
        Assert.assertEquals(mockTxHash, results.get(0).getUpdateTotalScalaResp().getTxHash());
    }

    @Test
    @SneakyThrows
    public void testSpeedupTx() {
        var req = SpeedupTxReq.newBuilder()
                .setChainType(ChainTypeEnum.LAYER_ONE.name())
                .setType(TransactionTypeEnum.BATCH_COMMIT_TX.name())
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();
        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.valueOf(RandomUtil.randomInt()))
                .senderAccount(Numeric.toHexString(RandomUtil.randomBytes(20)))
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();
        when(l1Client.speedUpRollupTx(notNull(), notNull(), notNull(), notNull())).thenReturn(txInfo);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.valueOf(req.getChainType())), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_COMMIT_TX))).thenReturn(
                ReliableTransactionDO.builder()
                        .state(ReliableTransactionStateEnum.TX_PENDING)
                        .build()
        );

        adminGrpcService.speedupTx(req, responseObserver);
        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(
                x -> txInfo.getTxHash().equals(x.getLatestTxHash())
        ));
        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
    }

    @Test
    @SneakyThrows
    public void testUpdateGasPriceConfig() {
        var req = UpdateGasPriceConfigReq.newBuilder()
                .setChainType("ethereum")
                .setConfigKey("gasPriceIncreasedPercentage")
                .setConfigValue("1.5")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateGasPriceConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(l1GasPriceProviderConfig, times(1)).setGasPriceIncreasedPercentage(1.5);
    }

    @Test
    @SneakyThrows
    public void testUpdateGasPriceConfigUnknownChain() {
        var req = UpdateGasPriceConfigReq.newBuilder()
                .setChainType("unknown")
                .setConfigKey("gasPriceIncreasedPercentage")
                .setConfigValue("1.5")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateGasPriceConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("unknown chain type"));
    }

    @Test
    @SneakyThrows
    public void testGetGasPriceConfig() {
        when(l1GasPriceProviderConfig.getGasPriceIncreasedPercentage()).thenReturn(1.5);

        var req = GetGasPriceConfigReq.newBuilder()
                .setChainType("ethereum")
                .setConfigKey("gasPriceIncreasedPercentage")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.getGasPriceConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasGetGasPriceConfigResp());
        Assert.assertEquals("1.5", results.get(0).getGetGasPriceConfigResp().getConfigValue());
    }

    @Test
    @SneakyThrows
    public void testUpdateRollupEconomicStrategyConfig() {
        var req = UpdateRollupEconomicStrategyConfigReq.newBuilder()
                .setConfigKey("maxPendingBatchCount")
                .setConfigValue("10")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateRollupEconomicStrategyConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(rollupEconomicStrategyConfig, times(1)).setMaxPendingBatchCount(10);
    }

    @Test
    @SneakyThrows
    public void testUpdateRollupEconomicStrategyConfigFailure() {
        var req = UpdateRollupEconomicStrategyConfigReq.newBuilder()
                .setConfigKey("unknownKey")
                .setConfigValue("10")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateRollupEconomicStrategyConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("unknown economic strategy config key"));
    }

    @Test
    @SneakyThrows
    public void testGetRollupEconomicConfig() {
        when(rollupEconomicStrategyConfig.getMaxPendingBatchCount()).thenReturn(10);

        var req = GetRollupEconomicConfigReq.newBuilder()
                .setConfigKey("maxPendingBatchCount")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.getRollupEconomicConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasGetRollupEconomicConfigResp());
        Assert.assertEquals("10", results.get(0).getGetRollupEconomicConfigResp().getConfigValue());
    }
}
