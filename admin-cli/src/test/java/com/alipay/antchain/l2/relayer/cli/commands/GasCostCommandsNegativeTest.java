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
 * Negative test cases for GasCostCommands
 * Tests network failures, invalid inputs, error responses, and error recovery paths
 */
public class GasCostCommandsNegativeTest {

    private GasCostCommands gasCostCommands;
    private AdminServiceGrpc.AdminServiceBlockingStub mockStub;

    @Before
    public void setUp() {
        gasCostCommands = new GasCostCommands();
        mockStub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", mockStub);
    }

    // ==================== Network Failure Tests ====================

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateEthGasPriceIncreasedPercentage_NetworkFailure_GrpcUnavailable() {
        // Simulate gRPC UNAVAILABLE error (network failure) - will throw StatusRuntimeException
        when(mockStub.updateGasPriceConfig(any())).thenThrow(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Connection refused"))
        );

        gasCostCommands.updateEthGasPriceIncreasedPercentage(10.5);
    }

    @Test(expected = StatusRuntimeException.class)
    public void testGetEthGasPriceIncreasedPercentage_NetworkFailure_Timeout() {
        // Simulate gRPC DEADLINE_EXCEEDED error (timeout) - will throw StatusRuntimeException
        when(mockStub.getGasPriceConfig(any())).thenThrow(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"))
        );

        gasCostCommands.getEthGasPriceIncreasedPercentage();
    }

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateEthMaxPriceLimit_NetworkFailure_ConnectionLost() {
        // Simulate gRPC CANCELLED error (connection lost) - will throw StatusRuntimeException
        when(mockStub.updateGasPriceConfig(any())).thenThrow(
                new StatusRuntimeException(Status.CANCELLED.withDescription("Connection lost"))
        );

        gasCostCommands.updateEthMaxPriceLimit("1000000000");
    }

    @Test(expected = StatusRuntimeException.class)
    public void testUpdateEthBaseFeeMultiplier_NetworkFailure_Internal() {
        // Simulate gRPC INTERNAL error - will throw StatusRuntimeException
        when(mockStub.updateGasPriceConfig(any())).thenThrow(
                new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error"))
        );

        gasCostCommands.updateEthBaseFeeMultiplier(2);
    }

    @Test(expected = StatusRuntimeException.class)
    public void testGetEthMaxPriceLimit_NetworkFailure_Unauthenticated() {
        // Simulate gRPC UNAUTHENTICATED error - will throw StatusRuntimeException
        when(mockStub.getGasPriceConfig(any())).thenThrow(
                new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authentication failed"))
        );

        gasCostCommands.getEthMaxPriceLimit();
    }

    // ==================== Invalid Input Tests ====================

