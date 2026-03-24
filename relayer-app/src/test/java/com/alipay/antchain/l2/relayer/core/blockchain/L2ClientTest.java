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
import java.math.BigInteger;

import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.TxNotFoundButRetryException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.config.BlockchainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbFastRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.IGasPriceProvider;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasLimitPolicyEnum;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.service.IOracleService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class L2ClientTest extends TestBase {

    private static final String mockGasOracleContract = "0x8100000000000000000000000000000000000000";
    private static final String mockCoinbaseContract = "0x7100000000000000000000000000000000000000";
    private static final String mockSender = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
    @MockitoBean
    private L1Client l1Client;
    @MockitoBean(name = "l2Web3j")
    private Web3j l2Web3j;
    @MockitoBean(name = "l2ChainId")
    private BigInteger l2ChainId;
    @MockitoBean
    private RollupConfig rollupConfig;
    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;
    @MockitoBean
    private L2Client l2Client;
    @Resource
    private BlockchainConfig blockchainConfig;
    @MockitoBean(name = "l2TransactionManager")
    private BaseRawTransactionManager l2TransactionManager;
    @Resource(name = "l2GasPriceProvider")
    private IGasPriceProvider l2GasPriceProvider;
    @Value("${l2-relayer.l2-client.gas-limit-policy:ESTIMATE}")
    private GasLimitPolicyEnum gasLimitPolicy;
    @Value("${l2-relayer.l2-client.extra-gas:0}")
    private BigInteger extraGas;
    @Value("${l2-relayer.l1-client.static-gas-limit:9000000}")
    private BigInteger staticGasLimit;
    @Value("${l2-relayer.l2-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l2NoncePolicy;
    @Resource
    private RedissonClient redisson;
    @MockitoBean
    private IOracleService oracleService;
    private L2Client testClient;

    @Before
    @SneakyThrows
    public void init() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);

        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        Request ethGetCodeReq = mock(Request.class);
        EthGetCode mockGetCode = new EthGetCode();
        mockGetCode.setResult("0x1234");
        when(ethGetCodeReq.send()).thenReturn(mockGetCode);
        when(l2Web3j.ethGetCode(notNull(), any())).thenReturn(ethGetCodeReq);

        var l2TxManager = l2NoncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(l2Web3j, blockchainConfig.getL2TxSignService(), 123, redisson, null, ChainTypeEnum.LAYER_ONE, null)
                : new AcbRawTransactionManager(l2Web3j, blockchainConfig.getL2TxSignService(), 123, redisson, null);
        testClient = new L2Client(
                l2Web3j, l2TxManager,
                l2GasPriceProvider, mockGasOracleContract, mockCoinbaseContract, gasLimitPolicy, extraGas, staticGasLimit
        );
        testClient.getLegacyPoolTxManager().setTxHashVerifier(txHashVerifier);
    }

    @Test
    @SneakyThrows
    public void testSendL1Msg() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);

        Request ethGetTransactionCountReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("10");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);

        when(l2Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        when(l2Web3j.ethGetTransactionCount(anyString(), any(DefaultBlockParameterName.class))).thenReturn(ethGetTransactionCountReq);
        TransactionInfo result = testClient.sendL1MsgTx(tx);

        Assert.assertEquals(txhash, result.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testResendL1Msg() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);

        Request ethGetTransactionCountReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("10");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);

        when(l2Web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        when(l2Web3j.ethGetTransactionCount(anyString(), any(DefaultBlockParameterName.class))).thenReturn(ethGetTransactionCountReq);
        TransactionInfo result = testClient.resendL1MsgTx(tx);

        Assert.assertEquals(txhash, result.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testQueryL2GasOracleLastBatchDaFee() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l2Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger lastBatchDaFee = testClient.queryL2GasOracleLastBatchDaFee();
        Assert.assertEquals(BigInteger.valueOf(1), lastBatchDaFee);
    }

    @Test
    @SneakyThrows
    public void testQueryL2GasOracleLastBatchExecFee() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l2Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger lastBatchExecFee = testClient.queryL2GasOracleLastBatchExecFee();
        Assert.assertEquals(BigInteger.valueOf(1), lastBatchExecFee);
    }

    @Test
    @SneakyThrows
    public void testQueryL2GasOracleLastBatchByteLength() {
        Request ethCallReq = mock(Request.class);
        EthCall result = new EthCall();
        result.setResult("0000000000000000000000000000000000000000000000000000000000000001");
        when(ethCallReq.send()).thenReturn(result);
        when(l2Web3j.ethCall(notNull(), notNull())).thenReturn(ethCallReq);

        BigInteger lastBatchByteLength = testClient.queryL2GasOracleLastBatchByteLength();
        Assert.assertEquals(BigInteger.valueOf(1), lastBatchByteLength);
    }

    @Test
    @SneakyThrows
    public void testUpdateBatchRollupFee() {
        BigInteger lastBatchDaFee = BigInteger.valueOf(RandomUtil.randomInt()).abs();
        BigInteger lastBatchExecFee = BigInteger.valueOf(RandomUtil.randomInt()).abs();
        BigInteger lastBatchByteLength = BigInteger.valueOf(RandomUtil.randomInt()).abs();
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");

        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));

        mockSendTx(txHash);

        TransactionInfo result = testClient.updateBatchRollupFee(lastBatchDaFee, lastBatchExecFee, lastBatchByteLength);

        Assert.assertEquals(txHash, result.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testUpdateBaseFeeScala() {
        BigInteger baseFeeScala = BigInteger.valueOf(RandomUtil.randomInt()).abs();
        BigInteger blobBaseFeeScala = BigInteger.valueOf(RandomUtil.randomInt()).abs();

        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        mockSendTx(txHash);

        TransactionInfo txInfo = testClient.updateBaseFeeScala(baseFeeScala, blobBaseFeeScala);
        Assert.assertEquals(txHash, txInfo.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testUpdateFixedProfit() {
        BigInteger fixedProfit = BigInteger.valueOf(RandomUtil.randomInt()).abs();

        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        mockSendTx(txHash);

        TransactionInfo txInfo = testClient.updateFixedProfit(fixedProfit);
        Assert.assertEquals(txHash, txInfo.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testUpdateTotalScala() {
        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        mockSendTx(txHash);

        TransactionInfo txInfo = testClient.updateTotalScala(BigInteger.valueOf(RandomUtil.randomInt()).abs());
        Assert.assertEquals(txHash, txInfo.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testWithdrawVault() {
        String to = mockSender;
        BigInteger account = BigInteger.valueOf(RandomUtil.randomLong()).abs();

        String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        mockSendTx(txHash);

        TransactionInfo txInfo = testClient.withdrawVault(to, account);
        Assert.assertEquals(txHash, txInfo.getTxHash());
    }

    @Test
    @SneakyThrows
    public void testQueryTxWithRetry() {
        Request ethGetTransactionByHashReq = mock(Request.class);
        var ethTransaction = new EthTransaction();
        ethTransaction.setResult(new Transaction());
        var ethTransactionNull = new EthTransaction();
        ethTransactionNull.setResult(null);
        when(ethGetTransactionByHashReq.send()).thenReturn(
                ethTransactionNull,
                ethTransactionNull,
                ethTransaction
        );
        when(l2Web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTransactionByHashReq);

        Request ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x1");
        var ethGetTransactionCount2 = new EthGetTransactionCount();
        ethGetTransactionCount2.setResult("0x2");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount, ethGetTransactionCount2, ethGetTransactionCount);
        when(l2Web3j.ethGetTransactionCount(anyString(), notNull())).thenReturn(ethGetTransactionCountReq);

        Assert.assertThrows(TxNotFoundButRetryException.class, () -> testClient.queryTxWithRetry("test", "test", BigInteger.ONE));
        var res = testClient.queryTxWithRetry("test", "test", BigInteger.ONE);
        Assert.assertNull(res);

        res = testClient.queryTxWithRetry("test", "test", BigInteger.ONE);
        Assert.assertNotNull(res);
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
        // String txHash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txHash);
        when(ethSendRawReq.send()).thenReturn(ethSendTransaction);
        when(l2Web3j.ethSendRawTransaction(any())).thenReturn(ethSendRawReq);
    }
}
