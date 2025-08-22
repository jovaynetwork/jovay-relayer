package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
    private IRollupRepository rollupRepository;

    @Resource
    private IMailboxRepository mailboxRepository;

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
                                .version(ByteUtil.intToByte(request.getBatchHeaderInfo().getVersion()))
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
                                    .setVersion(batchHeader.getVersion())
                                    .setParentBatchHash(HexUtil.encodeHexStr(batchHeader.getParentBatchHash()))
                                    .setDataHash(HexUtil.encodeHexStr(batchHeader.getDataHash()))
                                    .setL1MsgRollingHash(Numeric.toHexString(batchHeader.getL1MsgRollingHash()))
                                    .build()
                    ).addAllChunks(
                            batchWrapper.getBatch().getChunks().stream().map(x -> {
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
            responseObserver.onNext(
                    Response.newBuilder().setGetRawBatchResp(
                            GetRawBatchResp.newBuilder()
                                    .setBatchHeader(ByteString.copyFrom(batchWrapper.getBatch().getBatchHeader().serialize()))
                                    .addAllChunks(
                                            batchWrapper.getBatch().getChunks().stream()
                                                    .map(x -> RawChunkInfo.newBuilder()
                                                            .setHash(HexUtil.encodeHexStr(Assert.notNull(x.getHash())))
                                                            .setRawChunk(ByteString.copyFrom(x.serialize()))
                                                            .build()
                                                    ).collect(Collectors.toList())
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
}
