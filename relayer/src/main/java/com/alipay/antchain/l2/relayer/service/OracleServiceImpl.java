package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.BreakOracleRequestException;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

@Service
@Slf4j
public class OracleServiceImpl implements IOracleService {
    @Resource
    private IOracleRepository oracleRepository;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private L2Client l2Client;

    @Resource
    private TransactionTemplate transactionTemplate;

    private final OracleFeeInfo latestOracleFeeInfo = new OracleFeeInfo();

    @Value("${l2-relayer.tasks.oracle-gas-feed.oracle-req-number-per-round-limit: 10}")
    private int oracleReqNumberPerRoundLimit;

    @Value("${l2-relayer.tasks.oracle-gas-feed.oracle-base-fee-update-threshold: 0}")
    private int oracleBaseFeeUpdateThreshold; // floating percentage with 100 times magnification

    @PostConstruct
    public void initService() {
        // initialize batchRollupFee in latestOracleFeeInfo，query from l2's l1GasOracle contract
        latestOracleFeeInfo.setLastBatchDaFee(l2Client.queryL2GasOracleLastBatchDaFee());
        latestOracleFeeInfo.setLastBatchExecFee(l2Client.queryL2GasOracleLastBatchExecFee());
        latestOracleFeeInfo.setLastBatchByteLength(l2Client.queryL2GasOracleLastBatchByteLength());

        // initialize startBatchIndex in latestOracleFeeInfo
        var latestBatchProvedIndex = oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE, OracleTransactionStateEnum.COMMITED);
        if (ObjectUtil.isNotNull(latestBatchProvedIndex)) {
            latestOracleFeeInfo.setStartBatchIndex(latestBatchProvedIndex);
        }

        // initialize baseFee、blobBaseFee、feeScala in latestOracleFeeInfo，get from db's oracle_request table
        OracleRequestDO lastBaseFeeRequest = oracleRepository.peekLatestRequest(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                OracleTransactionStateEnum.COMMITED
        );
        if (ObjectUtil.isNotEmpty(lastBaseFeeRequest)) {
            byte[] rawData = lastBaseFeeRequest.getRawData();
            L1BlockFeeInfo l1BlockFeeInfo = JSON.parseObject(rawData, L1BlockFeeInfo.class);
            // calculate next block's base fee and blob base fee
            latestOracleFeeInfo.setLastL1BaseFee(predictL1NextBaseFee(l1BlockFeeInfo));
            latestOracleFeeInfo.setLastL1BlobBaseFee(predictL1NextBlobBaseFee(l1BlockFeeInfo));
        }

