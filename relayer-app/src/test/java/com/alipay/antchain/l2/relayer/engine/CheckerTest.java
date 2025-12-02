package com.alipay.antchain.l2.relayer.engine;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.checker.DefaultDistributedTaskChecker;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.Mockito.when;

public class CheckerTest extends TestBase {

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

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Resource(name = "defaultDistributedTaskChecker")
    private DefaultDistributedTaskChecker checker;

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
    public void testDefaultDistributedTaskChecker() {
        var future = CompletableFuture.runAsync(() -> {
            System.out.println("test");
            throw new RuntimeException();
        });
        checker.addLocalFuture(BizTaskTypeEnum.RELIABLE_TX_TASK, future);

        Assert.assertThrows(RuntimeException.class, future::join);

        Assert.assertTrue(checker.checkIfContinue(BizTaskTypeEnum.RELIABLE_TX_TASK));
    }
}