    @Test(expected = NullPointerException.class)
    public void testUpdateEthGasPriceIncreasedPercentage_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .toString()
        gasCostCommands.updateEthGasPriceIncreasedPercentage(null);
    }

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_InvalidInput_NegativeValue() {
        // Test with negative percentage
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Percentage must be non-negative")
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(-10.5);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_InvalidInput_ExcessiveValue() {
        // Test with excessive percentage (> 100%)
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Percentage exceeds maximum allowed value")
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(150.0);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingVal_InvalidInput_NullValue() {
        // Test with null value
        var result = gasCostCommands.updateEthFeePerBlobGasDividingVal(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingVal_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = gasCostCommands.updateEthFeePerBlobGasDividingVal("invalid_number");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingVal_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = gasCostCommands.updateEthFeePerBlobGasDividingVal("-1000");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingVal_InvalidInput_ZeroValue() {
        // Test with zero value (division by zero risk)
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Dividing value must be greater than zero")
                        .build()
        );

        var result = gasCostCommands.updateEthFeePerBlobGasDividingVal("0");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateEthLargerFeePerBlobGasMultiplier_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .toString()
        gasCostCommands.updateEthLargerFeePerBlobGasMultiplier(null);
    }

    @Test
    public void testUpdateEthLargerFeePerBlobGasMultiplier_InvalidInput_NegativeValue() {
        // Test with negative multiplier
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Multiplier must be positive")
                        .build()
        );

        var result = gasCostCommands.updateEthLargerFeePerBlobGasMultiplier(-1.5);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthLargerFeePerBlobGasMultiplier_InvalidInput_ZeroValue() {
        // Test with zero multiplier
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Multiplier must be greater than zero")
                        .build()
        );

        var result = gasCostCommands.updateEthLargerFeePerBlobGasMultiplier(0.0);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateEthBaseFeeMultiplier_InvalidInput_NullValue() {
        // Test with null value - will throw NullPointerException when calling .toString()
        gasCostCommands.updateEthBaseFeeMultiplier(null);
    }

    @Test
    public void testUpdateEthBaseFeeMultiplier_InvalidInput_NegativeValue() {
        // Test with negative multiplier
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Multiplier must be positive")
                        .build()
        );

        var result = gasCostCommands.updateEthBaseFeeMultiplier(-1);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthBaseFeeMultiplier_InvalidInput_ZeroValue() {
        // Test with zero multiplier
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Multiplier must be greater than zero")
                        .build()
        );

        var result = gasCostCommands.updateEthBaseFeeMultiplier(0);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_InvalidInput_NullValue() {
        // Test with null value
        var result = gasCostCommands.updateEthMaxPriceLimit(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = gasCostCommands.updateEthMaxPriceLimit("not_a_number");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_InvalidInput_NegativeValue() {
        // Test with negative value
        var result = gasCostCommands.updateEthMaxPriceLimit("-1000000");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthExtraGasPrice_InvalidInput_NullValue() {
        // Test with null value
        var result = gasCostCommands.updateEthExtraGasPrice(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthExtraGasPrice_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = gasCostCommands.updateEthExtraGasPrice("invalid");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMinimumEip4844PriorityPrice_InvalidInput_NullValue() {
        // Test with null value
        var result = gasCostCommands.updateEthMinimumEip4844PriorityPrice(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMinimumEip4844PriorityPrice_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = gasCostCommands.updateEthMinimumEip4844PriorityPrice("abc");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMinimumEip1559PriorityPrice_InvalidInput_NullValue() {
        // Test with null value
        var result = gasCostCommands.updateEthMinimumEip1559PriorityPrice(null);
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    @Test
    public void testUpdateEthMinimumEip1559PriorityPrice_InvalidInput_NonNumeric() {
        // Test with non-numeric value
        var result = gasCostCommands.updateEthMinimumEip1559PriorityPrice("xyz");
        Assert.assertTrue(result.toString().contains("not a number"));
    }

    // ==================== Error Response Tests ====================

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_ErrorResponse_ConfigLocked() {
        // Test server returns error: config is locked
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(423)
                        .setErrorMsg("Configuration is currently locked")
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(10.5);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("locked"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_ErrorResponse_ValueTooHigh() {
        // Test server returns error: value exceeds limit
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Price limit exceeds maximum allowed value")
                        .build()
        );

        var result = gasCostCommands.updateEthMaxPriceLimit("999999999999999999");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("exceeds"));
    }

    @Test
    public void testUpdateEthBaseFeeMultiplier_ErrorResponse_InvalidRange() {
        // Test server returns error: value out of valid range
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Multiplier value out of valid range [1, 10]")
                        .build()
        );

        var result = gasCostCommands.updateEthBaseFeeMultiplier(100);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("range"));
    }

    @Test
    public void testGetEthGasPriceIncreasedPercentage_ErrorResponse_ConfigNotFound() {
        // Test server returns error: config not found
        when(mockStub.getGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("Configuration key not found")
                        .build()
        );

        var result = gasCostCommands.getEthGasPriceIncreasedPercentage();
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not found"));
    }

    @Test
    public void testUpdateEthPriorityFeePerGasIncreasedPercentage_ErrorResponse_DatabaseError() {
        // Test server returns error: database error
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(500)
                        .setErrorMsg("Database update failed")
                        .build()
        );

        var result = gasCostCommands.updateEthPriorityFeePerGasIncreasedPercentage(15.0);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Database"));
    }

    @Test
    public void testUpdateEthExtraGasPrice_ErrorResponse_ServiceUnavailable() {
        // Test server returns error: service unavailable
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(503)
                        .setErrorMsg("Service temporarily unavailable")
                        .build()
        );

        var result = gasCostCommands.updateEthExtraGasPrice("1000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("unavailable"));
    }

    @Test
    public void testGetEthMaxPriceLimit_ErrorResponse_PermissionDenied() {
        // Test server returns error: permission denied
        when(mockStub.getGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(403)
                        .setErrorMsg("Permission denied to access this configuration")
                        .build()
        );

        var result = gasCostCommands.getEthMaxPriceLimit();
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Permission"));
    }

    @Test
    public void testUpdateEthMinimumEip4844PriorityPrice_ErrorResponse_ConflictingValue() {
        // Test server returns error: conflicting with other config
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Value conflicts with existing configuration")
                        .build()
        );

        var result = gasCostCommands.updateEthMinimumEip4844PriorityPrice("1000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("conflicts"));
    }

    // ==================== Empty/Null Response Tests ====================

    @Test
    public void testGetEthGasPriceIncreasedPercentage_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetGasPriceConfigResp(
                                GetGasPriceConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = gasCostCommands.getEthGasPriceIncreasedPercentage();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("gasPriceIncreasedPercentage"));
    }

    @Test
    public void testGetEthMaxPriceLimit_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetGasPriceConfigResp(
                                GetGasPriceConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = gasCostCommands.getEthMaxPriceLimit();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("maxPriceLimit"));
    }

    @Test
    public void testGetEthBaseFeeMultiplier_EmptyConfigValue() {
        // Test server returns empty config value
        when(mockStub.getGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setGetGasPriceConfigResp(
                                GetGasPriceConfigResp.newBuilder()
                                        .setConfigValue("")
                                        .build()
                        )
                        .build()
        );

        var result = gasCostCommands.getEthBaseFeeMultiplier();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("baseFeeMultiplier"));
    }

    // ==================== Boundary Value Tests ====================

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_BoundaryValue_VerySmall() {
        // Test with very small percentage (near zero)
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(0.001);
        Assert.assertEquals("success", result);
    }

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_BoundaryValue_VeryLarge() {
        // Test with very large percentage
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Percentage too large")
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(999.99);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_BoundaryValue_MaxLong() {
        // Test with maximum long value
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .build()
        );

        var result = gasCostCommands.updateEthMaxPriceLimit(String.valueOf(Long.MAX_VALUE));
        Assert.assertEquals("success", result);
    }

    @Test
    public void testUpdateEthBaseFeeMultiplier_BoundaryValue_MaxInteger() {
        // Test with maximum integer value
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Multiplier exceeds maximum")
                        .build()
        );

        var result = gasCostCommands.updateEthBaseFeeMultiplier(Integer.MAX_VALUE);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    // ==================== Concurrent Update Tests ====================

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage_ErrorResponse_ConcurrentModification() {
        // Test server returns error: concurrent modification detected
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(409)
                        .setErrorMsg("Configuration was modified by another process")
                        .build()
        );

        var result = gasCostCommands.updateEthGasPriceIncreasedPercentage(10.5);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("modified"));
    }

    @Test
    public void testUpdateEthMaxPriceLimit_ErrorResponse_RateLimitExceeded() {
        // Test server returns error: too many requests
        when(mockStub.updateGasPriceConfig(any())).thenReturn(
                Response.newBuilder()
                        .setCode(429)
                        .setErrorMsg("Too many update requests, please slow down")
                        .build()
        );

        var result = gasCostCommands.updateEthMaxPriceLimit("1000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Too many"));
    }
}
