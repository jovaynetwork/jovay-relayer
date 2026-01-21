package com.alipay.antchain.l2.relayer.cli.commands;

import cn.hutool.core.util.ReflectUtil;
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
 * Negative test cases for RollupEconomicCommands
 * Tests network failures, invalid inputs, error responses, and error recovery paths
 */
public class RollupEconomicCommandsNegativeTest {

    private RollupEconomicCommands rollupEconomicCommands;
    private AdminServiceGrpc.AdminServiceBlockingStub mockStub;

    @Before
    public void setUp() {
        rollupEconomicCommands = new RollupEconomicCommands();
        mockStub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", mockStub);
    }

    // ==================== Network Failure Tests ====================

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateMidEip1559PriceLimit_NetworkFailure_GrpcUnavailable() {
        // Simulate gRPC UNAVAILABLE error (network failure) - will throw StatusRuntimeException
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenThrow(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Connection refused"))
        );

        rollupEconomicCommands.updateMidEip1559PriceLimit("1000000000");
    }

    @Test(expected = StatusRuntimeException.class)
    public void testGetMidEip1559PriceLimit_NetworkFailure_Timeout() {
        // Simulate gRPC DEADLINE_EXCEEDED error (timeout) - will throw StatusRuntimeException
        when(mockStub.getRollupEconomicConfig(any())).thenThrow(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"))
        );

        rollupEconomicCommands.getMidEip1559PriceLimit();
    }

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateMaxPendingBatchCount_NetworkFailure_ConnectionLost() {
        // Simulate gRPC CANCELLED error (connection lost) - will throw StatusRuntimeException
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenThrow(
                new StatusRuntimeException(Status.CANCELLED.withDescription("Connection lost"))
        );

        rollupEconomicCommands.updateMaxPendingBatchCount(10);
    }

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateMaxBatchWaitingTime_NetworkFailure_Internal() {
        // Simulate gRPC INTERNAL error - will throw StatusRuntimeException
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenThrow(
                new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error"))
        );

        rollupEconomicCommands.updateMaxBatchWaitingTime(300L);
    }

    @Test(expected = StatusRuntimeException.class)
    public void testGetHighEip1559PriceLimit_NetworkFailure_Unauthenticated() {
        // Simulate gRPC UNAUTHENTICATED error - will throw StatusRuntimeException
        when(mockStub.getRollupEconomicConfig(any())).thenThrow(
                new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authentication failed"))
        );

        rollupEconomicCommands.getHighEip1559PriceLimit();
    }

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateMaxProofWaitingTime_NetworkFailure_PermissionDenied() {
        // Simulate gRPC PERMISSION_DENIED error - will throw StatusRuntimeException
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenThrow(
                new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Permission denied"))
        );

        rollupEconomicCommands.updateMaxProofWaitingTime(600L);
    }

    // ==================== Invalid Input Tests ====================

