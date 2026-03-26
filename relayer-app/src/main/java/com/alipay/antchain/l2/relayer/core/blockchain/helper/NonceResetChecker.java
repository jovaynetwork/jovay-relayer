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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/**
 * Enum providing concrete implementations of nonce reset checking for different blockchain types.
 * <p>
 * This enum implements {@link INonceResetChecker} and provides chain-specific logic for
 * detecting when transaction nonces need to be reset. Each enum constant represents a
 * different blockchain implementation with its own error patterns and reset detection logic.
 * </p>
 * <p>
 * Supported blockchain types:
 * <ul>
 *   <li><b>ETHEREUM_GETH</b>: For Ethereum networks running Geth client, handles both
 *       "nonce too low" errors and blob pool nonce gap scenarios</li>
 *   <li><b>JOVAY</b>: For JOVAY blockchain, uses error code-based detection</li>
 * </ul>
 * </p>
 *
 * @see INonceResetChecker for the interface definition
 * @see CachedNonceManager for usage in nonce management
 */
@Slf4j
public enum NonceResetChecker implements INonceResetChecker {

    /**
     * Nonce reset checker for Ethereum Geth client.
     * <p>
     * This implementation handles two types of nonce-related errors from Ethereum Geth:
     * </p>
     * <ol>
     *   <li><b>Nonce too low</b>: Occurs when the submitted transaction nonce is lower than
     *       the account's current nonce on-chain. This typically happens after chain reorgs
     *       or when the local nonce cache is stale. Error code: -32000</li>
     *   <li><b>Blob pool nonce gap</b>: Specific to EIP-4844 blob transactions, occurs when
     *       there's a gap in the blob pool nonce sequence. The checker parses the error message
     *       to extract the gapped nonce and compares it with the local storage to determine
     *       if a reset is needed.</li>
     * </ol>
     */
    ETHEREUM_GETH {
        @Override
        public boolean check(EthSendTransaction result, IRollupRepository rollupRepository, ChainTypeEnum chainType, String account) {
            // Check for "nonce too low" error from Ethereum Geth
            if (result.getError().getCode() == -32000 && StrUtil.containsAny(result.getError().getMessage(), "nonce too low")) {
                return true;
            }

            // Check for blob pool nonce gap error
            var msgMatcher = BLOB_POOL_NONCE_TOO_HIGHT_PATTERN.matcher(result.getError().getMessage());
            if (msgMatcher.find()) {
                log.warn("rpc call to send blob tx returns that nonce too high: {}", result.getError().getMessage());
                var nonces = parseNonceFromError(result.getError().getMessage());
                if (ObjectUtil.isEmpty(nonces)) {
                    return false;
                }
                log.info("parsed nonces from error message: tx nonce {}, gapped nonce {}", nonces[0], nonces[1]);
                var latestNonce = rollupRepository.queryLatestNonce(chainType, account);
                log.info("latest nonce from local storage: {}", latestNonce);
                // Reset if local nonce is behind the gapped nonce
                return latestNonce.compareTo(nonces[1]) < 0;
            }
            return false;
        }
    },

    /**
     * Nonce reset checker for JOVAY blockchain.
     * <p>
     * This implementation uses a simple error code-based detection mechanism.
     * When the JOVAY node returns error code 112, it indicates that the nonce
     * is out of range and needs to be reset.
     * </p>
     * <p>
     * Error code 112 typically indicates:
     * <ul>
     *   <li>The submitted nonce is too far ahead or behind the expected nonce</li>
     *   <li>The transaction pool has been cleared or reset</li>
     *   <li>The node has restarted and lost pending transaction state</li>
     * </ul>
     * </p>
     */
    JOVAY {
        @Override
        public boolean check(EthSendTransaction result, IRollupRepository rollupRepository, ChainTypeEnum chainType, String account) {
            // Check for JOVAY-specific "nonce out of range" error code
            return result.getError().getCode() == 112;
        }
    }
}
