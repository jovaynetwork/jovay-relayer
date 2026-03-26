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

package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NonceResetChecker enum implementations.
 * <p>
 * This test class verifies the nonce reset detection logic for different blockchain types:
 * <ul>
 *   <li>ETHEREUM_GETH: Tests "nonce too low" and blob pool nonce gap scenarios</li>
 *   <li>JOVAY: Tests error code-based nonce reset detection</li>
 * </ul>
 * </p>
 * <p>
 * This is a pure unit test that doesn't require Spring context, using Mockito for mocking.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class NonceResetCheckerTest {

    @Mock
    private IRollupRepository rollupRepository;

    private static final String TEST_ACCOUNT = "0x1234567890123456789012345678901234567890";

    /**
     * Tests ETHEREUM_GETH checker with "nonce too low" error.
     * Should return true when error code is -32000 and message contains "nonce too low".
     */
    @Test
    public void testEthereumGeth_NonceTooLow() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32000);
        error.setMessage("nonce too low");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertTrue("Should reset nonce when error is 'nonce too low'", shouldReset);
    }

    /**
     * Tests ETHEREUM_GETH checker with blob pool nonce gap error when local nonce is behind.
     * Should return true when local nonce is less than the gapped nonce.
     */
    @Test
    public void testEthereumGeth_BlobPoolNonceGap_LocalNonceBehind() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32000);
        error.setMessage("nonce too high: tx nonce 105, gapped nonce 100");
        result.setError(error);

        // Mock local nonce is 95, which is less than gapped nonce 100
        when(rollupRepository.queryLatestNonce(eq(ChainTypeEnum.LAYER_ONE), eq(TEST_ACCOUNT)))
                .thenReturn(BigInteger.valueOf(95));

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertTrue("Should reset nonce when local nonce is behind gapped nonce", shouldReset);
    }

    /**
     * Tests ETHEREUM_GETH checker with blob pool nonce gap error when local nonce is ahead.
     * Should return false when local nonce is greater than or equal to the gapped nonce.
     */
    @Test
    public void testEthereumGeth_BlobPoolNonceGap_LocalNonceAhead() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32000);
        error.setMessage("nonce too high: tx nonce 105, gapped nonce 100");
        result.setError(error);

        // Mock local nonce is 100, which equals gapped nonce
        when(rollupRepository.queryLatestNonce(eq(ChainTypeEnum.LAYER_ONE), eq(TEST_ACCOUNT)))
                .thenReturn(BigInteger.valueOf(100));

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertFalse("Should not reset nonce when local nonce is ahead or equal to gapped nonce", shouldReset);
    }

    /**
     * Tests ETHEREUM_GETH checker with blob pool nonce gap error but invalid message format.
     * Should return false when error message doesn't match the expected pattern.
     */
    @Test
    public void testEthereumGeth_BlobPoolNonceGap_InvalidMessageFormat() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32000);
        error.setMessage("nonce too high: invalid format");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertFalse("Should not reset nonce when error message format is invalid", shouldReset);
    }

    /**
     * Tests ETHEREUM_GETH checker with unrelated error.
     * Should return false for errors that don't match nonce reset patterns.
     */
    @Test
    public void testEthereumGeth_UnrelatedError() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32001);
        error.setMessage("insufficient funds");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertFalse("Should not reset nonce for unrelated errors", shouldReset);
    }

    /**
     * Tests JOVAY checker with error code 112 (nonce out of range).
     * Should return true when error code is 112.
     */
    @Test
    public void testJovay_NonceOutOfRange() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(112);
        error.setMessage("nonce out of range");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.JOVAY.check(
                result, rollupRepository, ChainTypeEnum.LAYER_TWO, TEST_ACCOUNT
        );

        // Assert
        assertTrue("Should reset nonce when JOVAY returns error code 112", shouldReset);
    }

    /**
     * Tests JOVAY checker with different error code.
     * Should return false for error codes other than 112.
     */
    @Test
    public void testJovay_OtherErrorCode() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(111);
        error.setMessage("some other error");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.JOVAY.check(
                result, rollupRepository, ChainTypeEnum.LAYER_TWO, TEST_ACCOUNT
        );

        // Assert
        assertFalse("Should not reset nonce for error codes other than 112", shouldReset);
    }

    /**
     * Tests the parseNonceFromError method with valid error message.
     * Should correctly extract tx nonce and gapped nonce from error message.
     */
    @Test
    public void testParseNonceFromError_ValidMessage() {
        // Arrange
        String errorMessage = "nonce too high: tx nonce 105, gapped nonce 100";

        // Act
        BigInteger[] nonces = NonceResetChecker.ETHEREUM_GETH.parseNonceFromError(errorMessage);

        // Assert
        assertNotNull("Should parse nonces from valid error message", nonces);
        assertEquals("Should extract correct tx nonce", BigInteger.valueOf(105), nonces[0]);
        assertEquals("Should extract correct gapped nonce", BigInteger.valueOf(100), nonces[1]);
    }

    /**
     * Tests the parseNonceFromError method with invalid error message.
     * Should return null when error message doesn't match the pattern.
     */
    @Test
    public void testParseNonceFromError_InvalidMessage() {
        // Arrange
        String errorMessage = "some other error message";

        // Act
        BigInteger[] nonces = NonceResetChecker.ETHEREUM_GETH.parseNonceFromError(errorMessage);

        // Assert
        assertNull("Should return null for invalid error message format", nonces);
    }

    /**
     * Tests the parseNonceFromError method with null error message.
     * Should return null when error message is null.
     */
    @Test
    public void testParseNonceFromError_NullMessage() {
        // Act
        BigInteger[] nonces = NonceResetChecker.ETHEREUM_GETH.parseNonceFromError(null);

        // Assert
        assertNull("Should return null for null error message", nonces);
    }

    /**
     * Tests the parseNonceFromError method with large nonce values.
     * Should correctly handle large BigInteger values.
     */
    @Test
    public void testParseNonceFromError_LargeNonces() {
        // Arrange
        String errorMessage = "nonce too high: tx nonce 999999999999999999, gapped nonce 888888888888888888";

        // Act
        BigInteger[] nonces = NonceResetChecker.ETHEREUM_GETH.parseNonceFromError(errorMessage);

        // Assert
        assertNotNull("Should parse large nonces from error message", nonces);
        assertEquals("Should extract correct large tx nonce", 
                new BigInteger("999999999999999999"), nonces[0]);
        assertEquals("Should extract correct large gapped nonce", 
                new BigInteger("888888888888888888"), nonces[1]);
    }

    /**
     * Tests ETHEREUM_GETH checker with error code -32000 but without "nonce too low" message.
     * Should return false when error code matches but message doesn't contain the key phrase.
     */
    @Test
    public void testEthereumGeth_ErrorCode32000_WithoutNonceTooLowMessage() {
        // Arrange
        EthSendTransaction result = new EthSendTransaction();
        Response.Error error = new Response.Error();
        error.setCode(-32000);
        error.setMessage("replacement transaction underpriced");
        result.setError(error);

        // Act
        boolean shouldReset = NonceResetChecker.ETHEREUM_GETH.check(
                result, rollupRepository, ChainTypeEnum.LAYER_ONE, TEST_ACCOUNT
        );

        // Assert
        assertFalse("Should not reset nonce when error code is -32000 but message doesn't contain 'nonce too low'", 
                shouldReset);
    }
}
