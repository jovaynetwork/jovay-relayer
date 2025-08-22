package com.alipay.antchain.l2.relayer.core.tracer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockTraceNotReadyException;
import com.alipay.antchain.l2.relayer.commons.exceptions.CallRemoteServiceFailedException;
import com.alipay.antchain.l2.relayer.commons.exceptions.RemoteServiceRetryException;
import com.alipay.antchain.l2.relayer.utils.RemoteServiceUtils;
import com.alipay.antchain.l2.status.L2ErrorCode;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import com.alipay.antchain.l2.tracer.*;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Setter
@EnableRetry
public class TraceServiceClient {

    private static final Logger ZK_CYCLE_LOGGER = LoggerFactory.getLogger("zk-cycle");

    @Value("${l2-relayer.alarm.zk-cycle.threshold:1000000000}")
    private long zkCycleThreshold;

    @GrpcClient("tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub stub;

    @Value("${l2-relayer.tracer-client.req-retry.max-block-stable-gap:10}")
    private int maxBlockStableGap;

    /**
     * Fetch basic block trace from tracer
     *
     * @param startBlockNumber included
     * @param endBlockNumber   excluded
     */
    public List<BasicBlockTrace> fetchBasicTrace(BigInteger startBlockNumber, BigInteger endBlockNumber) {
        log.info("fetchBasicTrace from tracer startBlockNumber: {}, endBlockNumber: {}", startBlockNumber, endBlockNumber);
        Iterator<BasicBlockTrace> stream = stub.fetchBasicTrace(
                BlockRange.newBuilder()
                        .setBegin(startBlockNumber.longValue())
                        .setEnd(endBlockNumber.longValue())
                        .build()
        );

        List<BasicBlockTrace> result = new ArrayList<>();
        while (stream.hasNext()) {
            BasicBlockTrace nextBlockTrace = stream.next();
            if (ObjectUtil.isNull(nextBlockTrace)) {
                throw new RuntimeException("unexpected null block trace");
            }
            log.info("get block trace for {}", nextBlockTrace.getHeader().getNumber());
            result.add(nextBlockTrace);
        }
        return result.stream().sorted(Comparator.comparingLong(o -> o.getHeader().getNumber())).collect(Collectors.toList());
    }

    @WithSpan
    @Retryable(
            retryFor = RemoteServiceRetryException.class,
            maxAttemptsExpression = "#{${l2-relayer.tracer-client.req-retry.max-attempts:5}}",
            backoff = @Backoff(
                    delayExpression = "#{${l2-relayer.tracer-client.req-retry.backoff-delay:5}}",
                    multiplierExpression = "#{${l2-relayer.tracer-client.req-retry.backoff-multiplier:10}}",
                    maxDelayExpression = "#{${l2-relayer.tracer-client.req-retry.backoff-max-delay:500}}"
            )
    )
    public BasicBlockTrace getBasicTrace(BigInteger blockNumber) {
        log.info("getBasicTrace {} from tracer with retry time {}", blockNumber, RetrySynchronizationManager.getContext().getRetryCount());

        GetBasicTraceResponse response = stub.getBasicTrace(
                GetBasicTraceRequest.newBuilder()
                        .setWithoutStorageTrace(true)
                        .setBlockNumber(blockNumber.longValue())
                        .build()
        );
        if (response.getStatus().getErrorCode() != L2ErrorCode.L2_OK) {
            boolean isLatestTraceDelayed = response.getStatus().getErrorCode() == L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER
                                           && blockNumber.compareTo(BigInteger.valueOf(response.getStableBlockNumber() + maxBlockStableGap)) <= 0;
            if (isLatestTraceDelayed) {
                log.debug("tracer not catch up the latest height: {}-{}", response.getStatus().getErrorCode().getNumber(), response.getStatus().getErrorMessage());
                throw new BlockTraceNotReadyException("{} not ready and stable is {}", blockNumber, response.getStableBlockNumber());
            }
            if (RemoteServiceUtils.isL2StatusRetryable(response.getStatus())) {
                log.debug("getBasicTrace retry with rpc error: {}-{}", response.getStatus().getErrorCode().getNumber(), response.getStatus().getErrorMessage());
                throw new RemoteServiceRetryException(
                        response.getStatus().getErrorCode(), response.getStatus().getErrorMessage(),
                        "failed to get basic for {}", blockNumber.toString()
                );
            }
            log.info("stable number from tracer {} and block from seq {}", response.getStableBlockNumber(), blockNumber);
            throw new CallRemoteServiceFailedException(
                    response.getStatus().getErrorCode(), response.getStatus().getErrorMessage(),
                    "failed to get basic for {}", blockNumber.toString()
            );
        }

        if (response.getBasicTrace().getZkCycles() >= zkCycleThreshold) {
            ZK_CYCLE_LOGGER.error("📢 zk cycle {} from block#{} is too large", response.getBasicTrace().getZkCycles(), blockNumber);
        }

        return response.getBasicTrace();
    }

    public void notifyProofCommitted(BigInteger blockNumberToDel, ProveTypeEnum proofTxType) {
        log.info("📢 notify block {} is {} ProofCommitted to tracer", blockNumberToDel, proofTxType);
        NotifyProofCommittedResponse response = stub.notifyProofCommitted(
                NotifyProofCommittedRequest.newBuilder()
                        .setBlockNumber(blockNumberToDel.longValue())
                        .setCommitProofType(proofTxType == ProveTypeEnum.TEE_PROOF ? CommitProofType.COMMIT_PROOF_TYPE_TEE : CommitProofType.COMMIT_PROOF_TYPE_ZK)
                        .build()
        );
        if (response.getStatus().getErrorCode() != L2ErrorCode.L2_OK) {
            throw new CallRemoteServiceFailedException(
                    response.getStatus().getErrorCode(), response.getStatus().getErrorMessage(),
                    "failed to notify {} proof committed for {}", proofTxType, blockNumberToDel.toString()
            );
        }
    }
}
