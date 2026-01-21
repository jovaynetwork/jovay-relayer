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
 * Negative test cases for OracleCommands
 * Tests network failures, invalid inputs, error responses, and error recovery paths
 */
public class OracleCommandsNegativeTest {

    private OracleCommands oracleCommands;
    private AdminServiceGrpc.AdminServiceBlockingStub mockStub;

    @Before
    public void setUp() {
        oracleCommands = new OracleCommands();
        mockStub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        ReflectUtil.setFieldValue(oracleCommands, "adminServiceBlockingStub", mockStub);
    }

    // ==================== Network Failure Tests ====================

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_NetworkFailure_GrpcUnavailable() {
        // Simulate gRPC UNAVAILABLE error (network failure)
        when(mockStub.withdrawFromVault(any())).thenThrow(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Connection refused"))
        );

        oracleCommands.withdrawFromVault("0x123", 100);
    }

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_NetworkFailure_Timeout() {
        // Simulate gRPC DEADLINE_EXCEEDED error (timeout)
        when(mockStub.withdrawFromVault(any())).thenThrow(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"))
        );

        oracleCommands.withdrawFromVault("0x123", 100);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateFixedProfit_NetworkFailure_ConnectionLost() {
        // Simulate gRPC CANCELLED error (connection lost)
        when(mockStub.updateFixedProfit(any())).thenThrow(
                new StatusRuntimeException(Status.CANCELLED.withDescription("Connection lost"))
        );

        oracleCommands.updateFixedProfit("1000000000000000000");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateFixedProfit_NetworkFailure_Internal() {
        // Simulate gRPC INTERNAL error
        when(mockStub.updateFixedProfit(any())).thenThrow(
                new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error"))
        );

        oracleCommands.updateFixedProfit("1000000000000000000");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateTotalScala_NetworkFailure_Unknown() {
        // Simulate gRPC UNKNOWN error
        when(mockStub.updateTotalScala(any())).thenThrow(
                new StatusRuntimeException(Status.UNKNOWN.withDescription("Unknown error"))
        );

        oracleCommands.updateTotalScala("1000");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateTotalScala_NetworkFailure_Unauthenticated() {
        // Simulate gRPC UNAUTHENTICATED error
        when(mockStub.updateTotalScala(any())).thenThrow(
                new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authentication failed"))
        );

        oracleCommands.updateTotalScala("1000");
    }

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_NetworkFailure_PermissionDenied() {
        // Simulate gRPC PERMISSION_DENIED error
        when(mockStub.withdrawFromVault(any())).thenThrow(
                new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Permission denied"))
        );

        oracleCommands.withdrawFromVault("0x123", 100);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateFixedProfit_NetworkFailure_ResourceExhausted() {
        // Simulate gRPC RESOURCE_EXHAUSTED error
        when(mockStub.updateFixedProfit(any())).thenThrow(
                new StatusRuntimeException(Status.RESOURCE_EXHAUSTED.withDescription("Resource exhausted"))
        );

        oracleCommands.updateFixedProfit("1000000000000000000");
    }

    // ==================== Invalid Input Tests ====================

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_InvalidInput_NullAddress() {
        // Test with null address - will throw RuntimeException wrapping NullPointerException
        oracleCommands.withdrawFromVault(null, 100);
    }

    @Test
    public void testWithdrawFromVault_InvalidInput_EmptyAddress() {
        // Test with empty address
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Invalid address: empty")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("", 100);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testWithdrawFromVault_InvalidInput_MalformedAddress() {
        // Test with malformed address (not starting with 0x)
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Invalid address format")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("invalid_address", 100);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testWithdrawFromVault_InvalidInput_InvalidAddressLength() {
        // Test with invalid address length
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Invalid address length")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x123", 100);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testWithdrawFromVault_InvalidInput_NegativeAmount() {
        // Test with negative amount
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Amount must be positive")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", -100);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testWithdrawFromVault_InvalidInput_ZeroAmount() {
        // Test with zero amount
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Amount must be greater than zero")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 0);
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateFixedProfit_InvalidInput_NullProfit() {
        // Test with null profit - will throw RuntimeException wrapping NullPointerException
        oracleCommands.updateFixedProfit(null);
    }

    @Test
    public void testUpdateFixedProfit_InvalidInput_EmptyProfit() {
        // Test with empty profit
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Invalid profit: empty")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateFixedProfit_InvalidInput_NonNumericProfit() {
        // Test with non-numeric profit
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Profit must be a valid number")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("invalid_number");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateFixedProfit_InvalidInput_NegativeProfit() {
        // Test with negative profit
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Profit must be non-negative")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("-1000000000000000000");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateTotalScala_InvalidInput_NullScala() {
        // Test with null scala - will throw RuntimeException wrapping NullPointerException
        oracleCommands.updateTotalScala(null);
    }

    @Test
    public void testUpdateTotalScala_InvalidInput_EmptyScala() {
        // Test with empty scala
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Invalid scala: empty")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateTotalScala_InvalidInput_NonNumericScala() {
        // Test with non-numeric scala
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Scala must be a valid number")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("not_a_number");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateTotalScala_InvalidInput_NegativeScala() {
        // Test with negative scala
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Scala must be positive")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("-1000");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testUpdateTotalScala_InvalidInput_ZeroScala() {
        // Test with zero scala
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(1)
                        .setErrorMsg("Scala must be greater than zero")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("0");
        Assert.assertTrue(result.toString().contains("failed"));
    }

    // ==================== Error Response Tests ====================

    @Test
    public void testWithdrawFromVault_ErrorResponse_InsufficientBalance() {
        // Test server returns error: insufficient balance
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(402)
                        .setErrorMsg("Insufficient balance in vault")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 1000000);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Insufficient balance"));
    }

    @Test
    public void testWithdrawFromVault_ErrorResponse_VaultLocked() {
        // Test server returns error: vault is locked
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(423)
                        .setErrorMsg("Vault is currently locked")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 100);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("locked"));
    }

    @Test
    public void testWithdrawFromVault_ErrorResponse_TransactionFailed() {
        // Test server returns error: transaction failed
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(500)
                        .setErrorMsg("Transaction submission failed")
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 100);
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Transaction"));
    }

    @Test
    public void testUpdateFixedProfit_ErrorResponse_ContractNotFound() {
        // Test server returns error: contract not found
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(404)
                        .setErrorMsg("L1Oracle contract not found")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("1000000000000000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("not found"));
    }

    @Test
    public void testUpdateFixedProfit_ErrorResponse_InvalidProfitValue() {
        // Test server returns error: invalid profit value
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Profit value exceeds maximum allowed")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("999999999999999999999999999");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("exceeds maximum"));
    }

    @Test
    public void testUpdateFixedProfit_ErrorResponse_GasPriceTooHigh() {
        // Test server returns error: gas price too high
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(402)
                        .setErrorMsg("Gas price too high, transaction aborted")
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("1000000000000000000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("Gas price"));
    }

    @Test
    public void testUpdateTotalScala_ErrorResponse_ScalaOutOfRange() {
        // Test server returns error: scala out of range
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(400)
                        .setErrorMsg("Scala value out of valid range")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("999999999999");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("out of"));
    }

    @Test
    public void testUpdateTotalScala_ErrorResponse_UpdateTooFrequent() {
        // Test server returns error: update too frequent
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(429)
                        .setErrorMsg("Update too frequent, please wait")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("1000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("frequent"));
    }

    @Test
    public void testUpdateTotalScala_ErrorResponse_ContractCallFailed() {
        // Test server returns error: contract call failed
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(500)
                        .setErrorMsg("Contract call reverted")
                        .build()
        );

        var result = oracleCommands.updateTotalScala("1000");
        Assert.assertTrue(result.toString().contains("failed"));
        Assert.assertTrue(result.toString().contains("reverted"));
    }

    // ==================== Empty/Null Response Tests ====================

    @Test
    public void testWithdrawFromVault_EmptyTxHash() {
        // Test server returns empty transaction hash
        when(mockStub.withdrawFromVault(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setWithdrawFromVaultResp(
                                WithdrawFromVaultResp.newBuilder()
                                        .setTxHash("")
                                        .build()
                        )
                        .build()
        );

        var result = oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 100);
        Assert.assertNotNull(result);
        Assert.assertEquals("", result);
    }

    @Test
    public void testUpdateFixedProfit_EmptyTxHash() {
        // Test server returns empty transaction hash
        when(mockStub.updateFixedProfit(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setUpdateFixedProfitResp(
                                UpdateFixedProfitResp.newBuilder()
                                        .setTxHash("")
                                        .build()
                        )
                        .build()
        );

        var result = oracleCommands.updateFixedProfit("1000000000000000000");
        Assert.assertNotNull(result);
        Assert.assertEquals("", result);
    }

    @Test
    public void testUpdateTotalScala_EmptyTxHash() {
        // Test server returns empty transaction hash
        when(mockStub.updateTotalScala(any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setUpdateTotalScalaResp(
                                UpdateTotalScalaResp.newBuilder()
                                        .setTxHash("")
                                        .build()
                        )
                        .build()
        );

        var result = oracleCommands.updateTotalScala("1000");
        Assert.assertNotNull(result);
        Assert.assertEquals("", result);
    }

    // ==================== Unexpected Exception Tests ====================

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_UnexpectedException_NullPointer() {
        // Test unexpected NullPointerException
        when(mockStub.withdrawFromVault(any())).thenThrow(new NullPointerException("Unexpected null"));

        oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 100);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateFixedProfit_UnexpectedException_IllegalArgument() {
        // Test unexpected IllegalArgumentException
        when(mockStub.updateFixedProfit(any())).thenThrow(new IllegalArgumentException("Invalid argument"));

        oracleCommands.updateFixedProfit("1000000000000000000");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateTotalScala_UnexpectedException_IllegalState() {
        // Test unexpected IllegalStateException
        when(mockStub.updateTotalScala(any())).thenThrow(new IllegalStateException("Invalid state"));

        oracleCommands.updateTotalScala("1000");
    }

    @Test(expected = RuntimeException.class)
    public void testWithdrawFromVault_UnexpectedException_ArithmeticError() {
        // Test unexpected ArithmeticException
        when(mockStub.withdrawFromVault(any())).thenThrow(new ArithmeticException("Arithmetic error"));

        oracleCommands.withdrawFromVault("0x1234567890123456789012345678901234567890", 100);
    }
}
