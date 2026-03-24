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

package com.alipay.antchain.l2.relayer.core.tracer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
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

    @GrpcClient("tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub stub;

    public BigInteger getLatestProcessedBlock() {
        return BigInteger.valueOf(stub.getLatestProcessedBlock(GetLatestProcessedBlockRequest.newBuilder().build()).getBlockNumber());
    }

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