    @Test
    public void testUpdateMidEip1559PriceLimit_InvalidInput_NullValue() {
        // Test with null value
        var result = rollupEconomicCommands.updateMidEip1559PriceLimit(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimit_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("invalid_number");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimit_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("-1000000");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimit_InvalidInput_ZeroValue() {
        // Test with zero value
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Price limit must be greater than zero")
                        .build()
        );

        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("0");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimit_InvalidInput_NullValue() {
        // Test with null value
        var result = rollupEconomicCommands.updateHighEip1559PriceLimit(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimit_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = rollupEconomicCommands.updateHighEip1559PriceLimit("abc123");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimit_InvalidInput_LowerThanMid() {
        // Test with value lower than mid price limit
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("High price limit must be greater than mid price limit")
                        .build()
        );

        var result = rollupEconomicCommands.updateHighEip1559PriceLimit("100");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateMaxPendingBatchCount_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .intValue()
        rollupEconomicCommands.updateMaxPendingBatchCount(null);
    }

    @Test
    public void testUpdateMaxPendingBatchCount_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = rollupEconomicCommands.updateMaxPendingBatchCount(-10);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount_InvalidInput_ZeroValue() {
        // Test with zero value
        var result = rollupEconomicCommands.updateMaxPendingBatchCount(0);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateMaxPendingProofCount_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .intValue()
        rollupEconomicCommands.updateMaxPendingProofCount(null);
    }

    @Test
    public void testUpdateMaxPendingProofCount_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = rollupEconomicCommands.updateMaxPendingProofCount(-5);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test
    public void testUpdateMaxPendingProofCount_InvalidInput_ZeroValue() {
        // Test with zero value
        var result = rollupEconomicCommands.updateMaxPendingProofCount(0);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateMaxBatchWaitingTime_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .longValue()
        rollupEconomicCommands.updateMaxBatchWaitingTime(null);
    }

    @Test
    public void testUpdateMaxBatchWaitingTime_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = rollupEconomicCommands.updateMaxBatchWaitingTime(-100L);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test
    public void testUpdateMaxBatchWaitingTime_InvalidInput_ZeroValue() {
        // Test with zero value
        var result = rollupEconomicCommands.updateMaxBatchWaitingTime(0L);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateMaxProofWaitingTime_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .longValue()
        rollupEconomicCommands.updateMaxProofWaitingTime(null);
    }

    @Test
    public void testUpdateMaxProofWaitingTime_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = rollupEconomicCommands.updateMaxProofWaitingTime(-200L);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    @Test
    public void testUpdateMaxProofWaitingTime_InvalidInput_ZeroValue() {
        // Test with zero value
        var result = rollupEconomicCommands.updateMaxProofWaitingTime(0L);
        Assert.assertTrue(result.toString().contains("must be positive"));
    }

    // ==================== Error Response Tests ====================

    @Test
    public void testUpdateMidEip1559PriceLimit_ErrorResponse_ValueTooHigh() {
        // Test server returns error: value too high
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Price limit exceeds maximum allowed value")
                        .build()
        );

        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("999999999999999999");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("exceeds"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimit_ErrorResponse_ConfigLocked() {
        // Test server returns error: config is locked
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(423)
                        .setErrorMsg("Configuration is currently locked")
                        .build()
        );

        var result = rollupEconomicCommands.updateHighEip1559PriceLimit("5000000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("locked"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount_ErrorResponse_ValueTooLarge() {
        // Test server returns error: count too large
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Batch count exceeds system capacity")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingBatchCount(10000);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("exceeds"));
    }

    @Test
    public void testUpdateMaxPendingProofCount_ErrorResponse_InsufficientResources() {
        // Test server returns error: insufficient resources
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(507)
                        .setErrorMsg("Insufficient resources to handle this many proofs")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingProofCount(5000);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Insufficient"));
    }

    @Test
    public void testUpdateMaxBatchWaitingTime_ErrorResponse_TimeTooShort() {
        // Test server returns error: time too short
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Waiting time too short, minimum is 60 seconds")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxBatchWaitingTime(30L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("too short"));
    }

    @Test
    public void testUpdateMaxProofWaitingTime_ErrorResponse_TimeTooLong() {
        // Test server returns error: time too long
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Waiting time too long, maximum is 3600 seconds")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxProofWaitingTime(7200L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("too long"));
    }

    @Test
    public void testGetMidEip1559PriceLimit_ErrorResponse_ConfigNotFound() {
        // Test server returns error: config not found
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Configuration key not found")
                        .build()
        );

        var result = rollupEconomicCommands.getMidEip1559PriceLimit();
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not found"));
    }

    @Test
    public void testGetMaxPendingBatchCount_ErrorResponse_DatabaseError() {
        // Test server returns error: database error
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(500)
                        .setErrorMsg("Database query failed")
                        .build()
        );

        var result = rollupEconomicCommands.getMaxPendingBatchCount();
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Database"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimit_ErrorResponse_ServiceUnavailable() {
        // Test server returns error: service unavailable
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(503)
                        .setErrorMsg("Service temporarily unavailable")
                        .build()
        );

        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("2000000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("unavailable"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount_ErrorResponse_ConflictingValue() {
        // Test server returns error: conflicting with other config
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Value conflicts with maxPendingProofCount configuration")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingBatchCount(100);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("conflicts"));
    }

    // ==================== Empty/Null Response Tests ====================

    @Test
    public void testGetMidEip1559PriceLimit_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetRollupEconomicConfigResp(
                                GetRollupEconomicConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = rollupEconomicCommands.getMidEip1559PriceLimit();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("midEip1559PriceLimit"));
    }

    @Test
    public void testGetHighEip1559PriceLimit_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetRollupEconomicConfigResp(
                                GetRollupEconomicConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = rollupEconomicCommands.getHighEip1559PriceLimit();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("highEip1559PriceLimit"));
    }

    @Test
    public void testGetMaxPendingBatchCount_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetRollupEconomicConfigResp(
                                GetRollupEconomicConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = rollupEconomicCommands.getMaxPendingBatchCount();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("maxPendingBatchCount"));
    }

    @Test
    public void testGetMaxBatchWaitingTime_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getRollupEconomicConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetRollupEconomicConfigResp(
                                GetRollupEconomicConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = rollupEconomicCommands.getMaxBatchWaitingTime();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("maxBatchWaitingTime"));
    }

    // ==================== Boundary Value Tests ====================

    @Test
    public void testUpdateMidEip1559PriceLimit_BoundaryValue_MaxLong() {
        // Test with maximum long value
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .build()
        );

        var result = rollupEconomicCommands.updateMidEip1559PriceLimit(String.valueOf(Long.MAX_VALUE));
        Assert.assertEquals("success", result);
    }

    @Test
    public void testUpdateMaxPendingBatchCount_BoundaryValue_MaxInteger() {
        // Test with maximum integer value
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Count exceeds maximum")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingBatchCount(Integer.MAX_VALUE);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateMaxBatchWaitingTime_BoundaryValue_VeryLarge() {
        // Test with very large time value
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Waiting time exceeds maximum allowed")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxBatchWaitingTime(Long.MAX_VALUE);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount_BoundaryValue_MinimumValid() {
        // Test with minimum valid value (1)
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingBatchCount(1);
        Assert.assertEquals("success", result);
    }

    // ==================== Concurrent Update Tests ====================

    @Test
    public void testUpdateMidEip1559PriceLimit_ErrorResponse_ConcurrentModification() {
        // Test server returns error: concurrent modification detected
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Configuration was modified by another process")
                        .build()
        );

        var result = rollupEconomicCommands.updateMidEip1559PriceLimit("3000000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("modified"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount_ErrorResponse_RateLimitExceeded() {
        // Test server returns error: too many requests
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(429)
                        .setErrorMsg("Too many update requests, please slow down")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxPendingBatchCount(50);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Too many"));
    }

    @Test
    public void testUpdateMaxBatchWaitingTime_ErrorResponse_ValidationFailed() {
        // Test server returns error: validation failed
        when(mockStub.updateRollupEconomicStrategyConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(422)
                        .setErrorMsg("Validation failed: waiting time incompatible with current batch size")
                        .build()
        );

        var result = rollupEconomicCommands.updateMaxBatchWaitingTime(120L);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Validation"));
    }
}
