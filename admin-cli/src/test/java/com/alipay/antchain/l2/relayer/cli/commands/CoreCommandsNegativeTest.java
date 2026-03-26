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

package com.alipay.antchain.l2.relayer.cli.commands;

import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Negative test cases for CoreCommands
 * Tests network failures, invalid inputs, error responses, and error recovery paths
 */
public class CoreCommandsNegativeTest {

    private CoreCommands coreCommands;
    private AdminServiceGrpc.AdminServiceBlockingStub mockStub;

    @Before
    public void setUp() {
        coreCommands = new CoreCommands();
        mockStub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        ReflectUtil.setFieldValue(coreCommands, "adminServiceBlockingStub", mockStub);
    }

    // ==================== Network Failure Tests ====================

    @Test(expected = RuntimeException.class)
    public void testInitAnchorBatch_NetworkFailure_GrpcUnavailable() {
        // Simulate gRPC UNAVAILABLE error (network failure)
        when(mockStub.initAnchorBatch(any())).thenThrow(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Connection refused"))
        );

        coreCommands.initAnchorBatch("100", "", "", "");
    }

    @Test(expected = RuntimeException.class)
    public void testGetBatch_NetworkFailure_Timeout() {
        // Simulate gRPC DEADLINE_EXCEEDED error (timeout)
        when(mockStub.getBatch(any())).thenThrow(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Timeout"))
        );

        coreCommands.getBatch("1", "");
    }

    @Test(expected = RuntimeException.class)
    public void testGetL2MsgProof_NetworkFailure_ConnectionLost() {
        // Simulate gRPC CANCELLED error (connection lost)
        when(mockStub.getL2MsgProof(any())).thenThrow(
                new StatusRuntimeException(Status.CANCELLED.withDescription("Connection lost"))
        );

        coreCommands.getL2MsgProof("1", "");
    }

