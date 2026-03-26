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

package com.alipay.antchain.l2.relayer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionBatch;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class L1ListenServiceTest extends TestBase {
    @Resource
    private IL1ListenService l1ListenService;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IMailboxRepository mailboxRepository;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    private static final BigInteger BLOB_BASE_FEE_UPDATE_FRACTION = new BigInteger("5007716");

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx2 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));

        EthBlock ethBlock = mockEthBlock(
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(BLOB_BASE_FEE_UPDATE_FRACTION)
        );
        ethBlock.getBlock().setNumber(String.valueOf(2));

        when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
        when(l1Client.queryLatestBlockHeader(notNull())).thenReturn(ethBlock);
        when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));
        CompletableFuture<L1MsgTransactionBatch> future1 = mock(CompletableFuture.class);
        when(future1.get(anyLong(), notNull())).thenReturn(
                new L1MsgTransactionBatch(
                        ListUtil.toList(tx1), BigInteger.ONE
                )
        );
        CompletableFuture<L1MsgTransactionBatch> future2 = mock(CompletableFuture.class);
        when(future2.get(anyLong(), notNull())).thenReturn(
                new L1MsgTransactionBatch(
                        ListUtil.toList(tx2), BigInteger.valueOf(2)
                )
        );

        when(l1Client.flowableL1MsgFromMailbox(notNull(), notNull()))
                .thenReturn(Flowable.merge(
                        Flowable.fromFuture(future1, 30, TimeUnit.SECONDS),
                        Flowable.fromFuture(future2, 30, TimeUnit.SECONDS)
                ));

        l1ListenService.pollL1MsgBatch();

        verify(mailboxRepository, times(1)).saveMessages(argThat(argument -> argument.get(0).getSourceTxHash().equals(tx1.getSourceTxHash())));
        verify(mailboxRepository, times(1)).saveMessages(argThat(argument -> argument.get(0).getSourceTxHash().equals(tx2.getSourceTxHash())));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(2)));
        verify(oracleRepository, times(1)).saveBlockFeeInfo(argThat(argument -> argument.getNumber().equals(ethBlock.getBlock().getNumber())));
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
    // ==================== Negative Case Tests ====================

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_L1ClientException() {
        when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
        when(l1Client.queryLatestBlockHeader(notNull())).thenThrow(new RuntimeException("L1 client connection failed"));

        try {
            l1ListenService.pollL1MsgBatch();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("L1 client connection failed"));
        }
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_BlockHeaderQueryFailed() {
        when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
        when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));
        when(l1Client.queryLatestBlockHeader(notNull())).thenThrow(new RuntimeException(new IOException("Failed to query block header")));

        try {
            l1ListenService.pollL1MsgBatch();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Failed to query block header"));
        }
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_RepositorySaveException() {
        Logger logger = (Logger) LoggerFactory.getLogger(L1ListenServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(
                    new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123"),
                    BigInteger.ONE,
                    HexUtil.encodeHexStr(RandomUtil.randomBytes(32))
            );

            EthBlock ethBlock = mockEthBlock(
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(BLOB_BASE_FEE_UPDATE_FRACTION)
            );
            ethBlock.getBlock().setNumber(String.valueOf(2));

            when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
            when(l1Client.queryLatestBlockHeader(notNull())).thenReturn(ethBlock);
            when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));

            CompletableFuture<L1MsgTransactionBatch> future1 = mock(CompletableFuture.class);
            when(future1.get(anyLong(), notNull())).thenReturn(
                    new L1MsgTransactionBatch(ListUtil.toList(tx1), BigInteger.ONE)
            );

            when(l1Client.flowableL1MsgFromMailbox(notNull(), notNull()))
                    .thenReturn(Flowable.fromFuture(future1, 30, TimeUnit.SECONDS));

            doThrow(new RuntimeException("Database save failed")).when(mailboxRepository).saveMessages(any());

            l1ListenService.pollL1MsgBatch();

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains("Database save failed"));
            assertTrue("Expected info log 'Database save failed' not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_InvalidBlockData() {
        when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
        when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));

        EthBlock ethBlock = new EthBlock();
        ethBlock.setResult(null);

        when(l1Client.queryLatestBlockHeader(notNull())).thenReturn(ethBlock);

        try {
            l1ListenService.pollL1MsgBatch();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_TimeoutException() {
        when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
        when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));

        EthBlock ethBlock = mockEthBlock(
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(RandomUtil.randomLong()),
                String.valueOf(BLOB_BASE_FEE_UPDATE_FRACTION)
        );
        ethBlock.getBlock().setNumber(String.valueOf(3));

        when(l1Client.queryLatestBlockHeader(notNull())).thenReturn(ethBlock);

        CompletableFuture<L1MsgTransactionBatch> positiveFuture1 = mock(CompletableFuture.class);
        when(positiveFuture1.get(anyLong(), notNull())).thenReturn(
                new L1MsgTransactionBatch(ListUtil.empty(), BigInteger.ONE)
        );

        CompletableFuture<L1MsgTransactionBatch> negativeFuture = mock(CompletableFuture.class);
        when(negativeFuture.get(anyLong(), notNull())).thenThrow(new java.util.concurrent.TimeoutException("Timeout waiting for transaction batch"));

        CompletableFuture<L1MsgTransactionBatch> positiveFuture2 = mock(CompletableFuture.class);
        when(positiveFuture2.get(anyLong(), notNull())).thenReturn(
                new L1MsgTransactionBatch(ListUtil.empty(), BigInteger.valueOf(3))
        );

        when(l1Client.flowableL1MsgFromMailbox(notNull(), notNull()))
                .thenReturn(Flowable.merge(
                        Flowable.fromFuture(positiveFuture1, 30, TimeUnit.SECONDS),
                        Flowable.fromFuture(negativeFuture, 30, TimeUnit.SECONDS),
                        Flowable.fromFuture(positiveFuture2, 30, TimeUnit.SECONDS)
                ));

        l1ListenService.pollL1MsgBatch();
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ONE));
        verify(rollupRepository, never()).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.TWO));
        verify(rollupRepository, never()).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.valueOf(3)));
    }

    @Test
    @SneakyThrows
    public void testPollL1MsgBatch_EmptyTransactionBatch() {
        // 设置日志捕获
        Logger logger = (Logger) LoggerFactory.getLogger(L1ListenServiceImpl.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            EthBlock ethBlock = mockEthBlock(
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(RandomUtil.randomLong()),
                    String.valueOf(BLOB_BASE_FEE_UPDATE_FRACTION)
            );
            ethBlock.getBlock().setNumber(String.valueOf(2));

            when(rollupRepository.getRollupNumberRecord(notNull(), notNull())).thenReturn(BigInteger.ZERO);
            when(l1Client.queryLatestBlockHeader(notNull())).thenReturn(ethBlock);
            when(l1Client.queryLatestBlockNumber(notNull())).thenReturn(BigInteger.valueOf(2));

            CompletableFuture<L1MsgTransactionBatch> future = mock(CompletableFuture.class);
            when(future.get(anyLong(), notNull())).thenReturn(
                    new L1MsgTransactionBatch(ListUtil.empty(), BigInteger.ONE)
            );

            when(l1Client.flowableL1MsgFromMailbox(notNull(), notNull()))
                    .thenReturn(Flowable.fromFuture(future, 30, TimeUnit.SECONDS));

            l1ListenService.pollL1MsgBatch();

            boolean foundInfoLog = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("get empty l1Msg batch for 1"));
            assertTrue("Expected info log not found", foundInfoLog);
        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(Level.INFO);
        }
    }
}
