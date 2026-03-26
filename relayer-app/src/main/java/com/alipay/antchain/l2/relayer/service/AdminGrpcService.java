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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.cache.Cache;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.L1ContractWarnException;
import com.alipay.antchain.l2.relayer.commons.exceptions.ProofNotReadyException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.DaProof;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.CachedNonceManager;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.DynamicGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.*;
import com.alipay.antchain.l2.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

@Component
@Slf4j
public class AdminGrpcService extends AdminServiceGrpc.AdminServiceImplBase {

    @Resource
    private IRollupService rollupService;

    @Resource
    private IMailboxService mailboxService;

    @Resource
    @Lazy
    private IOracleService oracleService;

    @Resource
    private IRollupRepository rollupRepository;

    @Resource
    private IMailboxRepository mailboxRepository;

    @Resource
    private IL2MerkleTreeRepository l2MerkleTreeRepository;

    @Resource
    private IOracleRepository oracleRepository;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private L1Client l1Client;

    @Resource
    private L2Client l2Client;

    @Resource(name = "l1-gasprice-provider-conf")
    private IGasPriceProviderConfig l1GasPriceProviderConfig;

    @Resource
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private Cache<BigInteger, BasicBlockTrace> l2BlockTracesCacheForCurrChunk;

    @Resource
    private ScheduleContext scheduleContext;
    
    @Resource
    private ScheduleRepository scheduleRepository;

    @Value("#{rollupConfig.daType}")
    private DaType daType;

