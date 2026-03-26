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

import java.io.IOError;
import java.math.BigInteger;
import java.nio.charset.Charset;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.ParentChainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip4844GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.crypto.Blob;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
public class TxManagerWithoutBlobKeyTest extends TestBase {

    private static final String A_BLOB = FileUtil.readString("data/a_blob", Charset.defaultCharset());

    @TestBean(name = "l1Web3j")
    private Web3j web3j;

    private static Web3j web3j() {
        return mock(Web3j.class);
    }

    @TestBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    private static BigInteger l1ChainId() {
        return BigInteger.valueOf(123);
    }

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @Resource
    private ParentChainConfig parentChainConfig;

    @Resource
    private INonceManager l1LegacyNonceManager;

    @Resource
    private INonceResetChecker l1NonceResetChecker;

    @Resource
    private RedissonClient redisson;

    @Resource(name = "l1LegacyPoolTxTransactionManager")
    private BaseRawTransactionManager manager;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testParentChainHasNoBlobStuff() {
        Assert.assertEquals(NonceResetChecker.JOVAY, l1NonceResetChecker);
        Assert.assertNull(parentChainConfig.getL1BlobPoolTxSignService());
        Assert.assertFalse(applicationContext.containsBean("l1BlobPoolTxTransactionManager"));
        Assert.assertFalse(applicationContext.containsBean("l1BlobNonceManager"));
        Assert.assertTrue(applicationContext.containsBean("ethBlobForkConfig"));
        Assert.assertEquals("org.springframework.beans.factory.support.NullBean", applicationContext.getBean("ethBlobForkConfig").getClass().getName());
    }

    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, null,
                new RemoteNonceManager(parentChainConfig.getL1LegacyPoolTxSignService().getAddress(), web3j));
        manager.setTxHashVerifier(txHashVerifier);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        SendTxResult result = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);
        Assert.assertEquals(txhash, result.getEthSendTransaction().getTransactionHash());

        var res = manager.sendTransaction(BigInteger.ONE, BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);
        Assert.assertEquals(txhash, res.getTransactionHash());

        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(ClientConnectionException.class);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        var ethGetTxReq = mock(Request.class);
        var transaction = new EthTransaction();
        transaction.setResult(new Transaction());
        when(ethGetTxReq.send()).thenReturn(transaction);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        result = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);
        Assert.assertTrue(HexUtil.isHexNumber(result.getEthSendTransaction().getTransactionHash()));
        Assert.assertFalse(result.getEthSendTransaction().hasError());
        Assert.assertNull(result.getEthSendTransaction().getError());

        transaction.setResult(null);
        result = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);
        Assert.assertTrue(result.getEthSendTransaction().hasError());
        Assert.assertEquals(-1, result.getEthSendTransaction().getError().getCode());
    }

    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).delete();

        var ethSendRawTxReq = mock(Request.class);
        var ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        ethSendTransaction.setError(new Response.Error(1, "error"));
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction).thenThrow(ClientConnectionException.class);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        var ethGetNonceReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        var result = manager.sendTransaction(BigInteger.ONE, BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(txhash, result.getTransactionHash());

        Assert.assertEquals(1, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());

        var ethGetTxReq = mock(Request.class);
        var transaction = new EthTransaction();
        transaction.setResult(new Transaction());
        when(ethGetTxReq.send()).thenReturn(transaction);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        var txResult = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);

        Assert.assertEquals(2, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());
        Assert.assertTrue(HexUtil.isHexNumber(txResult.getEthSendTransaction().getTransactionHash()));
        Assert.assertFalse(txResult.getEthSendTransaction().hasError());
        Assert.assertNull(txResult.getEthSendTransaction().getError());

        transaction.setResult(null);

        txResult = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);

        Assert.assertEquals(2, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());
        Assert.assertTrue(txResult.getEthSendTransaction().hasError());
        Assert.assertEquals(-1, txResult.getEthSendTransaction().getError().getCode());

        ethSendTransaction.setError(new Response.Error(112, ""));
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        txResult = manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false);

        // nonce has been reset
        Assert.assertFalse(redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).isExists());

        // assume that network is broken.
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(ClientConnectionException.class);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        ethGetTxReq = mock(Request.class);
        when(ethGetTxReq.send()).thenThrow(ClientConnectionException.class);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        Assert.assertThrows(
                RuntimeException.class,
                () -> manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false)
        );
        Assert.assertEquals(1, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());

        // something wrong with Error
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(IOError.class);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        ethGetTxReq = mock(Request.class);
        when(ethGetTxReq.send()).thenThrow(IOError.class);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        Assert.assertThrows(
                RuntimeException.class,
                () -> manager.sendTx(new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", "", BigInteger.ZERO, false)
        );
        Assert.assertEquals(1, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());

        Assert.assertThrows(NullPointerException.class, () -> manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, ""));
    }

    /**
     * Test network failure scenarios for AcbRawTransactionManager
     * Verifies behavior when network connection is lost during transaction sending
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_NetworkFailure() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, null,
                new RemoteNonceManager(parentChainConfig.getL1LegacyPoolTxSignService().getAddress(), web3j));
        manager.setTxHashVerifier(txHashVerifier);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate network timeout during transaction sending
        Request ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(new ClientConnectionException("Connection timeout"));
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        // Simulate network failure when querying transaction status
        Request ethGetTxReq = mock(Request.class);
        when(ethGetTxReq.send()).thenThrow(new ClientConnectionException("Connection timeout"));
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        // Should throw RuntimeException after retry attempts exhausted
        Assert.assertThrows(
                RuntimeException.class,
                () -> manager.sendTx(
                        new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                        BigInteger.ONE,
                        "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                        "",
                        BigInteger.ZERO,
                        false
                )
        );
    }

    /**
     * Test invalid input parameters for AcbRawTransactionManager
     * Verifies proper handling of null and invalid parameters
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_InvalidInputs() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, null,
                new RemoteNonceManager(parentChainConfig.getL1LegacyPoolTxSignService().getAddress(), web3j));
        manager.setTxHashVerifier(txHashVerifier);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Test with invalid address format - should throw exception before RPC call
        Assert.assertThrows(
                NumberFormatException.class,
                () -> manager.sendTx(
                        new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                        BigInteger.ONE,
                        "invalid_address",
                        "",
                        BigInteger.ZERO,
                        false
                )
        );

        // Test with gas limit error from RPC
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(-32000, "gas limit too low"));
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        SendTxResult result = manager.sendTx(
                new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                BigInteger.ONE,
                "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                "",
                BigInteger.ZERO,
                false
        );

        Assert.assertTrue(result.getEthSendTransaction().hasError());
        Assert.assertEquals(-32000, result.getEthSendTransaction().getError().getCode());
    }

    /**
     * Test transaction hash mismatch scenario
     * Verifies proper handling when local and remote transaction hashes don't match
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_TxHashMismatch() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        // Simulate hash verification failure
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(false);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, parentChainConfig.getL1LegacyPoolTxSignService(), 123, redisson, null,
                new RemoteNonceManager(parentChainConfig.getL1LegacyPoolTxSignService().getAddress(), web3j));
        manager.setTxHashVerifier(txHashVerifier);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        // Should throw TxHashMismatchException
        Assert.assertThrows(
                org.web3j.tx.exceptions.TxHashMismatchException.class,
                () -> manager.sendTx(
                        new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                        BigInteger.ONE,
                        "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                        "",
                        BigInteger.ZERO,
                        false
                )
        );
    }

    /**
     * Test nonce management error recovery for AcbFastRawTransactionManager
     * Verifies proper nonce handling when transactions fail
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_NonceRecovery() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x04");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // First transaction fails with insufficient funds
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(-32000, "insufficient funds for gas * price + value"));
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        SendTxResult result = manager.sendTx(
                new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                BigInteger.ONE,
                "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                "",
                BigInteger.ZERO,
                false
        );

        Assert.assertTrue(result.getEthSendTransaction().hasError());
        // Verify nonce was returned to pool (should be 4 after first call gets nonce 4 and returns it)
        Assert.assertEquals(4, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());

        // Second transaction succeeds
        ethSendTransaction = new EthSendTransaction();
        String txhash = Numeric.toHexString(RandomUtil.randomBytes(32));
        ethSendTransaction.setResult(txhash);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);

        result = manager.sendTx(
                new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                BigInteger.ONE,
                "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                "",
                BigInteger.ZERO,
                false
        );

        Assert.assertFalse(result.getEthSendTransaction().hasError());
        // After successful transaction, nonce should be 5
        Assert.assertEquals(5, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());
    }

    /**
     * Test concurrent nonce management issues
     * Verifies proper handling of nonce conflicts in concurrent scenarios
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_NonceConflict() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x09");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate nonce too high error (gap in nonce sequence)
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(112, "nonce too high: tx nonce 15, gapped nonce 10"));
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        SendTxResult result = manager.sendTx(
                new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                BigInteger.ONE,
                "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                "",
                BigInteger.ZERO,
                false
        );

        Assert.assertTrue(result.getEthSendTransaction().hasError());
        Assert.assertFalse(redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).isExists());
    }

    /**
     * Test RPC error response handling
     * Verifies proper handling of various RPC error codes
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_RpcErrors() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        manager.setTxHashVerifier(txHashVerifier);

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x01");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Test various RPC error codes
        int[] errorCodes = {-32700, -32600, -32601, -32602, -32603};
        String[] errorMessages = {
                "Parse error",
                "Invalid Request",
                "Method not found",
                "Invalid params",
                "Internal error"
        };

        for (int i = 0; i < errorCodes.length; i++) {
            Request ethSendRawTxReq = mock(Request.class);
            EthSendTransaction ethSendTransaction = new EthSendTransaction();
            ethSendTransaction.setError(new Response.Error(errorCodes[i], errorMessages[i]));
            when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
            when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

            SendTxResult result = manager.sendTx(
                    new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                    BigInteger.ONE,
                    "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                    "",
                    BigInteger.ZERO,
                    false
            );

            Assert.assertTrue(result.getEthSendTransaction().hasError());
            Assert.assertEquals(errorCodes[i], result.getEthSendTransaction().getError().getCode());
        }
    }

    /**
     * Test transaction replacement scenarios
     * Verifies proper handling when trying to replace pending transactions
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_TransactionReplacement() {
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x02");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate replacement transaction underpriced error
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(113, "TX_REPLAY_ATTACK"));
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        SendTxResult result = manager.sendTx(
                new Eip1559GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                BigInteger.ONE,
                "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                "",
                BigInteger.ZERO,
                false
        );

        Assert.assertTrue(result.getEthSendTransaction().hasError());
        Assert.assertTrue(result.getEthSendTransaction().getError().getMessage().contains("TX_REPLAY_ATTACK"));
        // Verify nonce was returned (first call gets nonce 2, increments to 3, then returns it back to 2)
        Assert.assertEquals(2, redisson.getAtomicLong(manager.getNonceManager().getEthNonceValKey(123, parentChainConfig.getL1LegacyPoolTxSignService().getAddress())).get());
    }
}