        log.info("🎉 oracle service start success!! Init fee info like lastL1BaseFee: {}, lastL1BlobBaseFee: {}", latestOracleFeeInfo.getLastL1BaseFee(), latestOracleFeeInfo.getLastL1BlobBaseFee());
    }

    @Override
    @SneakyThrows
    public void processBlockOracle() {
        // pull a batch of INIT state request from db's oracle_request table, then divert them to different interfaces
        // pull l1_block_update request record
        List<OracleRequestDO> l1BlockUpdateOracleRequestDOS = oracleRepository.peekRequests(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT, oracleReqNumberPerRoundLimit);

        for (OracleRequestDO oracleRequest : l1BlockUpdateOracleRequestDOS) {
            transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            updateBlobBaseFeeScalaAndTxFeeScala(oracleRequest);
                        }
                    }
            );
        }
    }

    @Override
    public void processBatchOracle() {
        // pull l2_batch_prove request record
        List<OracleRequestDO> l2BatchProveOracleRequestDOS = oracleRepository.peekRequests(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE, OracleTransactionStateEnum.INIT, oracleReqNumberPerRoundLimit);

        for (OracleRequestDO oracleRequest : l2BatchProveOracleRequestDOS) {
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                updateBatchBlobFeeAndTxFee(oracleRequest);
                            }
                        }
                );
            } catch (BreakOracleRequestException e) {
                log.warn("⚠️ batch oracle request not ready for feedback: {}", e.getMessage());
            }
        }
    }

    @Override
    public void updateBatchBlobFeeAndTxFee(OracleRequestDO oracleRequestDO) {
        BigInteger index = oracleRequestDO.getRequestIndex();
        log.info("process batch blob fee and tx fee update, with oracle batchIndex: {}, oracleType: {}, oracleRequestType: {}.", index, oracleRequestDO.getOracleType(), oracleRequestDO.getOracleTaskType());

        // no compact judge, only index increment limit
        if (!latestOracleFeeInfo.getStartBatchIndex().equals(BigInteger.ONE)) {
            var latestBatchProvedIndex = oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_PROVE, OracleTransactionStateEnum.COMMITED);
            if (ObjectUtil.isNotEmpty(latestBatchProvedIndex) && index.compareTo(latestBatchProvedIndex) <= 0) {
                // set same id request of type: `L2_BATCH_COMMIT` to SKIP state
                try {
                    var batchCommitDO = getBatchCommitDO(index.toString());
                    oracleRepository.updateRequestState(index.toString(), batchCommitDO.getOracleType(), batchCommitDO.getOracleTaskType(), OracleTransactionStateEnum.SKIP);
                    log.info("🔔 higher batch: {}'s oracle gas feed request had commited, skip batch: {}'s commit oracle request.",
                            latestBatchProvedIndex, index);
                } catch (BreakOracleRequestException e) {
                    log.warn("🚨 batch: {} commit oracle request not found... error reason: {}", index, e.getMessage());
                }
                // set request of type state skip
                oracleRepository.updateRequestState(index.toString(), oracleRequestDO.getOracleType(), oracleRequestDO.getOracleTaskType(), OracleTransactionStateEnum.SKIP);
                log.info("🔔 higher batch: {}'s oracle gas feed request had commited, skip batch: {}'s prove oracle request.",
                        latestBatchProvedIndex, oracleRequestDO.getRequestIndex());
                return;
            }
        }

        // check batchCommit request same batchIndex exist
        OracleRequestDO batchCommitRequestDO = getBatchCommitDO(index.toString());

        // calculate next batch's daFee, executeFee and lastBatchByteLength
        OracleFeeInfo newOracleFeeInfo = calcNextBatchDaFeeAndExecFee(batchCommitRequestDO, oracleRequestDO);

        // send tx to feed L2's gas oracle contract, and store txInfo to table:{oracle_request}
        TransactionInfo txInfo;
        try {
            txInfo = l2Client.updateBatchRollupFee(
                    newOracleFeeInfo.getLastBatchDaFee(),
                    newOracleFeeInfo.getLastBatchExecFee(),
                    newOracleFeeInfo.getLastBatchByteLength()
            );
            oracleRepository.updateRequestState(
                    String.valueOf(index),
                    OracleTypeEnum.L2_GAS_ORACLE,
                    OracleRequestTypeEnum.L2_BATCH_COMMIT,
                    OracleTransactionStateEnum.COMMITED
            );
            oracleRepository.updateRequestState(
                    String.valueOf(index),
                    OracleTypeEnum.L2_GAS_ORACLE,
                    OracleRequestTypeEnum.L2_BATCH_PROVE,
                    OracleTransactionStateEnum.COMMITED
            );
            // insert tx to reliable_transaction table
            rollupRepository.insertReliableTransaction(
                    ReliableTransactionDO.builder()
                            .rawTx(txInfo.getRawTx())
                            .latestTxHash(txInfo.getTxHash())
                            .originalTxHash(txInfo.getTxHash())
                            .nonce(txInfo.getNonce().longValue())
                            .state(ReliableTransactionStateEnum.TX_PENDING)
                            .chainType(ChainTypeEnum.LAYER_TWO)
                            .senderAccount(txInfo.getSenderAccount())
                            .latestTxSendTime(txInfo.getSendTxTime())
                            .batchIndex(oracleRequestDO.getRequestIndex())
                            .transactionType(TransactionTypeEnum.L2_ORACLE_BATCH_FEE_FEED_TX)
                            .build()
            );
            // update oracle service memory
            if (latestOracleFeeInfo.getStartBatchIndex().equals(BigInteger.ONE)) {
                latestOracleFeeInfo.setStartBatchIndex(index);
            }
            latestOracleFeeInfo.setLastBatchDaFee(newOracleFeeInfo.getLastBatchDaFee());
            latestOracleFeeInfo.setLastBatchExecFee(newOracleFeeInfo.getLastBatchExecFee());
            latestOracleFeeInfo.setLastBatchByteLength(newOracleFeeInfo.getLastBatchByteLength());

            log.info("🎉 update batch rollup fee success! txHash: {}, batchIndex: {}, lastBatchDaFee: {}, lastBatchExecFee: {}, lastBatchByteLength: {}",
                    txInfo.getTxHash(), oracleRequestDO.getRequestIndex(), newOracleFeeInfo.getLastBatchDaFee(), newOracleFeeInfo.getLastBatchExecFee(), newOracleFeeInfo.getLastBatchByteLength());
        } catch (Exception e) {
            throw new RuntimeException("call l2client to update next batch's blobFee and TxFee failed", e);
        }
    }

    @Override
    public void updateBlobBaseFeeScalaAndTxFeeScala(OracleRequestDO oracleRequestDO) {
        byte[] rawData = oracleRequestDO.getRawData();
        BigInteger blockNumber = oracleRequestDO.getRequestIndex();

        // if exist higher block's oracle request had processed, this current oracle request skip over.
        var latestBaseFeeUpdateIndex = oracleRepository.peekLatestRequestIndex(OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.COMMITED);

        if (ObjectUtil.isNotEmpty(latestBaseFeeUpdateIndex) && blockNumber.compareTo(latestBaseFeeUpdateIndex) <= 0) {
            // set current oracle request state to `SKIP`
            oracleRepository.updateRequestState(blockNumber.toString(), oracleRequestDO.getOracleType(), oracleRequestDO.getOracleTaskType(), OracleTransactionStateEnum.SKIP);
            log.info("🔔 exist higher block number:{} oracle gas feed request had commited, skip lower batchIndex: {} directly.",
                    latestBaseFeeUpdateIndex, blockNumber);
            return;
        }

        TransactionInfo txInfo;
        try {
            L1BlockFeeInfo l1BlockFeeInfo = JSON.parseObject(rawData, L1BlockFeeInfo.class);

            // calculate next block's base fee and blob base fee
            BigInteger nextL1BlockBaseFee = predictL1NextBaseFee(l1BlockFeeInfo);
            BigInteger nextL1BlockBlobBaseFee = predictL1NextBlobBaseFee(l1BlockFeeInfo);

            // if feed gas to oracle contract based on the scalar threshold
            BigInteger baseFeeScala = calcBaseFeeScala(BigInteger.ONE.max(latestOracleFeeInfo.getLastL1BaseFee()), nextL1BlockBaseFee, oracleBaseFeeUpdateThreshold);
            BigInteger blobBaseFeeScala = calcBaseFeeScala(BigInteger.ONE.max(latestOracleFeeInfo.getLastL1BlobBaseFee()), nextL1BlockBlobBaseFee, oracleBaseFeeUpdateThreshold);

            // calibrate base fee when next base fee less than last base fee's one percent
            // EIP-4844 require MIN_BLOB_BASE_FEE = 1Gwei
            nextL1BlockBaseFee = BigInteger.ONE.max(calibrateBaseFeeTooLow(latestOracleFeeInfo.getLastL1BaseFee(), nextL1BlockBaseFee, baseFeeScala));
            nextL1BlockBlobBaseFee = BigInteger.ONE.max(calibrateBaseFeeTooLow(latestOracleFeeInfo.getLastL1BlobBaseFee(), nextL1BlockBlobBaseFee, blobBaseFeeScala));

            // calibrate base fee when next base fee too high, which standard is from L1GasOracle contract, now base fee's upper limit is 1Gwei
            nextL1BlockBaseFee = calibrateBaseFeeTooHigh(nextL1BlockBaseFee, BigInteger.valueOf(1000000000));
            nextL1BlockBlobBaseFee = calibrateBaseFeeTooHigh(nextL1BlockBlobBaseFee, BigInteger.valueOf(1000000000));

            // both baseFeeScala, blobBaseFeeScala equal 100, means next base fee and next blob base fee are not exceed the threshold, so just skip.
            if (Objects.equals(baseFeeScala, BigInteger.valueOf(100)) && Objects.equals(blobBaseFeeScala, BigInteger.valueOf(100))) {
                oracleRepository.updateRequestState(
                        String.valueOf(blockNumber),
                        OracleTypeEnum.L2_GAS_ORACLE,
                        OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                        OracleTransactionStateEnum.SKIP
                );
                return;
            }

            txInfo = l2Client.updateBaseFeeScala(baseFeeScala, blobBaseFeeScala);
            oracleRepository.updateRequestState(
                    String.valueOf(blockNumber),
                    OracleTypeEnum.L2_GAS_ORACLE,
                    OracleRequestTypeEnum.L1_BLOCK_UPDATE,
                    OracleTransactionStateEnum.COMMITED
            );
            // insert tx to reliable_transaction table
            rollupRepository.insertReliableTransaction(
                    ReliableTransactionDO.builder()
                            .rawTx(txInfo.getRawTx())
                            .latestTxHash(txInfo.getTxHash())
                            .originalTxHash(txInfo.getTxHash())
                            .nonce(txInfo.getNonce().longValue())
                            .state(ReliableTransactionStateEnum.TX_PENDING)
                            .chainType(ChainTypeEnum.LAYER_TWO)
                            .senderAccount(txInfo.getSenderAccount())
                            .latestTxSendTime(txInfo.getSendTxTime())
                            .batchIndex(blockNumber)
                            .transactionType(TransactionTypeEnum.L2_ORACLE_BASE_FEE_FEED_TX)
                            .build()
            );
            // update current block's baseFee and blobBaseFee
            latestOracleFeeInfo.setLastL1BaseFee(nextL1BlockBaseFee);
            latestOracleFeeInfo.setLastL1BlobBaseFee(nextL1BlockBlobBaseFee);
            latestOracleFeeInfo.setBaseFeeScala(baseFeeScala);
            latestOracleFeeInfo.setBlobBaseFeeScala(blobBaseFeeScala);

            log.info("🎉 update blob base fee success! txHash: {}, block number: {}, localL1BlockBaseFee: {}, localL1BlobBaseFee: {}, send baseFeeScala: {}, blobBaseFeeScala: {}",
                    txInfo.getTxHash(), oracleRequestDO.getRequestIndex(), nextL1BlockBaseFee, nextL1BlockBlobBaseFee, baseFeeScala, blobBaseFeeScala);
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("l2 client update baseFeeScala failed. block number: {}", blockNumber), e);
        }
    }

    @Override
    @SneakyThrows
    public TransactionInfo updateFixedProfit(BigInteger profit) {
        return l2Client.updateFixedProfit(profit);
    }

    @Override
    @SneakyThrows
    public TransactionInfo updateTotalScala(BigInteger totalScala) {
        return l2Client.updateTotalScala(totalScala);
    }

    @Override
    @SneakyThrows
    public TransactionInfo withdrawVault(String address, BigInteger amount) { // 接口被adminGrpc调用
        return l2Client.withdrawVault(address, amount);
    }

    private OracleFeeInfo calcNextBatchDaFeeAndExecFee(OracleRequestDO batchCommit, OracleRequestDO batchProve) {
        if (!batchCommit.getRequestIndex().equals(batchProve.getRequestIndex())) {
            throw new RuntimeException(StrUtil.format("🚨 oracle service calcNextBatchDaFeeAndExecFee failed, batchCommitIndex: {}, batchProveIndex: {}.",
                    batchCommit.getRequestIndex(), batchProve.getRequestIndex()));
        }

        BigInteger index = batchProve.getRequestIndex();
        // deal batchCommitReceipt
        TransactionReceipt batchCommitReceipt = JSON.parseObject(batchCommit.getRawData(), TransactionReceipt.class);
        BigInteger bcEffectiveGasPrice = Numeric.toBigInt(batchCommitReceipt.getEffectiveGasPrice());
        BigInteger bcGasUsed = batchCommitReceipt.getGasUsed();
        BigInteger bcBlobGasPrice = Numeric.toBigInt(batchCommitReceipt.getBlobGasPrice());
        BigInteger bcBlobGasUsed = Numeric.toBigInt(batchCommitReceipt.getBlobGasUsed());

        // deal batchProveReceipt
        TransactionReceipt batchProveReceipt = JSON.parseObject(batchProve.getRawData(), TransactionReceipt.class);
        BigInteger bpEffectiveGasPrice = Numeric.toBigInt(batchProveReceipt.getEffectiveGasPrice());
        BigInteger bpGasUsed = batchProveReceipt.getGasUsed();

        // calc `_lastBatchDaFee` and `_lastBatchExecFee` on L1GasOracle contract
        BigInteger lastBatchDaFee = bcBlobGasPrice.multiply(bcBlobGasUsed);
        BigInteger lastBatchExecFee = bcEffectiveGasPrice.multiply(bcGasUsed).add(bpEffectiveGasPrice.multiply(bpGasUsed));

        // calc `_lastBatchByteLength`
        BatchWrapper batchWrapper = rollupRepository.getBatch(index);
        long lastBatchTxsLength = batchWrapper.getBatch().getBatchTxsLength();

        return OracleFeeInfo.builder()
                .lastBatchDaFee(lastBatchDaFee)
                .lastBatchExecFee(lastBatchExecFee)
                .lastBatchByteLength(BigInteger.valueOf(lastBatchTxsLength))
                .build();
    }

    private BigInteger predictL1NextBlobBaseFee(L1BlockFeeInfo l1BlockFeeInfo) {
        BigInteger excessBlobGas = new BigInteger(l1BlockFeeInfo.getExcessBlobGas()); // 遵循EIP-4844计算规则
        return Utils.fakeExponential(excessBlobGas);
    }

    private BigInteger predictL1NextBaseFee(L1BlockFeeInfo l1BlockFeeInfo) {
        // follow EIP-1559 protocol's calculate rule
        BigInteger baseFeePerGas = new BigInteger(l1BlockFeeInfo.getBaseFeePerGas());
        BigInteger gasUsed = new BigInteger(l1BlockFeeInfo.getGasUsed());
        BigInteger gasLimit = new BigInteger(l1BlockFeeInfo.getGasLimit());
        BigInteger gasTarget = gasLimit.divide(BigInteger.valueOf(2));

        // calculate next block's base fee
        BigInteger gasUsedDelta = gasUsed.subtract(gasTarget);
        if (gasUsed.compareTo(gasTarget) == 0) {
            return baseFeePerGas;
        } else if (gasUsed.compareTo(gasTarget) > 0) {
            // improve gasPrice, calc rule: parentBaseFee + max(1, parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            BigInteger incremental = (baseFeePerGas.multiply(gasUsedDelta).divide(gasTarget).divide(latestOracleFeeInfo.getBaseFeeChangeDenominator())).max(BigInteger.valueOf(1));
            return baseFeePerGas.add(incremental);
        } else {
            // decrease gasPrice, calc rule: max(0, parentBaseFee - parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            BigInteger decremental = baseFeePerGas.multiply(gasUsedDelta).divide(gasTarget).divide(latestOracleFeeInfo.getBaseFeeChangeDenominator());
            return BigInteger.ZERO.max(baseFeePerGas.subtract(decremental));
        }
    }

    private BigInteger calcBaseFeeScala(BigInteger currentBaseFee, BigInteger nextBaseFee, int threshold) {
        // When the actual calculation is returned, Scala multiplies by 100 to provide decimal places of precision.
        BigInteger feeDeltaEnlarge = BigInteger.valueOf(100).multiply(nextBaseFee.subtract(currentBaseFee).abs());
        BigInteger feeThreshold = currentBaseFee.multiply(BigInteger.valueOf(threshold));

        if (feeDeltaEnlarge.compareTo(feeThreshold) > 0) {
            if(currentBaseFee.equals(BigInteger.ZERO)) {
                throw new RuntimeException("calc base fee scala but last base/blob base fee is ZERO.");
            }
            BigInteger scala = (nextBaseFee.multiply(BigInteger.valueOf(100))).divide(currentBaseFee);
            // precision only attach .00, and scala cannot be ZERO, so convert to ONE forcefully.
            return scala.equals(BigInteger.ZERO) ? BigInteger.ONE : scala;
        } else {
            return BigInteger.valueOf(100);
        }
    }

    private BigInteger calibrateBaseFeeTooLow(BigInteger lastBaseFee, BigInteger nextBaseFee, BigInteger baseFeeScala) {
        // scala maybe convert to one forcefully when calc scala result was ZERO, so calibrate nextBaseFee to lastBaseFee's ONE percent
        if (baseFeeScala.equals(BigInteger.ONE)) {
            return lastBaseFee.multiply(baseFeeScala).divide(BigInteger.valueOf(100));
        }
        return nextBaseFee;
    }

    private BigInteger calibrateBaseFeeTooHigh(BigInteger nextBaseFee, BigInteger upperLimit) {
        if (nextBaseFee.compareTo(upperLimit) > 0) {
            return upperLimit;
        }
        return nextBaseFee;
    }

    private OracleRequestDO getBatchCommitDO(String index) {
        var batchCommitRequestDO = oracleRepository.peekRequestByTypeAndIndex(
                OracleTypeEnum.L2_GAS_ORACLE,
                OracleRequestTypeEnum.L2_BATCH_COMMIT,
                index
        );
        if (ObjectUtil.isEmpty(batchCommitRequestDO)) {
            throw new BreakOracleRequestException(
                    "oracle req not ready so skip for now, oracleType: {}, oracleRequestType: {}, index: {}",
                    OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L2_BATCH_COMMIT, index
            );
        }
        if (!batchCommitRequestDO.getTxState().equals(OracleTransactionStateEnum.INIT)) {
            throw new BreakOracleRequestException(
                    "🚨 update batch blob fee and tx fee failed, batchIndex: {}, same batchIndex's batchCommit oracle request has been deal.",
                    index
            );
        }
        return batchCommitRequestDO;
    }
}
