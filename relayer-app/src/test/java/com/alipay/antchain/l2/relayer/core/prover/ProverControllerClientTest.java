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

package com.alipay.antchain.l2.relayer.core.prover;

import java.math.BigInteger;
import jakarta.annotation.Resource;

import com.alipay.antchain.l2.prover.controller.GetProofResponse;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.CallRemoteServiceFailedException;
import com.alipay.antchain.l2.relayer.commons.exceptions.ProofNotReadyException;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.status.L2ErrorCode;
import com.alipay.antchain.l2.status.L2Status;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProverControllerClientTest extends TestBase {

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    private ProverControllerServerGrpc.ProverControllerServerBlockingStub stub = mock(ProverControllerServerGrpc.ProverControllerServerBlockingStub.class);

    @Resource
    private ProverControllerClient proverControllerClient;

    @Before
    public void init() {
        proverControllerClient.setStub(stub);
    }

    @Test
    public void testNotifyBlock() {
        when(stub.notifyBlock(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR).build()
        );

        proverControllerClient.notifyBlock(BigInteger.ONE, 1, BigInteger.ONE);
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.notifyBlock(BigInteger.ONE, 1, BigInteger.ONE));
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.notifyBlock(BigInteger.ONE, 1, BigInteger.ONE));
    }

    @Test
    public void testNotifyChunk() {
        when(stub.notifyChunk(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR).build()
        );

        proverControllerClient.notifyChunk(BigInteger.ONE, 1, BigInteger.ONE, BigInteger.TWO);
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.notifyChunk(BigInteger.ONE, 1, BigInteger.ONE, BigInteger.TWO));
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.notifyChunk(BigInteger.ONE, 1, BigInteger.ONE, BigInteger.TWO));
    }

    @Test
    public void testProveBatch() {
        when(stub.proveBatch(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED).build(),
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR).build()
        );

        proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER);
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER));
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER));
    }

    @Test
    public void testGetBatchProof() {
        when(stub.getProof(notNull())).thenReturn(
                GetProofResponse.newBuilder().setProofData(ByteString.copyFrom("1234".getBytes())).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK)).build(),
                GetProofResponse.newBuilder().setProofData(ByteString.copyFrom("1234".getBytes())).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_PROVER_ERROR_TASK_PENDING)).build(),
                GetProofResponse.newBuilder().setProofData(ByteString.copyFrom("1234".getBytes())).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_RESOURCE_EXHAUSTED)).build(),
                GetProofResponse.newBuilder().setProofData(ByteString.copyFrom("1234".getBytes())).setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR)).build()
        );

        Assert.assertArrayEquals("1234".getBytes(), proverControllerClient.getBatchProof(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
        Assert.assertThrows(ProofNotReadyException.class, () -> proverControllerClient.getBatchProof(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.getBatchProof(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
        Assert.assertThrows(CallRemoteServiceFailedException.class, () -> proverControllerClient.getBatchProof(ProveTypeEnum.TEE_PROOF, BigInteger.ONE));
    }
}
