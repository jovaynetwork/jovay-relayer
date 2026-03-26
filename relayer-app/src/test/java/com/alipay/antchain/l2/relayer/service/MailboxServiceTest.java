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
import java.util.Date;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionInfo;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.prover.ProverControllerClient;
import com.alipay.antchain.l2.relayer.core.tracer.TraceServiceClient;
import com.alipay.antchain.l2.relayer.dal.repository.*;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import jakarta.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MailboxServiceTest extends TestBase {

    @Resource
    private IMailboxService mailboxService;

    @MockitoBean
    private ISystemConfigRepository systemConfigRepository;

    @MockitoBean
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean
    private IL2MerkleTreeRepository l2MerkleTreeRepository;

    @MockitoBean
    private IMailboxRepository mailboxRepository;

    @MockitoBean
    private ProverControllerClient proverControllerClient;

    @MockitoBean
    private TraceServiceClient traceServiceClient;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(4);
    }

    @Test
    public void testInitService() {
        mailboxService.initService(BigInteger.ONE);
        verify(rollupRepository, times(1)).updateRollupNumberRecord(eq(ChainTypeEnum.LAYER_ONE), eq(RollupNumberRecordTypeEnum.BLOCK_PROCESSED), eq(BigInteger.ZERO));
    }

    @Test
    public void testProcessL1MsgBatch_processMissedL1Msg_processL1MsgReady() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(3), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx2 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(4), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx3 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));

        List<InterBlockchainMessageDO> messageDOS = ListUtil.toList(
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.ONE, tx1.getSourceTxHash(), tx1.getL1MsgTransaction()),
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(2), tx2.getSourceTxHash(), tx2.getL1MsgTransaction())
        );

        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.ONE);
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(messageDOS);

        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(tx3.getSourceBlockHeight(), tx3.getSourceTxHash(), tx3.getL1MsgTransaction());
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        when(mailboxRepository.getMessage(notNull(), eq(2L))).thenReturn(messageDO);

        when(l2Client.sendL1MsgTx(any(L1MsgTransaction.class))).thenReturn(
                TransactionInfo.builder()
                        .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                        .sendTxTime(new Date())
                        .rawTx("123".getBytes())
                        .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                        .nonce(BigInteger.valueOf(2))
                        .build()
        );

        mailboxService.processL1MsgBatch();

        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(3L),
                eq(InterBlockchainMessageStateEnum.MSG_PENDING)
        );
        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(4L),
                eq(InterBlockchainMessageStateEnum.MSG_PENDING)
        );
        verify(rollupRepository, times(2)).insertReliableTransaction(argThat(
                argument -> argument.getSenderAccount().equals("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
        ));
    }

    @Test
    public void testProcessL1MsgBatch_processMissedL1Msg_processL1MsgPending() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(3), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx2 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(4), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx3 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));

        List<InterBlockchainMessageDO> messageDOS = ListUtil.toList(
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(3), tx1.getSourceTxHash(), tx1.getL1MsgTransaction()),
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(4), tx2.getSourceTxHash(), tx2.getL1MsgTransaction())

        );

        List<InterBlockchainMessageDO> messageDOSPending = ListUtil.toList(
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(2), tx3.getSourceTxHash(), tx3.getL1MsgTransaction())
        );

        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.valueOf(3));
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(messageDOS);
        when(mailboxRepository.peekPendingMessages(notNull(), anyInt())).thenReturn(messageDOSPending);

        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(tx3.getSourceBlockHeight(), tx3.getSourceTxHash(), tx3.getL1MsgTransaction());
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        when(mailboxRepository.getMessage(notNull(), eq(2L))).thenReturn(messageDO);

        when(l2Client.sendL1MsgTx(any(L1MsgTransaction.class))).thenReturn(
                TransactionInfo.builder()
                        .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                        .sendTxTime(new Date())
                        .rawTx("123".getBytes())
                        .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                        .nonce(BigInteger.valueOf(2))
                        .build()
        );

        mailboxService.processL1MsgBatch();

        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(3L),
                eq(InterBlockchainMessageStateEnum.MSG_PENDING)
        );
        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(4L),
                eq(InterBlockchainMessageStateEnum.MSG_PENDING)
        );

        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(2L),
                eq(InterBlockchainMessageStateEnum.MSG_COMMITTED)
        );
        verify(rollupRepository, times(2)).insertReliableTransaction(argThat(
                argument -> argument.getSenderAccount().equals("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
        ));
    }


    @Test
    public void testProcessL1MsgBatch_processL1MsgPackaged_processL1MsgReadyToCommitted() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(3), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));
        L1MsgTransactionInfo tx2 = new L1MsgTransactionInfo(new L1MsgTransaction(BigInteger.valueOf(4), BigInteger.valueOf(1_000), "123"), BigInteger.ONE, HexUtil.encodeHexStr(RandomUtil.randomBytes(32)));


        List<InterBlockchainMessageDO> messageDOS = ListUtil.toList(
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(3), tx1.getSourceTxHash(), tx1.getL1MsgTransaction()),
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(4), tx2.getSourceTxHash(), tx2.getL1MsgTransaction())

        );

        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.valueOf(4));
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(messageDOS);

        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(tx2.getSourceBlockHeight(), tx2.getSourceTxHash(), tx2.getL1MsgTransaction());
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        when(mailboxRepository.getMessage(notNull(), eq(2L))).thenReturn(messageDO);

        when(l2Client.sendL1MsgTx(any(L1MsgTransaction.class))).thenReturn(
                TransactionInfo.builder()
                        .txHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                        .sendTxTime(new Date())
                        .rawTx("123".getBytes())
                        .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                        .nonce(BigInteger.valueOf(2))
                        .build()
        );

        mailboxService.processL1MsgBatch();

        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(3L),
                eq(InterBlockchainMessageStateEnum.MSG_COMMITTED)
        );
        verify(mailboxRepository, times(1)).updateMessageState(
                eq(InterBlockchainMessageTypeEnum.L1_MSG),
                eq(4L),
                eq(InterBlockchainMessageStateEnum.MSG_PENDING)
        );
        verify(rollupRepository, times(1)).insertReliableTransaction(argThat(
                argument -> argument.getSenderAccount().equals("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
        ));
    }

    @Test
    public void testProveL2Msg() {
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.hasBatch(eq(BigInteger.ONE))).thenReturn(true);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(new BatchProveRequestDO(BigInteger.ONE, ProveTypeEnum.TEE_PROOF, new byte[]{}, BatchProveRequestStateEnum.PROVE_READY, new Date()));
        when(l2MerkleTreeRepository.getMerkleTree(eq(BigInteger.ZERO))).thenReturn(
                new AppendMerkleTree(BigInteger.ZERO, new byte[]{})
        );
        when(mailboxRepository.getMsgHashes(eq(InterBlockchainMessageTypeEnum.L2_MSG), eq(BigInteger.ONE))).thenReturn(ListUtil.toList(RandomUtil.randomBytes(32)));

        mailboxService.proveL2Msg();

        verify(l2MerkleTreeRepository, times(1)).saveMerkleTree(notNull(), eq(BigInteger.ONE));
        verify(mailboxRepository, times(1)).saveL2MsgProofs(notNull());
        verify(rollupRepository, times(1)).updateRollupNumberRecord(
                eq(ChainTypeEnum.LAYER_TWO),
                eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH),
                eq(BigInteger.valueOf(2))
        );
    }

    // ==================== Negative Case Tests ====================

    @Test
    public void testProcessL1MsgBatch_L2ClientException() {
        when(l2Client.queryFinalizeL1MsgNonce()).thenThrow(new RuntimeException("L2 client connection failed"));

        try {
            mailboxService.processL1MsgBatch();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("L2 client connection failed"));
        }
    }

    @Test
    public void testProcessL1MsgBatch_MessageNotFound() {
        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.ONE);
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(ListUtil.empty());
        when(mailboxRepository.getMessage(notNull(), anyLong())).thenReturn(null);

        mailboxService.processL1MsgBatch();

        verify(l2Client, never()).sendL1MsgTx(any());
    }

    @Test
    public void testProcessL1MsgBatch_SendTransactionFailed() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(
                new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "123"),
                BigInteger.ONE,
                HexUtil.encodeHexStr(RandomUtil.randomBytes(32))
        );

        List<InterBlockchainMessageDO> messageDOS = ListUtil.toList(
                InterBlockchainMessageDO.fromL1MsgTx(BigInteger.ONE, tx1.getSourceTxHash(), tx1.getL1MsgTransaction())
        );

        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.ONE);
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(messageDOS);
        when(l2Client.sendL1MsgTx(notNull())).thenThrow(new RuntimeException("Transaction send failed"));

        try {
            mailboxService.processL1MsgBatch();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Transaction send failed"));
        }
    }

    @Test
    public void testProveL2Msg_BatchNotFound() {
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.hasBatch(eq(BigInteger.ONE))).thenReturn(false);

        mailboxService.proveL2Msg();

        verify(l2MerkleTreeRepository, never()).saveMerkleTree(any(), any());
        verify(mailboxRepository, never()).saveL2MsgProofs(any());
    }

    @Test
    public void testProveL2Msg_ProveRequestNotCommitted() {
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.hasBatch(eq(BigInteger.ONE))).thenReturn(true);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(new BatchProveRequestDO(BigInteger.ONE, ProveTypeEnum.TEE_PROOF, new byte[]{}, BatchProveRequestStateEnum.PENDING, new Date()));

        mailboxService.proveL2Msg();

        verify(l2MerkleTreeRepository, never()).saveMerkleTree(any(), any());
        verify(mailboxRepository, never()).saveL2MsgProofs(any());
    }

    @Test
    public void testProveL2Msg_MerkleTreeException() {
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.hasBatch(eq(BigInteger.ONE))).thenReturn(true);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(new BatchProveRequestDO(BigInteger.ONE, ProveTypeEnum.TEE_PROOF, new byte[]{}, BatchProveRequestStateEnum.COMMITTED, new Date()));
        when(l2MerkleTreeRepository.getMerkleTree(eq(BigInteger.ZERO))).thenThrow(new RuntimeException("Merkle tree corruption"));

        try {
            mailboxService.proveL2Msg();
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Merkle tree corruption"));
        }
    }

    @Test
    public void testProcessL1MsgBatch_InvalidMessageState() {
        L1MsgTransactionInfo tx1 = new L1MsgTransactionInfo(
                new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "123"),
                BigInteger.ONE,
                HexUtil.encodeHexStr(RandomUtil.randomBytes(32))
        );

        when(l2Client.queryFinalizeL1MsgNonce()).thenReturn(BigInteger.ONE);
        when(mailboxRepository.peekReadyMessages(notNull(), anyInt())).thenReturn(ListUtil.empty());

        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(
                tx1.getSourceBlockHeight(),
                tx1.getSourceTxHash(),
                tx1.getL1MsgTransaction()
        );
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);
        when(mailboxRepository.getMessage(notNull(), eq(2L))).thenReturn(messageDO);

        mailboxService.processL1MsgBatch();

        verify(l2Client, never()).sendL1MsgTx(any());
    }

    @Test
    public void testProveL2Msg_EmptyMessageHashes() {
        when(rollupRepository.getRollupNumberRecord(eq(ChainTypeEnum.LAYER_TWO), eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH)))
                .thenReturn(BigInteger.ONE);
        when(rollupRepository.hasBatch(eq(BigInteger.ONE))).thenReturn(true);
        when(rollupRepository.getBatchProveRequest(eq(BigInteger.ONE), eq(ProveTypeEnum.TEE_PROOF)))
                .thenReturn(new BatchProveRequestDO(BigInteger.ONE, ProveTypeEnum.TEE_PROOF, new byte[]{}, BatchProveRequestStateEnum.COMMITTED, new Date()));
        when(l2MerkleTreeRepository.getMerkleTree(eq(BigInteger.ZERO))).thenReturn(
                new AppendMerkleTree(BigInteger.ZERO, new byte[]{})
        );
        when(mailboxRepository.getMsgHashes(eq(InterBlockchainMessageTypeEnum.L2_MSG), eq(BigInteger.ONE))).thenReturn(ListUtil.empty());

        mailboxService.proveL2Msg();

        verify(l2MerkleTreeRepository, times(1)).saveMerkleTree(notNull(), eq(BigInteger.ONE));
        verify(rollupRepository, times(1)).updateRollupNumberRecord(
                eq(ChainTypeEnum.LAYER_TWO),
                eq(RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH),
                eq(BigInteger.valueOf(2))
        );
    }
}
