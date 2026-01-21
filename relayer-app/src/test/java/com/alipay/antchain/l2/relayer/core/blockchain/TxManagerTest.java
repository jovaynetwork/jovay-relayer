package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;
import java.nio.charset.Charset;

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBpoBlobConfig;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.BlockchainConfig;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbFastRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.AcbRawTransactionManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip4844GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
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
import static org.mockito.Mockito.*;

public class TxManagerTest extends TestBase {

    private static final String A_BLOB = FileUtil.readString("data/a_blob", Charset.defaultCharset());

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @Resource
    private BlockchainConfig blockchainConfig;

    @Resource
    private RedissonClient redisson;

    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
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
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

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

        Assert.assertEquals(1, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());

        var ethGetTxReq = mock(Request.class);
        var transaction = new EthTransaction();
        transaction.setResult(new Transaction());
        when(ethGetTxReq.send()).thenReturn(transaction);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        var resultBlob = manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, "");

        Assert.assertEquals(2, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
        Assert.assertTrue(HexUtil.isHexNumber(resultBlob.getEthSendTransaction().getTransactionHash()));
        Assert.assertFalse(resultBlob.getEthSendTransaction().hasError());
        Assert.assertNull(resultBlob.getEthSendTransaction().getError());

        transaction.setResult(null);

        resultBlob = manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, "");

        Assert.assertEquals(2, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
        Assert.assertTrue(resultBlob.getEthSendTransaction().hasError());
        Assert.assertEquals(-1, resultBlob.getEthSendTransaction().getError().getCode());

        ethSendTransaction.setError(new Response.Error(-32000, "nonce too high: tx nonce 2360, gapped nonce 2359"));
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        resultBlob = manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, "");

        Assert.assertTrue(redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).isExists());
        Assert.assertEquals(2, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());

        ethSendTransaction.setError(new Response.Error(-32000, "nonce too low"));
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenReturn(ethSendTransaction);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        resultBlob = manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, "");

        // nonce has been reset
        Assert.assertFalse(redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).isExists());

        // assume that network is broken.
        ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(ClientConnectionException.class);
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);
        ethGetTxReq = mock(Request.class);
        when(ethGetTxReq.send()).thenThrow(ClientConnectionException.class);
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        Assert.assertThrows(
                RuntimeException.class,
                () ->
                    manager.sendTx(ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                            new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE), BigInteger.ONE, "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4", BigInteger.ZERO, "")
        );
        Assert.assertEquals(1, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }

    /**
     * Test network failure scenarios for AcbRawTransactionManager
     * Verifies behavior when network connection is lost during transaction sending
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_NetworkFailure() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
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

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
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

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
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
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

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
        Assert.assertEquals(4, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());

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
        Assert.assertEquals(5, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }

    /**
     * Test concurrent nonce management issues
     * Verifies proper handling of nonce conflicts in concurrent scenarios
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_NonceConflict() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x09");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate nonce too high error (gap in nonce sequence)
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(-32000, "nonce too high: tx nonce 15, gapped nonce 10"));
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
        // Verify nonce was returned (first call gets nonce 9, increments to 10, then returns it back to 9)
        Assert.assertEquals(9, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }

    /**
     * Test blob transaction failures with network issues
     * Verifies proper error handling for EIP-4844 blob transactions
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_BlobTxNetworkFailure() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x00");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate network failure during blob transaction
        Request ethSendRawTxReq = mock(Request.class);
        when(ethSendRawTxReq.send()).thenThrow(new ClientConnectionException("Network unreachable"));
        when(web3j.ethSendRawTransaction(anyString())).thenReturn(ethSendRawTxReq);

        Request ethGetTxReq = mock(Request.class);
        when(ethGetTxReq.send()).thenThrow(new ClientConnectionException("Network unreachable"));
        when(web3j.ethGetTransactionByHash(anyString())).thenReturn(ethGetTxReq);

        // Should throw RuntimeException and nonce should be returned
        Assert.assertThrows(
                RuntimeException.class,
                () -> manager.sendTx(
                        ListUtil.toList(new Blob(Numeric.hexStringToByteArray(A_BLOB))),
                        new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                        BigInteger.ONE,
                        "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                        BigInteger.ZERO,
                        ""
                )
        );

        // Verify nonce was returned to pool (first call gets nonce 0, increments to 1, then returns it back to 0)
        Assert.assertEquals(0, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }

    /**
     * Test invalid blob data handling
     * Verifies proper error handling when blob data is malformed
     */
    @Test
    @SneakyThrows
    public void testAcbFastRawTransactionManager_InvalidBlobData() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x00");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Invalid blob data will throw exception during transaction creation
        Assert.assertThrows(
                ethereum.ckzg4844.CKZGException.class,
                () -> manager.sendTx(
                        ListUtil.toList(new Blob(Numeric.hexStringToByteArray("0x1234"))), // Invalid blob data
                        new Eip4844GasPrice(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE),
                        BigInteger.ONE,
                        "0x26E4259c4f391D9a7EFDe9b0A871b0509cD143D4",
                        BigInteger.ZERO,
                        ""
                )
        );

        // Verify nonce was returned after exception
        Assert.assertEquals(0, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }

    /**
     * Test RPC error response handling
     * Verifies proper handling of various RPC error codes
     */
    @Test
    @SneakyThrows
    public void testAcbRawTransactionManager_RpcErrors() {
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        AcbRawTransactionManager manager = new AcbRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
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
        Web3j web3j = mock(Web3j.class);
        TxHashVerifier txHashVerifier = mock(TxHashVerifier.class);
        when(txHashVerifier.verify(anyString(), anyString())).thenReturn(true);

        var bpoConfig = mock(EthBpoBlobConfig.class);
        when(bpoConfig.getName()).thenReturn("test");
        when(bpoConfig.getBlobSidecarVersion()).thenReturn(1);
        when(bpoConfig.getUpdateFraction()).thenReturn(BigInteger.valueOf(5007716));
        when(bpoConfig.fakeExponential(notNull())).thenReturn(BigInteger.ONE);
        var ethForkBlobConfig = mock(EthBlobForkConfig.class);
        when(ethForkBlobConfig.getCurrConfig()).thenReturn(bpoConfig);

        var manager = new AcbFastRawTransactionManager(web3j, blockchainConfig.getL1BlobPoolTxSignService(), 123, redisson, ethForkBlobConfig);
        manager.setTxHashVerifier(txHashVerifier);

        // Clear any existing nonce
        redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).delete();

        Request ethGetNonceReq = mock(Request.class);
        EthGetTransactionCount ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("0x02");
        when(ethGetNonceReq.send()).thenReturn(ethGetTransactionCount);
        when(web3j.ethGetTransactionCount(notNull(), notNull())).thenReturn(ethGetNonceReq);

        // Simulate replacement transaction underpriced error
        Request ethSendRawTxReq = mock(Request.class);
        EthSendTransaction ethSendTransaction = new EthSendTransaction();
        ethSendTransaction.setError(new Response.Error(-32000, "replacement transaction underpriced"));
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
        Assert.assertTrue(result.getEthSendTransaction().getError().getMessage().contains("replacement transaction underpriced"));
        // Verify nonce was returned (first call gets nonce 2, increments to 3, then returns it back to 2)
        Assert.assertEquals(2, redisson.getAtomicLong(manager.getEthNonceValKey(123, blockchainConfig.getL1BlobPoolTxSignService().getAddress())).get());
    }
}
