package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.io.IOException;
import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.TxSimulateException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class EstimateGasLimitProviderTest {

    @Mock
    private Web3j web3j;

    @Mock
    @SuppressWarnings("rawtypes")
    private Request ethEstimateGasRequest;

    @Mock
    private EthEstimateGas ethEstimateGas;

    private EstimateGasLimitProvider provider;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        provider = EstimateGasLimitProvider.builder()
                .web3j(web3j)
                .fromAddress("0x1234567890123456789012345678901234567890")
                .toAddress("0x0987654321098765432109876543210987654321")
                .dataHex("0xabcdef")
                .extraGasLimit(BigInteger.valueOf(10000))
                .build();
    }

    /**
     * Test: Gas estimation returns error
     * Scenario: ethEstimateGas.hasError() returns true
     * Expected: Throws TxSimulateException
     */
    @Test
    public void testGetGasLimit_EstimateGasError() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(true);
        
        org.web3j.protocol.core.Response.Error error = new org.web3j.protocol.core.Response.Error();
        error.setCode(-32000);
        error.setMessage("execution reverted");
        when(ethEstimateGas.getError()).thenReturn(error);

        // Act & Assert
        TxSimulateException exception = Assert.assertThrows(
                TxSimulateException.class,
                () -> provider.getGasLimit()
        );
        
        Assert.assertNotNull(exception.getMessage());
        Assert.assertTrue(exception.getMessage().contains("-32000"));
        Assert.assertTrue(exception.getMessage().contains("execution reverted"));
    }

    /**
     * Test: Gas estimation network IO exception
     * Scenario: send() throws IOException
     * Expected: Throws RuntimeException (wrapped by @SneakyThrows)
     */
    @Test
    public void testGetGasLimit_IOException() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenThrow(new IOException("Network error"));

        // Act & Assert
        IOException exception = Assert.assertThrows(
                IOException.class,
                () -> provider.getGasLimit()
        );
        Assert.assertEquals("Network error", exception.getMessage());
    }

    /**
     * Test: Gas estimation returns null
     * Scenario: ethEstimateGas.getAmountUsed() returns null
     * Expected: Throws NullPointerException
     */
    @Test
    public void testGetGasLimit_NullAmountUsed() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(false);
        when(ethEstimateGas.getAmountUsed()).thenReturn(null);

        // Act & Assert
        Assert.assertThrows(
                NullPointerException.class,
                () -> provider.getGasLimit()
        );
    }

    /**
     * Test: Gas estimation contract function call failure
     * Scenario: Using "deploy" as contractFunc parameter, but estimation fails
     * Expected: Throws TxSimulateException
     */
    @Test
    public void testGetGasLimit_DeployContractError() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(true);
        
        org.web3j.protocol.core.Response.Error error = new org.web3j.protocol.core.Response.Error();
        error.setCode(-32000);
        error.setMessage("insufficient funds for gas * price + value");
        when(ethEstimateGas.getError()).thenReturn(error);

        // Act & Assert
        TxSimulateException exception = Assert.assertThrows(
                TxSimulateException.class,
                () -> provider.getGasLimit("deploy")
        );
        
        Assert.assertNotNull(exception.getMessage());
        Assert.assertTrue(exception.getMessage().contains("insufficient funds for gas * price + value"));
    }

    /**
     * Test: Gas estimation exceeds gas limit
     * Scenario: Contract execution requires more gas than block gas limit
     * Expected: Throws TxSimulateException
     */
    @Test
    public void testGetGasLimit_GasLimitExceeded() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(true);
        
        org.web3j.protocol.core.Response.Error error = new org.web3j.protocol.core.Response.Error();
        error.setCode(-32000);
        error.setMessage("gas required exceeds allowance");
        when(ethEstimateGas.getError()).thenReturn(error);

        // Act & Assert
        TxSimulateException exception = Assert.assertThrows(
                TxSimulateException.class,
                () -> provider.getGasLimit("transfer")
        );
        
        Assert.assertTrue(exception.getMessage().contains("gas required exceeds allowance"));
    }

    /**
     * Test: Gas estimation contract revert
     * Scenario: Contract execution encounters revert
     * Expected: Throws TxSimulateException
     */
    @Test
    public void testGetGasLimit_ContractRevert() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(true);
        
        org.web3j.protocol.core.Response.Error error = new org.web3j.protocol.core.Response.Error();
        error.setCode(-32000);
        error.setMessage("execution reverted: custom error message");
        error.setData("0x08c379a0...");
        when(ethEstimateGas.getError()).thenReturn(error);

        // Act & Assert
        TxSimulateException exception = Assert.assertThrows(
                TxSimulateException.class,
                () -> provider.getGasLimit()
        );
        
        Assert.assertTrue(exception.getMessage().contains("execution reverted: custom error message"));
    }

    /**
     * Test: Gas estimation with invalid contract address
     * Scenario: toAddress is an invalid contract address
     * Expected: Throws TxSimulateException
     */
    @Test
    public void testGetGasLimit_InvalidContractAddress() throws IOException {
        // Arrange
        doReturn(ethEstimateGasRequest).when(web3j).ethEstimateGas(any());
        when(ethEstimateGasRequest.send()).thenReturn(ethEstimateGas);
        when(ethEstimateGas.hasError()).thenReturn(true);
        
        org.web3j.protocol.core.Response.Error error = new org.web3j.protocol.core.Response.Error();
        error.setCode(-32000);
        error.setMessage("invalid address");
        when(ethEstimateGas.getError()).thenReturn(error);

        // Act & Assert
        TxSimulateException exception = Assert.assertThrows(
                TxSimulateException.class,
                () -> provider.getGasLimit()
        );
        
        Assert.assertTrue(exception.getMessage().contains("invalid address"));
    }
}
