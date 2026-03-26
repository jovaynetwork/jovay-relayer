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
import java.util.concurrent.ExecutorService;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.DaProof;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.config.ContractConfig;
import com.alipay.antchain.l2.relayer.config.ParentChainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBpoBlobConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbFastRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.INonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json",
                "l2-relayer.rollup.config.parent-chain-type=JOVAY",
                "l2-relayer.rollup.da-type=DAS",
                "l2-relayer.l1-client.gas-price-provider-conf.minimum-eip1559-priority-price=1000000000"
        }
)
@Slf4j
public class JovayL1ClientTest extends TestBase {

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

    private static RollupConfig rollupConfig() {
        var mockRollupConfig = mock(RollupConfig.class);
        when(mockRollupConfig.getParentChainType()).thenReturn(ParentChainType.JOVAY);
        when(mockRollupConfig.getDaType()).thenReturn(DaType.DAS);
        return mockRollupConfig;
    }

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private L2Client l2Client;

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
    private ParentChainConfig parentChainConfig;

    @Resource
    private INonceManager l1LegacyNonceManager;

    @Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Resource
    private RollupEconomicStrategy rollupEconomicStrategy;

    @Resource
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @Resource
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    private L1Client testClient;

    private BatchWrapper batchWrapper1;

    @Before
    @SneakyThrows
    public void init() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);

        Request getCodeRequest = mock(Request.class);
        EthGetCode ethGetCode = new EthGetCode();
        ethGetCode.setResult("0x1234");
        doReturn(ethGetCode).when(getCodeRequest).send();
        when(l1Web3j.ethGetCode(notNull(), notNull())).thenReturn(getCodeRequest);

        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        batchWrapper1 = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_WRAPPER,
                BASIC_BLOCK_TRACE2.getHeader().getStateRoot().toByteArray(),
                BASIC_BLOCK_TRACE2.getL1MsgRollingHash().getValue().toByteArray(),
                Bytes32.DEFAULT.getValue(),
                0,
                ListUtil.toList(new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(1), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)))
        );
        batchWrapper1.setGmtCreate(System.currentTimeMillis());

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var l1LegacyPoolTxManager = l1NoncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(l1Web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, ethForkBlobConfig, l1LegacyNonceManager)
                : new AcbRawTransactionManager(l1Web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, ethForkBlobConfig, l1LegacyNonceManager);

        testClient = new L1Client(
                l1Web3j, null, l1LegacyPoolTxManager,
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
        field = ReflectUtil.getField(L1Client.class, "daType");
        field.setAccessible(true);
        field.set(testClient, DaType.BLOBS);
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
    public void testCommitBatchWithDaProof() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
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
        TransactionInfo result = testClient.commitBatch(batchWrapper1, new DaProof(new byte[0]));
        Assert.assertEquals(txhash, result.getTxHash());

        // commit batch with 4844 is not allowed
        Assert.assertThrows(RuntimeException.class, () -> testClient.commitBatch(batchWrapper1));

        var prevConfigVal = rollupEconomicStrategyConfig.getMidEip1559PriceLimit();
        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(Convert.toWei("1", Convert.Unit.GWEI).toBigInteger());
        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class, () -> testClient.commitBatch(batchWrapper1, new DaProof(new byte[0])));
        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(prevConfigVal);
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCall() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
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

        TransactionInfo result = testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0]));

        Assert.assertNotNull(result);
        Assert.assertEquals(txhash, result.getTxHash());
        Assert.assertNotNull(result.getRawTx());
        Assert.assertNotNull(result.getNonce());

        // Verify eth call was made before sending transaction
        verify(l1Web3j, times(1)).ethCall(argThat(argument ->
                Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull());
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCallRevertedWithWarning() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "WARNING"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
                .thenReturn(ethCallReq);

        Assert.assertThrows(L1ContractWarnException.class, () -> testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0])));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCallRevertedWithInvalidPermission() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "INVALID_PERMISSION"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
                .thenReturn(ethCallReq);

        Assert.assertThrows(L1ContractInvalidPermissionException.class, () -> testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0])));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCallRevertedWithInvalidParameter() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "INVALID_PARAMETER"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
                .thenReturn(ethCallReq);

        Assert.assertThrows(L1ContractInvalidParameterException.class, () -> testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0])));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCallRevertedWithError() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "ERROR"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
                .thenReturn(ethCallReq);

        Assert.assertThrows(L1ContractSeriousException.class, () -> testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0])));
    }

    @Test
    @SneakyThrows
    public void testCommitBatchWithEthCallRevertedWithGenericError() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "execution reverted"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("68b13362")), notNull()))
                .thenReturn(ethCallReq);

        Assert.assertThrows(L2RelayerException.class, () -> testClient.commitBatchWithEthCall(batchWrapper1, new DaProof(new byte[0])));
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
    public void testVerifyBatchWithEthCall() {
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

        TransactionInfo result = testClient.verifyBatchWithEthCall(batchWrapper1, req);
        Assert.assertEquals(txhash, result.getTxHash());
        Assert.assertNotNull(result.getRawTx());
        Assert.assertNotNull(result.getNonce());

        // Verify eth call was made before sending transaction
        verify(l1Web3j, times(1)).ethCall(argThat(argument ->
                Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull());
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallZkProof() {
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
        req.setProof(RandomUtil.randomBytes(64));
        req.setProveType(ProveTypeEnum.ZK_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        when(rollupRepository.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(0);

        TransactionInfo result = testClient.verifyBatchWithEthCall(batchWrapper1, req);
        Assert.assertEquals(txhash, result.getTxHash());
        Assert.assertNotNull(result.getRawTx());
        Assert.assertNotNull(result.getNonce());
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallRevertedWithWarning() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "WARNING"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        Assert.assertThrows(L1ContractWarnException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallRevertedWithInvalidPermission() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "INVALID_PERMISSION"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        Assert.assertThrows(L1ContractInvalidPermissionException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallRevertedWithInvalidParameter() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "INVALID_PARAMETER"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        Assert.assertThrows(L1ContractInvalidParameterException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallRevertedWithError() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "ERROR"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        Assert.assertThrows(L1ContractSeriousException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallRevertedWithGenericError() {
        Request ethCallReq = mock(Request.class);
        EthCall revertCall = new EthCall();
        revertCall.setError(new Response.Error(1, "execution reverted"));
        when(ethCallReq.send()).thenReturn(revertCall);
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        Assert.assertThrows(L2RelayerException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallSendRawTxError() {
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
        when(ethSendRawTxReq.send()).thenThrow(new IOException("Send tx error"));
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);

        when(rollupRepository.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(0);

        Assert.assertThrows(RuntimeException.class, () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallEconomicStrategyCheck() {
        Request ethCallReq = mock(Request.class);
        when(ethCallReq.send()).thenReturn(new EthCall());
        when(l1Web3j.ethCall(argThat(argument -> Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull()))
                .thenReturn(ethCallReq);

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

        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x1234");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Set economic strategy to a very restrictive limit
        var prevConfigVal = rollupEconomicStrategyConfig.getMidEip1559PriceLimit();
        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(Convert.toWei("1", Convert.Unit.WEI).toBigInteger());

        var req = new BatchProveRequestDO();
        req.setProof("123".getBytes());
        req.setProveType(ProveTypeEnum.TEE_PROOF);
        req.setBatchIndex(BigInteger.ONE);
        req.setGmtModified(new Date());

        Assert.assertThrows(RollupEconomicStrategyNotAllowedException.class,
                () -> testClient.verifyBatchWithEthCall(batchWrapper1, req));

        rollupEconomicStrategyConfig.setMidEip1559PriceLimit(prevConfigVal);
    }

    @Test
    @SneakyThrows
    public void testVerifyBatchWithEthCallMultipleBatches() {
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

        when(rollupRepository.calcWaitingProofCountBeyondIndex(notNull(), notNull())).thenReturn(0);

        // Test TEE proof verification
        var teeReq = new BatchProveRequestDO();
        teeReq.setProof(RandomUtil.randomBytes(32));
        teeReq.setProveType(ProveTypeEnum.TEE_PROOF);
        teeReq.setBatchIndex(BigInteger.ONE);

        TransactionInfo result1 = testClient.verifyBatchWithEthCall(batchWrapper1, teeReq);
        Assert.assertNotNull(result1);
        Assert.assertEquals(txhash, result1.getTxHash());

        // Test ZK proof verification
        var zkReq = new BatchProveRequestDO();
        zkReq.setProof(RandomUtil.randomBytes(64));
        zkReq.setProveType(ProveTypeEnum.ZK_PROOF);
        zkReq.setBatchIndex(BigInteger.valueOf(2));

        TransactionInfo result2 = testClient.verifyBatchWithEthCall(batchWrapper1, zkReq);
        Assert.assertNotNull(result2);
        Assert.assertEquals(txhash, result2.getTxHash());

        // Verify eth call was invoked for both batches
        verify(l1Web3j, atLeast(2)).ethCall(argThat(argument ->
                Numeric.cleanHexPrefix(argument.getData()).startsWith("7256b753")), notNull());
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
        var prevVal = l1GasPriceProviderConfig.getMinimumEip1559PriorityPrice();
        l1GasPriceProviderConfig.setMinimumEip1559PriorityPrice(BigInteger.ZERO);

        try {
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
            Assert.assertThrows(RuntimeException.class, () -> testClient.speedUpRollupTx(tx));

            when(rollupRepository.getReliableTransaction(notNull(), notNull(), notNull())).thenReturn(
                    ReliableTransactionDO.builder()
                            .state(ReliableTransactionStateEnum.TX_PACKAGED)
                            .gmtModified(new Date(System.currentTimeMillis() - 3600_000))
                            .build()
            );
            Assert.assertThrows(RuntimeException.class, () -> testClient.speedUpRollupTx(tx));
        } finally {
            l1GasPriceProviderConfig.setMinimumEip1559PriorityPrice(prevVal);
        }
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

    @Test
    @SneakyThrows
    public void testSendTransferValueTxFromBlobPoolManager() {
        String fromAddress = testClient.getLegacyPoolTxManager().getAddress();
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.valueOf(10);
        BigInteger value = Convert.toWei("1", Convert.Unit.ETHER).toBigInteger();

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        // Mock estimate gas
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x5208"); // 21000 gas for simple transfer
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        // Mock send raw tx
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txHash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        EthSendTransaction result = testClient.sendTransferValueTx(fromAddress, toAddress, nonce, value);

        Assert.assertNotNull(result);
        Assert.assertEquals(txHash, result.getTransactionHash());
        Assert.assertFalse(result.hasError());
    }

    @Test
    @SneakyThrows
    public void testSendTransferValueTxFromLegacyPoolManager() {
        String fromAddress = testClient.getLegacyPoolTxManager().getAddress();
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.valueOf(5);
        BigInteger value = Convert.toWei("0.5", Convert.Unit.ETHER).toBigInteger();

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        // Mock estimate gas
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x5208");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        // Mock send raw tx
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txHash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        EthSendTransaction result = testClient.sendTransferValueTx(fromAddress, toAddress, nonce, value);

        Assert.assertNotNull(result);
        Assert.assertEquals(txHash, result.getTransactionHash());
        verify(l1Web3j, times(1)).ethSendRawTransaction(anyString());
    }

    @Test
    @SneakyThrows
    public void testSendTransferValueTxWithZeroValue() {
        String fromAddress = testClient.getLegacyPoolTxManager().getAddress();
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.ZERO;
        BigInteger value = BigInteger.ZERO;

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        // Mock estimate gas
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x5208");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        // Mock send raw tx
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txHash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        EthSendTransaction result = testClient.sendTransferValueTx(fromAddress, toAddress, nonce, value);

        Assert.assertNotNull(result);
        Assert.assertEquals(txHash, result.getTransactionHash());
    }

    @Test
    @SneakyThrows
    public void testSendTransferValueTxWithInvalidFromAddress() {
        String invalidFromAddress = "0x0000000000000000000000000000000000000000";
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.valueOf(1);
        BigInteger value = Convert.toWei("1", Convert.Unit.ETHER).toBigInteger();

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        Assert.assertThrows(RuntimeException.class,
                () -> testClient.sendTransferValueTx(invalidFromAddress, toAddress, nonce, value));
    }

    @Test
    @SneakyThrows
    public void testSendTransferValueTxEstimateGasError() {
        String fromAddress = testClient.getLegacyPoolTxManager().getAddress();
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.valueOf(1);
        BigInteger value = Convert.toWei("1", Convert.Unit.ETHER).toBigInteger();

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        // Mock estimate gas with error
        Request ethEstimateGasReq = mock(Request.class);
        when(ethEstimateGasReq.send()).thenThrow(new IOException("Estimate gas error"));
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        Assert.assertThrows(IOException.class,
                () -> testClient.sendTransferValueTx(fromAddress, toAddress, nonce, value));
    }

    @Test
    @SneakyThrows
    public void testSendTransferValueTxSendRawTxError() {
        String fromAddress = testClient.getLegacyPoolTxManager().getAddress();
        String toAddress = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        BigInteger nonce = BigInteger.valueOf(1);
        BigInteger value = Convert.toWei("1", Convert.Unit.ETHER).toBigInteger();

        var ethGasMaxProrityPriceReq = mock(Request.class);
        var ethGasMaxProrityPrice = new EthMaxPriorityFeePerGas();
        ethGasMaxProrityPrice.setResult("0x0234");
        when(ethGasMaxProrityPriceReq.send()).thenReturn(ethGasMaxProrityPrice);
        when(l1Web3j.ethMaxPriorityFeePerGas()).thenReturn(ethGasMaxProrityPriceReq);

        var ethGetBlkHdrReq = mock(Request.class);
        var ethBlock = new EthBlock();
        var blk = new EthBlock.Block();
        blk.setBaseFeePerGas("0x1234");
        ethBlock.setResult(blk);
        when(ethGetBlkHdrReq.send()).thenReturn(ethBlock);
        when(l1Web3j.ethGetBlockByNumber(notNull(), eq(false))).thenReturn(ethGetBlkHdrReq);

        // Mock estimate gas
        Request ethEstimateGasReq = mock(Request.class);
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        ethEstimateGas.setResult("0x5208");
        when(ethEstimateGasReq.send()).thenReturn(ethEstimateGas);
        when(l1Web3j.ethEstimateGas(notNull())).thenReturn(ethEstimateGasReq);

        // Mock send raw tx with error
        Request ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(new IOException("Send tx error"));
        when(l1Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        Assert.assertThrows(RuntimeException.class,
                () -> testClient.sendTransferValueTx(fromAddress, toAddress, nonce, value));
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
