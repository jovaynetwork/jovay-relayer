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
import java.util.regex.Pattern;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/**
 * Interface for checking and handling nonce reset scenarios in blockchain transactions.
 * <p>
 * This interface provides mechanisms to detect when transaction nonces need to be reset
 * due to various blockchain events such as:
 * <ul>
 *   <li>Chain reorganizations (reorgs)</li>
 *   <li>Nonce gaps in the blob pool</li>
 *   <li>Transaction pool evictions</li>
 *   <li>Node restarts or state changes</li>
 * </ul>
 * </p>
 * <p>
 * Different blockchain implementations (Ethereum Geth, JOVAY, etc.) may have different
 * nonce reset behaviors and error patterns. Implementations of this interface provide
 * chain-specific logic for detecting and handling these scenarios.
 * </p>
 * <p>
 * The interface supports parsing nonce information from error messages, particularly
 * for EIP-4844 blob pool transactions where nonce gaps can occur.
 * </p>
 *
 * @see CachedNonceManager for usage in cached nonce management
 * @see NonceResetChecker for concrete implementations
 */
public interface INonceResetChecker {

    /**
     * Regular expression pattern for parsing blob pool nonce error messages.
     * <p>
     * This pattern matches error messages in the format:
     * "nonce too high: tx nonce {txNonce}, gapped nonce {gappedNonce}"
     * </p>
     * <p>
     * The pattern captures two groups:
     * <ul>
     *   <li>Group 1: The transaction nonce that was attempted</li>
     *   <li>Group 2: The expected gapped nonce (the actual next valid nonce)</li>
     * </ul>
     * </p>
     */
    Pattern BLOB_POOL_NONCE_TOO_HIGHT_PATTERN = Pattern.compile("nonce too high: tx nonce (\\d+), gapped nonce (\\d+)");

    /**
     * Checks if a nonce reset is required based on the transaction result.
     * <p>
     * This method analyzes the transaction send result to determine if the nonce
     * needs to be reset. It examines error messages, transaction status, and
     * potentially queries the blockchain state to make this determination.
     * </p>
     * <p>
     * Implementations should handle chain-specific error patterns and behaviors.
     * For example, Ethereum Geth and JOVAY may have different error message formats
     * or nonce management strategies.
     * </p>
     *
     * @param result           the transaction send result from the blockchain node
     * @param rollupRepository the repository for accessing rollup data and transaction history
     * @param chainType        the type of chain (LAYER_ONE or LAYER_TWO)
     * @param account          the account address whose nonce is being checked
     * @return {@code true} if the nonce should be reset, {@code false} otherwise
     */
    boolean check(EthSendTransaction result, IRollupRepository rollupRepository, ChainTypeEnum chainType, String account);

    /**
     * Parses nonce information from a blob pool error message.
     * <p>
     * This default method extracts the transaction nonce and gapped nonce from
     * error messages that match the blob pool nonce error pattern. This is particularly
     * useful for EIP-4844 blob transactions where nonce gaps can occur in the blob pool.
     * </p>
     * <p>
     * The method returns an array containing:
     * <ul>
     *   <li>Index 0: The transaction nonce that was attempted</li>
     *   <li>Index 1: The gapped nonce (the actual next valid nonce)</li>
     * </ul>
     * </p>
     *
     * @param errorMessage the error message from the transaction send result
     * @return an array of two BigIntegers [txNonce, gappedNonce], or {@code null} if
     *         the error message doesn't match the expected pattern or is null
     */
    default BigInteger[] parseNonceFromError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        var matcher = BLOB_POOL_NONCE_TOO_HIGHT_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            var txNonce = new BigInteger(matcher.group(1));
            var gappedNonce = new BigInteger(matcher.group(2));
            return new BigInteger[]{txNonce, gappedNonce};
        }
        return null;
    }
}
