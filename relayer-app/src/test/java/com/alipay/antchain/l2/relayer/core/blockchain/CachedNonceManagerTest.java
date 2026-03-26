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

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.CachedNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.INonceResetChecker;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class CachedNonceManagerTest extends TestBase {

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

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @Resource
    private RedissonClient redisson;

    @Resource
    private INonceResetChecker l1NonceResetChecker;

    @Test
    @SneakyThrows
    public void testGetNextNonce() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x1234", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        var nonce = cachedNonceManager.getNextNonce();

        assertEquals(1L, nonce.longValue());
    }

    @Test
    @SneakyThrows
    public void testGetNextNonceMultipleTimes() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("10");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x1234567890abcdef", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // First call should fetch from chain and return 10
        var nonce1 = cachedNonceManager.getNextNonce();
        assertEquals(10L, nonce1.longValue());

        // Second call should return 11 (from cache, incremented)
        cachedNonceManager.incrementNonce();
        var nonce2 = cachedNonceManager.getNextNonce();
        assertEquals(11L, nonce2.longValue());

        // Third call should return 12
        cachedNonceManager.incrementNonce();
        var nonce3 = cachedNonceManager.getNextNonce();
        assertEquals(12L, nonce3.longValue());

        // Verify ethGetTransactionCount was only called once (during initialization)
        verify(l1Web3j, times(1)).ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING));
    }

    @Test
    @SneakyThrows
    public void testQueryCurrNonceFromChain() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("5");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0xabcdef1234567890", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // Query before any getNextNonce call, should fetch from chain
        var currNonce = cachedNonceManager.getNextNonce();
        assertEquals(5L, currNonce.longValue());
    }

    @Test
    @SneakyThrows
    public void testQueryCurrNonceFromCache() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("20");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 2, "0x1111111111111111", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // First call to getNextNonce initializes cache
        var nonce1 = cachedNonceManager.getNextNonce();
        assertEquals(20L, nonce1.longValue());

        // Query current nonce from cache
        cachedNonceManager.incrementNonce();
        var currNonce = cachedNonceManager.getNextNonce();
        assertEquals(21L, currNonce.longValue());

        // Get next nonce again
        var nonce2 = cachedNonceManager.getNextNonce();
        assertEquals(21L, nonce2.longValue());

        // Query current nonce again
        cachedNonceManager.incrementNonce();
        var currNonce2 = cachedNonceManager.getNextNonce();
        assertEquals(22L, currNonce2.longValue());
    }

    @Test
    @SneakyThrows
    public void testIfResetNonceWithNonceTooLowError() {
        var result = new EthSendTransaction();
        var error = new EthSendTransaction.Error();
        error.setCode(-32000);
        error.setMessage("nonce too low");
        result.setError(error);

        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0xtest", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        assertTrue(cachedNonceManager.ifResetNonce(result));
    }

    @Test
    @SneakyThrows
    public void testIfResetNonceWithNonceTooHighError() {
        var result = new EthSendTransaction();
        var error = new EthSendTransaction.Error();
        error.setCode(-32000);
        error.setMessage("nonce too high: tx nonce 8810, gapped nonce 8805");
        result.setError(error);

        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        when(rollupRepository.queryLatestNonce(notNull(), notNull())).thenReturn(BigInteger.valueOf(8804), BigInteger.valueOf(8805));
        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0xtest", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        assertTrue(cachedNonceManager.ifResetNonce(result));
        assertFalse(cachedNonceManager.ifResetNonce(result));
    }

    @Test
    @SneakyThrows
    public void testIfResetNonceWithWrongErrorCode() {
        var result = new EthSendTransaction();
        var error = new EthSendTransaction.Error();
        error.setCode(-32001);
        error.setMessage("nonce too low");
        result.setError(error);

        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0xtest", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        assertFalse(cachedNonceManager.ifResetNonce(result));
    }

    @Test
    @SneakyThrows
    public void testIfResetNonceWithWrongMessage() {
        var result = new EthSendTransaction();
        var error = new EthSendTransaction.Error();
        error.setCode(-32000);
        error.setMessage("other error message");
        result.setError(error);

        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0xtest", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        assertFalse(cachedNonceManager.ifResetNonce(result));
    }

    @Test
    @SneakyThrows
    public void testResetNonce() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("100");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x2333333333333333", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // Initialize cache
        var nonce1 = cachedNonceManager.getNextNonce();
        assertEquals(100L, nonce1.longValue());

        // Get next nonce
        cachedNonceManager.incrementNonce();
        var nonce2 = cachedNonceManager.getNextNonce();
        assertEquals(101L, nonce2.longValue());

        // Reset nonce
        cachedNonceManager.resetNonce();

        // After reset, next call should query from chain again
        ethGetTransactionCount.setResult("100");
        var nonce3 = cachedNonceManager.getNextNonce();
        assertEquals(100L, nonce3.longValue());
    }

    @Test
    @SneakyThrows
    public void testIncrementNonce() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("100");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x2222222222222222", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);

        // Initialize cache
        cachedNonceManager.getNextNonce();

        cachedNonceManager.incrementNonce();
        Assert.assertEquals(BigInteger.valueOf(101), cachedNonceManager.getNextNonce());
    }

    @Test
    @SneakyThrows
    public void testSetNonceIntoCache() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("1");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x5555555555555555", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // Set nonce into cache directly
        cachedNonceManager.setNonceIntoCache(BigInteger.valueOf(1000));

        // Next getNextNonce should return 1000
        var nonce1 = cachedNonceManager.getNextNonce();
        assertEquals(1000L, nonce1.longValue());
    }

    @Test
    @SneakyThrows
    public void testDifferentChainIds() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("10");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var manager1 = new CachedNonceManager(redisson, l1Web3j, 1, "0xaddr1", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        var manager2 = new CachedNonceManager(redisson, l1Web3j, 2, "0xaddr1", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);

        // Both should initialize independently
        var nonce1 = manager1.getNextNonce();
        var nonce2 = manager2.getNextNonce();

        assertEquals(10L, nonce1.longValue());
        assertEquals(10L, nonce2.longValue());

        // Get next from manager1
        manager1.incrementNonce();
        var nonce1Next = manager1.getNextNonce();
        assertEquals(11L, nonce1Next.longValue());

        // manager2 should still be at 11
        manager2.incrementNonce();
        var nonce2Next = manager2.getNextNonce();
        assertEquals(11L, nonce2Next.longValue());
    }

    @Test
    @SneakyThrows
    public void testDifferentAddresses() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        ethGetTransactionCount.setResult("20");
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var manager1 = new CachedNonceManager(redisson, l1Web3j, 1, "0xaddress1111", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        var manager2 = new CachedNonceManager(redisson, l1Web3j, 1, "0xaddress2222", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);

        // Both should initialize independently
        var nonce1 = manager1.getNextNonce();
        var nonce2 = manager2.getNextNonce();

        assertEquals(20L, nonce1.longValue());
        assertEquals(20L, nonce2.longValue());

        // Incrementing manager1 should not affect manager2
        manager1.incrementNonce();  // 21

        var nonce2Next = manager2.getNextNonce();
        assertEquals(20L, nonce2Next.longValue());
    }

    @Test
    @SneakyThrows
    public void testGetTransactionCountError() {
        var ethGetTransactionCountReq = mock(Request.class);
        var ethGetTransactionCount = new EthGetTransactionCount();
        var error = new EthGetTransactionCount.Error();
        error.setCode(-32000);
        error.setMessage("internal server error");
        ethGetTransactionCount.setError(error);
        when(ethGetTransactionCountReq.send()).thenReturn(ethGetTransactionCount);
        when(l1Web3j.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))).thenReturn(ethGetTransactionCountReq);

        var cachedNonceManager = new CachedNonceManager(redisson, l1Web3j, 1, "0x6666666666666666", ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        
        // Should throw exception when querying transaction count fails
        assertThrows(RuntimeException.class, cachedNonceManager::getNextNonce);
    }
}
