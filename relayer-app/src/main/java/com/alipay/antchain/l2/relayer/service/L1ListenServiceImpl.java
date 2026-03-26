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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.L1BlockFeeInfo;
import com.alipay.antchain.l2.relayer.commons.models.L1MsgTransactionBatch;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

@Service
@Slf4j
public class L1ListenServiceImpl implements IL1ListenService {
    @Resource
    private IOracleRepository oracleRepository;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private IMailboxRepository mailboxRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RollupConfig rollupConfig;

    @Resource
    private L1Client l1Client;

    @Value("${l2-relayer.tasks.block-polling.l1.max-poling-block-size:32}")
    private int maxPollingBlockSize;

    @Value("${l2-relayer.tasks.block-polling.l1.policy:FINALIZED}")
    private DefaultBlockParameterName blockPollingPolicy;

    @Override
    public void pollL1MsgBatch() {
        BigInteger processedL1BlockNumber = getProcessedL1BlockNumber();
        if (ObjectUtil.isNull(processedL1BlockNumber)) {
            log.info("⌛️ set the start height for l1Msg polling task please, wait for it...");
            return;
        }

        var latestL1Block = l1Client.queryLatestBlockHeader(blockPollingPolicy);
        if (latestL1Block.hasError()) {
            throw new RuntimeException(StrUtil.format("failed to get latest l1 block, error: {}-{}",
                    latestL1Block.getError().getCode(), latestL1Block.getError().getMessage()));
        }
        if (latestL1Block.getBlock().getNumber().compareTo(processedL1BlockNumber) <= 0) {
            log.debug("already processed the latest height {} on L1", latestL1Block.getBlock().getNumber());
            return;
        }

        BigInteger maxPollingLimit = processedL1BlockNumber.add(BigInteger.valueOf(maxPollingBlockSize));
        maxPollingLimit = latestL1Block.getBlock().getNumber().compareTo(maxPollingLimit) <= 0 ?
                latestL1Block.getBlock().getNumber() :
                maxPollingLimit;
        log.info("process blocks from {} to {} included and latest {} on l1 chain",
                processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit, latestL1Block.getBlock().getNumber());

        BigInteger finalMaxPollingLimit = maxPollingLimit;
        l1Client.flowableL1MsgFromMailbox(processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit)
                .subscribe(
                        batch -> processBlock(batch, latestL1Block),
                        throwable -> {
                            log.error("something wrong inside l1 msg flow. ", throwable);
                        },
                        () -> log.info("fetch l1 msg from height {} to {} completed",
                                processedL1BlockNumber.add(BigInteger.ONE), finalMaxPollingLimit)
                );
        log.info("🎉 successful to process l1 block from {} to {}", processedL1BlockNumber.add(BigInteger.ONE), maxPollingLimit);
    }

    private void processBlock(L1MsgTransactionBatch batch, EthBlock latestL1Block) {
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        receiveL1Msgs(batch);
                        // create oracle request record, waiting for oracleGasFeedTask to deal
                        if (rollupConfig.getParentChainType().needRollupFeeFeed() && batch.getHeight().equals(latestL1Block.getBlock().getNumber())) {
                            saveL1BlockFeeInfo(latestL1Block);
                            log.info("🎉 successful to create l2 oracle request, oracleRequestType: {}, block number: {}",
                                    OracleRequestTypeEnum.L1_BLOCK_UPDATE, latestL1Block.getBlock().getNumber());
                        }
                    }
                }
        );
    }

    private void receiveL1Msgs(L1MsgTransactionBatch batch) {
        updateProcessedL1BlockNumber(batch.getHeight());
        if (batch.getL1MsgTransactionInfos().isEmpty()) {
            log.debug("get empty l1Msg batch for {}", batch.getHeight());
            return;
        }
        log.info("receiving {} l1Msgs from height {}", batch.getL1MsgTransactionInfos().size(), batch.getHeight());
        mailboxRepository.saveMessages(batch.toInterBlockchainMessages());
    }

    private void saveL1BlockFeeInfo(EthBlock ethBlock) {
        L1BlockFeeInfo blockFeeInfo = L1BlockFeeInfo.builder()
                .number(String.valueOf(ethBlock.getBlock().getNumber()))
                .baseFeePerGas(String.valueOf(ethBlock.getBlock().getBaseFeePerGas()))
                .gasUsed(String.valueOf(ethBlock.getBlock().getGasUsed()))
                .gasLimit(String.valueOf(ethBlock.getBlock().getGasLimit()))
                .blobGasUsed(String.valueOf(ethBlock.getBlock().getBlobGasUsed()))
                .excessBlobGas(String.valueOf(ethBlock.getBlock().getExcessBlobGas()))
                .build();

        oracleRepository.saveBlockFeeInfo(blockFeeInfo);
    }

    private BigInteger getProcessedL1BlockNumber() {
        return rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.BLOCK_PROCESSED);
    }

    private void updateProcessedL1BlockNumber(BigInteger blockNumber) {
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.BLOCK_PROCESSED, blockNumber);
    }
}
