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

package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.L1ContractWarnException;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlobsDaData;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.CachedNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.RemoteNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.DynamicGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IL2MerkleTreeRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.dal.repository.ScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import io.grpc.internal.testing.StreamRecorder;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import static org.mockito.Mockito.*;

public class AdminGrpcServiceTest extends TestBase {

    @MockitoBean
    private IRollupService rollupService;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleService oracleService;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean("l1BlobPoolTxTransactionManager")
    private BaseRawTransactionManager l1BlobPoolTxTransactionManager;

    @MockitoBean("l1LegacyPoolTxTransactionManager")
    private BaseRawTransactionManager l1LegacyPoolTxTransactionManager;

    @MockitoBean("l2TransactionManager")
    private BaseRawTransactionManager l2TransactionManager;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean("l1-gasprice-provider-conf")
    private DynamicGasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private IMailboxRepository mailboxRepository;

    @MockitoBean
    private IL2MerkleTreeRepository l2MerkleTreeRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private ScheduleRepository scheduleRepository;

    @MockitoBean
    private ScheduleContext scheduleContext;

    @MockitoBean
    private Cache<BigInteger, BasicBlockTrace> l2BlockTracesCacheForCurrChunk;

    @Resource
    private AdminGrpcService adminGrpcService;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
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

        BatchWrapper batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
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

        BatchWrapper batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
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

        var batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V1, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER, BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
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

