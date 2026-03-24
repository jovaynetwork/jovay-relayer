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

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;

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

    @MockitoBean
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
}