    @Value("#{rollupConfig.parentChainType}")
    private ParentChainType parentChainType;

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
                                    .setVersion(batchHeader.getVersion().getValueAsUint8())
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
                                        .setL2Transactions(ByteString.copyFrom(x.getL2Transactions()))
                                        .setNumBlocks(Math.toIntExact(x.getNumBlocks()))
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
                                                            .setRawChunk(ByteString.copyFrom(batchWrapper.getBatchHeader()
                                                                    .getVersion().getChunkCodec().serialize(x)))
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
    public void retryBatchTx(RetryBatchTxReq request, StreamObserver<Response> responseObserver) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
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
                }
            });
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
            if (!parentChainType.needRollupFeeFeed()) {
                throw new RuntimeException(StrUtil.format("parent chain is {}, no need to withdraw vault", parentChainType));
            }

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
            if (!parentChainType.needRollupFeeFeed()) {
                throw new RuntimeException(StrUtil.format("parent chain is {}, no need to update fixed profit", parentChainType));
            }

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
            if (!parentChainType.needRollupFeeFeed()) {
                throw new RuntimeException(StrUtil.format("parent chain is {}, no need to update total scala", parentChainType));
            }

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
                            .setL1BlobAddress(ObjectUtil.isNotNull(l1Client.getBlobPoolTxManager()) ? l1Client.getBlobPoolTxManager().getAddress() : "none")
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

    @Override
    public void wasteEthAccountNonce(WasteEthAccountNonceReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("waste eth account nonce : (chain: {}, account: {}, nonce: {})",
                    request.getChainType(), request.getAddress(), request.getNonce());
            EthSendTransaction result;
            if (request.getChainType() == ChainType.L1) {
                result = l1Client.sendTransferValueTx(request.getAddress(), request.getAddress(), BigInteger.valueOf(request.getNonce()), BigInteger.ZERO);
            } else {
                result = l2Client.sendTransferValueTx(request.getAddress(), request.getAddress(), BigInteger.valueOf(request.getNonce()), BigInteger.ZERO);
            }
            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setWasteEthAccountNonceResp(WasteEthAccountNonceResp.newBuilder().setTxHash(result.getTransactionHash())).build());
        } catch (Throwable t) {
            log.error("failed to waste eth account nonce: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void commitBatchManually(CommitBatchManuallyReq request, StreamObserver<Response> responseObserver) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    log.info("try to commit batch manually : (batchIndex: {})", request.getBatchIndex());
                    var lastCommittedBatchIdx = l1Client.lastCommittedBatch();
                    var batchIndex = BigInteger.valueOf(request.getBatchIndex());
                    if (lastCommittedBatchIdx.compareTo(batchIndex) >= 0) {
                        throw new RuntimeException(StrUtil.format(
                                "no need to commit this batch {} because of last committed batch is {}",
                                request.getBatchIndex(), lastCommittedBatchIdx
                        ));
                    }
                    if (!lastCommittedBatchIdx.equals(batchIndex.subtract(BigInteger.ONE))) {
                        throw new RuntimeException(StrUtil.format(
                                "you can't skip to commit, next batch is {}", lastCommittedBatchIdx.add(BigInteger.ONE)
                        ));
                    }
                    var txCommitted = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, batchIndex, TransactionTypeEnum.BATCH_COMMIT_TX);
                    if (ObjectUtil.isNotNull(txCommitted) && ReliableTransactionStateEnum.considerAsSuccess(txCommitted.getState())) {
                        throw new RuntimeException(StrUtil.format(
                                "no need to commit this batch {} because of batch has been confirmed success, tx: {}",
                                request.getBatchIndex(), txCommitted.getLatestTxHash()
                        ));
                    }
                    var batch = rollupRepository.getBatch(batchIndex);
                    if (ObjectUtil.isNull(batch)) {
                        throw new RuntimeException(StrUtil.format("batch {} not exists", request.getBatchIndex()));
                    }

                    TransactionInfo transactionInfo;
                    try {
                        transactionInfo = switch (daType) {
                            case BLOBS -> l1Client.commitBatchWithEthCall(batch);
                            case DAS -> l1Client.commitBatchWithEthCall(batch, new DaProof(request.getRawDaProof().toByteArray()));
                        };
                    } catch (L1ContractWarnException e) {
                        throw new RuntimeException(StrUtil.format("rollup contract shows that batch {} has been committed", batchIndex));
                    }
                    log.info("successful to commit batch {} manually with txhash {}", batchIndex, transactionInfo.getTxHash());
                    var reliableTx = ReliableTransactionDO.builder()
                            .rawTx(transactionInfo.getRawTx())
                            .latestTxHash(transactionInfo.getTxHash())
                            .originalTxHash(transactionInfo.getTxHash())
                            .nonce(transactionInfo.getNonce().longValue())
                            .state(ReliableTransactionStateEnum.TX_PENDING)
                            .chainType(ChainTypeEnum.LAYER_ONE)
                            .senderAccount(transactionInfo.getSenderAccount())
                            .latestTxSendTime(transactionInfo.getSendTxTime())
                            .batchIndex(batch.getBatch().getBatchIndex())
                            .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                            .build();
                    var batchCommittedLocally = rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED);
                    if (batchCommittedLocally.compareTo(batchIndex) < 0) {
                        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED, batchIndex);
                    }
                    if (ObjectUtil.isNotNull(txCommitted)) {
                        rollupRepository.updateReliableTransaction(reliableTx);
                    } else {
                        rollupRepository.insertReliableTransaction(reliableTx);
                    }
                    responseObserver.onNext(Response.newBuilder().setCode(0)
                            .setCommitBatchManuallyResp(CommitBatchManuallyResp.newBuilder().setTxHash(transactionInfo.getTxHash())).build());
                }
            });
        } catch (Throwable t) {
            log.error("failed to commit batch manually: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void commitProofManually(CommitProofManuallyReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("try to commit proof manually : (batchIndex: {}, type: {})", request.getBatchIndex(), request.getProofType());
            var proofType = request.getProofType() == ProofType.TEE ? ProveTypeEnum.TEE_PROOF : ProveTypeEnum.ZK_PROOF;
            var batchIndex = BigInteger.valueOf(request.getBatchIndex());

            var lastCommittedProofIdx = request.getProofType() == ProofType.TEE ? l1Client.lastTeeVerifiedBatch() : l1Client.lastZkVerifiedBatch();
            if (lastCommittedProofIdx.compareTo(batchIndex) >= 0) {
                throw new RuntimeException(StrUtil.format(
                        "no need to commit this proof {} because of last committed proof is {}",
                        request.getBatchIndex(), lastCommittedProofIdx
                ));
            }
            if (!lastCommittedProofIdx.equals(batchIndex.subtract(BigInteger.ONE))) {
                throw new RuntimeException(StrUtil.format(
                        "you can't skip to commit, next proof is {}", lastCommittedProofIdx.add(BigInteger.ONE)
                ));
            }

            var txType = request.getProofType() == ProofType.TEE ? TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX : TransactionTypeEnum.BATCH_ZK_PROOF_COMMIT_TX;
            var txCommitted = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, batchIndex, txType);
            if (ObjectUtil.isNotNull(txCommitted) && ReliableTransactionStateEnum.considerAsSuccess(txCommitted.getState())) {
                throw new RuntimeException(StrUtil.format(
                        "no need to commit this proof {} because of tx for this proof has been confirmed success, tx: {}",
                        request.getBatchIndex(), txCommitted.getLatestTxHash()
                ));
            }

            var proofRequest = rollupRepository.getBatchProveRequest(batchIndex, proofType);
            if (ObjectUtil.isNull(proofRequest) || ObjectUtil.isEmpty(proofRequest.getProof()) || proofRequest.getState() == BatchProveRequestStateEnum.PENDING) {
                log.debug("tee proof for next batch {} not ready, please wait...", request.getBatchIndex());
                throw new ProofNotReadyException(ProveTypeEnum.TEE_PROOF, batchIndex);
            }
            var batch = rollupRepository.getBatch(batchIndex);
            if (ObjectUtil.isNull(batch)) {
                throw new RuntimeException(StrUtil.format("batch {} not exists", request.getBatchIndex()));
            }

            TransactionInfo transactionInfo;
            try {
                transactionInfo = l1Client.verifyBatchWithEthCall(batch, proofRequest);
            } catch (L1ContractWarnException e) {
                throw new RuntimeException(StrUtil.format("rollup contract shows that {} batch proof {} has been committed", proofType, batchIndex));
            }
            log.info("successful to commit proof {} manually with txhash {}", batchIndex, transactionInfo.getTxHash());
            var reliableTx = ReliableTransactionDO.builder()
                    .rawTx(transactionInfo.getRawTx())
                    .latestTxHash(transactionInfo.getTxHash())
                    .originalTxHash(transactionInfo.getTxHash())
                    .nonce(transactionInfo.getNonce().longValue())
                    .state(ReliableTransactionStateEnum.TX_PENDING)
                    .chainType(ChainTypeEnum.LAYER_ONE)
                    .senderAccount(transactionInfo.getSenderAccount())
                    .latestTxSendTime(transactionInfo.getSendTxTime())
                    .batchIndex(batch.getBatch().getBatchIndex())
                    .transactionType(txType)
                    .build();

            if (ObjectUtil.isNotNull(txCommitted)) {
                rollupRepository.updateReliableTransaction(reliableTx);
            } else {
                rollupRepository.insertReliableTransaction(reliableTx);
            }
            responseObserver.onNext(Response.newBuilder().setCode(0)
                    .setCommitProofManuallyResp(CommitProofManuallyResp.newBuilder().setTxHash(transactionInfo.getTxHash())).build());
        } catch (Throwable t) {
            log.error("failed to commit proof manually: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateNonceManually(UpdateNonceManuallyReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("try to update nonce : (chainType: {}, accType: {})", request.getChainType(), request.getAccType());
            var nonceManager = switch (request.getChainType()) {
                case L1 -> switch (request.getAccType()) {
                    case BLOB -> l1Client.getBlobPoolTxManager().getNonceManager();
                    case LEGACY -> l1Client.getLegacyPoolTxManager().getNonceManager();
                    default -> throw new RuntimeException("unknown acc type");
                };
                case L2 -> switch (request.getAccType()) {
                    case BLOB -> l2Client.getBlobPoolTxManager().getNonceManager();
                    case LEGACY -> l2Client.getLegacyPoolTxManager().getNonceManager();
                    default -> throw new RuntimeException("unknown acc type");
                };
                default -> throw new RuntimeException("unknown chain type");
            };
            Assert.isInstanceOf(CachedNonceManager.class, nonceManager, "only supports cached nonce manager");
            ((CachedNonceManager) nonceManager).setNonceIntoCache(BigInteger.valueOf(request.getNonce()));
            responseObserver.onNext(Response.newBuilder().setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to update nonce: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryCurrNonce(QueryCurrNonceReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("try to query curr nonce : (chainType: {}, accType: {})", request.getChainType(), request.getAccType());
            responseObserver.onNext(Response.newBuilder().setQueryCurrNonceResp(QueryCurrNonceResp.newBuilder().setNonce(
                    (switch (request.getChainType()) {
                        case L1 -> switch (request.getAccType()) {
                            case BLOB -> l1Client.getBlobPoolTxManager().getNonceManager().getNextNonce();
                            case LEGACY -> l1Client.getLegacyPoolTxManager().getNonceManager().getNextNonce();
                            default -> throw new RuntimeException("unknown acc type");
                        };
                        case L2 -> switch (request.getAccType()) {
                            case BLOB -> l2Client.getBlobPoolTxManager().getNonceManager().getNextNonce();
                            case LEGACY -> l2Client.getLegacyPoolTxManager().getNonceManager().getNextNonce();
                            default -> throw new RuntimeException("unknown acc type");
                        };
                        default -> throw new RuntimeException("unknown chain type");
                    }).longValue()
            )).setCode(0).build());
        } catch (Throwable t) {
            log.error("failed to query curr nonce: ", t);
            responseObserver.onNext(Response.newBuilder().setCode(-1).setErrorMsg(t.getMessage()).build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void refetchProof(RefetchProofReq request, StreamObserver<Response> responseObserver) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    log.info("refetch {} proof for batch range [{}, {}]",
                            request.getProofType(), request.getFromBatchIndex(), request.getToBatchIndex());
                    var startBatch = new BigInteger(request.getFromBatchIndex());
                    var endBatch = new BigInteger(request.getToBatchIndex());
                    var proofType = ProveTypeEnum.valueOf(request.getProofType().toUpperCase());
                    for (var curr = startBatch; curr.compareTo(endBatch) <= 0; curr = curr.add(BigInteger.ONE)) {
                        var proveReq = rollupRepository.getBatchProveRequest(curr, proofType);
                        if (ObjectUtil.isNull(proveReq)) {
                            log.warn("{} batch prove request for batch {} not found", proofType, curr);
                            continue;
                        }
                        rollupRepository.updateBatchProveRequestState(curr, proofType, BatchProveRequestStateEnum.PENDING);
                        log.info("successful to update batch prove request state for batch {}", curr);
                    }
                    responseObserver.onNext(Response.newBuilder().setCode(0).build());
                }
            });
        } catch (Throwable t) {
            log.error("failed to refetch proof: ", t);
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

    @Override
    public void rollbackToSubchainHeight(RollbackToSubchainHeightReq request, StreamObserver<Response> responseObserver) {
        var targetBatchIndex = BigInteger.valueOf(request.getTargetBatchIndex());
        var targetBlockHeight = BigInteger.valueOf(request.getTargetBlockHeight());
        var l1MsgNonceThreshold = request.getL1MsgNonceThreshold();

        log.info("try to rollback to subchain height: (targetBatchIndex: {}, targetBlockHeight: {}, l1MsgNonceThreshold: {})",
                targetBatchIndex, targetBlockHeight, l1MsgNonceThreshold);

        var acquiredLocks = new ArrayList<RLock>();
        try {
            checkRelayerNodes();

            // Step 0: Find the chunk that contains the target block height
            var chunks = rollupRepository.getChunks(targetBatchIndex);
            if (ObjectUtil.isEmpty(chunks)) {
                throw new RuntimeException(StrUtil.format("Chunks for batch {} is empty", targetBatchIndex));
            }
            if (chunks.get(0).getStartBlockNumber().compareTo(targetBlockHeight) > 0) {
                throw new RuntimeException(StrUtil.format("Block {} not inside the chunks for batch {}", targetBlockHeight, targetBatchIndex));
            }
            ChunkWrapper targetChunk = null;
            var gasSum = BigInteger.ZERO;
            for (var chunk : chunks) {
                if (chunk.getEndBlockNumber().compareTo(targetBlockHeight) >= 0) {
                    targetChunk = chunk;
                    break;
                }
                gasSum = gasSum.add(BigInteger.valueOf(chunk.getGasSum()));
            }
            if (ObjectUtil.isNull(targetChunk)) {
                throw new RuntimeException(StrUtil.format("No chunk found for block {} from batch {}", targetBlockHeight, targetBatchIndex));
            }
            var targetChunkIndex = targetChunk.getChunkIndex();
            log.info("Found target chunk index: {} for block height: {} in batch: {}", targetChunkIndex, targetBlockHeight, targetBatchIndex);

            // Step 1: Acquire all distributed locks to prevent Duty from executing tasks
            for (var taskType : BizTaskTypeEnum.values()) {
                var lock = redissonClient.getLock(getTaskLockKey(taskType));
                if (!lock.tryLock(60, TimeUnit.SECONDS)) {
                    throw new RuntimeException(StrUtil.format(
                            "Failed to acquire lock for task {}, there might be running tasks. Please retry later.", taskType));
                }
                acquiredLocks.add(lock);
                log.info("Acquired lock for task: {}", taskType);
            }

            // Step 2: Execute rollback within a database transaction
            var finalGasSum = gasSum;
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    executeRollback(targetBatchIndex, targetChunkIndex, targetBlockHeight, l1MsgNonceThreshold, finalGasSum);
                }
            });
            clearCacheToRollback();

            var summary = StrUtil.format(
                    "Subchain height rollback completed successfully. Target: batchIndex={}, chunkIndex={}, blockHeight={}",
                    targetBatchIndex, targetChunkIndex, targetBlockHeight);
            log.info(summary);
            responseObserver.onNext(Response.newBuilder()
                    .setCode(0)
                    .setRollbackToSubchainHeightResp(RollbackToSubchainHeightResp.newBuilder()
                            .setSummary(summary)
                            .build())
                    .build());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rollback interrupted while acquiring locks", e);
            responseObserver.onNext(Response.newBuilder()
                    .setCode(-1)
                    .setErrorMsg("Rollback interrupted: " + e.getMessage())
                    .build());
        } catch (Throwable t) {
            log.error("Failed to rollback L2", t);
            responseObserver.onNext(Response.newBuilder()
                    .setCode(-1)
                    .setErrorMsg("Rollback failed: " + t.getMessage())
                    .build());
        } finally {
            // Release all acquired locks
            for (var lock : acquiredLocks) {
                try {
                    lock.forceUnlock();
                    log.info("Released lock for task: {}", lock.getName());
                } catch (Exception e) {
                    log.error("Failed to release lock {}", lock.getName(), e);
                }
            }
            responseObserver.onCompleted();
        }
    }

    /**
     * Checks the status of all Relayer nodes before executing rollback.
     * <p>
     * This method ensures that:
     * 1. The local Relayer node (the one this CLI is connected to) is ONLINE
     * 2. All other Relayer nodes are OFFLINE
     * <p>
     * This is a critical safety check to prevent data inconsistency during rollback.
     * If multiple nodes are running during rollback, they might write conflicting data
     * to the database or cache, causing the rollback to fail or produce incorrect results.
     * 
     * @throws RuntimeException if the local node is offline or any other node is still online
     */
    private void checkRelayerNodes() {
        var activeNodes = scheduleRepository.getAllActiveNodes();
        for (var activeNode : activeNodes) {
            // Check if this is the local node (the one we're connected to)
            if (activeNode.getNodeId().equals(scheduleContext.getNodeId())) {
                // The local node must be online to execute rollback
                if (activeNode.getStatus() == ActiveNodeStatusEnum.OFFLINE) {
                    throw new RuntimeException("Local relayer node is not online");
                }
                continue;
            }
            // All other nodes must be offline before rollback
            if (activeNode.getStatus() == ActiveNodeStatusEnum.ONLINE) {
                throw new RuntimeException("Please wait all other relayer nodes to be offline before rollback");
            }
        }
    }

    private void executeRollback(BigInteger targetBatchIndex, long targetChunkIndex, BigInteger targetBlockHeight, long l1MsgNonceThreshold, BigInteger gasSum) {
        log.info("Starting rollback execution...");

        // 1. Update rollup_number_record
        log.info("Updating rollup number records...");
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH, targetBatchIndex);
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH, targetBatchIndex);
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED, targetBatchIndex.subtract(BigInteger.ONE));
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED, targetBlockHeight.subtract(BigInteger.ONE));
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK_GAS_ACCUMULATOR, gasSum);
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_CHUNK, BigInteger.valueOf(targetChunkIndex));
        log.info("Rollup number records updated");

        // 2. Delete batches where batch_index >= targetBatchIndex
        var deletedBatches = rollupRepository.deleteBatchesFrom(targetBatchIndex);
        log.info("Deleted {} batches", deletedBatches);

        // 3. Delete chunks for rollback
        var deletedChunks = rollupRepository.deleteChunksForRollback(targetBatchIndex, targetChunkIndex);
        log.info("Deleted {} chunks", deletedChunks);

        // 4. Delete batch prove requests where batch_index >= targetBatchIndex
        var deletedProveRequests = rollupRepository.deleteBatchProveRequestsFrom(targetBatchIndex);
        log.info("Deleted {} batch prove requests", deletedProveRequests);

        // 5. Delete rollup reliable transactions (BATCH_COMMIT_TX, BATCH_TEE_PROOF_COMMIT_TX, BATCH_ZK_PROOF_COMMIT_TX)
        var deletedRollupTxs = rollupRepository.deleteRollupReliableTransactionsFrom(targetBatchIndex);
        log.info("Deleted {} rollup reliable transactions", deletedRollupTxs);

        // 6. Delete L1 message reliable transactions where nonce > threshold
        var deletedL1MsgTxs = rollupRepository.deleteL1MsgReliableTransactionsAboveNonce(l1MsgNonceThreshold);
        log.info("Deleted {} L1 message reliable transactions", deletedL1MsgTxs);

        // 7. Delete L2 Merkle trees where batch_index >= targetBatchIndex
        var deletedMerkleTrees = l2MerkleTreeRepository.deleteMerkleTreesFrom(targetBatchIndex);
        log.info("Deleted {} L2 Merkle trees", deletedMerkleTrees);

        // 8. Reset L1 messages with nonce > threshold to MSG_READY state
        var resetL1Msgs = mailboxRepository.resetL1MsgsAboveNonce(l1MsgNonceThreshold);
        log.info("Reset {} L1 messages to MSG_READY state", resetL1Msgs);

        // 9. Delete L2 messages for rollback
        var deletedL2Msgs = mailboxRepository.deleteL2MsgsForRollback(targetBatchIndex, targetBlockHeight);
        log.info("Deleted {} L2 messages", deletedL2Msgs);

        // 10. Reset L2 messages to MSG_READY state for rollback
        var resetL2Msgs = mailboxRepository.resetL2MsgsForRollback(targetBatchIndex, targetBlockHeight);
        log.info("Reset {} L2 messages to MSG_READY state", resetL2Msgs);

        // 11. Delete batch oracle requests (L2_BATCH_COMMIT, L2_BATCH_PROVE)
        var deletedOracleRequests = oracleRepository.deleteBatchOracleRequestsFrom(targetBatchIndex);
        log.info("Deleted {} batch oracle requests", deletedOracleRequests);

        // 12. Delete L2 oracle batch fee feed transactions
        var deletedOracleBatchFeeFeedTxs = rollupRepository.deleteOracleBatchFeeFeedTransactionsFrom(targetBatchIndex);
        log.info("Deleted {} L2 oracle batch fee feed transactions", deletedOracleBatchFeeFeedTxs);

        log.info("Rollback execution completed");
    }

    private String getTaskLockKey(BizTaskTypeEnum taskType) {
        return String.format("relayer:task:%s", taskType.name());
    }

    private void clearCacheToRollback() {
        log.info("Delete all keys for block traces: {}", redissonClient.getKeys().deleteByPattern("L2_BLOCK_TRACE@*"));
        log.info("Delete all keys for chunks: {}", redissonClient.getKeys().deleteByPattern("L2_CHUNK@*"));
        log.info("Delete all keys for batches: {}", redissonClient.getKeys().deleteByPattern("L2_BATCH@*"));
        log.info("Delete all keys for merkle trees: {}", redissonClient.getKeys().deleteByPattern("L2_MERKLE_TREE-*"));
        l2BlockTracesCacheForCurrChunk.clear();
    }
}
