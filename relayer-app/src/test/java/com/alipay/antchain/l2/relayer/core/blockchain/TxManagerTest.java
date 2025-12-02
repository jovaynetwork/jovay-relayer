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
}
