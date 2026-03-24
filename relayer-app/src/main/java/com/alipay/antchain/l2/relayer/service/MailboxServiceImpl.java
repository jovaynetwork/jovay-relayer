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
import java.util.List;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.InitMailboxServiceException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.layer2.IL2MerkleTreeAggregator;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class MailboxServiceImpl implements IMailboxService {

    @Resource
    private IMailboxRepository mailboxRepository;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private IL2MerkleTreeAggregator l2MerkleTreeAggregator;

    @Resource
    private L2Client l2Client;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${l2-relayer.tasks.mailbox.l1msg-per-batch-limit:32}")
    private int l1MsgNumberPerBatchLimit;

    @Override
    public void initService(BigInteger startBlockNumber) {
        if (ObjectUtil.isNotNull(getProcessedL1BlockNumber())) {
            throw new InitMailboxServiceException("l1 block processed number has been set");
        }
        log.info("init mailbox service that scanning work from height {}", startBlockNumber);
        updateProcessedL1BlockNumber(startBlockNumber.subtract(BigInteger.ONE));
    }

    @Override
    public void processL1MsgBatch() {
        BigInteger finalizeL1MsgNonce = l2Client.queryFinalizeL1MsgNonce();

        List<InterBlockchainMessageDO> messageDOSPending = mailboxRepository.peekPendingMessages(InterBlockchainMessageTypeEnum.L1_MSG, l1MsgNumberPerBatchLimit);
        for (InterBlockchainMessageDO interBlockchainMessageDO : messageDOSPending) {
            if (interBlockchainMessageDO.getNonce().compareTo(finalizeL1MsgNonce) < 0) {
                processL1MsgCommitted(interBlockchainMessageDO, interBlockchainMessageDO.toL1MsgTransaction());
            }
        }

        List<InterBlockchainMessageDO> messageDOS = mailboxRepository.peekReadyMessages(InterBlockchainMessageTypeEnum.L1_MSG, l1MsgNumberPerBatchLimit);
        if (ObjectUtil.isEmpty(messageDOS)) {
            log.debug("no l1Msg to process now!");
            return;
        }

        for (InterBlockchainMessageDO messageDO : messageDOS) {
            L1MsgTransaction l1MsgTransaction = messageDO.toL1MsgTransaction();
            log.info("process l1Msg {} with nonce {} now! ", messageDO.getMsgHashHex(), l1MsgTransaction.getNonce());

            if (messageDO.getNonce().compareTo(finalizeL1MsgNonce) < 0) {
                processL1MsgCommitted(messageDO, l1MsgTransaction);
                continue;
            }

            processL1MsgReady(messageDO, l1MsgTransaction);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void proveL2Msg() {
        BigInteger nextBatchIndex = getNextBatchIndexToProveL2Msg();
        if (ObjectUtil.isNull(nextBatchIndex)) {
            log.info("⚠️ next batch index to prove l2Msg not set, please set it! ");
            return;
        }
        if (!isBatchReadyForL2MsgProof(nextBatchIndex)) {
            log.debug("batch {} not ready to generate proof for l2Msgs", nextBatchIndex);
            return;
        }

        Map<BigInteger, byte[]> proofResult = l2MerkleTreeAggregator.aggregate(nextBatchIndex);
        if (ObjectUtil.isEmpty(proofResult)) {
            log.info("empty merkle proof back for batch#{}, just update the next batch number", nextBatchIndex);
        }

        mailboxRepository.saveL2MsgProofs(proofResult);
        updateNextBatchIndexToProveL2Msg(nextBatchIndex.add(BigInteger.ONE));

        log.info("🎉 successful to generate {} l2Msg proof for batch#{}",
                ObjectUtil.defaultIfNull(proofResult, MapUtil.empty()).size(), nextBatchIndex);
    }

    private void processL1MsgCommitted(InterBlockchainMessageDO messageDO, L1MsgTransaction l1MsgTransaction) {
        log.info("l1Msg {} with nonce {} has been found packaged on L2, just update its state to MSG_COMMITTED on DB",
                messageDO.getMsgHashHex(), messageDO.getNonce());
        mailboxRepository.updateMessageState(
                InterBlockchainMessageTypeEnum.L1_MSG,
                l1MsgTransaction.getNonce().longValue(),
                InterBlockchainMessageStateEnum.MSG_COMMITTED
        );
    }

    private void processL1MsgReady(InterBlockchainMessageDO messageDO, L1MsgTransaction l1MsgTransaction) {
        TransactionInfo transactionInfo = l2Client.sendL1MsgTx(l1MsgTransaction);
        log.info("commit l1Msg {} with nonce {} with txhash {}", messageDO.getMsgHashHex(), l1MsgTransaction.getNonce(), transactionInfo.getTxHash());

        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        mailboxRepository.updateMessageState(
                                InterBlockchainMessageTypeEnum.L1_MSG,
                                l1MsgTransaction.getNonce().longValue(),
                                InterBlockchainMessageStateEnum.MSG_PENDING
                        );
                        rollupRepository.insertReliableTransaction(
                                ReliableTransactionDO.builder()
                                        .rawTx(transactionInfo.getRawTx())
                                        .latestTxHash(transactionInfo.getTxHash())
                                        .originalTxHash(transactionInfo.getTxHash())
                                        .nonce(transactionInfo.getNonce().longValue())
                                        .state(ReliableTransactionStateEnum.TX_PENDING)
                                        .chainType(ChainTypeEnum.LAYER_TWO)
                                        .senderAccount(transactionInfo.getSenderAccount())
                                        .latestTxSendTime(transactionInfo.getSendTxTime())
                                        .batchIndex(l1MsgTransaction.getNonce())
                                        .transactionType(TransactionTypeEnum.L1_MSG_TX)
                                        .build()
                        );
                    }
                }
        );
    }

    private boolean isBatchReadyForL2MsgProof(BigInteger batchIndex) {
        if (!rollupRepository.hasBatch(batchIndex)) {
            return false;
        }
        var teeProveReq = rollupRepository.getBatchProveRequest(batchIndex, ProveTypeEnum.TEE_PROOF);
        if (teeProveReq.getState() == BatchProveRequestStateEnum.PROVE_READY
            || teeProveReq.getState() == BatchProveRequestStateEnum.COMMITTED) {
            return true;
        }
        var zkProveReq = rollupRepository.getBatchProveRequest(batchIndex, ProveTypeEnum.ZK_PROOF);
        return ObjectUtil.isNotNull(zkProveReq) && (zkProveReq.getState() == BatchProveRequestStateEnum.PROVE_READY
                                                    || zkProveReq.getState() == BatchProveRequestStateEnum.COMMITTED);
    }

    private BigInteger getProcessedL1BlockNumber() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.BLOCK_PROCESSED);
    }

    private void updateProcessedL1BlockNumber(BigInteger blockNumber) {
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.BLOCK_PROCESSED, blockNumber);
    }

    private BigInteger getNextBatchIndexToProveL2Msg() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH);
    }

    private void updateNextBatchIndexToProveL2Msg(BigInteger batchIndex) {
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH, batchIndex);
    }
}