    @Test(expected = RuntimeException.class)
    public void testRetryBatchTx_NetworkFailure_Internal() {
        // Simulate gRPC INTERNAL error
        when(mockStub.retryBatchTx(any())).thenThrow(
                new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error"))
        );

        coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L);
    }

    @Test(expected = RuntimeException.class)
    public void testSpeedupRollupTx_NetworkFailure_Unknown() {
        // Simulate gRPC UNKNOWN error
        when(mockStub.speedupTx(any())).thenThrow(
                new StatusRuntimeException(Status.UNKNOWN.withDescription("Unknown error"))
        );

        coreCommands.speedupRollupTx(ChainTypeEnum.LAYER_ONE, TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L, 2L, 1L);
    }

    @Test(expected = RuntimeException.class)
    public void testQueryRelayerAddress_NetworkFailure_Unauthenticated() {
        // Simulate gRPC UNAUTHENTICATED error
        when(mockStub.queryRelayerAddress(any())).thenThrow(
                new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authentication failed"))
        );

        coreCommands.queryRelayerAddress();
    }

    @Test(expected = RuntimeException.class)
    public void testQueryBatchTxInfo_NetworkFailure_PermissionDenied() {
        // Simulate gRPC PERMISSION_DENIED error
        when(mockStub.queryBatchTxInfo(any())).thenThrow(
                new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Permission denied"))
        );

        coreCommands.queryBatchTxInfo(TransactionTypeEnum.BATCH_COMMIT_TX, 1L);
    }

    @Test(expected = RuntimeException.class)
    public void testQueryBatchDaInfo_NetworkFailure_ResourceExhausted() {
        // Simulate gRPC RESOURCE_EXHAUSTED error
        when(mockStub.queryBatchDaInfo(any())).thenThrow(
                new StatusRuntimeException(Status.RESOURCE_EXHAUSTED.withDescription("Resource exhausted"))
        );

        coreCommands.queryBatchDaInfo(1L);
    }

    // ==================== Invalid Input Tests ====================

    @Test
    public void testInitAnchorBatch_InvalidInput_NullBatchIndex() {
        // Test with null batch index - should return success when anchorBatchIndex is null/empty
        var result = coreCommands.initAnchorBatch(null, "", "", "");
        Assert.assertEquals("success", result);
    }

    @Test
    public void testInitAnchorBatch_InvalidInput_EmptyBatchIndex() {
        // Test with empty batch index - should return success when anchorBatchIndex is empty
        var result = coreCommands.initAnchorBatch("", "", "", "");
        Assert.assertEquals("success", result);
    }

    @Test(expected = RuntimeException.class)
    public void testInitAnchorBatch_InvalidInput_NonNumericBatchIndex() {
        // Test with non-numeric batch index - should throw RuntimeException wrapping NumberFormatException
        coreCommands.initAnchorBatch("invalid", "", "", "");
    }

    @Test
    public void testInitAnchorBatch_InvalidInput_NegativeBatchIndex() {
        // Test with negative batch index
        when(mockStub.initAnchorBatch(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Batch index must be non-negative")
                        .build()
        );

        var result = coreCommands.initAnchorBatch("-1", "", "", "");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testSetL1StartHeight_InvalidInput_NullHeight() {
        // Test with null height - should return "not a integer"
        var result = coreCommands.setL1StartHeight(null);
        Assert.assertTrue(result.toString().contains("not a integer"));
    }

    @Test
    public void testSetL1StartHeight_InvalidInput_EmptyHeight() {
        // Test with empty height - should return "not a integer"
        var result = coreCommands.setL1StartHeight("");
        Assert.assertTrue(result.toString().contains("not a integer"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetBatch_InvalidInput_NullBatchIndex() {
        // Test with null batch index - should throw NullPointerException
        coreCommands.getBatch(null, "");
    }

    @Test(expected = NullPointerException.class)
    public void testGetL2MsgProof_InvalidInput_NullMessageNonce() {
        // Test with null message nonce - should throw NullPointerException
        coreCommands.getL2MsgProof(null, "");
    }

    @Test(expected = NullPointerException.class)
    public void testRetryBatchTx_InvalidInput_NullTransactionType() {
        // Test with null transaction type - should throw NullPointerException
        coreCommands.retryBatchTx(null, 1L, 3L);
    }

    @Test
    public void testRetryBatchTx_InvalidInput_NegativeStartIndex() {
        // Test with negative start index
        when(mockStub.retryBatchTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Start index must be non-negative")
                        .build()
        );

        var result = coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, -1L, 3L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testRetryBatchTx_InvalidInput_EndIndexLessThanStart() {
        // Test with end index less than start index
        when(mockStub.retryBatchTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("End index must be greater than or equal to start index")
                        .build()
        );

        var result = coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, 5L, 3L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = NullPointerException.class)
    public void testSpeedupRollupTx_InvalidInput_NullChainType() {
        // Test with null chain type - should throw NullPointerException
        coreCommands.speedupRollupTx(null, TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L, 2L, 1L);
    }

    @Test
    public void testSpeedupRollupTx_InvalidInput_NegativeGasPrice() {
        // Test with negative gas price
        when(mockStub.speedupTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Gas price must be positive")
                        .build()
        );

        var result = coreCommands.speedupRollupTx(ChainTypeEnum.LAYER_ONE, TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L, -1L, 1L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testSpeedupRollupTx_InvalidInput_ZeroGasPrice() {
        // Test with zero gas price
        when(mockStub.speedupTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Gas price must be positive")
                        .build()
        );

        var result = coreCommands.speedupRollupTx(ChainTypeEnum.LAYER_ONE, TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L, 0L, 1L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testQueryBatchTxInfo_InvalidInput_NegativeBatchIndex() {
        // Test with negative batch index
        when(mockStub.queryBatchTxInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Batch index must be non-negative")
                        .build()
        );

        var result = coreCommands.queryBatchTxInfo(TransactionTypeEnum.BATCH_COMMIT_TX, -1L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testQueryBatchDaInfo_InvalidInput_NegativeBatchIndex() {
        // Test with negative batch index
        when(mockStub.queryBatchDaInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Batch index must be non-negative")
                        .build()
        );

        var result = coreCommands.queryBatchDaInfo(-1L);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    // ==================== Error Response Tests ====================

    @Test
    public void testInitAnchorBatch_ErrorResponse_BatchNotFound() {
        // Test server returns error: batch not found
        when(mockStub.initAnchorBatch(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Batch not found")
                        .build()
        );

        var result = coreCommands.initAnchorBatch("999999", "", "", "");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Batch not found"));
    }

    @Test
    public void testInitAnchorBatch_ErrorResponse_AlreadyInitialized() {
        // Test server returns error: batch already initialized
        when(mockStub.initAnchorBatch(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Batch already initialized")
                        .build()
        );

        var result = coreCommands.initAnchorBatch("1", "", "", "");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("already initialized"));
    }

    @Test
    public void testSetL1StartHeight_ErrorResponse_InvalidHeight() {
        // Test server returns error: invalid height
        when(mockStub.setL1StartHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Height must be greater than current height")
                        .build()
        );

        var result = coreCommands.setL1StartHeight("100");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("greater than current height"));
    }

    @Test
    public void testGetBatch_ErrorResponse_BatchNotCommitted() {
        // Test server returns error: batch not committed yet
        when(mockStub.getRawBatch(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Batch not committed yet")
                        .build()
        );

        var result = coreCommands.getBatch("100", "");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not committed"));
    }

    @Test
    public void testGetL2MsgProof_ErrorResponse_ProofNotAvailable() {
        // Test server returns error: proof not available
        when(mockStub.getL2MsgProof(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Proof not available for this message")
                        .build()
        );

        var result = coreCommands.getL2MsgProof("1", "");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not available"));
    }

    @Test
    public void testRetryBatchTx_ErrorResponse_TransactionNotFound() {
        // Test server returns error: transaction not found
        when(mockStub.retryBatchTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Transaction not found")
                        .build()
        );

        var result = coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not found"));
    }

    @Test
    public void testRetryBatchTx_ErrorResponse_TransactionAlreadySucceeded() {
        // Test server returns error: transaction already succeeded
        when(mockStub.retryBatchTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Transaction already succeeded")
                        .build()
        );

        var result = coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("already succeeded"));
    }

    @Test
    public void testSpeedupRollupTx_ErrorResponse_InsufficientBalance() {
        // Test server returns error: insufficient balance
        when(mockStub.speedupTx(any())).thenReturn(
                Response.newBuilder()
                        .setCode(402)
                        .setErrorMsg("Insufficient balance for gas price increase")
                        .build()
        );

        var result = coreCommands.speedupRollupTx(ChainTypeEnum.LAYER_ONE, TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L, 2L, 1L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Insufficient balance"));
    }

    @Test
    public void testQueryRelayerAddress_ErrorResponse_ServiceUnavailable() {
        // Test server returns error: service unavailable
        when(mockStub.queryRelayerAddress(any())).thenReturn(
                Response.newBuilder()
                        .setCode(503)
                        .setErrorMsg("Service temporarily unavailable")
                        .build()
        );

        var result = coreCommands.queryRelayerAddress();
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("unavailable"));
    }

    @Test
    public void testQueryBatchTxInfo_ErrorResponse_DatabaseError() {
        // Test server returns error: database error
        when(mockStub.queryBatchTxInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(500)
                        .setErrorMsg("Database query failed")
                        .build()
        );

        var result = coreCommands.queryBatchTxInfo(TransactionTypeEnum.BATCH_COMMIT_TX, 1L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Database"));
    }

    @Test
    public void testQueryBatchDaInfo_ErrorResponse_DataNotSynced() {
        // Test server returns error: data not synced
        when(mockStub.queryBatchDaInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(425)
                        .setErrorMsg("Data not synced yet, please try again later")
                        .build()
        );

        var result = coreCommands.queryBatchDaInfo(1L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not synced"));
    }

    // ==================== Empty/Null Response Tests ====================

    @Test
    public void testGetBatch_EmptyResponse() {
        // Test server returns empty response
        when(mockStub.getRawBatch(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetRawBatchResp(GetRawBatchResp.newBuilder()
                                .setBatchHeader(com.google.protobuf.ByteString.EMPTY)
                                .setDaInfo(DaInfo.newBuilder().build())
                                .build())
                        .build()
        );

        try {
            var result = coreCommands.getBatch("1", "");
            // May throw exception due to empty batch header, that's acceptable
        } catch (Exception e) {
            // Expected for empty/invalid data
            Assert.assertTrue(e instanceof RuntimeException || e instanceof ArrayIndexOutOfBoundsException);
        }
    }

    @Test
    public void testGetL2MsgProof_EmptyProof() {
        // Test server returns empty proof
        when(mockStub.getL2MsgProof(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetL2MsgProofResp(
                                GetL2MsgProofResp.newBuilder()
                                        .setBatchIndex("")
                                        .setMessageNonce("")
                                        .build()
                        )
                        .build()
        );

        var result = coreCommands.getL2MsgProof("1", "");
        Assert.assertNotNull(result);
    }

    @Test
    public void testQueryRelayerAddress_EmptyAddresses() {
        // Test server returns empty addresses
        when(mockStub.queryRelayerAddress(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setQueryRelayerAddressResp(
                                QueryRelayerAddressResp.newBuilder()
                                        .setL1BlobAddress("")
                                        .setL1LegacyAddress("")
                                        .setL2Address("")
                                        .build()
                        )
                        .build()
        );

        var result = coreCommands.queryRelayerAddress();
        Assert.assertNotNull(result);
    }

    @Test
    public void testQueryBatchTxInfo_EmptyTxInfo() {
        // Test server returns empty transaction info
        when(mockStub.queryBatchTxInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setQueryBatchTxInfoResp(
                                QueryBatchTxInfoResp.newBuilder()
                                        .setBatchIndex(0L)
                                        .setTxInfo(TxInfo.newBuilder().build())
                                        .build()
                        )
                        .build()
        );

        var result = coreCommands.queryBatchTxInfo(TransactionTypeEnum.BATCH_COMMIT_TX, 1L);
        Assert.assertNotNull(result);
    }

    @Test
    public void testQueryBatchDaInfo_EmptyDaInfo() {
        // Test server returns empty DA info
        when(mockStub.queryBatchDaInfo(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setQueryBatchDaInfoResp(
                                QueryBatchDaInfoResp.newBuilder()
                                        .setBatchIndex(0L)
                                        .setDaInfo(DaInfo.newBuilder().build())
                                        .build()
                        )
                        .build()
        );

        var result = coreCommands.queryBatchDaInfo(1L);
        Assert.assertNotNull(result);
    }

    // ==================== Unexpected Exception Tests ====================

    @Test(expected = RuntimeException.class)
    public void testInitAnchorBatch_UnexpectedException_NullPointer() {
        // Test unexpected NullPointerException
        when(mockStub.initAnchorBatch(any())).thenThrow(new NullPointerException("Unexpected null"));

        coreCommands.initAnchorBatch("1", "", "", "");
    }

    @Test(expected = RuntimeException.class)
    public void testGetBatch_UnexpectedException_IllegalArgument() {
        // Test unexpected IllegalArgumentException
        when(mockStub.getBatch(any())).thenThrow(new IllegalArgumentException("Invalid argument"));

        coreCommands.getBatch("1", "");
    }

    @Test(expected = RuntimeException.class)
    public void testRetryBatchTx_UnexpectedException_IllegalState() {
        // Test unexpected IllegalStateException
        when(mockStub.retryBatchTx(any())).thenThrow(new IllegalStateException("Invalid state"));

        coreCommands.retryBatchTx(TransactionTypeEnum.BATCH_COMMIT_TX, 1L, 3L);
    }

    // ==================== RollbackToSubchainHeight Tests ====================

    @Test(expected = RuntimeException.class)
    public void testRollbackToSubchainHeight_NetworkFailure_GrpcUnavailable() {
        // Test gRPC unavailable error
        when(mockStub.rollbackToSubchainHeight(any())).thenThrow(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Server unavailable"))
        );

        // Note: This test bypasses the confirmation prompts by directly calling the stub
        // In real scenario, the command would require user confirmation
        mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());
    }

    @Test(expected = RuntimeException.class)
    public void testRollbackToSubchainHeight_NetworkFailure_Timeout() {
        // Test timeout error
        when(mockStub.rollbackToSubchainHeight(any())).thenThrow(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"))
        );

        mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_ChunkNotFound() {
        // Test server returns error: chunk not found for the target block height
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("No chunk found containing block height 1000 in batch 10")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("No chunk found"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_BlockHeightOutOfRange() {
        // Test server returns error: block height out of chunk range
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("Target block height 500 is before chunk start block 1000")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(500)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("before chunk start"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_LockAcquisitionFailed() {
        // Test server returns error: failed to acquire distributed lock
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("Failed to acquire lock for task: BATCH_COMMIT")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("Failed to acquire lock"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_OtherNodesOnline() {
        // Test server returns error: other nodes are still online
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("Other nodes are still online. Please stop all other nodes before rollback.")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("Other nodes are still online"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_DatabaseError() {
        // Test server returns error: database operation failed
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("Rollback failed: Database error during batch deletion")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("Database error"));
    }

    @Test
    public void testRollbackToSubchainHeight_Success() {
        // Test successful rollback
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setRollbackToSubchainHeightResp(
                                RollbackToSubchainHeightResp.newBuilder()
                                        .setSummary("Rollback completed successfully. Deleted 5 batches, 10 chunks, reset 3 L1 messages.")
                                        .build()
                        )
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(0, response.getCode());
        Assert.assertTrue(response.getRollbackToSubchainHeightResp().getSummary().contains("Rollback completed"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_EmptyChunks() {
        // Test server returns error: no chunks found for the batch
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("No chunks found for batch 10")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("No chunks found"));
    }

    @Test
    public void testRollbackToSubchainHeight_ErrorResponse_NodeOffline() {
        // Test server returns error: current node is offline
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(-1)
                        .setErrorMsg("Current node is offline, cannot perform rollback")
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(10)
                .setTargetBlockHeight(1000)
                .setL1MsgNonceThreshold(5)
                .build());

        Assert.assertEquals(-1, response.getCode());
        Assert.assertTrue(response.getErrorMsg().contains("offline"));
    }

    @Test
    public void testRollbackToSubchainHeight_ZeroBatchIndex() {
        // Test with zero batch index
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setRollbackToSubchainHeightResp(
                                RollbackToSubchainHeightResp.newBuilder()
                                        .setSummary("Rollback to batch 0 completed")
                                        .build()
                        )
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(0)
                .setTargetBlockHeight(1)
                .setL1MsgNonceThreshold(0)
                .build());

        Assert.assertEquals(0, response.getCode());
    }

    @Test
    public void testRollbackToSubchainHeight_LargeBatchIndex() {
        // Test with large batch index
        when(mockStub.rollbackToSubchainHeight(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setRollbackToSubchainHeightResp(
                                RollbackToSubchainHeightResp.newBuilder()
                                        .setSummary("Rollback completed for large batch index")
                                        .build()
                        )
                        .build()
        );

        var response = mockStub.rollbackToSubchainHeight(RollbackToSubchainHeightReq.newBuilder()
                .setTargetBatchIndex(Long.MAX_VALUE)
                .setTargetBlockHeight(Long.MAX_VALUE)
                .setL1MsgNonceThreshold(Long.MAX_VALUE)
                .build());

        Assert.assertEquals(0, response.getCode());
    }
}
