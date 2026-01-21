package com.alipay.antchain.l2.relayer.core.prover;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.RemoteServiceRetryException;
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

    // ==================== Negative Case Tests ====================

    /**
     * Test notify block when prover is unavailable
     */
    @Test
    public void testNotifyBlock_ProverUnavailable() {
        when(stub.notifyBlock(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR).setErrorMessage("Prover service unavailable").build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> proverControllerClient.notifyBlock(BigInteger.ONE, 1, BigInteger.ONE));
    }

    /**
     * Test notify chunk when timeout occurs
     */
    @Test
    public void testNotifyChunk_Timeout() {
        when(stub.notifyChunk(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_TIMEOUT).setErrorMessage("Request timeout").build()
        );

        Assert.assertThrows(RemoteServiceRetryException.class,
            () -> proverControllerClient.notifyChunk(BigInteger.ONE, 1, BigInteger.ONE, BigInteger.TWO));
    }

    /**
     * Test prove batch when prover is busy
     */
    @Test
    public void testProveBatch_ProverBusy() {
        when(stub.proveBatch(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_SERVER_BUSY).setErrorMessage("Prover is busy").build()
        );
        Assert.assertThrows(RemoteServiceRetryException.class,
            () -> proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER));

        when(stub.proveBatch(notNull())).thenReturn(
                L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_INTERNAL_ERROR).setErrorMessage("Internal error").build()
        );
        Assert.assertThrows(CallRemoteServiceFailedException.class,
                () -> proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER));
    }

    /**
     * Test get batch proof when proof data is empty
     */
    @Test
    public void testGetBatchProof_EmptyProofData() {
        when(stub.getProof(notNull())).thenReturn(
                GetProofResponse.newBuilder()
                    .setProofData(ByteString.EMPTY)
                    .setStatus(L2Status.newBuilder().setErrorCode(L2ErrorCode.L2_OK))
                    .build()
        );

        byte[] proof = proverControllerClient.getBatchProof(ProveTypeEnum.ZK_PROOF, BigInteger.ONE);
        Assert.assertEquals(0, proof.length);
    }

    /**
     * Test get batch proof when prover returns invalid argument error
     */
    @Test
    public void testGetBatchProof_InvalidArgument() {
        when(stub.getProof(notNull())).thenReturn(
                GetProofResponse.newBuilder()
                    .setProofData(ByteString.copyFrom("test".getBytes()))
                    .setStatus(L2Status.newBuilder()
                        .setErrorCode(L2ErrorCode.L2_INVALID_ARGUMENT)
                        .setErrorMessage("Invalid batch index"))
                    .build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> proverControllerClient.getBatchProof(ProveTypeEnum.TEE_PROOF, BigInteger.valueOf(-1)));
    }

    /**
     * Test notify block with invalid block number
     */
    @Test
    public void testNotifyBlock_InvalidBlockNumber() {
        when(stub.notifyBlock(notNull())).thenReturn(
                L2Status.newBuilder()
                    .setErrorCode(L2ErrorCode.L2_INVALID_ARGUMENT)
                    .setErrorMessage("Block number out of range")
                    .build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> proverControllerClient.notifyBlock(BigInteger.ONE, 1, BigInteger.valueOf(-1)));
    }

    /**
     * Test prove batch when batch data is corrupted
     */
    @Test
    public void testProveBatch_CorruptedData() {
        when(stub.proveBatch(notNull())).thenReturn(
                L2Status.newBuilder()
                    .setErrorCode(L2ErrorCode.L2_INVALID_ARGUMENT)
                    .setErrorMessage("Batch data is corrupted")
                    .build()
        );

        Assert.assertThrows(CallRemoteServiceFailedException.class,
            () -> proverControllerClient.proveBatch(ZERO_BATCH_WRAPPER));
    }
}
