package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.FetchL1MsgException;
import com.alipay.antchain.l2.relayer.commons.exceptions.InitMailboxServiceException;
import com.alipay.antchain.l2.relayer.commons.exceptions.ProcessL1MsgException;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionBatch;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.layer2.IL2MerkleTreeAggregator;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.protocol.core.DefaultBlockParameterName;

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
    private L1Client l1Client;

    @Resource
    private L2Client l2Client;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${l2-relayer.tasks.block-polling.l1.max-poling-block-size:32}")
    private int maxPollingBlockSize;

    @Value("${l2-relayer.tasks.block-polling.l1.policy:FINALIZED}")
    private DefaultBlockParameterName blockPollingPolicy;

    @Value("${l2-relayer.tasks.mailbox.max-pending-l1msg:256}")
    private int maxPendingL1Msg;

    @Value("${l2-relayer.tasks.mailbox.l1msg-per-batch-limit:32}")
    private int l1MsgNumberPerBatchLimit;

    @Value("${l2-relayer.tasks.mailbox.l2msg-to-prove-per-batch-limit:32}")
    private int l2MsgNumberToProvePerBatchLimit;

    @Override
    public void initService(BigInteger startBlockNumber) {
        if (ObjectUtil.isNotNull(getProcessedL1BlockNumber())) {
            throw new InitMailboxServiceException("l1 block processed number has been set");
        }
        log.info("init mailbox service that scanning work from height {}", startBlockNumber);
        updateProcessedL1BlockNumber(startBlockNumber.subtract(BigInteger.ONE));
    }

    @Override
    public void pollL1MsgBatch() {
        BigInteger processedL1BlockNumber = getProcessedL1BlockNumber();
        BigInteger latestL1BlockNumber = l1Client.queryLatestBlockNumber(blockPollingPolicy);
        if (ObjectUtil.isNull(processedL1BlockNumber)) {
            log.info("⌛️ set the start height for l1Msg polling task please, wait for it...");
            return;
        }
        if (latestL1BlockNumber.compareTo(processedL1BlockNumber) <= 0) {
            log.debug("already processed the latest height {} on L1", latestL1BlockNumber);
            return;
        }

        BigInteger maxPollingLimit = processedL1BlockNumber.add(BigInteger.valueOf(maxPollingBlockSize));
        maxPollingLimit = latestL1BlockNumber.compareTo(maxPollingLimit) <= 0 ? latestL1BlockNumber : maxPollingLimit;
        log.info("process blocks from {} to {} included and latest {} on l1 chain", processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit, latestL1BlockNumber);

        BigInteger finalMaxPollingLimit = maxPollingLimit;
        l1Client.flowableL1MsgFromMailbox(processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit)
                .subscribe(
                        this::receiveL1Msgs,
                        throwable -> {
                            throw new FetchL1MsgException(throwable, "failed to fetch l1 msg");
                        },
                        () -> log.info("fetch l1 msg from height {} to {} completed", processedL1BlockNumber.add(BigInteger.ONE), finalMaxPollingLimit)
                );

        log.info("🎉 successful to process l1 block from {} to {}", processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit);
    }

    @Override
    public void processL1MsgBatch() {
        BigInteger pendingNonce = l2Client.queryL2MailboxPendingNonce();
        BigInteger latestNonce = l2Client.queryL2MailboxLatestNonce();
        log.debug("current pending l1Msg number is {}, latest l1Msg number is {}", pendingNonce, latestNonce);
        if (pendingNonce.subtract(latestNonce).intValue() > maxPendingL1Msg) {
            log.warn("Meet current limiting conditions for l1Msg committing: {} pending over limit {}", pendingNonce.subtract(latestNonce), maxPendingL1Msg);
            return;
        }

        List<InterBlockchainMessageDO> messageDOS = mailboxRepository.peekReadyMessages(InterBlockchainMessageTypeEnum.L1_MSG, l1MsgNumberPerBatchLimit);
        if (ObjectUtil.isEmpty(messageDOS)) {
            log.debug("no l1Msg to process now!");
            return;
        }

        BigInteger minNonceInBatch = messageDOS.stream().map(InterBlockchainMessageDO::getNonce).min(BigInteger::compareTo).orElseThrow(() -> new RuntimeException("no l1Msg"));
        if (minNonceInBatch.compareTo(pendingNonce.add(BigInteger.ONE)) > 0) {
            processMissedL1Msg(pendingNonce, minNonceInBatch);
        }

        for (InterBlockchainMessageDO messageDO : messageDOS) {
            L1MsgTransaction l1MsgTransaction = messageDO.toL1MsgTransaction();
            log.info("process l1Msg {} with nonce {} now! ", messageDO.getMsgHashHex(), l1MsgTransaction.getNonce());

            if (messageDO.getNonce().compareTo(latestNonce) <= 0) {
                processL1MsgPackaged(messageDO, l1MsgTransaction);
                continue;
            } else if (messageDO.getNonce().compareTo(pendingNonce) <= 0) {
                processL1MsgPending(messageDO, l1MsgTransaction);
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

    private void receiveL1Msgs(L1MsgTransactionBatch batch) {
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        updateProcessedL1BlockNumber(batch.getHeight());
                        if (batch.getL1MsgTransactionInfos().isEmpty()) {
                            log.debug("get empty l1Msg batch for {}", batch.getHeight());
                            return;
                        }
                        log.info("receiving {} l1Msgs from height {}", batch.getL1MsgTransactionInfos().size(), batch.getHeight());
                        mailboxRepository.saveMessages(batch.toInterBlockchainMessages());
                    }
                }
        );
    }

    private void processL1MsgPackaged(InterBlockchainMessageDO messageDO, L1MsgTransaction l1MsgTransaction) {
        log.info("l1Msg {} with nonce {} has been found packaged on L2, just update its state to MSG_COMMITTED on DB",
                messageDO.getMsgHashHex(), messageDO.getNonce());
        mailboxRepository.updateMessageState(
                InterBlockchainMessageTypeEnum.L1_MSG,
                l1MsgTransaction.getNonce().longValue(),
                InterBlockchainMessageStateEnum.MSG_COMMITTED
        );
    }

    private void processL1MsgPending(InterBlockchainMessageDO messageDO, L1MsgTransaction l1MsgTransaction) {
        log.info("l1Msg {} with nonce {} has been found pending on L2, update its state to MSG_COMMITTED on DB",
                messageDO.getMsgHashHex(), messageDO.getNonce());

        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        mailboxRepository.updateMessageState(
                                InterBlockchainMessageTypeEnum.L1_MSG,
                                l1MsgTransaction.getNonce().longValue(),
                                InterBlockchainMessageStateEnum.MSG_COMMITTED
                        );

                        ReliableTransactionDO reliableTransactionDO = rollupRepository.getReliableTransaction(messageDO.getMsgHashHex());
                        if (ObjectUtil.isNull(reliableTransactionDO)) {
                            log.info("None reliable tx data for l1Msg {} with nonce {}", messageDO.getMsgHashHex(), messageDO.getNonce());
                            rollupRepository.insertReliableTransaction(
                                    ReliableTransactionDO.builder()
                                            // need to sign it if resend this l1Msg
                                            .rawTx(messageDO.getRawMessage())
                                            .latestTxHash(messageDO.getMsgHashHex())
                                            .originalTxHash(messageDO.getMsgHashHex())
                                            .nonce(l1MsgTransaction.getNonce().longValue())
                                            .state(ReliableTransactionStateEnum.TX_PENDING)
                                            .chainType(ChainTypeEnum.LAYER_TWO)
                                            .senderAccount(L1MsgTransaction.L1_MAILBOX_AS_SENDER.toString())
                                            .latestTxSendTime(new Date())
                                            .batchIndex(l1MsgTransaction.getNonce())
                                            .transactionType(TransactionTypeEnum.L1_MSG_TX)
                                            .build()
                            );
                        } else {
                            log.error("🚨 l1Msg {} with nonce {} has state {} but its reliable tx data has been exist",
                                    messageDO.getMsgHashHex(), messageDO.getNonce(), reliableTransactionDO.getState());
                        }
                    }
                }
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
                                InterBlockchainMessageStateEnum.MSG_COMMITTED
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

    private void processMissedL1Msg(BigInteger pendingNonce, BigInteger minNonceInBatch) {
        BigInteger startNonce = pendingNonce.add(BigInteger.ONE);
        BigInteger endNonce = minNonceInBatch.subtract(BigInteger.ONE);
        log.info("Missed l1Msg from nonce {} to nonce {}", startNonce, endNonce);

        for (BigInteger curr = startNonce; curr.compareTo(endNonce) <= 0; curr = curr.add(BigInteger.ONE)) {
            log.info("processing missed l1Msg with nonce {}", curr);

            InterBlockchainMessageDO messageDO = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L1_MSG, curr.longValue());
            if (ObjectUtil.isNull(messageDO)) {
                throw new ProcessL1MsgException("none message data found for l1Msg {}", curr.toString());
            }

            ReliableTransactionDO transactionDO = rollupRepository.getReliableTransaction(messageDO.getMsgHashHex());
            if (ObjectUtil.isNotNull(transactionDO)) {
                log.info("Reliable tx data already exist for l1Msg with nonce {}, let reliable task deal with it", curr);
            }

            L1MsgTransaction l1MsgTransaction = messageDO.toL1MsgTransaction();
            TransactionInfo transactionInfo = l2Client.sendL1MsgTx(l1MsgTransaction);
            log.info("commit missed l1Msg {} of nonce {} with txhash {}", messageDO.getMsgHashHex(), l1MsgTransaction.getNonce(), transactionInfo.getTxHash());

            transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            if (messageDO.getState() != InterBlockchainMessageStateEnum.MSG_COMMITTED) {
                                mailboxRepository.updateMessageState(
                                        InterBlockchainMessageTypeEnum.L1_MSG,
                                        l1MsgTransaction.getNonce().longValue(),
                                        InterBlockchainMessageStateEnum.MSG_COMMITTED
                                );
                            }
                            ReliableTransactionDO resendTransactionDO = ReliableTransactionDO.builder()
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
                                    .build();
                            if (ObjectUtil.isNull(transactionDO)) {
                                rollupRepository.insertReliableTransaction(resendTransactionDO);
                            } else {
                                rollupRepository.updateReliableTransaction(resendTransactionDO);
                            }
                        }
                    }
            );
        }
    }

    private boolean isBatchReadyForL2MsgProof(BigInteger batchIndex) {
        return rollupRepository.hasBatch(batchIndex) && rollupRepository.getBatchProveRequest(batchIndex, ProveTypeEnum.TEE_PROOF).getState() == BatchProveRequestStateEnum.COMMITTED;
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
