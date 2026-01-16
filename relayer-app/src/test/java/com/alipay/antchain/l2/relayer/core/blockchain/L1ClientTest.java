package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.Batch;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.config.BlockchainConfig;
import com.alipay.antchain.l2.relayer.config.ContractConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBpoBlobConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbFastRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasLimitPolicyEnum;
import com.alipay.antchain.l2.relayer.core.layer2.economic.NopeChecker;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategy;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.selfreport.ISelfReportMetric;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

import static org.mockito.Mockito.*;

@Slf4j
@SuppressWarnings("all")
public class L1ClientTest extends TestBase {

    public static final String L1MSG_LOGS_1 = """
            [
              {
                "address": "0x70256e48bac22cd411b65d162d043ea225e5bf0c",
                "blockHash": "0xdf92c2e5c78d8659f9e9e7cd52e881cdd918b4d7e0e764db3c6f2505cafcee6d",
                "blockNumber": "0x7d7ec6",
                "data": "0x000000000000000000000000000000000000000000000000016345785d8a0000000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000006acfc0509fd8bc855d02b6e6e915ddf39f1cfe5c64fbea6c98dfd654f8ff87bcc6c3ba000000000000000000000000000000000000000000000000000000000000018408a2c0bf00000000000000000000000010ec05757af363080443110bfd2e86c4406e473200000000000000000000000038675d92813338953b0f67e9cc36be59282b77e3000000000000000000000000000000000000000000000000016345785d8a0000000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000a46ef6d62600000000000000000000000054af26adce6225ba00ca40349ef38ea06793bc7300000000000000000000000054af26adce6225ba00ca40349ef38ea06793bc73000000000000000000000000000000000000000000000000016345785d8a0000000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                "logIndex": "0x286",
                "removed": false,
                "topics": [
                  "0xae3d495073f8b68ac52caee883181e95d8a4ee28cf92341dcb2548e0e4610505",
                  "0x00000000000000000000000010ec05757af363080443110bfd2e86c4406e4732",
                  "0x00000000000000000000000038675d92813338953b0f67e9cc36be59282b77e3"
                ],
                "transactionHash": "0x47a7ce460964cc07a781e3124f0ce80efddaef016cf4de5a78285b3d755ed167",
                "transactionIndex": "0x94"
              }
            ]""";

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean(name = "l1BlobPoolTxTransactionManager")
    private RawTransactionManager l1BlobPoolTxTransactionManager;

    @MockitoBean
    private RawTransactionManager l1LegacyPoolTxTransactionManager;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @Resource(name = "l1GasPriceProvider")
    private IGasPriceProvider l1GasPriceProvider;

    @Value("${l2-relayer.l1-client.gas-limit-policy:ESTIMATE}")
    private GasLimitPolicyEnum gasLimitPolicy;

    @Value("${l2-relayer.l1-client.extra-gas:0}")
    private BigInteger extraGas;

    @Value("${l2-relayer.l1-client.static-gas-limit:9000000}")
    private BigInteger staticGasLimit;

    @Value("${l2-relayer.l1-client.rollup-contract}")
    private String rollupContractAddress;

    @Value("${l2-relayer.l1-client.mailbox-contract}")
    private String mailboxContractAddress;

    @Resource(name = "fetchingL1MsgThreadsPool")
    private ExecutorService fetchingL1MsgThreadsPool;