    @Test
    @SneakyThrows
    public void testWasteEthAccountNonce() {
        WasteEthAccountNonceReq req = WasteEthAccountNonceReq.newBuilder()
                .setChainType(ChainType.L1)
                .setAddress("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .setNonce(100)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        var mockTxResult = mock(EthSendTransaction.class);
        when(mockTxResult.getTransactionHash()).thenReturn("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");

        when(l1Client.sendTransferValueTx(
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq(BigInteger.valueOf(100)),
                eq(BigInteger.ZERO)
        )).thenReturn(mockTxResult);

        adminGrpcService.wasteEthAccountNonce(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasWasteEthAccountNonceResp());
        Assert.assertEquals("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                results.get(0).getWasteEthAccountNonceResp().getTxHash());

        verify(l1Client, times(1)).sendTransferValueTx(
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq(BigInteger.valueOf(100)),
                eq(BigInteger.ZERO)
        );
    }

    @Test
    @SneakyThrows
    public void testWasteEthAccountNonceL2() {
        WasteEthAccountNonceReq req = WasteEthAccountNonceReq.newBuilder()
                .setChainType(ChainType.L2)
                .setAddress("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .setNonce(50)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        var mockTxResult = mock(EthSendTransaction.class);
        when(mockTxResult.getTransactionHash()).thenReturn("0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        when(l2Client.sendTransferValueTx(
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq(BigInteger.valueOf(50)),
                eq(BigInteger.ZERO)
        )).thenReturn(mockTxResult);

        adminGrpcService.wasteEthAccountNonce(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasWasteEthAccountNonceResp());
        Assert.assertEquals("0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                results.get(0).getWasteEthAccountNonceResp().getTxHash());

        verify(l2Client, times(1)).sendTransferValueTx(
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4"),
                eq(BigInteger.valueOf(50)),
                eq(BigInteger.ZERO)
        );
    }

    @Test
    @SneakyThrows
    public void testWasteEthAccountNonceWithException() {
        WasteEthAccountNonceReq req = WasteEthAccountNonceReq.newBuilder()
                .setChainType(ChainType.L1)
                .setAddress("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .setNonce(100)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.sendTransferValueTx(
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class)
        )).thenThrow(new RuntimeException("Failed to send transaction"));

        adminGrpcService.wasteEthAccountNonce(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("Failed to send transaction"));
    }

    @Test
    @SneakyThrows
    public void testWasteEthAccountNonceMultipleNonces() {
        // Test with different nonce values
        long[] nonces = {0, 1, 100, 999999};

        for (long nonce : nonces) {
            WasteEthAccountNonceReq req = WasteEthAccountNonceReq.newBuilder()
                    .setChainType(ChainType.L1)
                    .setAddress("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                    .setNonce(nonce)
                    .build();
            StreamRecorder<Response> responseObserver = StreamRecorder.create();

            var mockTxResult = mock(EthSendTransaction.class);
            var mockTxHash = "0x" + String.format("%064x", nonce);
            when(mockTxResult.getTransactionHash()).thenReturn(mockTxHash);

            when(l1Client.sendTransferValueTx(
                    anyString(),
                    anyString(),
                    eq(BigInteger.valueOf(nonce)),
                    eq(BigInteger.ZERO)
            )).thenReturn(mockTxResult);

            adminGrpcService.wasteEthAccountNonce(req, responseObserver);

            Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
            Assert.assertNull(responseObserver.getError());
            List<Response> results = responseObserver.getValues();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(0, results.get(0).getCode());
            Assert.assertEquals(mockTxHash, results.get(0).getWasteEthAccountNonceResp().getTxHash());
        }
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyTeeProof() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(1)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        var proofRequest = new BatchProveRequestDO();
        proofRequest.setProof(RandomUtil.randomBytes(32));
        proofRequest.setState(BatchProveRequestStateEnum.COMMITTED);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(proofRequest);
        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.verifyBatchWithEthCall(any(BatchWrapper.class), any(BatchProveRequestDO.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasCommitProofManuallyResp());
        Assert.assertEquals(txInfo.getTxHash(), results.get(0).getCommitProofManuallyResp().getTxHash());

        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX &&
                tx.getBatchIndex().equals(BigInteger.ONE) &&
                tx.getState() == ReliableTransactionStateEnum.TX_PENDING
        ));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyZkProof() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(2)
                .setProofType(ProofType.ZK)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastZkVerifiedBatch()).thenReturn(BigInteger.ONE);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        var proofRequest = new BatchProveRequestDO();
        proofRequest.setProof(RandomUtil.randomBytes(64));
        proofRequest.setState(BatchProveRequestStateEnum.COMMITTED);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.valueOf(2)), eq(ProveTypeEnum.ZK_PROOF)))
                .thenReturn(proofRequest);
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(2)))).thenReturn(batchWrapper);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.valueOf(2))
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.verifyBatchWithEthCall(any(BatchWrapper.class), any(BatchProveRequestDO.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasCommitProofManuallyResp());
        Assert.assertEquals(txInfo.getTxHash(), results.get(0).getCommitProofManuallyResp().getTxHash());

        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX &&
                tx.getBatchIndex().equals(BigInteger.valueOf(2)) &&
                tx.getState() == ReliableTransactionStateEnum.TX_PENDING
        ));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyProofNotReady() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(1)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);

        // Proof is still pending
        var proofRequest = new BatchProveRequestDO();
        proofRequest.setState(BatchProveRequestStateEnum.PENDING);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(proofRequest);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("not ready"));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyBatchNotExists() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(999)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.valueOf(998));

        var proofRequest = new BatchProveRequestDO();
        proofRequest.setProof(RandomUtil.randomBytes(32));
        proofRequest.setState(BatchProveRequestStateEnum.COMMITTED);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.valueOf(999)), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(proofRequest);
        when(rollupRepository.getBatch(eq(BigInteger.valueOf(999)))).thenReturn(null);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("not exists"));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyAlreadyCommitted() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(1)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.valueOf(2));

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("no need to commit"));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallySkipBatch() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(3)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ONE);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("can't skip"));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyUpdateExistingTx() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(1)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        var proofRequest = new BatchProveRequestDO();
        proofRequest.setProof(RandomUtil.randomBytes(32));
        proofRequest.setState(BatchProveRequestStateEnum.COMMITTED);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(proofRequest);
        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);

        // Existing transaction
        var existingTx = ReliableTransactionDO.builder()
                .batchIndex(BigInteger.ONE)
                .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_FAILED)
                .build();
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE),
                eq(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX))).thenReturn(existingTx);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.verifyBatchWithEthCall(any(BatchWrapper.class), any(BatchProveRequestDO.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX &&
                tx.getBatchIndex().equals(BigInteger.ONE) &&
                tx.getLatestTxHash().equals(txInfo.getTxHash())
        ));
    }

    @Test
    @SneakyThrows
    public void testCommitProofManuallyL1ContractWarn() {
        CommitProofManuallyReq req = CommitProofManuallyReq.newBuilder()
                .setBatchIndex(1)
                .setProofType(ProofType.TEE)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastTeeVerifiedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        var proofRequest = new BatchProveRequestDO();
        proofRequest.setProof(RandomUtil.randomBytes(32));
        proofRequest.setState(BatchProveRequestStateEnum.COMMITTED);

        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(proofRequest);
        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);

        when(l1Client.verifyBatchWithEthCall(any(BatchWrapper.class), any(BatchProveRequestDO.class)))
                .thenThrow(new L1ContractWarnException(L2RelayerErrorCodeEnum.CALL_WITH_WARNING, "proof already committed"));

        adminGrpcService.commitProofManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(-1, results.get(0).getCode());

        // Should not insert or update transaction when contract warns
        verify(rollupRepository, never()).insertReliableTransaction(any());
        verify(rollupRepository, never()).updateReliableTransaction(any());
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManually() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                .thenReturn(null);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ZERO);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.commitBatchWithEthCall(any(BatchWrapper.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).hasCommitBatchManuallyResp());
        Assert.assertEquals(txInfo.getTxHash(), results.get(0).getCommitBatchManuallyResp().getTxHash());

        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionTypeEnum.BATCH_COMMIT_TX &&
                tx.getBatchIndex().equals(BigInteger.ONE) &&
                tx.getState() == ReliableTransactionStateEnum.TX_PENDING
        ));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyAlreadyCommitted() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.valueOf(2));

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("no need to commit"));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallySkipBatch() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(3)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ONE);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("can't skip"));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyBatchNotExists() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);
        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(null);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("not exists"));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyL1ContractWarn() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                .thenReturn(null);

        when(l1Client.commitBatchWithEthCall(any(BatchWrapper.class)))
                .thenThrow(new L1ContractWarnException(L2RelayerErrorCodeEnum.CALL_WITH_WARNING, "batch already committed"));

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("been committed"));

        verify(rollupRepository, never()).insertReliableTransaction(any());
        verify(rollupRepository, never()).updateReliableTransaction(any());
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyUpdateExistingTx() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        when(rollupRepository.getBatch(eq(BigInteger.ONE))).thenReturn(batchWrapper);

        // Existing transaction in failed state
        var existingTx = ReliableTransactionDO.builder()
                .batchIndex(BigInteger.ONE)
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_FAILED)
                .build();
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                .thenReturn(existingTx);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ZERO);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.commitBatchWithEthCall(any(BatchWrapper.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(rollupRepository, times(1)).updateReliableTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionTypeEnum.BATCH_COMMIT_TX &&
                tx.getBatchIndex().equals(BigInteger.ONE) &&
                tx.getLatestTxHash().equals(txInfo.getTxHash())
        ));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(
                eq(ChainTypeEnum.LAYER_TWO),
                eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED),
                eq(batchWrapper.getBatchIndex())
        );
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyUpdateBatchCommittedRecord() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(2)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ONE);

        var batchWrapper = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(2), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );

        when(rollupRepository.getBatch(eq(BigInteger.valueOf(2)))).thenReturn(batchWrapper);
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.valueOf(2)), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                .thenReturn(null);
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                .thenReturn(BigInteger.ONE);

        var txInfo = TransactionInfo.builder()
                .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .nonce(BigInteger.valueOf(2))
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l1Client.commitBatchWithEthCall(any(BatchWrapper.class)))
                .thenReturn(txInfo);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        verify(rollupRepository, times(1)).updateRollupNumberRecord(
                eq(ChainTypeEnum.LAYER_TWO),
                eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED),
                eq(BigInteger.valueOf(2))
        );
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyTxAlreadySucceed() {
        CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.ZERO);

        // Transaction already confirmed
        var succeededTx = ReliableTransactionDO.builder()
                .batchIndex(BigInteger.ONE)
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .state(ReliableTransactionStateEnum.TX_SUCCESS)
                .latestTxHash("0x1234567890abcdef")
                .build();
        when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.ONE), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                .thenReturn(succeededTx);

        adminGrpcService.commitBatchManually(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("confirmed success"));

        verify(rollupRepository, never()).insertReliableTransaction(any());
    }

    @Test
    @SneakyThrows
    public void testCommitBatchManuallyMultipleBatches() {
        // Test committing multiple batches sequentially
        for (int i = 1; i <= 3; i++) {
            CommitBatchManuallyReq req = CommitBatchManuallyReq.newBuilder()
                    .setBatchIndex(i)
                    .build();
            StreamRecorder<Response> responseObserver = StreamRecorder.create();

            when(l1Client.lastCommittedBatch()).thenReturn(BigInteger.valueOf(i - 1));

            var batchWrapper = BatchWrapper.createBatch(
                    BatchVersionEnum.BATCH_V0, BigInteger.valueOf(i), ZERO_BATCH_WRAPPER,
                    BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                    BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                    Bytes32.DEFAULT.getValue(),
                    0,
                    ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(i), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
            );

            when(rollupRepository.getBatch(eq(BigInteger.valueOf(i)))).thenReturn(batchWrapper);
            when(rollupRepository.getReliableTransaction(eq(ChainTypeEnum.LAYER_ONE), eq(BigInteger.valueOf(i)), eq(TransactionTypeEnum.BATCH_COMMIT_TX)))
                    .thenReturn(null);
            when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.BATCH_COMMITTED)))
                    .thenReturn(BigInteger.valueOf(i - 1));

            var txInfo = TransactionInfo.builder()
                    .txHash("0x" + String.format("%064x", i))
                    .nonce(BigInteger.valueOf(i))
                    .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                    .sendTxTime(new Date())
                    .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                    .build();

            when(l1Client.commitBatchWithEthCall(any(BatchWrapper.class)))
                    .thenReturn(txInfo);

            adminGrpcService.commitBatchManually(req, responseObserver);

            Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
            Assert.assertNull(responseObserver.getError());
            List<Response> results = responseObserver.getValues();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(0, results.get(0).getCode());
            Assert.assertEquals(txInfo.getTxHash(), results.get(0).getCommitBatchManuallyResp().getTxHash());
        }
    }

    @Test
    @SneakyThrows
    public void testUpdateNonceManuallyL1() {
        when(l1Client.getBlobPoolTxManager()).thenReturn(l1BlobPoolTxTransactionManager);
        var nonceManager = mock(CachedNonceManager.class);
        when(l1BlobPoolTxTransactionManager.getNonceManager()).thenReturn(nonceManager);
        doNothing().when(nonceManager).setNonceIntoCache(notNull());

        // Update L1 nonce successfully
        var updateReq = UpdateNonceManuallyReq.newBuilder()
                .setChainType(ChainType.L1)
                .setAccType(AccType.BLOB)
                .setNonce(1000L)
                .build();

        StreamRecorder<Response> responseObserver = StreamRecorder.create();
        adminGrpcService.updateNonceManually(updateReq, responseObserver);

        // Wait for response
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        // Verify nonce was updated in cache
        verify(nonceManager, times(1)).setNonceIntoCache(eq(BigInteger.valueOf(1000L)));

        var nonceManager1 = mock(RemoteNonceManager.class);
        when(l1BlobPoolTxTransactionManager.getNonceManager()).thenReturn(nonceManager1);

        StreamRecorder.create();
        adminGrpcService.updateNonceManually(updateReq, responseObserver);
        // Wait for response
        results = responseObserver.getValues();
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(-1, results.get(1).getCode());
        Assert.assertTrue(results.get(1).getErrorMsg().contains("only supports cached nonce manager"));
    }

    @Test
    @SneakyThrows
    public void testQueryCurrNonceL1() {
        // Query L1 nonce from cache
        when(l1Client.getBlobPoolTxManager()).thenReturn(l1BlobPoolTxTransactionManager);
        var nonceManager = mock(CachedNonceManager.class);
        when(l1BlobPoolTxTransactionManager.getNonceManager()).thenReturn(nonceManager);
        when(nonceManager.getNextNonce()).thenReturn(BigInteger.valueOf(5000L));

        var queryReq = QueryCurrNonceReq.newBuilder()
                .setChainType(ChainType.L1)
                .setAccType(AccType.BLOB)
                .build();

        StreamRecorder<Response> responseObserver = StreamRecorder.create();
        adminGrpcService.queryCurrNonce(queryReq, responseObserver);

        // Wait for response
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertEquals(5000L, results.get(0).getQueryCurrNonceResp().getNonce());

        // Verify query was called
        verify(nonceManager, times(1)).getNextNonce();
    }

    @Test
    @SneakyThrows
    public void testRefetchProof() {
        // pick a valid proof type dynamically to avoid hard-coding enum name
        var proofType = ProveTypeEnum.values()[0].name();

        var req = RefetchProofReq.newBuilder()
                .setProofType(proofType)
                .setFromBatchIndex("1")
                .setToBatchIndex("3")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // prepare a mock non-null return for certain batches using reflection to create correct return type
        var m = IRollupRepository.class.getMethod("getBatchProveRequest", BigInteger.class, ProveTypeEnum.class);
        var mockProveReq = mock(BatchProveRequestDO.class);

        // return non-null for batch 1 and 3, default (null) for batch 2
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.valueOf(1)), any())).thenReturn(mockProveReq);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.valueOf(3)), any())).thenReturn(mockProveReq);

        adminGrpcService.refetchProof(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        var results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        // verify update called only for batches 1 and 3 with PENDING state
        verify(rollupRepository, times(1)).updateBatchProveRequestState(
                eq(BigInteger.valueOf(1)), eq(ProveTypeEnum.valueOf(proofType.toUpperCase())), eq(BatchProveRequestStateEnum.PENDING)
        );
        verify(rollupRepository, times(1)).updateBatchProveRequestState(
                eq(BigInteger.valueOf(3)), eq(ProveTypeEnum.valueOf(proofType.toUpperCase())), eq(BatchProveRequestStateEnum.PENDING)
        );
        verify(rollupRepository, never()).updateBatchProveRequestState(
                eq(BigInteger.valueOf(2)), any(), any()
        );
    }

    // ==================== Negative Case Tests ====================

    @Test
    @SneakyThrows
    public void testInitAnchorBatch_InvalidBatchIndex() {
        var req = InitAnchorBatchReq.newBuilder()
                .setAnchorBatchIndex(999)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        doThrow(new IllegalArgumentException("Invalid batch index")).when(rollupService).setAnchorBatch(any(BigInteger.class));

        adminGrpcService.initAnchorBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testGetBatch_BatchNotFound() {
        var req = GetBatchReq.newBuilder()
                .setBatchIndex("999")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(rollupRepository.getBatch(any())).thenReturn(null);

        adminGrpcService.getBatch(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testRetryBatchTx_InvalidTransactionType() {
        var req = RetryBatchTxReq.newBuilder()
                .setType("INVALID_TYPE")
                .setFromBatchIndex(1)
                .setToBatchIndex(3)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.retryBatchTx(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testQueryBatchTxInfo_TransactionNotFound() {
        var req = QueryBatchTxInfoReq.newBuilder()
                .setType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX.name())
                .setBatchIndex(999)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(rollupRepository.getReliableTransaction(any(), any(), any())).thenReturn(null);

        adminGrpcService.queryBatchTxInfo(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testWithdrawFromVault_InvalidAddress() {
        var req = WithdrawFromVaultReq.newBuilder()
                .setTo("invalid_address")
                .setAmount(100)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(oracleService.withdrawVault(anyString(), any())).thenThrow(new IllegalArgumentException("Invalid address format"));

        adminGrpcService.withdrawFromVault(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testUpdateFixedProfit_InvalidValue() {
        var req = UpdateFixedProfitReq.newBuilder()
                .setProfit("-100")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(oracleService.updateFixedProfit(any())).thenThrow(new IllegalArgumentException("Invalid profit value"));

        adminGrpcService.updateFixedProfit(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testSpeedupTx_TransactionNotPending() {
        var req = SpeedupTxReq.newBuilder()
                .setChainType(ChainTypeEnum.LAYER_ONE.name())
                .setType(TransactionTypeEnum.BATCH_COMMIT_TX.name())
                .setBatchIndex(1)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        when(rollupRepository.getReliableTransaction(any(), any(), any())).thenReturn(
                ReliableTransactionDO.builder()
                        .state(ReliableTransactionStateEnum.TX_SUCCESS)
                        .build()
        );

        adminGrpcService.speedupTx(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testUpdateGasPriceConfig_InvalidConfigValue() {
        var req = UpdateGasPriceConfigReq.newBuilder()
                .setChainType("ethereum")
                .setConfigKey("gasPriceIncreasedPercentage")
                .setConfigValue("invalid_number")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.updateGasPriceConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    @Test
    @SneakyThrows
    public void testGetGasPriceConfig_UnknownConfigKey() {
        var req = GetGasPriceConfigReq.newBuilder()
                .setChainType("ethereum")
                .setConfigKey("unknownKey")
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        adminGrpcService.getGasPriceConfig(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
    }

    // ==================== Rollback to Subchain Height Tests ====================

    /**
     * Test successful rollback to subchain height
     * Should execute all rollback steps and clear caches
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_Success() {
        long targetBatchIndex = 10L;
        long targetBlockHeight = 1000L;
        long l1MsgNonceThreshold = 50L;

        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(targetBatchIndex)
                .setTargetBlockHeight(targetBlockHeight)
                .setL1MsgNonceThreshold(l1MsgNonceThreshold)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock schedule context and repository for node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Mock chunks for the target batch
        var chunk1 = createMockChunkWrapper(BigInteger.valueOf(targetBatchIndex), 0, 
                BigInteger.valueOf(900), BigInteger.valueOf(950), 1000L);
        var chunk2 = createMockChunkWrapper(BigInteger.valueOf(targetBatchIndex), 1, 
                BigInteger.valueOf(951), BigInteger.valueOf(1050), 2000L);
        when(rollupRepository.getChunks(eq(BigInteger.valueOf(targetBatchIndex))))
                .thenReturn(ListUtil.toList(chunk1, chunk2));

        // Mock distributed locks
        for (var taskType : BizTaskTypeEnum.values()) {
            var mockLock = mock(RLock.class);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(redissonClient.getLock(eq("relayer:task:" + taskType.name()))).thenReturn(mockLock);
        }

        // Mock repository delete operations
        when(rollupRepository.deleteBatchesFrom(any())).thenReturn(5);
        when(rollupRepository.deleteChunksForRollback(any(), anyLong())).thenReturn(10);
        when(rollupRepository.deleteBatchProveRequestsFrom(any())).thenReturn(5);
        when(rollupRepository.deleteRollupReliableTransactionsFrom(any())).thenReturn(3);
        when(rollupRepository.deleteL1MsgReliableTransactionsAboveNonce(anyLong())).thenReturn(2);
        when(l2MerkleTreeRepository.deleteMerkleTreesFrom(any())).thenReturn(5);
        when(mailboxRepository.resetL1MsgsAboveNonce(anyLong())).thenReturn(10);
        when(mailboxRepository.deleteL2MsgsForRollback(any(), any())).thenReturn(8);
        when(mailboxRepository.resetL2MsgsForRollback(any(), any())).thenReturn(6);
        when(oracleRepository.deleteBatchOracleRequestsFrom(any())).thenReturn(4);
        when(rollupRepository.deleteOracleBatchFeeFeedTransactionsFrom(any())).thenReturn(2);

        // Mock cache operations
        var mockKeys = mock(org.redisson.api.RKeys.class);
        when(redissonClient.getKeys()).thenReturn(mockKeys);
        when(mockKeys.deleteByPattern(anyString())).thenReturn(10L);

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(10, TimeUnit.SECONDS));
        Assert.assertNull(responseObserver.getError());
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getRollbackToSubchainHeightResp().getSummary().contains("completed successfully"));

        // Verify rollup number records were updated
        verify(rollupRepository, times(6)).updateRollupNumberRecord(any(), any(), any());

        // Verify delete operations were called
        verify(rollupRepository).deleteBatchesFrom(eq(BigInteger.valueOf(targetBatchIndex)));
        verify(rollupRepository).deleteChunksForRollback(eq(BigInteger.valueOf(targetBatchIndex)), eq(1L));
        verify(l2MerkleTreeRepository).deleteMerkleTreesFrom(eq(BigInteger.valueOf(targetBatchIndex)));
        verify(mailboxRepository).resetL1MsgsAboveNonce(eq(l1MsgNonceThreshold));
        verify(oracleRepository).deleteBatchOracleRequestsFrom(eq(BigInteger.valueOf(targetBatchIndex)));

        // Verify cache was cleared
        verify(mockKeys).deleteByPattern(eq("L2_BLOCK_TRACE@*"));
        verify(mockKeys).deleteByPattern(eq("L2_CHUNK@*"));
        verify(mockKeys).deleteByPattern(eq("L2_BATCH@*"));
        verify(mockKeys).deleteByPattern(eq("L2_MERKLE_TREE-*"));
        verify(l2BlockTracesCacheForCurrChunk).clear();
    }

    /**
     * Test rollback fails when chunks are empty
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_EmptyChunks() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Return empty chunks
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.empty());

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("empty"));
    }

    /**
     * Test rollback fails when block height is not inside chunks
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_BlockNotInChunks() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(500L)  // Block height before chunk start
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Chunk starts at block 1000, but target is 500
        var chunk = createMockChunkWrapper(BigInteger.TEN, 0, 
                BigInteger.valueOf(1000), BigInteger.valueOf(1100), 1000L);
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.toList(chunk));

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("not inside"));
    }

    /**
     * Test rollback fails when no chunk found for target block
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_NoChunkForBlock() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(2000L)  // Block height after all chunks
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Chunk ends at block 1100, but target is 2000
        var chunk = createMockChunkWrapper(BigInteger.TEN, 0, 
                BigInteger.valueOf(1000), BigInteger.valueOf(1100), 1000L);
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.toList(chunk));

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("No chunk found"));
    }

    /**
     * Test rollback fails when distributed lock acquisition fails
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_LockAcquisitionFailed() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Mock chunks
        var chunk = createMockChunkWrapper(BigInteger.TEN, 0, 
                BigInteger.valueOf(900), BigInteger.valueOf(1050), 1000L);
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.toList(chunk));

        // First lock succeeds, second lock fails
        var taskTypes = BizTaskTypeEnum.values();
        for (int i = 0; i < taskTypes.length; i++) {
            var mockLock = mock(RLock.class);
            if (i == 1) {
                // Second lock fails
                when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);
            } else {
                when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            }
            when(redissonClient.getLock(eq("relayer:task:" + taskTypes[i].name()))).thenReturn(mockLock);
        }

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("Failed to acquire lock"));
    }

    /**
     * Test rollback fails when local node is offline
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_LocalNodeOffline() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock local node as offline
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.OFFLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("not online"));
    }

    /**
     * Test rollback fails when other nodes are still online
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_OtherNodesOnline() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock local node online but another node also online
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        var otherNode = new ActiveNode();
        otherNode.setNodeId("node-2");
        otherNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode, otherNode));

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());
        Assert.assertTrue(results.get(0).getErrorMsg().contains("other relayer nodes to be offline"));
    }

    /**
     * Test rollback with multiple chunks and gas sum calculation
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_MultipleChunksGasSum() {
        long targetBatchIndex = 10L;
        long targetBlockHeight = 1500L;

        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(targetBatchIndex)
                .setTargetBlockHeight(targetBlockHeight)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Create multiple chunks - target block is in chunk 2
        var chunk0 = createMockChunkWrapper(BigInteger.valueOf(targetBatchIndex), 0, 
                BigInteger.valueOf(1000), BigInteger.valueOf(1200), 1000L);
        var chunk1 = createMockChunkWrapper(BigInteger.valueOf(targetBatchIndex), 1, 
                BigInteger.valueOf(1201), BigInteger.valueOf(1400), 2000L);
        var chunk2 = createMockChunkWrapper(BigInteger.valueOf(targetBatchIndex), 2, 
                BigInteger.valueOf(1401), BigInteger.valueOf(1600), 3000L);
        when(rollupRepository.getChunks(eq(BigInteger.valueOf(targetBatchIndex))))
                .thenReturn(ListUtil.toList(chunk0, chunk1, chunk2));

        // Mock distributed locks
        for (var taskType : BizTaskTypeEnum.values()) {
            var mockLock = mock(RLock.class);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(redissonClient.getLock(eq("relayer:task:" + taskType.name()))).thenReturn(mockLock);
        }

        // Mock repository operations
        when(rollupRepository.deleteBatchesFrom(any())).thenReturn(1);
        when(rollupRepository.deleteChunksForRollback(any(), anyLong())).thenReturn(1);
        when(rollupRepository.deleteBatchProveRequestsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteRollupReliableTransactionsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteL1MsgReliableTransactionsAboveNonce(anyLong())).thenReturn(1);
        when(l2MerkleTreeRepository.deleteMerkleTreesFrom(any())).thenReturn(1);
        when(mailboxRepository.resetL1MsgsAboveNonce(anyLong())).thenReturn(1);
        when(mailboxRepository.deleteL2MsgsForRollback(any(), any())).thenReturn(1);
        when(mailboxRepository.resetL2MsgsForRollback(any(), any())).thenReturn(1);
        when(oracleRepository.deleteBatchOracleRequestsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteOracleBatchFeeFeedTransactionsFrom(any())).thenReturn(1);

        var mockKeys = mock(org.redisson.api.RKeys.class);
        when(redissonClient.getKeys()).thenReturn(mockKeys);
        when(mockKeys.deleteByPattern(anyString())).thenReturn(1L);

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(10, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        // Verify chunk index 2 was used (target block 1500 is in chunk 2)
        verify(rollupRepository).deleteChunksForRollback(eq(BigInteger.valueOf(targetBatchIndex)), eq(2L));

        // Verify gas sum was calculated correctly (chunk0 + chunk1 = 1000 + 2000 = 3000)
        verify(rollupRepository).updateRollupNumberRecord(
                eq(ChainTypeEnum.LAYER_TWO), 
                eq(RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR), 
                eq(BigInteger.valueOf(3000L)));
    }

    /**
     * Test rollback locks are released even on failure
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_LocksReleasedOnFailure() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Mock chunks
        var chunk = createMockChunkWrapper(BigInteger.TEN, 0, 
                BigInteger.valueOf(900), BigInteger.valueOf(1050), 1000L);
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.toList(chunk));

        // Mock locks
        var mockLocks = new ArrayList<RLock>();
        for (var taskType : BizTaskTypeEnum.values()) {
            var mockLock = mock(RLock.class);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(mockLock.getName()).thenReturn("relayer:task:" + taskType.name());
            when(redissonClient.getLock(eq("relayer:task:" + taskType.name()))).thenReturn(mockLock);
            mockLocks.add(mockLock);
        }

        // Make rollback fail during execution
        when(rollupRepository.deleteBatchesFrom(any())).thenThrow(new RuntimeException("Database error"));

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(-1, results.get(0).getCode());

        // Verify all locks were released
        for (var mockLock : mockLocks) {
            verify(mockLock).forceUnlock();
        }
    }

    /**
     * Test rollback with cache clearing
     */
    @Test
    @SneakyThrows
    public void testRollbackToSubchainHeight_CacheClearing() {
        var req = RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10L)
                .setTargetBlockHeight(1000L)
                .setL1MsgNonceThreshold(50L)
                .build();
        StreamRecorder<Response> responseObserver = StreamRecorder.create();

        // Mock node check
        when(scheduleContext.getNodeId()).thenReturn("node-1");
        var localNode = new ActiveNode();
        localNode.setNodeId("node-1");
        localNode.setStatus(ActiveNodeStatusEnum.ONLINE);
        when(scheduleRepository.getAllActiveNodes()).thenReturn(ListUtil.toList(localNode));

        // Mock chunks
        var chunk = createMockChunkWrapper(BigInteger.TEN, 0, 
                BigInteger.valueOf(900), BigInteger.valueOf(1050), 1000L);
        when(rollupRepository.getChunks(any())).thenReturn(ListUtil.toList(chunk));

        // Mock locks
        for (var taskType : BizTaskTypeEnum.values()) {
            var mockLock = mock(RLock.class);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(redissonClient.getLock(eq("relayer:task:" + taskType.name()))).thenReturn(mockLock);
        }

        // Mock repository operations
        when(rollupRepository.deleteBatchesFrom(any())).thenReturn(1);
        when(rollupRepository.deleteChunksForRollback(any(), anyLong())).thenReturn(1);
        when(rollupRepository.deleteBatchProveRequestsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteRollupReliableTransactionsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteL1MsgReliableTransactionsAboveNonce(anyLong())).thenReturn(1);
        when(l2MerkleTreeRepository.deleteMerkleTreesFrom(any())).thenReturn(1);
        when(mailboxRepository.resetL1MsgsAboveNonce(anyLong())).thenReturn(1);
        when(mailboxRepository.deleteL2MsgsForRollback(any(), any())).thenReturn(1);
        when(mailboxRepository.resetL2MsgsForRollback(any(), any())).thenReturn(1);
        when(oracleRepository.deleteBatchOracleRequestsFrom(any())).thenReturn(1);
        when(rollupRepository.deleteOracleBatchFeeFeedTransactionsFrom(any())).thenReturn(1);

        // Mock cache operations
        var mockKeys = mock(org.redisson.api.RKeys.class);
        when(redissonClient.getKeys()).thenReturn(mockKeys);
        when(mockKeys.deleteByPattern("L2_BLOCK_TRACE@*")).thenReturn(100L);
        when(mockKeys.deleteByPattern("L2_CHUNK@*")).thenReturn(50L);
        when(mockKeys.deleteByPattern("L2_BATCH@*")).thenReturn(10L);
        when(mockKeys.deleteByPattern("L2_MERKLE_TREE-*")).thenReturn(10L);

        adminGrpcService.rollbackToSubchainHeight(req, responseObserver);

        Assert.assertTrue(responseObserver.awaitCompletion(10, TimeUnit.SECONDS));
        List<Response> results = responseObserver.getValues();
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0, results.get(0).getCode());

        // Verify all cache patterns were cleared
        verify(mockKeys).deleteByPattern("L2_BLOCK_TRACE@*");
        verify(mockKeys).deleteByPattern("L2_CHUNK@*");
        verify(mockKeys).deleteByPattern("L2_BATCH@*");
        verify(mockKeys).deleteByPattern("L2_MERKLE_TREE-*");

        // Verify in-memory cache was cleared
        verify(l2BlockTracesCacheForCurrChunk).clear();
    }

    /**
     * Helper method to create a mock ChunkWrapper
     */
    private ChunkWrapper createMockChunkWrapper(BigInteger batchIndex, long chunkIndex, 
            BigInteger startBlock, BigInteger endBlock, long gasSum) {
        var chunk = mock(ChunkWrapper.class);
        when(chunk.getBatchIndex()).thenReturn(batchIndex);
        when(chunk.getChunkIndex()).thenReturn(chunkIndex);
        when(chunk.getStartBlockNumber()).thenReturn(startBlock);
        when(chunk.getEndBlockNumber()).thenReturn(endBlock);
        when(chunk.getGasSum()).thenReturn(gasSum);
        return chunk;
    }
}
