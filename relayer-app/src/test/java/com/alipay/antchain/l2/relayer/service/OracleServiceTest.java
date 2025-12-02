package com.alipay.antchain.l2.relayer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBpoBlobConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

public class OracleServiceTest extends TestBase {
    @Resource
    private IOracleService oracleService;

    @MockitoBean(name = "l2Web3j")
    private Web3j l2Web3j;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3jBean;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    private Web3j l1Web3j;

    private final EthBpoBlobConfig bpoConfig = new EthBpoBlobConfig("test", 1, BLOB_BASE_FEE_UPDATE_FRACTION);

    private final EthBlobForkConfig ethForkBlobConfig = mock(EthBlobForkConfig.class);

    private static final String mockSender = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
    private static final String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";

    private static final BigInteger BLOB_BASE_FEE_UPDATE_FRACTION = new BigInteger("5007716");

    @Before
    @SneakyThrows
    public void initMock() {
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000L);

        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var field = ReflectUtil.getField(OracleServiceImpl.class, "ethBlobForkConfig");
        field.setAccessible(true);
        field.set(oracleService, ethForkBlobConfig);
    }

    @Test
    public void testInitService() {
        when(l2Client.queryL2GasOracleLastBatchDaFee()).thenReturn(BigInteger.valueOf(RandomUtil.randomLong()));
        when(l2Client.queryL2GasOracleLastBatchExecFee()).thenReturn(BigInteger.valueOf(RandomUtil.randomLong()));
        when(l2Client.queryL2GasOracleLastBatchByteLength()).thenReturn(BigInteger.valueOf(RandomUtil.randomLong()));

        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(6192878))
                .build();

        when(oracleRepository.peekLatestRequestIndex(any(), any(), any())).thenReturn(BigInteger.TWO);
        when(oracleRepository.peekLatestRequest(any(), any(), any())).thenReturn(
                OracleRequestDO.builder()
                        .rawData(JSON.toJSONBytes(mockL1BlockFee))
                        .build()
        );

        var oracleService1 = new OracleServiceImpl();
        ReflectionTestUtils.setField(oracleService1, "l2Client", l2Client);
        ReflectionTestUtils.setField(oracleService1, "oracleRepository", oracleRepository);
        ReflectionTestUtils.setField(oracleService1, "ethBlobForkConfig", ethForkBlobConfig);
        oracleService1.initService();

        OracleFeeInfo latestOracleFeeInfo = (OracleFeeInfo) ReflectionTestUtils.getField(oracleService1, "latestOracleFeeInfo");
        assert latestOracleFeeInfo != null;
        Assert.equals(BigInteger.TWO, latestOracleFeeInfo.getStartBatchIndex());
    }

    @Test
    public void testProcessL1BlockUpdate() throws IOException {
        EthBlock ethBlock = mockEthBlock(
                String.valueOf(4000000000L), // 4Gwei
                String.valueOf(500000),
                String.valueOf(1000000),
                String.valueOf(20000),
                String.valueOf(BLOB_BASE_FEE_UPDATE_FRACTION)
        );
        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();
        BigInteger mockRequestIndex = BigInteger.valueOf(2);
        OracleRequestDO oracleRequestDO = OracleRequestDO.builder()
                .oracleType(OracleTypeEnum.L2_GAS_ORACLE)
                .oracleTaskType(OracleRequestTypeEnum.L1_BLOCK_UPDATE)
                .requestIndex(mockRequestIndex)
                .rawData(JSON.toJSONBytes(l1BlockFeeInfo))
                .build();
        when(oracleRepository.peekRequests(eq(OracleTypeEnum.L2_GAS_ORACLE), eq(OracleRequestTypeEnum.L1_BLOCK_UPDATE), eq(OracleTransactionStateEnum.INIT), anyInt())).thenReturn(
                ListUtil.toList(
                        oracleRequestDO
                )
        );
        when(l2Client.updateBaseFeeScala(any(), any())).thenReturn(mockTransactionInfo());

        oracleService.processBlockOracle();

        verify(oracleRepository, atLeast(1)).peekRequests(
                eq(OracleTypeEnum.L2_GAS_ORACLE),
                any(OracleRequestTypeEnum.class),
                any(OracleTransactionStateEnum.class),
                anyInt()
        );
        verify(oracleRepository, atLeast(1)).updateRequestState(
                eq(String.valueOf(mockRequestIndex)),
                eq(OracleTypeEnum.L2_GAS_ORACLE),
                any(OracleRequestTypeEnum.class),
                any(OracleTransactionStateEnum.class)
        );
    }

    @Test
    public void testProcessL2BatchProve() throws IOException {
        TransactionReceipt txReceipt = mockTransactionReceipt(
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong())
        );
        OracleRequestDO oracleRequestDO2 = OracleRequestDO.builder()
                .oracleType(OracleTypeEnum.L2_GAS_ORACLE)
                .oracleTaskType(OracleRequestTypeEnum.L2_BATCH_PROVE)
                .requestIndex(BigInteger.valueOf(1))
                .txState(OracleTransactionStateEnum.COMMITED)
                .rawData(JSON.toJSONBytes(txReceipt))
                .build();
        OracleRequestDO oracleRequestDO3 = OracleRequestDO.builder()
                .oracleType(OracleTypeEnum.L2_GAS_ORACLE)
                .oracleTaskType(OracleRequestTypeEnum.L2_BATCH_COMMIT)
                .requestIndex(BigInteger.valueOf(2))
                .txState(OracleTransactionStateEnum.COMMITED)
                .rawData(JSON.toJSONBytes(txReceipt))
                .build();
        OracleRequestDO oracleRequestDO4 = OracleRequestDO.builder()
                .oracleType(OracleTypeEnum.L2_GAS_ORACLE)
                .oracleTaskType(OracleRequestTypeEnum.L2_BATCH_PROVE)
                .requestIndex(BigInteger.valueOf(2))
                .txState(OracleTransactionStateEnum.COMMITED)
                .rawData(JSON.toJSONBytes(txReceipt))
                .build();
        BatchWrapper batchV0 = mockBatchV0();

        when(oracleRepository.peekRequests(eq(OracleTypeEnum.L2_GAS_ORACLE), eq(OracleRequestTypeEnum.L2_BATCH_PROVE), eq(OracleTransactionStateEnum.INIT), anyInt())).thenReturn(
                ListUtil.toList(
                        oracleRequestDO3, oracleRequestDO4
                )
        );
        when(oracleRepository.peekRequestByTypeAndIndex(
                eq(OracleTypeEnum.L2_GAS_ORACLE),
                eq(OracleRequestTypeEnum.L2_BATCH_PROVE),
                eq(String.valueOf(1))
        )).thenReturn(oracleRequestDO2);
        when(oracleRepository.peekRequestByTypeAndIndex(
                eq(OracleTypeEnum.L2_GAS_ORACLE),
                eq(OracleRequestTypeEnum.L2_BATCH_COMMIT),
                eq(String.valueOf(2))
        )).thenReturn(oracleRequestDO3);
        when(l2Client.updateBaseFeeScala(any(), any())).thenReturn(mockTransactionInfo());
        when(rollupRepository.getBatch(notNull())).thenReturn(batchV0);
        when(l2Client.updateBatchRollupFee(any(), any(), any())).thenReturn(mockTransactionInfo());

        oracleService.processBatchOracle();

        verify(oracleRepository, times(1)).peekRequests(
                eq(OracleTypeEnum.L2_GAS_ORACLE),
                any(OracleRequestTypeEnum.class),
                any(OracleTransactionStateEnum.class),
                anyInt()
        );
    }


    @Test
    public void testUpdateBatchBlobFeeAndTxFee() throws Exception {
        OracleFeeInfo latestOracleFeeInfo = (OracleFeeInfo) ReflectionTestUtils.getField(oracleService, "latestOracleFeeInfo");
        assert latestOracleFeeInfo != null;
        latestOracleFeeInfo.setStartBatchIndex(BigInteger.ONE);

        TransactionReceipt txReceipt = mockTransactionReceipt(String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()), String.valueOf(RandomUtil.randomInt()));

        OracleRequestDO oracleRequestDO = OracleRequestDO.builder()
                .requestIndex(BigInteger.valueOf(2))
                .rawData(JSON.toJSONBytes(txReceipt))
                .txState(OracleTransactionStateEnum.INIT)
                .build();

        OracleRequestDO oracleRequestDO1 = OracleRequestDO.builder()
                .requestIndex(BigInteger.valueOf(1))
                .txState(OracleTransactionStateEnum.COMMITED)
                .build();
        OracleRequestDO oracleRequestDO2 = OracleRequestDO.builder()
                .requestIndex(BigInteger.valueOf(2))
                .rawData(JSON.toJSONBytes(txReceipt))
                .txState(OracleTransactionStateEnum.INIT)
                .build();
        OracleRequestDO oracleRequestDO3 = OracleRequestDO.builder()
                .requestIndex(BigInteger.valueOf(1))
                .txState(OracleTransactionStateEnum.INIT)
                .build();
        when(oracleRepository.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE, String.valueOf(oracleRequestDO.getRequestIndex().subtract(BigInteger.ONE))))
                .thenReturn(oracleRequestDO1);
        when(oracleRepository.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, String.valueOf(oracleRequestDO.getRequestIndex())))
                .thenReturn(oracleRequestDO3);

        try {
            oracleService.updateBatchBlobFeeAndTxFee(oracleRequestDO);
        } catch (Exception e) {
            Assert.notNull(e);
        }

        BatchWrapper batchV0 = mockBatchV0();
        when(rollupRepository.getBatch(any(BigInteger.class))).thenReturn(batchV0);

        when(oracleRepository.peekRequestByTypeAndIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, String.valueOf(oracleRequestDO.getRequestIndex())))
                .thenReturn(oracleRequestDO2);
        TransactionInfo txInfo = mockTransactionInfo();
        when(l2Client.updateBatchRollupFee(any(), any(), any())).thenReturn(txInfo);

        oracleService.updateBatchBlobFeeAndTxFee(oracleRequestDO);
        Assert.notEquals(BigInteger.ZERO, latestOracleFeeInfo.getLastBatchDaFee());
        Assert.notEquals(BigInteger.ZERO, latestOracleFeeInfo.getLastBatchExecFee());
        Assert.notEquals(BigInteger.ZERO, latestOracleFeeInfo.getLastBatchByteLength());

        verify(rollupRepository, times(1)).getBatch(any(BigInteger.class));
        verify(l2Client, times(1)).updateBatchRollupFee(any(), any(), any());
        verify(oracleRepository, times(2)).updateRequestState(any(), any(), any(), any());
        verify(rollupRepository, times(1)).insertReliableTransaction(any(ReliableTransactionDO.class));

        oracleRequestDO1.setTxState(OracleTransactionStateEnum.INIT);
        oracleService.updateBatchBlobFeeAndTxFee(oracleRequestDO);
        verify(oracleRepository, atLeast(1)).peekLatestRequestIndex(any(), any(), any());
        verify(oracleRepository, times(4)).updateRequestState(any(), any(), any(), any());
        verify(oracleRepository, times(3)).peekRequestByTypeAndIndex(any(), any(), anyString());

        latestOracleFeeInfo.setStartBatchIndex(BigInteger.TWO);
        when(oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE, OracleTransactionStateEnum.COMMITED))
                .thenReturn(BigInteger.TEN);
        OracleRequestDO oracleRequestDO4 = OracleRequestDO.builder()
                .requestIndex(BigInteger.valueOf(4))
                .oracleTaskType(OracleRequestTypeEnum.L2_BATCH_PROVE)
                .txState(OracleTransactionStateEnum.INIT)
                .build();
        oracleService.updateBatchBlobFeeAndTxFee(oracleRequestDO4);
        verify(oracleRepository, times(5)).updateRequestState(any(), any(), any(), any());
    }

    // @Test
    public void testUpdateBlobBaseFeeScalaAndTxFeeScala() throws IOException {
        BigInteger mockRequestIndex = BigInteger.valueOf(2);
        int mockNumber = RandomUtil.randomInt();
        L1BlockFeeInfo mockL1BlockFee = L1BlockFeeInfo.builder()
                .number(String.valueOf(mockNumber))
                .baseFeePerGas(String.valueOf(RandomUtil.randomLong()))
                .gasUsed(String.valueOf(RandomUtil.randomLong()))
                .gasLimit(String.valueOf(RandomUtil.randomLong()))
                .blobGasUsed(String.valueOf(RandomUtil.randomLong()))
                .excessBlobGas(String.valueOf(RandomUtil.randomLong()))
                .build();
        OracleRequestDO oracleRequestDO = OracleRequestDO.builder()
                .requestIndex(mockRequestIndex)
                .rawData(JSON.toJSONBytes(mockL1BlockFee))
                .build();

        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        mockSendTx(txHash);

        TransactionInfo txInfo = TransactionInfo.builder()
                .txHash(mockTxHash)
                .nonce(BigInteger.valueOf(RandomUtil.randomInt()))
                .senderAccount(mockSender)
                .sendTxTime(new Date())
                .rawTx(JSON.toJSONBytes(HexUtil.decodeHex("00")))
                .build();

        when(l2Client.updateBaseFeeScala(any(), any())).thenReturn(txInfo);

        oracleService.updateBlobBaseFeeScalaAndTxFeeScala(oracleRequestDO);

        verify(oracleRepository, times(1)).updateRequestState(anyString(), eq(OracleTypeEnum.L2_GAS_ORACLE), eq(OracleRequestTypeEnum.L1_BLOCK_UPDATE), eq(OracleTransactionStateEnum.COMMITED));
        verify(rollupRepository, times(1)).insertReliableTransaction(any(ReliableTransactionDO.class));

        // 测试scala = 0
        ReflectionTestUtils.setField(oracleService, "oracleBaseFeeUpdateThreshold", 1000000000);
        oracleService.updateBlobBaseFeeScalaAndTxFeeScala(oracleRequestDO);
        verify(oracleRepository, times(1)).updateRequestState(anyString(), eq(OracleTypeEnum.L2_GAS_ORACLE), eq(OracleRequestTypeEnum.L1_BLOCK_UPDATE), eq(OracleTransactionStateEnum.SKIP));
    }

    @Test
    public void testUpdateFixedProfit() {
        BigInteger newFixedProfit = BigInteger.valueOf(RandomUtil.randomLong());
        oracleService.updateFixedProfit(newFixedProfit);

        when(l2Client.updateFixedProfit(any())).thenThrow(new RuntimeException("l2Client updateFixedProfit failed."));
        try {
            oracleService.updateFixedProfit(newFixedProfit);
        } catch (Exception e) {
            Assert.notNull(e);
        }
    }

    @Test
    public void testUpdateTotalScala() {
        BigInteger newTotal = BigInteger.valueOf(RandomUtil.randomLong());
        oracleService.updateTotalScala(newTotal);

        when(l2Client.updateTotalScala(any())).thenThrow(new RuntimeException("l2Client updateTotalScala failed."));
        try {
            oracleService.updateTotalScala(newTotal);
        } catch (Exception e) {
            Assert.notNull(e);
        }
    }

    @Test
    public void testWithdrawVault() {
        BigInteger account = BigInteger.valueOf(RandomUtil.randomLong());
        oracleService.withdrawVault(mockSender, account);

        when(l2Client.withdrawVault(anyString(), any())).thenThrow(new RuntimeException("l2Client withdrawVault failed."));
        try {
            oracleService.withdrawVault(anyString(), any());
        } catch (Exception e) {
            Assert.notNull(e);
        }
    }

    @Test
    public void testPredictNextBaseFee() {
        var l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .baseFeePerGas("1804488")
                .blobGasUsed("1179648")
                .excessBlobGas("116654080")
                .gasLimit("60000000")
                .gasUsed("59979840")
                .number("9168560")
                .build();
        BigInteger nextBaseFee = predictL1NextBaseFee(l1BlockFeeInfo);
        BigInteger nextBlobBaseFee = predictL1NextBlobBaseFee(l1BlockFeeInfo);
        BigInteger currentBaseFee = BigInteger.valueOf(1000000000);
        BigInteger currentBlobBaseFee = BigInteger.valueOf(1000000000);

        BigInteger baseFeeScala1 = calcBaseFeeScala2(currentBaseFee, nextBaseFee, 0);
        BigInteger blobBaseFeeScala1 = calcBaseFeeScala2(currentBlobBaseFee, nextBlobBaseFee, 0);

        // scala maybe convert to one forcefully when calc scala result was ZERO
        if (baseFeeScala1.equals(BigInteger.ONE)) {
            nextBaseFee = currentBaseFee.multiply(baseFeeScala1).divide(BigInteger.valueOf(100)); // if division made precision loss, not do any special deal.
        }
        if (blobBaseFeeScala1.equals(BigInteger.ONE)) {
            nextBlobBaseFee = currentBlobBaseFee.multiply(blobBaseFeeScala1).divide(BigInteger.valueOf(100));
        }

        // baseFee and blobBaseFee's upper limit both are 1Gwei
        if (nextBaseFee.compareTo(BigInteger.valueOf(1000000000)) > 0) {
            nextBaseFee = BigInteger.valueOf(1000000000);
        }
        if (nextBlobBaseFee.compareTo(BigInteger.valueOf(1000000000)) > 0) {
            nextBlobBaseFee = BigInteger.valueOf(1000000000);
        }
    }

    private EthBlock mockEthBlock(String baseFeePerGas, String gasUsed, String gasLimit, String blobGasUsed, String excessBlobGas) {
        EthBlock mockEthBlock = new EthBlock();
        EthBlock.Block mockBlock = new EthBlock.Block();
        mockBlock.setNumber(String.valueOf(2));
        mockBlock.setDifficulty("0x1");
        mockBlock.setNonce("0x1");
        mockBlock.setTimestamp("0x1");

        mockBlock.setBaseFeePerGas(baseFeePerGas);
        mockBlock.setGasUsed(gasUsed);
        mockBlock.setGasLimit(gasLimit);
        mockBlock.setBlobGasUsed(blobGasUsed);
        mockBlock.setExcessBlobGas(excessBlobGas);

        mockEthBlock.setResult(mockBlock);
        return mockEthBlock;
    }

    private TransactionReceipt mockTransactionReceipt(String number, String txIndex, String effectiveGasPrice, String gasUsed, String cumulativeGasUsed, String blobGasPrice, String blobGasUsed) {
        TransactionReceipt transactionReceipt = new TransactionReceipt();
        transactionReceipt.setBlockNumber(number);
        transactionReceipt.setTransactionIndex(txIndex);

        transactionReceipt.setEffectiveGasPrice(effectiveGasPrice);
        transactionReceipt.setGasUsed(gasUsed);
        transactionReceipt.setCumulativeGasUsed(cumulativeGasUsed);
        transactionReceipt.setBlobGasPrice(blobGasPrice);
        transactionReceipt.setBlobGasUsed(blobGasUsed);

        return transactionReceipt;
    }

    private TransactionInfo mockTransactionInfo() {
        return TransactionInfo.builder()
                .senderAccount(mockSender)
                .nonce(BigInteger.valueOf(RandomUtil.randomLong()))
                .txHash(mockTxHash)
                .rawTx(HexUtil.decodeHex("00"))
                .sendTxTime(new Date())
                .build();

    }

    private BatchWrapper mockBatchV0() {
        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        return BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
    }

    private void mockSendTx(String txHash) throws IOException {
        Request ethGetBlockByNumberReq = mock(Request.class);
        EthBlock ethBlock = new EthBlock();
        EthBlock.Block block = new EthBlock.Block();
        block.setBaseFeePerGas("123");
        ethBlock.setResult(block);
        when(ethGetBlockByNumberReq.send()).thenReturn(ethBlock);
        when(l2Web3j.ethGetBlockByNumber(eq(DefaultBlockParameterName.LATEST), anyBoolean())).thenReturn(ethGetBlockByNumberReq);

        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("10000");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l2Web3j.ethEstimateGas(any())).thenReturn(ethEstimateGasReq);

        Request ethMaxPriorityFeePerGasReq = mock(Request.class);
        EthMaxPriorityFeePerGas ethMaxPriorityFeePerGas = new EthMaxPriorityFeePerGas();
        ethMaxPriorityFeePerGas.setResult("1111111");
        when(ethMaxPriorityFeePerGasReq.send()).thenReturn(ethMaxPriorityFeePerGas);
        when(l2Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethMaxPriorityFeePerGasReq);

        Request ethGetTransactionCountReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("111");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l2Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        Request ethSendRawReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setResult(txHash);
        when(ethSendRawReq.send()).thenReturn(ethSendTransaction);
        when(l2Web3j.ethSendRawTransaction(any())).thenReturn(ethSendRawReq);
    }

    private void testPredictL1NextBaseFee() throws IOException {
        // sync block
        l1Web3j = Web3j.build(new HttpService("l1RpcUrl"));

        BigInteger number = BigInteger.valueOf(23216263);
        EthBlock.Block lastBlock = l1Web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), false).send().getBlock();

        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .number(number.toString())
                .baseFeePerGas(lastBlock.getBaseFeePerGas().toString())
                .gasUsed(lastBlock.getGasUsed().toString())
                .gasLimit(lastBlock.getGasLimit().toString())
                .blobGasUsed(lastBlock.getBlobGasUsed().toString())
                .excessBlobGas(lastBlock.getExcessBlobGas().toString())
                .build();
        BigInteger l1NextBlockBaseFee = predictL1NextBaseFee(l1BlockFeeInfo);

        // sync block
        EthBlock.Block nextBlock = l1Web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number.add(BigInteger.ONE)), false).send().getBlock();
        Assert.equals(nextBlock.getBaseFeePerGas(), l1NextBlockBaseFee);
    }

    private void testPredictL1NextBlobBaseFee() throws IOException {
        // sync block
        l1Web3j = Web3j.build(new HttpService("l1RpcUrl"));

        BigInteger number = BigInteger.valueOf(9059139);
        EthBlock.Block lastBlock = l1Web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), false).send().getBlock();

        L1BlockFeeInfo l1BlockFeeInfo = L1BlockFeeInfo.builder()
                .excessBlobGas(lastBlock.getExcessBlobGas().toString())
                .build();
        BigInteger l1NextBlobBaseFee = predictL1NextBlobBaseFee(l1BlockFeeInfo);

        // sync block
        EthBlock.Block nextBlock = l1Web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number.add(BigInteger.ONE)), false).send().getBlock();
        Assert.equals(l1NextBlobBaseFee, BigInteger.valueOf(2)); // Unit: WEI
    }

    private void testCalcBaseFeeScala() {
        // test currentBaseFee lower than nextBaseFee
        BigInteger currentBaseFee = BigInteger.valueOf(189735362);
        BigInteger nextBaseFee = BigInteger.valueOf(183120927); // 6614435
        int threshold = 3; // float 10%

        BigInteger baseFeeDelta = currentBaseFee.subtract(nextBaseFee);
        BigInteger floatPercentage = BigInteger.valueOf(100).multiply(baseFeeDelta).divide(currentBaseFee);

        BigInteger baseFeeScala = calcBaseFeeScala(currentBaseFee, nextBaseFee, threshold);
        Assert.equals(baseFeeScala, floatPercentage);

        // test not update baseFee
        threshold = 10; // float 10%
        baseFeeScala = calcBaseFeeScala(currentBaseFee, nextBaseFee, threshold);
        Assert.equals(baseFeeScala, BigInteger.ZERO);

        // swap currentBaseFee and nextBaseFee
        BigInteger temp = currentBaseFee;
        currentBaseFee = nextBaseFee;
        nextBaseFee = temp;

        // test currentBaseFee more than nextBaseFeeœ
        threshold = 3; // float: 3%
        baseFeeDelta = nextBaseFee.subtract(currentBaseFee);
        floatPercentage = BigInteger.valueOf(100).multiply(baseFeeDelta).divide(currentBaseFee);
        baseFeeScala = calcBaseFeeScala(currentBaseFee, nextBaseFee, threshold);
        Assert.equals(baseFeeScala, floatPercentage);

        // test not update baseFee
        threshold = 10;
        baseFeeScala = calcBaseFeeScala(currentBaseFee, nextBaseFee, threshold);
        Assert.equals(baseFeeScala, BigInteger.ZERO);
    }

    private BigInteger predictL1NextBlobBaseFee(L1BlockFeeInfo l1BlockFeeInfo) {
        BigInteger excessBlobGas = new BigInteger(l1BlockFeeInfo.getExcessBlobGas()); // 遵循EIP-4844计算规则
        return bpoConfig.fakeExponential(excessBlobGas);
    }

    private BigInteger predictL1NextBaseFee(L1BlockFeeInfo l1BlockFeeInfo) {
        OracleFeeInfo latestOracleFeeInfo = new OracleFeeInfo();

        // follow EIP-1559 protocol's calculate rule
        BigInteger baseFeePerGas = new BigInteger(l1BlockFeeInfo.getBaseFeePerGas());
        BigInteger gasUsed = new BigInteger(l1BlockFeeInfo.getGasUsed());
        BigInteger gasLimit = new BigInteger(l1BlockFeeInfo.getGasLimit());
        BigInteger gasTarget = gasLimit.divide(BigInteger.valueOf(2));

        // calculate next block's base fee
        BigInteger gasUsedDelta = gasUsed.subtract(gasTarget);
        if (gasUsed.compareTo(gasTarget) == 0) {
            return baseFeePerGas;
        } else if (gasUsed.compareTo(gasTarget) > 0) {
            // improve gasPrice, calc rule: parentBaseFee + max(1, parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            BigInteger incremental = baseFeePerGas.multiply(gasUsedDelta).divide(gasTarget).divide(latestOracleFeeInfo.getBaseFeeChangeDenominator()).max(BigInteger.valueOf(1));
            return baseFeePerGas.add(incremental);
        } else { // decrease gasPrice, calc rule: max(0, parentBaseFee - parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            BigInteger decremental = baseFeePerGas.multiply(gasUsedDelta).divide(gasTarget).divide(latestOracleFeeInfo.getBaseFeeChangeDenominator());
            return BigInteger.ZERO.max(baseFeePerGas.subtract(decremental));
        }
    }

    private BigInteger calcBaseFeeScala(BigInteger currentBaseFee, BigInteger nextBaseFee, int threshold) {
        // When the actual calculation is returned, Scala multiplies by 100 to provide decimal places of precision.
        if (nextBaseFee.compareTo(currentBaseFee) > 0 && ((BigInteger.valueOf(100).multiply(nextBaseFee.subtract(currentBaseFee))).compareTo(currentBaseFee.multiply(BigInteger.valueOf(threshold))) > 0)) {
            return (nextBaseFee.subtract(currentBaseFee).multiply(BigInteger.valueOf(100))).divide(currentBaseFee);
        } else if (nextBaseFee.compareTo(currentBaseFee) < 0 && (BigInteger.valueOf(100).multiply(currentBaseFee.subtract(nextBaseFee)).compareTo(currentBaseFee.multiply(BigInteger.valueOf(threshold))) > 0)) {
            return (currentBaseFee.subtract(nextBaseFee).multiply(BigInteger.valueOf(100)).divide(currentBaseFee));
        }
        return BigInteger.ZERO;
    }

    private BigInteger calcBaseFeeScala2(BigInteger currentBaseFee, BigInteger nextBaseFee, int threshold) {
        // When the actual calculation is returned, Scala multiplies by 100 to provide decimal places of precision.
        BigInteger feeDeltaEnlarge = BigInteger.valueOf(100).multiply(nextBaseFee.subtract(currentBaseFee).abs());
        BigInteger feeThreshold = currentBaseFee.multiply(BigInteger.valueOf(threshold));

        if (feeDeltaEnlarge.compareTo(feeThreshold) > 0) {
            BigInteger scala = (nextBaseFee.multiply(BigInteger.valueOf(100))).divide(currentBaseFee);
            // precision only attach .00, and scala cannot be ZERO, so convert to ONE forcefully.
            return scala.equals(BigInteger.ZERO) ? BigInteger.ONE : scala;
        } else {
            return BigInteger.valueOf(100);
        }
    }
}
