package com.alipay.antchain.l2.relayer.core.prover;

import java.math.BigInteger;
import java.util.stream.Collectors;

import com.alipay.antchain.l2.prover.common.BasicChunkInfo;
import com.alipay.antchain.l2.prover.common.BatchInfo;
import com.alipay.antchain.l2.prover.common.BlockInfo;
import com.alipay.antchain.l2.prover.common.ChunkInfo;
import com.alipay.antchain.l2.prover.controller.GetBatchProofRequest;
import com.alipay.antchain.l2.prover.controller.GetProofResponse;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.CallRemoteServiceFailedException;
import com.alipay.antchain.l2.relayer.commons.exceptions.ProofNotReadyException;
import com.alipay.antchain.l2.relayer.commons.exceptions.RemoteServiceRetryException;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.utils.RemoteServiceUtils;
import com.alipay.antchain.l2.status.L2ErrorCode;
import com.alipay.antchain.l2.status.L2Status;
import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@Setter
@Component
@Slf4j
@EnableRetry
public class ProverControllerClient {

    @GrpcClient("prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub stub;

    @Value("#{rollupConfig.maxTxsInChunks}")
    private int maxTxsInChunks;

    @Retryable(retryFor = RemoteServiceRetryException.class, backoff = @Backoff(delay = 100, multiplier = 2))
    public void notifyBlock(BigInteger batchIndex, long chunkIndex, BigInteger blockNumber) {
        log.info("notify block (batch: {}, chunk: {}, number: {}) to prover controller", batchIndex, chunkIndex, blockNumber);
        BlockInfo blockInfo = BlockInfo.newBuilder()
                .setBatchId(batchIndex.longValue())
                .setChunkId(chunkIndex)
                .setBlockIndex(blockNumber.longValue())
                .build();
        L2Status status = this.stub.notifyBlock(blockInfo);
        if (status.getErrorCode() != L2ErrorCode.L2_OK) {
            if (RemoteServiceUtils.isL2StatusRetryable(status)) {
                throw new RemoteServiceRetryException(
                        status.getErrorCode(), status.getErrorMessage(),
                        "failed to notify block (batch: {}, chunk: {}, number: {})", batchIndex, chunkIndex, blockNumber
                );
            }
            throw new CallRemoteServiceFailedException(
                    status.getErrorCode(), status.getErrorMessage(),
                    "failed to notify block (batch: {}, chunk: {}, number: {})", batchIndex, chunkIndex, blockNumber
            );
        }
    }

    @Retryable(retryFor = RemoteServiceRetryException.class, backoff = @Backoff(delay = 100, multiplier = 2))
    public void notifyChunk(BigInteger batchIndex, long chunkIndex) {
        log.info("notify chunk (batch: {}, chunk: {}) to prover controller", batchIndex, chunkIndex);
        ChunkInfo chunkInfo = ChunkInfo.newBuilder()
                .setBatchId(batchIndex.longValue())
                .setChunkId(chunkIndex)
                .build();
        L2Status status = this.stub.notifyChunk(chunkInfo);
        if (status.getErrorCode() != L2ErrorCode.L2_OK) {
            if (RemoteServiceUtils.isL2StatusRetryable(status)) {
                throw new RemoteServiceRetryException(
                        status.getErrorCode(), status.getErrorMessage(),
                        "failed to notify chunk (batch: {}, chunk: {})", batchIndex, chunkIndex
                );
            }
            throw new CallRemoteServiceFailedException(
                    status.getErrorCode(), status.getErrorMessage(),
                    "failed to notify chunk (batch: {}, chunk: {})", batchIndex, chunkIndex
            );
        }
    }

    @Retryable(retryFor = RemoteServiceRetryException.class, backoff = @Backoff(delay = 100, multiplier = 2))
    public void proveBatch(BatchWrapper batch) {
        log.info("request the prover controller to prove batch (batch: {}) ", batch.getBatch().getBatchIndex());
        BatchInfo batchInfo = BatchInfo.newBuilder()
                .setBatchId(batch.getBatch().getBatchIndex().longValue())
                .addAllChunkList(batch.getBatch().getChunks().stream().map(
                        chunk -> BasicChunkInfo.newBuilder()
                                .setBlockStart(chunk.getStartBlockNumber().longValue())
                                .setBlockEnd(chunk.getEndBlockNumber().longValue() + 1)
                                .build()
                ).collect(Collectors.toList()))
                .setParentBatchHash(ByteString.copyFrom(batch.getBatchHeader().getParentBatchHash()))
                .setMaxTxsInChunk(maxTxsInChunks)
                .build();
        L2Status status = this.stub.proveBatch(batchInfo);
        if (status.getErrorCode() != L2ErrorCode.L2_OK) {
            if (RemoteServiceUtils.isL2StatusRetryable(status)) {
                throw new RemoteServiceRetryException(
                        status.getErrorCode(), status.getErrorMessage(),
                        "failed to prove batch {}", batch.getBatch().getBatchIndex()
                );
            }
            throw new CallRemoteServiceFailedException(
                    status.getErrorCode(), status.getErrorMessage(),
                    "failed to prove batch {}", batch.getBatch().getBatchIndex()
            );
        }
    }

    @Retryable(retryFor = RemoteServiceRetryException.class, backoff = @Backoff(delay = 10, multiplier = 2))
    public byte[] getBatchProof(ProveTypeEnum proveType, BigInteger batchIndex) {
        log.info("try to get batch proof for {} with retry time {}", batchIndex, RetrySynchronizationManager.getContext().getRetryCount());
        GetProofResponse response = this.stub.getProof(
                GetBatchProofRequest.newBuilder()
                        .setProverType(proveType.getProverType())
                        .setBatchId(batchIndex.longValue())
                        .build()
        );
        if (response.getStatus().getErrorCode() != L2ErrorCode.L2_OK) {
            if (response.getStatus().getErrorCode() == L2ErrorCode.L2_PROVER_ERROR_TASK_RUNNING
                    || response.getStatus().getErrorCode() == L2ErrorCode.L2_PROVER_ERROR_TASK_PENDING) {
                log.debug("getBatchProof retry with rpc error: {}-{}", response.getStatus().getErrorCode().getNumber(), response.getStatus().getErrorMessage());
                throw new ProofNotReadyException(proveType, batchIndex);
            }
            if (RemoteServiceUtils.isL2StatusRetryable(response.getStatus())) {
                throw new RemoteServiceRetryException(
                        response.getStatus().getErrorCode(), response.getStatus().getErrorMessage(),
                        "failed to get batch proof for type {} and index {}", proveType.name(), batchIndex
                );
            }
            throw new CallRemoteServiceFailedException(
                    response.getStatus().getErrorCode(), response.getStatus().getErrorMessage(),
                    "failed to get batch proof for type {} and index {}", proveType.name(), batchIndex
            );
        }
        return response.getProofData().toByteArray();
    }
}