    @Value("${l2-relayer.l1-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l1NoncePolicy;

    @Resource
    private RedissonClient redisson;

    @Resource
    private BlockchainConfig blockchainConfig;

    @Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Resource
    private RollupEconomicStrategy rollupEconomicStrategy;

    @Resource
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    private L1Client testClient;

    private BatchWrapper batchWrapper1;

    @Before
    @SneakyThrows
    public void init() {
        when(rollupConfig.getMaxCallDataInChunk()).thenReturn(1000_000L);
        when(rollupConfig.getOneChunkBlocksLimit()).thenReturn(32L);
        when(rollupConfig.getMaxTxsInChunks()).thenReturn(1000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
        when(rollupConfig.getChunkZkCycleSumLimit()).thenReturn(940_000_000L);

        Request getCodeRequest = mock(Request.class);
        EthGetCode ethGetCode = new EthGetCode();
        ethGetCode.setResult("0x1234");
        doReturn(ethGetCode).when(getCodeRequest).send();
        when(l1Web3j.ethGetCode(notNull(), notNull())).thenReturn(getCodeRequest);

        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        int maxTxsInChunks = rollupConfig.getMaxTxsInChunks();
        batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2), maxTxsInChunks))
        );
        batchWrapper1.setGmtCreate(System.currentTimeMillis());

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var l1BlobPoolTxManager = l1NoncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(l1Web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig)
                : new AcbRawTransactionManager(l1Web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);

        var l1LegacyPoolTxManager = l1NoncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(l1Web3j, blockchainConfig.getL1LegacyPoolTxSignService(), 123, redisson, ethForkBlobConfig)
                : new AcbRawTransactionManager(l1Web3j, blockchainConfig.getL1LegacyPoolTxSignService(), 123, redisson, ethForkBlobConfig);

        testClient = new L1Client(
                l1Web3j, l1BlobPoolTxManager, l1LegacyPoolTxManager,
                l1GasPriceProvider, gasLimitPolicy, extraGas, staticGasLimit, rollupContractAddress, mailboxContractAddress,
                0.1, 1, 900000, BigInteger.valueOf(100_000_000_000L),
                BigInteger.valueOf(1000_000_000_000L), fetchingL1MsgThreadsPool, rollupEconomicStrategy
        );
        Field field = ReflectUtil.getField(L1Client.class, "selfReportMetric");
        field.setAccessible(true);
        field.set(testClient, mock(ISelfReportMetric.class));
        field = ReflectUtil.getField(L1Client.class, "rollupRepository");
        field.setAccessible(true);
        field.set(testClient, rollupRepository);
        field = ReflectUtil.getField(L1Client.class, "redisson");
        field.setAccessible(true);
        field.set(testClient, redisson);
        testClient.getBlobPoolTxManager().setTxHashVerifier(txHashVerifier);
        testClient.getLegacyPoolTxManager().setTxHashVerifier(txHashVerifier);

        var contractConfig = new ContractConfig();
        var resourcePatternResolverField = ReflectUtil.getField(ContractConfig.class, "resourcePatternResolver");
        resourcePatternResolverField.setAccessible(true);
        resourcePatternResolverField.set(contractConfig, resourcePatternResolver);
        var contractErrorParser = contractConfig.contractErrorParser();
        field = ReflectUtil.getField(L1Client.class, "contractErrorParser");
        field.setAccessible(true);
        field.set(testClient, contractErrorParser);
    }

    @Test
    @SneakyThrows
    public void testCommitBatch() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("a68575b4")), notNull()))
                .thenReturn(ethCallReq);
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        when(l1Web3j.ethGetBaseFeePerBlobGas()).thenReturn(BigInteger.valueOf(100));

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        TransactionInfo result = testClient.commitBatch(batchWrapper1, ZERO_BATCH_HEADER);
        Assert.assertEquals(txhash, result.getTxHash());

        var prevConfigVal = rollupEconomicStrategyConfig.getMidEip1559PriceLimit();
        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(Convert.toWei("1", Convert.Unit.GWEI).toBigInteger());

        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> testClient.commitBatch(batchWrapper1, ZERO_BATCH_HEADER));
        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(prevConfigVal);
    }

    @Test
    @SneakyThrows
    public void testVerifyBatch() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        when(rollupRepository.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(0);

        TransactionInfo result = testClient.verifyBatch(batchWrapper1, req);
        Assert.assertEquals(txhash, result.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testLastTeeVerifiedBatch() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.lastTeeVerifiedBatch();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testLastZkeVerifiedBatch() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.lastZkVerifiedBatch();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testLastCommittedBatch() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.lastCommittedBatch();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testMaxTxsInChunk() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.maxTxsInChunk();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testMaxZkCircleInChunk() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.maxZkCircleInChunk();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testL1BlobNumLimit() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        long actual = testClient.l1BlobNumLimit();
        Assert.assertEquals(1L, actual);
    }

    @Test
    @SneakyThrows
    public void testMaxBlockInChunk() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.maxBlockInChunk();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testMaxCallDataInChunk() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger actual = testClient.maxCallDataInChunk();
        Assert.assertEquals(BigInteger.valueOf(1), actual);
    }

    @Test
    @SneakyThrows
    public void testMaxTimeIntervalBetweenBatches() {
        var ethCallReq = mock(Request.class);
        var result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        long actual = testClient.maxTimeIntervalBetweenBatches();
        Assert.assertEquals(1L, actual);
    }

    @Test
    @SneakyThrows
    public void testZkVerificationStartBatch() {
        var ethCallReq = mock(Request.class);
        var result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        var actual = testClient.zkVerificationStartBatch();
        Assert.assertEquals(1L, actual.longValue());
    }

    @Test
    @SneakyThrows
    public void testCommittedBatchHash() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        byte[] actual = testClient.committedBatchHash(BigInteger.ONE);
        Assert.assertArrayEquals(HexUtil.decodeHex("0000000000000000000000000000000000000000000000000000000000000001"), actual);
    }

    @Test
    @SneakyThrows
    public void testResendRollupTx() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        TransactionInfo result = testClient.resendRollupTx(null, "");
        Assert.assertEquals(txhash, result.getTxHash());

        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "WARNING"));
        when(ethCallReq.send()).thenReturn(revertCall);
        Assert.assertThrows(L1ContractWarnException.class, () -> testClient.resendRollupTx(null, ""));

        revertCall.setError(new Response.Error(1, "INVALID_PERMISSION"));
        when(ethCallReq.send()).thenReturn(revertCall);
        Assert.assertThrows(L1ContractInvalidPermissionException.class, () -> testClient.resendRollupTx(null, ""));

        revertCall.setError(new Response.Error(1, "INVALID_PARAMETER"));
        when(ethCallReq.send()).thenReturn(revertCall);
        Assert.assertThrows(L1ContractInvalidParameterException.class, () -> testClient.resendRollupTx(null, ""));

        revertCall.setError(new Response.Error(1, "ERROR"));
        when(ethCallReq.send()).thenReturn(revertCall);
        Assert.assertThrows(L1ContractSeriousException.class, () -> testClient.resendRollupTx(null, ""));

        revertCall.setError(new Response.Error(1, "123"));
        when(ethCallReq.send()).thenReturn(revertCall);
        Assert.assertThrows(L2RelayerException.class, () -> testClient.resendRollupTx(null, ""));

        var err = new Response.Error(1, "execution reverted");
        err.setData("0x6dfcc65000000000000000000000000000000000000000000000000000000000000000f8000000000000000000000000000000000000000000000000000000000000007b");
        revertCall.setError(err);
        when(ethCallReq.send()).thenReturn(revertCall);
        try {
            testClient.resendRollupTx(null, "");
        } catch (L2RelayerException e) {
            Assert.assertTrue(StrUtil.contains(e.getMessage(), "SafeCastOverflowedUintDowncast:[248, 123]"));
        }
    }

    @Test
    @SneakyThrows
    public void testSpeedUpRollupTx() {
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        Request ethEstimateGasReq = mock(Request.class);
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x59682f00");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x11e1a300");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult(Numeric.encodeQuantity(BigInteger.valueOf(8147)));
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), notNull())).thenReturn(ethGetNonceReq);

        var rawTx = "0xF91A54821FD384B2D05E008405F5E10094F88F8C84D4C3DCECD6BE49F7B60754C2D888D33880B919E41325ACA000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000014000000000000000000000000000000000000000000000000000000000000019C0000000000000000000000000000000000000000000000000000000000000009900000000000000000000000000000000000000000000000000BAC4320768BC80B363E3D087C8DECDD621F65F9C335E4603BC63525ED57AAA7C0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000012000000000000000000000000000000000000000000000000000000000000008E000000000000000000000000000000000000000000000000000000000000010A00000000000000000000000000000000000000000000000000000000000000067010000000000000001000001949333DD290000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000001000000000026E58080840C84588094410000000000000000000000000000000000000080843CF80E6C808080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000781200000000000000002000001949333E8F20000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000003000001949333F4BA0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000400000194933400830000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000050000019493340C4B0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000600000194933418130000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000700000194933423D20000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000080000019493342F9A0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000090000019493343B630000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000A000001949334472B0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000B00000194933452F30000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000C0000019493345EBB0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000D0000019493346A830000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000E000001949334764C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000F00000194933482140000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000100000019493348DDC0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001100000194933499A40000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000012000001949334A56B0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000013000001949334B1330000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000014000001949334BCF20000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000015000001949334C8BA0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000016000001949334D4830000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000017000001949334E04B0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000018000001949334EC140000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000019000001949334F7DD0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001A00000194933503A50000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001B0000019493350F6D0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001C0000019493351B360000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001D00000194933526FF0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001E00000194933532C80000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000001F0000019493353E8F0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000200000019493354A570000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000021000001949335561F0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000078120000000000000002200000194933561DD0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000230000019493356DA60000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000024000001949335796E0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002500000194933585370000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002600000194933590FE0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000270000019493359CC40000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000028000001949335A88C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000029000001949335B4550000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002A000001949335C01D0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002B000001949335CBE50000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002C000001949335D7AD0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002D000001949335E36C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002E000001949335EF350000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000002F000001949335FAFD0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003000000194933606BC0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000031000001949336127A0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000320000019493361E430000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000330000019493362A0B0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003400000194933635D40000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000035000001949336419C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000360000019493364D5C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003700000194933659230000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003800000194933664EB0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003900000194933670B30000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003A0000019493367C7C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003B00000194933688440000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003C000001949336940C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003D0000019493369FD40000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003E000001949336AB9C0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000003F000001949336B7640000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000040000001949336C32D0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000041000001949336CEF50000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000781200000000000000042000001949336DABD0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000043000001949336E6860000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000044000001949336F24E0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000045000001949336FE170000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004600000194933709DF0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000047000001949337159E0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004800000194933721670000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000490000019493372D2F0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004A00000194933738F70000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004B00000194933744C00000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004C00000194933750890000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004D0000019493375C510000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004E000001949337681A0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000004F00000194933773E20000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000500000019493377FA10000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000510000019493378B5F0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005200000194933797280000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000053000001949337A2F00000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000054000001949337AEB80000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000055000001949337BA810000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000056000001949337C6490000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000057000001949337D2100000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000058000001949337DDD80000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000059000001949337E9A00000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005A000001949337F5680000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005B000001949338012F0000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005C0000019493380CF80000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005D00000194933818C00000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005E00000194933824890000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000005F00000194933830510000000000000000000000000000000000000000000000000000000000000000000000003B9ACA000000000000000000000000600000019493383C190000000000000000000000000000000000000000000000000000000000000000000000003B9ACA0000000000000000000000006100000194933847E20000000000000000000000000000000000000000000000000000000000000000000000003B9ACA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008718E5BB3ABD109FA05095A6853314A31DC2004DCA39DF365B287E105DDA22C53A6540888A1373BBBAA06F58C69288E2E846D80B6D805A56006E00A079326CCED718B2AF68757E3971F6";
        var tx = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .rawTx(Numeric.hexStringToByteArray(rawTx))
                .batchIndex(BigInteger.ONE)
                .latestTxSendTime(new Date())
                .build();
        var result = testClient.speedUpRollupTx(tx);
        Assert.assertEquals(txhash, result.getTxHash());

        ethGasMaxProrityPrice.setResult("0x00");
        Assert.assertThrows(NoNeedToSpeedUpException.class, () -> testClient.speedUpRollupTx(tx));

        // replace legacy with eip4844 tx
        rawTx = FileUtil.readString("data/raw_eip4844_tx", Charset.defaultCharset());
        tx.setRawTx(Numeric.hexStringToByteArray(rawTx));

        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(
                ReliableTransactionDO.builder()
                        .state(ReliableTransactionStateEnum.TX_PENDING)
                        .build()
        );
        Assert.assertThrows(PreviousTxNotReadyException.class, () -> testClient.speedUpRollupTx(tx));

        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(
                ReliableTransactionDO.builder()
                        .state(ReliableTransactionStateEnum.TX_PACKAGED)
                        .gmtModified(new Date(System.currentTimeMillis() - 3600_000))
                        .build()
        );
        result = testClient.speedUpRollupTx(tx);
        Assert.assertEquals(txhash, result.getTxHash());
        var resTx = EthTxDecoder.decode(Numeric.toHexString(result.getRawTx()));
        var resTx4844 = ((Transaction4844) resTx.getTransaction());
        Assert.assertEquals(BigInteger.valueOf(1000), resTx4844.getMaxFeePerBlobGas());
        Assert.assertEquals(BigInteger.valueOf(1000000000), resTx4844.getMaxPriorityFeePerGas());
        Assert.assertEquals(BigInteger.valueOf(4000000000L), resTx4844.getMaxFeePerGas());

        tx.setLatestTxSendTime(new Date(System.currentTimeMillis() - 1800_000));
        when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(
                ReliableTransactionDO.builder()
                        .state(ReliableTransactionStateEnum.TX_PACKAGED)
                        .gmtModified(new Date(System.currentTimeMillis() - 3600_000))
                        .build()
        );
        result = testClient.speedUpRollupTx(tx);
        Assert.assertEquals(txhash, result.getTxHash());

        var tx1 = ReliableTransactionDO.builder()
                .chainType(ChainTypeEnum.LAYER_ONE)
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .rawTx(Numeric.hexStringToByteArray(rawTx))
                .batchIndex(BigInteger.ONE)
                .latestTxSendTime(new Date())
                .build();
        result = testClient.speedUpRollupTx(tx1, BigInteger.valueOf(0), Convert.toWei("3", Convert.Unit.GWEI).toBigInteger(), BigInteger.valueOf(0));
        Assert.assertEquals(txhash, result.getTxHash());
        var lock = redisson.getLock(StrUtil.format("SPEEDUP_TX_{}-{}-{}", tx.getChainType().name(), tx.getTransactionType(), tx.getBatchIndex()));
        var f = CompletableFuture.runAsync(() -> {
            lock.lock(3, TimeUnit.SECONDS);
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        var cnt = 1;
        while (!lock.isLocked() && cnt++ < 100) {
            Thread.sleep(50);
        }
        Assert.assertThrows(SpeedupAsyncNowException.class, () -> testClient.speedUpRollupTx(tx1, BigInteger.valueOf(0), Convert.toWei("1", Convert.Unit.ETHER).toBigInteger(), BigInteger.valueOf(0)));
        lock.forceUnlock();
        f.cancel(true);
    }

    @Test
    @SneakyThrows
    public void testQuery() {
        Request ethGetBlockByNumberReq = mock(Request.class);
        EthBlock.Block block = new EthBlock.Block();
        block.setNumber("0x1234");
        EthBlock ethBlock = new EthBlock();
        ethBlock.setResult(block);
        when(ethGetBlockByNumberReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), anyBoolean())).thenReturn(ethGetBlockByNumberReq);

        Assert.assertEquals(block.getNumber(), testClient.queryLatestBlockNumber(DefaultBlockParameterName.LATEST));
        Assert.assertEquals(block, testClient.queryLatestBlockHeader(DefaultBlockParameterName.LATEST).getBlock());
        Assert.assertEquals(block, testClient.queryBlockByNumber(BigInteger.valueOf(1234)).getBlock());

        Request ethGetTransactionReceiptReq = mock(Request.class);
        EthGetTransactionReceipt receipt = new EthGetTransactionReceipt();
        receipt.setResult(new TransactionReceipt());
        when(ethGetTransactionReceiptReq.send()).thenReturn(receipt);
        when(l1Web3j.ethGetTransactionReceipt(anyString())).thenReturn(ethGetTransactionReceiptReq);

        Assert.assertNotNull(testClient.queryTxReceipt(""));

        Request ethGetTransactionByHashReq = mock(Request.class);
        EthTransaction ethTransaction = new EthTransaction();
        ethTransaction.setResult(new Transaction());
        when(ethGetTransactionByHashReq.send()).thenReturn(ethTransaction);
        when(l1Web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTransactionByHashReq);

        Assert.assertNotNull(testClient.queryTx(""));

        Request ethGetTransactionCountReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), notNull())).thenReturn(ethGetTransactionCountReq);

        Assert.assertEquals(BigInteger.ONE, testClient.queryTxCount("", DefaultBlockParameterName.LATEST));
    }

    @Test
    @SneakyThrows
    public void testSendTransaction() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        testClient.ethCall(rollupContractAddress, FunctionEncoder.encode(new Function("test", new ArrayList<>(), new ArrayList<>())));
        TransactionInfo result = testClient.sendTransaction(rollupContractAddress, new Function("test", new ArrayList<>(), new ArrayList<>()), NopeChecker.INSTANCE);
        Assert.assertEquals(txhash, result.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testSendRawTx() {
        var req = mock(Request.class);
        when(l1Web3j.ethSendRawTransaction(notNull())).thenReturn(req);
        var res = new EthSendTransaction();
        when(req.send()).thenReturn(res);
        var resp = testClient.sendRawTx(HexUtil.decodeHex("1234"));
        Assert.assertEquals(res, resp);

        when(req.send()).thenThrow(new IOException());
        Assert.assertThrows(RuntimeException.class, () -> testClient.sendRawTx(HexUtil.decodeHex("1234")));
    }

    @Test
    @SneakyThrows
    public void testGetBalance() {
        var req = mock(Request.class);
        when(l1Web3j.ethGetBalance(notNull(), notNull())).thenReturn(req);
        var res = new EthGetBalance();
        res.setResult("0x1234");
        when(req.send()).thenReturn(res);
        var resp = testClient.queryAccountBalance("0x1234", DefaultBlockParameterName.LATEST);
        Assert.assertEquals(res.getBalance(), resp);

        when(req.send()).thenThrow(new RuntimeException());
        Assert.assertThrows(RuntimeException.class, () -> testClient.queryAccountBalance("0x1234", DefaultBlockParameterName.LATEST));
    }

    @Test
    @SneakyThrows
    public void testFlowableL1MsgFromMailbox() {
        var req1 = mock(Request.class);
        var logs1 = new EthLog();
        List<EthLog.LogObject> objects1 = JSON.parseArray(L1MSG_LOGS_1, EthLog.LogObject.class).stream().toList();
        logs1.setResult(objects1.stream().map(x -> (EthLog.LogResult) x).toList());
        when(req1.send()).thenReturn(logs1);

        when(l1Web3j.ethGetLogs(argThat(
                arg -> ObjectUtil.isNotNull(arg) && StrUtil.equalsIgnoreCase(arg.getFromBlock().getValue(), "0x1")
        ))).thenReturn(req1);

        var req2 = mock(Request.class);
        var logs2 = new EthLog();
        List<EthLog.LogObject> objects2 = JSON.parseArray(L1MSG_LOGS_1, EthLog.LogObject.class).stream().toList();
        logs2.setResult(objects2.stream().map(x -> (EthLog.LogResult) x).toList());
        when(req2.send()).thenReturn(logs2);

        when(l1Web3j.ethGetLogs(argThat(
                arg -> ObjectUtil.isNotNull(arg) && StrUtil.equalsIgnoreCase(arg.getFromBlock().getValue(), "0x2")
        ))).thenReturn(req2);

        var req3 = mock(Request.class);
        var logs3 = new EthLog();
        logs3.setResult(new ArrayList<>());
        when(req3.send()).thenReturn(logs3);

        when(l1Web3j.ethGetLogs(argThat(
                arg -> ObjectUtil.isNotNull(arg) && StrUtil.equalsIgnoreCase(arg.getFromBlock().getValue(), "0x3")
        ))).thenReturn(req3);

        var flow = testClient.flowableL1MsgFromMailbox(BigInteger.ONE, BigInteger.valueOf(3));

        var resList = flow.toSortedList((o1, o2) -> o1.getHeight().compareTo(o2.getHeight())).blockingGet();
        var iterator = resList.iterator();
        var l1MsgTransactionBatch = iterator.next();
        Assert.assertEquals(BigInteger.ONE, l1MsgTransactionBatch.getHeight());
        Assert.assertEquals(1, l1MsgTransactionBatch.getL1MsgTransactionInfos().size());
        Assert.assertEquals(BigInteger.valueOf(28), l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getL1MsgTransaction().getNonce());
        Assert.assertEquals(
                objects1.get(0).get().getTransactionHash(),
                l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getSourceTxHash()
        );
        Assert.assertEquals(
                objects1.get(0).get().getBlockNumber(),
                l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getSourceBlockHeight()
        );

        l1MsgTransactionBatch = iterator.next();
        Assert.assertEquals(BigInteger.TWO, l1MsgTransactionBatch.getHeight());
        Assert.assertEquals(1, l1MsgTransactionBatch.getL1MsgTransactionInfos().size());
        Assert.assertEquals(BigInteger.valueOf(28), l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getL1MsgTransaction().getNonce());
        Assert.assertEquals(
                objects1.get(0).get().getTransactionHash(),
                l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getSourceTxHash()
        );
        Assert.assertEquals(
                objects1.get(0).get().getBlockNumber(),
                l1MsgTransactionBatch.getL1MsgTransactionInfos().get(0).getSourceBlockHeight()
        );

        Assert.assertTrue(iterator.next().getL1MsgTransactionInfos().isEmpty());
    }

    //    @Test
    @SneakyThrows
    public void testConnect() {
        BatchWrapper batchWrapper = new BatchWrapper();
        List<Chunk> chunkList = new ArrayList<>();
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000001000001946a40edac0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000002000001946a40f9740000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000003000001946a41053d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000004000001946a4111050000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000005000001946a411ccd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000006000001946a4128960000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000007000001946a41345e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000008000001946a4140260000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000009000001946a414bee0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000a000001946a4157b60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000b000001946a41637e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000c000001946a416f460000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000d000001946a417b0d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000e000001946a4186d60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000000f000001946a41929e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000010000001946a419e660000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000011000001946a41aa2d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000012000001946a41b5f70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000013000001946a41c1bf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000014000001946a41cd870000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000015000001946a41d94f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000016000001946a41e5170000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000017000001946a41f0de0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000018000001946a41fca70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000019000001946a42086f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001a000001946a4214370000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001b000001946a421ffe0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001c000001946a422bc60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001d000001946a42378f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001e000001946a4243570000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000001f000001946a424f200000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000020000001946a425adf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000021000001946a4266a60000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000022000001946a42726f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000023000001946a427e360000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000024000001946a4289ff0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000025000001946a4295c70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000026000001946a42a1850000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000027000001946a42ad4d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000028000001946a42b9160000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000029000001946a42c4de0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002a000001946a42d0a60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002b000001946a42dc6e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002c000001946a42e8350000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002d000001946a42f3f40000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002e000001946a42ffbc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000002f000001946a430b850000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000030000001946a43174d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000031000001946a4323140000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000032000001946a432edd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000033000001946a433aa50000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000034000001946a43466e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000035000001946a4352350000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000036000001946a435df40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000037000001946a4369bb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000038000001946a4375830000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000039000001946a43814b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003a000001946a438d130000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003b000001946a4398db0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003c000001946a43a4a30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003d000001946a43b06c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003e000001946a43bc340000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000003f000001946a43c7fc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000040000001946a43d3c30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000041000001946a43df8b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000042000001946a43eb530000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000043000001946a43f71c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000044000001946a4402e40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000045000001946a440ead0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000046000001946a441a740000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000047000001946a4426330000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000048000001946a4431fb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000049000001946a443dc20000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004a000001946a44498a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004b000001946a4455510000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004c000001946a44611a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004d000001946a446ce20000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004e000001946a4478a90000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000004f000001946a4484710000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000050000001946a44903a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000051000001946a449c020000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000052000001946a44a7c10000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000053000001946a44b3890000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000054000001946a44bf510000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000055000001946a44cb190000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000056000001946a44d6e10000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000057000001946a44e2aa0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000058000001946a44ee730000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000059000001946a44fa3c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005a000001946a4506040000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005b000001946a4511cd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005c000001946a451d940000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005d000001946a45295b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005e000001946a4535230000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000005f000001946a4540eb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000060000001946a454cb30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000061000001946a4558720000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000062000001946a45643b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000063000001946a4570030000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000064000001946a457bcb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000065000001946a4587920000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000066000001946a4593510000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000067000001946a459f1a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000068000001946a45aae20000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000069000001946a45b6aa0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006a000001946a45c2730000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006b000001946a45ce3b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006c000001946a45da030000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006d000001946a45e5cb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006e000001946a45f1940000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000006f000001946a45fd5c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000070000001946a4609240000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000071000001946a4614ed0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000072000001946a4620b30000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000073000001946a462c7c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000074000001946a4638430000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000075000001946a46440c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000076000001946a464fd40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000077000001946a465b9b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000078000001946a4667640000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000079000001946a46732c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007a000001946a467ef40000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007b000001946a468abc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007c000001946a4696850000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007d000001946a46a24d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007e000001946a46ae150000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000007f000001946a46b9de0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000080000001946a46c5a60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000081000001946a46d16f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000082000001946a46dd360000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000083000001946a46e8fe0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000084000001946a46f4c60000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000085000001946a47008e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000086000001946a470c560000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000087000001946a47181f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000088000001946a4723e70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000089000001946a472fa60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008a000001946a473b6e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008b000001946a4747360000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008c000001946a4752fe0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008d000001946a475ebd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008e000001946a476a850000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000008f000001946a47764d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000090000001946a4782150000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000091000001946a478ddd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000092000001946a4799a50000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000093000001946a47a5640000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000094000001946a47b1230000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000095000001946a47bcea0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000096000001946a47c8b20000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000097000001946a47d47b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000098000001946a47e0430000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000099000001946a47ec0c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009a000001946a47f7d40000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009b000001946a4803930000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009c000001946a480f510000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009d000001946a481b1a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009e000001946a4826e10000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000009f000001946a4832a90000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a0000001946a483e690000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x2000000000000000a1000001946a484a320000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a2000001946a4855fa0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a3000001946a4861b90000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a4000001946a486d810000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a5000001946a48794a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a6000001946a4885120000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a7000001946a4890da0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a8000001946a489ca30000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000a9000001946a48a86a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000aa000001946a48b4330000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ab000001946a48bffa0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ac000001946a48cbc10000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ad000001946a48d7890000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ae000001946a48e3510000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000af000001946a48ef190000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b0000001946a48fae10000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b1000001946a4906a90000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b2000001946a4912720000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b3000001946a491e390000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b4000001946a492a020000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b5000001946a4935cb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b6000001946a4941930000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b7000001946a494d5c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b8000001946a4959240000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000b9000001946a4964ed0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ba000001946a4970b50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000bb000001946a497c7d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000bc000001946a4988450000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000bd000001946a49940d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000be000001946a499fd60000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000bf000001946a49ab9e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c0000001946a49b7650000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x2000000000000000c1000001946a49c32d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c2000001946a49cef40000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c3000001946a49dabc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c4000001946a49e6840000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c5000001946a49f24c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c6000001946a49fe140000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c7000001946a4a09dc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c8000001946a4a15a50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000c9000001946a4a216d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ca000001946a4a2d350000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000cb000001946a4a38fe0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000cc000001946a4a44c60000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000cd000001946a4a508e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ce000001946a4a5c560000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000cf000001946a4a681f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d0000001946a4a73e70000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d1000001946a4a7faf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d2000001946a4a8b770000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d3000001946a4a973f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d4000001946a4aa3070000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d5000001946a4aaecf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d6000001946a4aba970000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d7000001946a4ac65f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d8000001946a4ad2270000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000d9000001946a4addf00000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000da000001946a4ae9b80000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000db000001946a4af5800000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000dc000001946a4b01490000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000dd000001946a4b0d110000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000de000001946a4b18da0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000df000001946a4b24a20000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e0000001946a4b306a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x2000000000000000e1000001946a4b3c330000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e2000001946a4b47fb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e3000001946a4b53b90000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e4000001946a4b5f820000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e5000001946a4b6b490000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e6000001946a4b77120000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e7000001946a4b82da0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e8000001946a4b8ea20000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000e9000001946a4b9a6a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ea000001946a4ba6330000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000eb000001946a4bb1fc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ec000001946a4bbdc40000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ed000001946a4bc98c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ee000001946a4bd5530000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ef000001946a4be11c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f0000001946a4bece50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f1000001946a4bf8ad0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f2000001946a4c04750000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f3000001946a4c10340000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f4000001946a4c1bfc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f5000001946a4c27c40000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f6000001946a4c338d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f7000001946a4c3f550000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f8000001946a4c4b1d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000f9000001946a4c56e50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000fa000001946a4c62ac0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000fb000001946a4c6e740000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000fc000001946a4c7a3d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000fd000001946a4c86040000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000fe000001946a4c91cd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000000ff000001946a4c9d940000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000100000001946a4ca95d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000101000001946a4cb5250000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000102000001946a4cc0ed0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000103000001946a4cccb60000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000104000001946a4cd87e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000105000001946a4ce4470000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000106000001946a4cf00f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000107000001946a4cfbd70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000108000001946a4d079f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000109000001946a4d13680000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010a000001946a4d1f300000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010b000001946a4d2af80000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010c000001946a4d36c10000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010d000001946a4d42880000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010e000001946a4d4e500000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000010f000001946a4d5a180000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000110000001946a4d65e10000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000111000001946a4d71a80000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000112000001946a4d7d710000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000113000001946a4d89390000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000114000001946a4d95020000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000115000001946a4da0ca0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000116000001946a4dac910000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000117000001946a4db85a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000118000001946a4dc4230000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000119000001946a4dcfeb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011a000001946a4ddbb20000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011b000001946a4de77b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011c000001946a4df3430000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011d000001946a4dff0b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011e000001946a4e0aca0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000011f000001946a4e16920000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000120000001946a4e225b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000121000001946a4e2e230000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000122000001946a4e39eb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000123000001946a4e45b30000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000124000001946a4e517b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000125000001946a4e5d430000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000126000001946a4e690c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000127000001946a4e74d40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000128000001946a4e809c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000129000001946a4e8c650000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012a000001946a4e982c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012b000001946a4ea3eb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012c000001946a4eafb30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012d000001946a4ebb7c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012e000001946a4ec7430000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000012f000001946a4ed30c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000130000001946a4eded30000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000131000001946a4eea9b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000132000001946a4ef6640000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000133000001946a4f022c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000134000001946a4f0df50000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000135000001946a4f19bc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000136000001946a4f25850000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000137000001946a4f314d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000138000001946a4f3d160000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000139000001946a4f48df0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013a000001946a4f54a70000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013b000001946a4f60700000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013c000001946a4f6c390000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013d000001946a4f78010000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013e000001946a4f83c90000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000013f000001946a4f8f870000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000140000001946a4f9b500000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000141000001946a4fa7180000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000142000001946a4fb2e00000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000143000001946a4fbea80000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000144000001946a4fca710000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000145000001946a4fd63a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000146000001946a4fe2020000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000147000001946a4fedca0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000148000001946a4ff9920000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000149000001946a50055a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014a000001946a5011220000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014b000001946a501ceb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014c000001946a5028b30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014d000001946a50347b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014e000001946a5040440000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000014f000001946a504c0d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000150000001946a5057d40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000151000001946a50639c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000152000001946a506f650000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000153000001946a507b2d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000154000001946a5086f50000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000155000001946a5092be0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000156000001946a509e860000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000157000001946a50aa4e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000158000001946a50b6160000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000159000001946a50c1de0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015a000001946a50cda70000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015b000001946a50d96e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015c000001946a50e5360000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015d000001946a50f0fe0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015e000001946a50fcc70000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000015f000001946a51088f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000160000001946a5114570000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000161000001946a51201f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000162000001946a512be70000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000163000001946a5137ae0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000164000001946a5143750000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000165000001946a514f3e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000166000001946a515b060000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000167000001946a5166cf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000168000001946a5172950000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000169000001946a517e5b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016a000001946a518a230000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016b000001946a5195eb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016c000001946a51a1b30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016d000001946a51ad7b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016e000001946a51b9440000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000016f000001946a51c50c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000170000001946a51d0d50000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000171000001946a51dc9d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000172000001946a51e8650000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000173000001946a51f42d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000174000001946a51ffec0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000175000001946a520bb40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000176000001946a52177c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000177000001946a5223440000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000178000001946a522f0b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000179000001946a523ad30000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017a000001946a52469b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017b000001946a5252630000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017c000001946a525e2c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017d000001946a5269f50000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017e000001946a5275bd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000017f000001946a5281850000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000180000001946a528d4c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x200000000000000181000001946a5299140000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000182000001946a52a4dc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000183000001946a52b0a40000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000184000001946a52bc6d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000185000001946a52c8350000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000186000001946a52d3fd0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000187000001946a52dfc60000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000188000001946a52eb8e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000189000001946a52f7570000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018a000001946a53031f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018b000001946a530ee70000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018c000001946a531aaf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018d000001946a5326770000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018e000001946a53323f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000018f000001946a533e070000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000190000001946a5349cf0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000191000001946a5355970000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000192000001946a5361600000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000193000001946a536d280000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000194000001946a5378f00000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000195000001946a5384b80000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000196000001946a53907f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000197000001946a539c470000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000198000001946a53a80f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca00000000000000000000000199000001946a53b3d60000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019a000001946a53bf9e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019b000001946a53cb660000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019c000001946a53d7240000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019d000001946a53e2ed0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019e000001946a53eeb50000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000000000000000019f000001946a53fa7c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a0000001946a5406440000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x2000000000000001a1000001946a54120d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a2000001946a541dd50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a3000001946a54299d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a4000001946a5435650000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a5000001946a54412d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a6000001946a544ceb0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a7000001946a5458b30000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a8000001946a54647b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001a9000001946a5470450000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001aa000001946a547c0d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ab000001946a5487d50000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ac000001946a54939a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ad000001946a549f620000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ae000001946a54ab2a0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001af000001946a54b6f20000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b0000001946a54c2ba0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b1000001946a54ce830000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b2000001946a54da4b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b3000001946a54e6130000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b4000001946a54f1da0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b5000001946a54fda30000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b6000001946a55096b0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b7000001946a5515340000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b8000001946a5520fc0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001b9000001946a552cc40000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ba000001946a55388d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001bb000001946a5544550000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001bc000001946a55501c0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001bd000001946a555be40000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001be000001946a5567ad0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001bf000001946a5573750000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c0000001946a557f3d0000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));
        chunkList.add(Chunk.deserializeFrom(Numeric.hexStringToByteArray("0x2000000000000001c1000001946a558b050000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c2000001946a5596ce0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c3000001946a55a2960000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c4000001946a55ae5e0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c5000001946a55ba260000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c6000001946a55c5ef0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c7000001946a55d1b70000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c8000001946a55dd7f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001c9000001946a55e9480000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ca000001946a55f5100000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001cb000001946a5600d80000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001cc000001946a560ca00000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001cd000001946a5618690000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001ce000001946a5624310000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001cf000001946a562ffa0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d0000001946a563bb80000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d1000001946a5647770000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d2000001946a56533f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d3000001946a565f080000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d4000001946a566ad00000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d5000001946a5676960000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d6000001946a56825f0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d7000001946a568e270000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d8000001946a5699ef0000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001d9000001946a56a5b80000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001da000001946a56b1800000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001db000001946a56bd490000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001dc000001946a56c9100000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001dd000001946a56d4d80000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001de000001946a56e0980000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001df000001946a56ec600000000000000000000000000000000000000000000000000000000000000000000000003b9aca000000000000000000000001e0000001946a56f8280000000000000000000000000000000000000000000000000000000000000000000000003b9aca0000000000")));

        batchWrapper.setBatch(new Batch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_HEADER,
                RandomUtil.randomBytes(32),
                chunkList
        ));
        BatchHeader batchHeader = BatchHeader.deserializeFrom(Numeric.hexStringToByteArray("0x00000000000000000000000000000000000000000000000000bac4320768bc80b363e3d087c8decdd621f65f9c335e4603bc63525ed57aaa7c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"));
        l1Client.commitBatch(batchWrapper, batchHeader);
        System.out.println(l1Client.lastCommittedBatch());
    }

    // ==================== Negative Test Cases ====================

    /**
     * Test network timeout during lastCommittedBatch call
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testLastCommittedBatch_NetworkTimeout() {
        Request mockRequest = mock(Request.class);
        when(mockRequest.send()).thenThrow(new SocketTimeoutException("Connection timeout"));
        when(l1Web3j.ethCall(any(), any())).thenReturn(mockRequest);

        testClient.lastCommittedBatch();
    }

    /**
     * Test socket exception during lastTeeVerifiedBatch call
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testLastTeeVerifiedBatch_SocketException() {
        Request mockRequest = mock(Request.class);
        when(mockRequest.send()).thenThrow(new SocketException("Connection reset"));
        when(l1Web3j.ethCall(any(), any())).thenReturn(mockRequest);

        testClient.lastTeeVerifiedBatch();
    }

    /**
     * Test client connection exception during lastZkVerifiedBatch call
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testLastZkVerifiedBatch_ClientConnectionException() {
        Request mockRequest = mock(Request.class);
        when(mockRequest.send()).thenThrow(new ClientConnectionException("Unable to connect to Ethereum client"));
        when(l1Web3j.ethCall(any(), any())).thenReturn(mockRequest);

        testClient.lastZkVerifiedBatch();
    }

    /**
     * Test contract revert with WARNING error
     */
    @Test
    @SneakyThrows
    public void testProcessFailedEthCall_WarningRevert() {
        EthCall ethCall = new EthCall();
        ethCall.setError(new Response.Error(3, "execution reverted: WARNING: batch already committed"));

        try {
            testClient.processFailedEthCall(ethCall, rollupContractAddress, "commitBatch");
            Assert.fail("Should throw L1ContractWarnException");
        } catch (L1ContractWarnException e) {
            Assert.assertTrue(e.getMessage().contains("WARNING"));
            log.info("✓ Contract WARNING revert handled correctly");
        }
    }

    /**
     * Test contract revert with INVALID_PERMISSION error
     */
    @Test
    @SneakyThrows
    public void testProcessFailedEthCall_InvalidPermissionRevert() {
        EthCall ethCall = new EthCall();
        ethCall.setError(new Response.Error(3, "execution reverted: INVALID_PERMISSION: caller is not relayer"));

        try {
            testClient.processFailedEthCall(ethCall, rollupContractAddress, "verifyBatch");
            Assert.fail("Should throw L1ContractInvalidPermissionException");
        } catch (L1ContractInvalidPermissionException e) {
            Assert.assertTrue(e.getMessage().contains("INVALID_PERMISSION"));
            log.info("✓ Contract INVALID_PERMISSION revert handled correctly");
        }
    }

    /**
     * Test contract revert with INVALID_PARAMETER error
     */
    @Test
    @SneakyThrows
    public void testProcessFailedEthCall_InvalidParameterRevert() {
        EthCall ethCall = new EthCall();
        ethCall.setError(new Response.Error(3, "execution reverted: INVALID_PARAMETER: batch index mismatch"));

        try {
            testClient.processFailedEthCall(ethCall, rollupContractAddress, "commitBatch");
            Assert.fail("Should throw L1ContractInvalidParameterException");
        } catch (L1ContractInvalidParameterException e) {
            Assert.assertTrue(e.getMessage().contains("INVALID_PARAMETER"));
            log.info("✓ Contract INVALID_PARAMETER revert handled correctly");
        }
    }

    /**
     * Test contract revert with serious ERROR
     */
    @Test
    @SneakyThrows
    public void testProcessFailedEthCall_SeriousErrorRevert() {
        EthCall ethCall = new EthCall();
        ethCall.setError(new Response.Error(3, "execution reverted: ERROR: critical state corruption"));

        try {
            testClient.processFailedEthCall(ethCall, rollupContractAddress, "verifyBatch");
            Assert.fail("Should throw L1ContractSeriousException");
        } catch (L1ContractSeriousException e) {
            Assert.assertTrue(e.getMessage().contains("ERROR"));
            log.info("✓ Contract serious ERROR revert handled correctly");
        }
    }

    /**
     * Test generic contract revert without specific error type
     */
    @Test
    @SneakyThrows
    public void testProcessFailedEthCall_GenericRevert() {
        EthCall ethCall = new EthCall();
        ethCall.setError(new Response.Error(3, "execution reverted: unknown error"));

        try {
            testClient.processFailedEthCall(ethCall, rollupContractAddress, "commitBatch");
            Assert.fail("Should throw L2RelayerException");
        } catch (L2RelayerException e) {
            Assert.assertTrue(e.getMessage().contains("failed to local call"));
            log.info("✓ Generic contract revert handled correctly");
        }
    }

    /**
     * Test queryTxReceipt with error response
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testQueryTxReceipt_ErrorResponse() {
        Request mockRequest = mock(Request.class);
        EthGetTransactionReceipt errorResponse = new EthGetTransactionReceipt();
        errorResponse.setError(new Response.Error(-32000, "Transaction not found"));
        when(mockRequest.send()).thenReturn(errorResponse);
        when(l1Web3j.ethGetTransactionReceipt(anyString())).thenReturn(mockRequest);

        testClient.queryTxReceipt("0x1234567890abcdef");
    }

    /**
     * Test queryTx with null transaction (not found)
     */
    @Test
    @SneakyThrows
    public void testQueryTx_TransactionNotFound() {
        Request mockRequest = mock(Request.class);
        EthTransaction ethTransaction = new EthTransaction();
        ethTransaction.setResult(null);
        when(mockRequest.send()).thenReturn(ethTransaction);
        when(l1Web3j.ethGetTransactionByHash(anyString())).thenReturn(mockRequest);

        org.web3j.protocol.core.methods.response.Transaction result = testClient.queryTx("0xnonexistent");
        Assert.assertNull(result);
        log.info("✓ Transaction not found handled correctly (returns null)");
    }

    /**
     * Test queryAccountBalance with network error
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testQueryAccountBalance_NetworkError() {
        Request mockRequest = mock(Request.class);
        when(mockRequest.send()).thenThrow(new ClientConnectionException("Connection refused"));
        when(l1Web3j.ethGetBalance(anyString(), any())).thenReturn(mockRequest);

        testClient.queryAccountBalance("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4", DefaultBlockParameterName.LATEST);
    }

    /**
     * Test sendRawTx with IOException
     */
    @Test(expected = RuntimeException.class)
    @SneakyThrows
    public void testSendRawTx_IOException() {
        Request mockRequest = mock(Request.class);
        when(mockRequest.send()).thenThrow(new IOException("Network I/O error"));
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(mockRequest);

        testClient.sendRawTx(new byte[]{0x01, 0x02, 0x03});
    }
}
