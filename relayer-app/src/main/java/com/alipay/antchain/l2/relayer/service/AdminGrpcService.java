package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.DynamicGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

@Component
@Slf4j
public class AdminGrpcService extends AdminServiceGrpc.AdminServiceImplBase {

    @Resource
    private IRollupService rollupService;

    @Resource
    private IMailboxService mailboxService;

    @Resource
    private IOracleService oracleService;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private IMailboxRepository mailboxRepository;

    @Resource
    private L1Client l1Client;

    @Resource
    private L2Client l2Client;

    @Resource(name = "l1-gasprice-provider-conf")
    private IGasPriceProviderConfig l1GasPriceProviderConfig;

    @Resource(name = "l2-gasprice-provider-conf")
    private IGasPriceProviderConfig l2GasPriceProviderConfig;

    @Resource
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @Value("${l2-relayer.tasks.reliable-tx.retry-limit:0}")
    private int retryCountLimit;

    @Override
    public void initAnchorBatch(InitAnchorBatchReq request, StreamObserver<Response> responseObserver) {
        try {
            if (request.hasBatchHeaderInfo()) {
                rollupService.setAnchorBatch(
                        BatchHeader.builder()
                                .batchIndex(BigInteger.valueOf(request.getBatchHeaderInfo().getBatchIndex()))
                                .dataHash(HexUtil.decodeHex(request.getBatchHeaderInfo().getDataHash()))
                                .version(BatchVersionEnum.from(request.getBatchHeaderInfo().getVersion()))
                                .parentBatchHash(HexUtil.decodeHex(request.getBatchHeaderInfo().getParentBatchHash()))
                                .l1MsgRollingHash(Numeric.hexStringToByteArray(request.getBatchHeaderInfo().getL1MsgRollingHash()))
                                .build(),
                        request.getAnchorMerkleTree().getBranchesList().isEmpty() ? null :
                                AppendMerkleTree.builder()
                                        .nextMessageNonce(BigInteger.valueOf(request.getAnchorMerkleTree().getNextMsgNonce()))
                                        .branches(request.getAnchorMerkleTree().getBranchesList().stream().map(ByteString::toByteArray).map(Bytes32::new).toArray(Bytes32[]::new))
                                        .build()
                );
            } else {
                rollupService.setAnchorBatch(BigInteger.valueOf(request.getAnchorBatchIndex()));
            }

            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("initAnchorBatch error", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void setL1StartHeight(SetL1StartHeightReq request, StreamObserver<Response> responseObserver) {
        try {
            mailboxService.initService(new BigInteger(request.getStartHeight()));
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("mailboxService#initService error", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getBatch(GetBatchReq request, StreamObserver<Response> responseObserver) {
        try {
            BatchWrapper batchWrapper = rollupRepository.getBatch(new BigInteger(request.getBatchIndex()));
            BatchHeader batchHeader = batchWrapper.getBatch().getBatchHeader();

            Batch batch = Batch.newBuilder()
                    .setHeader(
                            BatchHeaderInfo.newBuilder()
                                    .setHash(batchHeader.getHashHex())
                                    .setBatchIndex(batchHeader.getBatchIndex().longValue())
                                    .setVersion(batchHeader.getVersion().getValue())
                                    .setParentBatchHash(HexUtil.encodeHexStr(batchHeader.getParentBatchHash()))
                                    .setDataHash(HexUtil.encodeHexStr(batchHeader.getDataHash()))
                                    .setL1MsgRollingHash(Numeric.toHexString(batchHeader.getL1MsgRollingHash()))
                                    .build()
                    ).addAllChunks(
                            ((ChunksPayload) batchWrapper.getBatch().getPayload()).chunks().stream().map(x -> {
                                List<BlockContext> blockContexts = x.getBlocks().stream().map(blockContext -> BlockContext.newBuilder()
                                        .setBlockNumber(blockContext.getBlockNumber().longValue())
                                        .setBaseFee(blockContext.getBaseFee().longValue())
                                        .setGasLimit(blockContext.getGasLimit().longValue())
                                        .setTimestamp(blockContext.getTimestamp())
                                        .setNumL1Messages(blockContext.getNumL1Messages())
                                        .setNumTransactions(blockContext.getNumTransactions())
                                        .build()
                                ).collect(Collectors.toList());
                                return Chunk.newBuilder()
                                        .setHash(HexUtil.encodeHexStr(x.getHash()))
                                        .setL2Transactions(ByteString.copyFrom(x.getL2Transactions()))
                                        .setNumBlocks(x.getNumBlocks())
                                        .addAllBlocks(blockContexts)
                                        .build();
                            }).collect(Collectors.toList())
                    ).build();
            responseObserver.onNext(Response.newBuilder()
                    .setGetBatchResp(
                            GetBatchResp.newBuilder().setBatch(batch).build()
                    ).build()
            );
        } catch (Throwable t) {
            log.error("failed to get batch: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getRawBatch(GetRawBatchReq request, StreamObserver<Response> responseObserver) {
        try {
            BatchWrapper batchWrapper = rollupRepository.getBatch(new BigInteger(request.getBatchIndex()));
            if (ObjectUtil.isNull(batchWrapper)) {
                responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg("batch not exist").build());
                responseObserver.onCompleted();
                return;
            }
            var daData = batchWrapper.getBatch().getDaData();
            var rawPayload = batchWrapper.getBatch().getPayload().serialize();

            responseObserver.onNext(
                    Response.newBuilder().setGetRawBatchResp(
                            GetRawBatchResp.newBuilder()
                                    .setBatchHeader(ByteString.copyFrom(batchWrapper.getBatch().getBatchHeader().serialize()))
                                    .addAllChunks(
                                            ((ChunksPayload) batchWrapper.getBatch().getPayload()).chunks().stream()
                                                    .map(x -> RawChunkInfo.newBuilder()
                                                            .setHash(HexUtil.encodeHexStr(Assert.notNull(x.getHash())))
                                                            .setRawChunk(ByteString.copyFrom(x.serialize()))
                                                            .build()
                                                    ).collect(Collectors.toList())
                                    ).setDaInfo(
                                            DaInfo.newBuilder()
                                                    .setDaVersion(daData.getDaVersion().toByte())
                                                    .setCompressed(daData.getDaVersion().isCompressed())
                                                    .setCompressionRatio((double) rawPayload.length / daData.getDataLen())
                                                    .setTxCount(batchWrapper.getBatch().getPayload().getL2TxCount())
                                                    .setBlobInfo(
                                                            BlobInfo.newBuilder()
                                                                    .setBlobSize(batchWrapper.getBatch().getEthBlobs().blobs().size())
                                                                    .setValidBlobBytesSize(daData.getDataLen())
                                                    ).build()
                                    ).build()
                    ).build()
            );
        } catch (Throwable t) {
            log.error("failed to get batch: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getL2MsgProof(GetL2MsgProofReq request, StreamObserver<Response> responseObserver) {
        try {
            L2MsgProofData proof = mailboxRepository.getL2MsgProof(new BigInteger(request.getMessageNonce()));
            responseObserver.onNext(
                    Response.newBuilder().setGetL2MsgProofResp(
                            GetL2MsgProofResp.newBuilder()
                                    .setMessageNonce(proof.getMsgNonce().toString())
                                    .setBatchIndex(proof.getBatchIndex().toString())
                                    .setProof(ByteString.copyFrom(proof.getMerkleProof()))
                                    .build()
                    ).build()
            );
        } catch (Throwable t) {
            log.error("failed to get batch: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryBatchTx(RetryBatchTxReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("retry batch txs from {} to {}", request.getFromBatchIndex(), request.getToBatchIndex());
            if (retryCountLimit <= 0) {
                throw new RuntimeException("setting `l2-relayer.tasks.reliable-tx.retry-limit` has to be none-zero");
            }
            for (long i = request.getFromBatchIndex(); i <= request.getToBatchIndex(); i++) {
                var tx = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, BigInteger.valueOf(i), TransactionTypeEnum.valueOf(request.getType()));
                if (tx.getState() != ReliableTransactionStateEnum.TX_FAILED) {
                    log.warn("tx for batch#{} with type {} is not failed, only support that retry the failed tx", i, request.getType());
                    continue;
                }
                tx.setRetryCount(0);
                rollupRepository.updateReliableTransaction(tx);
                log.info("set tx for batch#{} with type {} to pending and reset retry count to zero", i, request.getType());
            }
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to retry batch tx: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryBatchTxInfo(QueryBatchTxInfoReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query batch {} tx info now", request.getBatchIndex());
            var tx = rollupRepository.getReliableTransaction(
                    ChainTypeEnum.LAYER_ONE,
                    BigInteger.valueOf(request.getBatchIndex()),
                    TransactionTypeEnum.valueOf(request.getType())
            );
            if (ObjectUtil.isNull(tx)) {
                throw new RuntimeException("tx for batch#" + request.getBatchIndex() + " with type " + request.getType() + " not found");
            }
            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setQueryBatchTxInfoResp(
                            QueryBatchTxInfoResp.newBuilder()
                                    .setBatchIndex(request.getBatchIndex())
                                    .setTxInfo(
                                            TxInfo.newBuilder()
                                                    .setType(request.getType())
                                                    .setState(tx.getState().name())
                                                    .setOriginalTx(tx.getOriginalTxHash())
                                                    .setLatestTx(tx.getLatestTxHash())
                                                    .setLatestSendDate(DateUtil.format(tx.getLatestTxSendTime(), "yyyy-MM-dd HH:mm:ss.SSS"))
                                                    .setRetryCount(tx.getRetryCount())
                                                    .setRevertReason(ObjectUtil.defaultIfNull(tx.getRevertReason(), ""))
                                                    .build()
                                    )
                    ).build()
            );
        } catch (Throwable t) {
            log.error("failed to retry batch tx: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryBatchDaInfo(QueryBatchDaInfoReq request, StreamObserver<Response> responseObserver) {
        try {
            var batchWrapper = rollupRepository.getBatch(BigInteger.valueOf(request.getBatchIndex()));
            if (ObjectUtil.isNull(batchWrapper)) {
                responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg("batch not exist").build());
                responseObserver.onCompleted();
                return;
            }
            var daData = batchWrapper.getBatch().getDaData();
            if (ObjectUtil.isNull(daData.getDaVersion())) {
                // when eth blobs still in redis cache of this batch,
                // we need to trigger that da data deserialization from blobs by calling `toBatchPayload`.
                daData.toBatchPayload();
            }
            var rawPayload = batchWrapper.getBatch().getPayload().serialize();

            responseObserver.onNext(Response.newBuilder().setQueryBatchDaInfoResp(
                    QueryBatchDaInfoResp.newBuilder()
                            .setBatchIndex(batchWrapper.getBatchIndex().longValue())
                            .setDaInfo(DaInfo.newBuilder()
                                    .setDaVersion(daData.getDaVersion().toByte())
                                    .setCompressed(daData.getDaVersion().isCompressed())
                                    .setCompressionRatio((double) rawPayload.length / daData.getDataLen())
                                    .setTxCount(batchWrapper.getBatch().getPayload().getL2TxCount())
                                    .setBlobInfo(
                                            BlobInfo.newBuilder()
                                                    .setBlobSize(batchWrapper.getBatch().getEthBlobs().blobs().size())
                                                    .setValidBlobBytesSize(daData.getDataLen())
                                    ).build()
                            ).build()
            ).build());
        } catch (Throwable t) {
            log.error("failed to get batch: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void withdrawFromVault(WithdrawFromVaultReq request, StreamObserver<Response> responseObserver) {
        try {
            TransactionInfo transactionInfo = oracleService.withdrawVault(
                    request.getTo(),
                    BigInteger.valueOf(request.getAmount())
            );

            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setWithdrawFromVaultResp(
                            WithdrawFromVaultResp.newBuilder()
                                    .setTxHash(transactionInfo.getTxHash())
                    ).build());
        } catch (Throwable t) {
            log.error("withdrawFromVault error", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateFixedProfit(UpdateFixedProfitReq request, StreamObserver<Response> responseObserver) {
        try {
            TransactionInfo transactionInfo = oracleService.updateFixedProfit(new BigInteger(request.getProfit()));

            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setUpdateFixedProfitResp(
                            UpdateFixedProfitResp.newBuilder()
                                    .setTxHash(transactionInfo.getTxHash())
                    )
                    .build());
        } catch (Throwable t) {
            log.error("updateFixedProfit error", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateTotalScala(UpdateTotalScalaReq request, StreamObserver<Response> responseObserver) {
        try {
            TransactionInfo transactionInfo = oracleService.updateTotalScala(new BigInteger(request.getTotalScala()));

            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setUpdateTotalScalaResp(
                            UpdateTotalScalaResp.newBuilder()
                                    .setTxHash(transactionInfo.getTxHash())
                    )
                    .build());
        } catch (Throwable t) {
            log.error("updateTotalScala error", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    @Transactional
    public void speedupTx(SpeedupTxReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("⏩ speedup tx for {}-{}-{}", request.getChainType(), request.getType(), request.getBatchIndex());
            if (!StrUtil.equalsIgnoreCase(request.getChainType(), ChainTypeEnum.LAYER_ONE.name())) {
                throw new RuntimeException("only support speedup tx for layer one for now");
            }
            var tx = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, BigInteger.valueOf(request.getBatchIndex()), TransactionTypeEnum.valueOf(request.getType()));
            if (ObjectUtil.isNull(tx)) {
                throw new RuntimeException("tx for batch#" + request.getBatchIndex() + " with type " + request.getType() + " not found");
            }
            if (tx.getState() != ReliableTransactionStateEnum.TX_PENDING) {
                throw new RuntimeException("tx for batch#" + request.getBatchIndex() + " with type " + request.getType() + " is not pending");
            }

            var transactionInfo = l1Client.speedUpRollupTx(
                    tx,
                    BigInteger.valueOf(request.getMaxFeePerGas()),
                    BigInteger.valueOf(request.getMaxPriorityFeePerGas()),
                    BigInteger.valueOf(request.getMaxFeePerBlobGas())
            );

            tx.setLatestTxHash(transactionInfo.getTxHash());
            tx.setLatestTxSendTime(transactionInfo.getSendTxTime());
            tx.setRawTx(transactionInfo.getRawTx());
            tx.setSenderAccount(transactionInfo.getSenderAccount());
            rollupRepository.updateReliableTransaction(tx);

            log.info("successful to speed up tx {} for batch {}-{} manually", tx.getLatestTxHash(), tx.getTransactionType(), tx.getBatchIndex());
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to speed up batch tx: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryRelayerAddress(Empty request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query relayer addresses now");
            responseObserver.onNext(Response.newBuilder().setQueryRelayerAddressResp(
                    QueryRelayerAddressResp.newBuilder()
                            .setL1BlobAddress(l1Client.getBlobPoolTxManager().getAddress())
                            .setL1LegacyAddress(l1Client.getLegacyPoolTxManager().getAddress())
                            .setL2Address(l2Client.getLegacyPoolTxManager().getAddress())
            ).build());
        } catch (Throwable t) {
            log.error("failed to get relayer addresses: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateGasPriceConfig(UpdateGasPriceConfigReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("update gas price config : (chain: {}, key: {}, value: {})",
                    request.getChainType(), request.getConfigKey(), request.getConfigValue());
            switch (request.getChainType().toLowerCase()) {
                case "ethereum":
                    adjustL1GasPriceConfig(request.getConfigKey(), request.getConfigValue());
                    break;
                case "jovay":
                    throw new RuntimeException("For jovay, relayer has no needs to adjust the price config for now.");
                default:
                    throw new RuntimeException("unknown chain type: " + request.getChainType());
            }
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to update gas price config: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getGasPriceConfig(GetGasPriceConfigReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("get gas price config : (chain: {}, key: {})", request.getChainType(), request.getConfigKey());
            switch (request.getChainType().toLowerCase()) {
                case "ethereum":
                    if (!(l1GasPriceProviderConfig instanceof DynamicGasPriceProviderConfig config)) {
                        throw new RuntimeException("l1 gasprice has no dynamic config");
                    }
                    responseObserver.onNext(Response.newBuilder().setGetGasPriceConfigResp(
                            GetGasPriceConfigResp.newBuilder()
                                    .setConfigValue(getL1GasPriceConfig(request.getConfigKey()))
                    ).build());
                    break;
                default:
                    throw new RuntimeException("unknown chain type: " + request.getChainType());
            }
        } catch (Throwable t) {
            log.error("failed to get gas price config: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateRollupEconomicStrategyConfig(UpdateRollupEconomicStrategyConfigReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("update rollup economic strategy config : (key: {}, value: {})", request.getConfigKey(), request.getConfigValue());
            adjustEconomicStrategyConfig(request.getConfigKey(), request.getConfigValue());
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to update rollup economic strategy config: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getRollupEconomicConfig(GetRollupEconomicConfigReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("get rollup economic strategy config : (key: {})", request.getConfigKey());
            responseObserver.onNext(Response.newBuilder().setGetRollupEconomicConfigResp(
                    GetRollupEconomicConfigResp.newBuilder()
                            .setConfigValue(getRollupEconomicConfig(request.getConfigKey()))
            ).build());
        } catch (Throwable t) {
            log.error("failed to get rollup economic strategy config: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    private void adjustL1GasPriceConfig(String key, String value) {
        if (!(l1GasPriceProviderConfig instanceof DynamicGasPriceProviderConfig config)) {
            throw new RuntimeException("l1 gasprice has no dynamic config");
        }
        switch (key) {
            case GasPriceProviderConfig.Fields.extraGasPrice:
                config.setExtraGasPrice(new BigInteger(value));
                break;
            case GasPriceProviderConfig.Fields.maxPriceLimit:
                config.setMaxPriceLimit(new BigInteger(value));
                break;
            case GasPriceProviderConfig.Fields.gasPriceIncreasedPercentage:
                config.setGasPriceIncreasedPercentage(Double.parseDouble(value));
                break;
            case GasPriceProviderConfig.Fields.feePerBlobGasDividingVal:
                config.setFeePerBlobGasDividingVal(new BigInteger(value));
                break;
            case GasPriceProviderConfig.Fields.largerFeePerBlobGasMultiplier:
                config.setLargerFeePerBlobGasMultiplier(Double.parseDouble(value));
                break;
            case GasPriceProviderConfig.Fields.smallerFeePerBlobGasMultiplier:
                config.setSmallerFeePerBlobGasMultiplier(Double.parseDouble(value));
                break;
            case GasPriceProviderConfig.Fields.baseFeeMultiplier:
                config.setBaseFeeMultiplier(Integer.parseInt(value));
                break;
            case GasPriceProviderConfig.Fields.priorityFeePerGasIncreasedPercentage:
                config.setPriorityFeePerGasIncreasedPercentage(Double.parseDouble(value));
                break;
            case GasPriceProviderConfig.Fields.eip4844PriorityFeePerGasIncreasedPercentage:
                config.setEip4844PriorityFeePerGasIncreasedPercentage(Double.parseDouble(value));
                break;
            case GasPriceProviderConfig.Fields.minimumEip1559PriorityPrice:
                config.setMinimumEip1559PriorityPrice(new BigInteger(value));
                break;
            case GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice:
                config.setMinimumEip4844PriorityPrice(new BigInteger(value));
                break;
            default:
                throw new RuntimeException("unknown gas price config key: " + key);
        }
    }

    private void adjustEconomicStrategyConfig(String key, String value) {
        switch (key) {
            case RollupEconomicStrategyConfig.Fields.highEip1559PriceLimit:
                rollupEconomicStrategyConfig.setHighEip1559PriceLimit(new BigInteger(value));
                break;
            case RollupEconomicStrategyConfig.Fields.midEip1559PriceLimit:
                rollupEconomicStrategyConfig.setMidEip1559PriceLimit(new BigInteger(value));
                break;
            case RollupEconomicStrategyConfig.Fields.maxPendingBatchCount:
                rollupEconomicStrategyConfig.setMaxPendingBatchCount(Integer.parseInt(value));
                break;
            case RollupEconomicStrategyConfig.Fields.maxPendingProofCount:
                rollupEconomicStrategyConfig.setMaxPendingProofCount(Integer.parseInt(value));
                break;
            case RollupEconomicStrategyConfig.Fields.maxBatchWaitingTime:
                rollupEconomicStrategyConfig.setMaxBatchWaitingTime(Long.parseLong(value));
                break;
            case RollupEconomicStrategyConfig.Fields.maxProofWaitingTime:
                rollupEconomicStrategyConfig.setMaxProofWaitingTime(Long.parseLong(value));
                break;
            default:
                throw new RuntimeException("unknown economic strategy config key: " + key);
        }
    }

    private String getL1GasPriceConfig(String key) {
        if (!(l1GasPriceProviderConfig instanceof DynamicGasPriceProviderConfig config)) {
            throw new RuntimeException("l1 gasprice has no dynamic config");
        }
        switch (key) {
            case GasPriceProviderConfig.Fields.extraGasPrice:
                return config.getExtraGasPrice().toString();
            case GasPriceProviderConfig.Fields.maxPriceLimit:
                return config.getMaxPriceLimit().toString();
            case GasPriceProviderConfig.Fields.gasPriceIncreasedPercentage:
                return String.valueOf(config.getGasPriceIncreasedPercentage());
            case GasPriceProviderConfig.Fields.feePerBlobGasDividingVal:
                return config.getFeePerBlobGasDividingVal().toString();
            case GasPriceProviderConfig.Fields.largerFeePerBlobGasMultiplier:
                return String.valueOf(config.getLargerFeePerBlobGasMultiplier());
            case GasPriceProviderConfig.Fields.smallerFeePerBlobGasMultiplier:
                return String.valueOf(config.getSmallerFeePerBlobGasMultiplier());
            case GasPriceProviderConfig.Fields.baseFeeMultiplier:
                return String.valueOf(config.getBaseFeeMultiplier());
            case GasPriceProviderConfig.Fields.priorityFeePerGasIncreasedPercentage:
                return String.valueOf(config.getPriorityFeePerGasIncreasedPercentage());
            case GasPriceProviderConfig.Fields.eip4844PriorityFeePerGasIncreasedPercentage:
                return String.valueOf(config.getEip4844PriorityFeePerGasIncreasedPercentage());
            case GasPriceProviderConfig.Fields.minimumEip1559PriorityPrice:
                return config.getMinimumEip1559PriorityPrice().toString();
            case GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice:
                return config.getMinimumEip4844PriorityPrice().toString();
            default:
                throw new RuntimeException("unknown gas price config key: " + key);
        }
    }

    private String getRollupEconomicConfig(String key) {
        switch (key) {
            case RollupEconomicStrategyConfig.Fields.highEip1559PriceLimit:
                return rollupEconomicStrategyConfig.getHighEip1559PriceLimit().toString();
            case RollupEconomicStrategyConfig.Fields.midEip1559PriceLimit:
                return rollupEconomicStrategyConfig.getMidEip1559PriceLimit().toString();
            case RollupEconomicStrategyConfig.Fields.maxPendingBatchCount:
                return String.valueOf(rollupEconomicStrategyConfig.getMaxPendingBatchCount());
            case RollupEconomicStrategyConfig.Fields.maxPendingProofCount:
                return String.valueOf(rollupEconomicStrategyConfig.getMaxPendingProofCount());
            case RollupEconomicStrategyConfig.Fields.maxBatchWaitingTime:
                return String.valueOf(rollupEconomicStrategyConfig.getMaxBatchWaitingTime());
            case RollupEconomicStrategyConfig.Fields.maxProofWaitingTime:
                return String.valueOf(rollupEconomicStrategyConfig.getMaxProofWaitingTime());
            default:
                throw new RuntimeException("unknown economic strategy config key: " + key);
        }
    }
}
