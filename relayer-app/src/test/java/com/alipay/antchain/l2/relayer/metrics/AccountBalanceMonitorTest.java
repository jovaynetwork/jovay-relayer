package com.alipay.antchain.l2.relayer.metrics;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.BaseRawTransactionManager;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.relayer.metrics.monitor.AccountBalanceMonitor;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountBalanceMonitorTest extends TestBase {

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource
    private AccountBalanceMonitor monitor;

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
    public void testMonitorL1() {
        var legacyTxManager = mock(BaseRawTransactionManager.class);
        var blobTxManager = mock(BaseRawTransactionManager.class);
        when(legacyTxManager.getAddress()).thenReturn("0x123");
        when(blobTxManager.getAddress()).thenReturn("0x456");
        when(l1Client.getLegacyPoolTxManager()).thenReturn(legacyTxManager);
        when(l1Client.getBlobPoolTxManager()).thenReturn(blobTxManager);
        when(l1Client.queryAccountBalance(anyString(), notNull())).thenReturn(
                Convert.toWei(BigDecimal.valueOf(3.1), Convert.Unit.ETHER).toBigInteger(),
                Convert.toWei(BigDecimal.valueOf(2.1), Convert.Unit.ETHER).toBigInteger()
        );
        monitor.monitorL1Acc();
        monitor.monitorL1Acc();
    }
}
