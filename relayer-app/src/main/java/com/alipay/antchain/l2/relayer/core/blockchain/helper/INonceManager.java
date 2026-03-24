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

import cn.hutool.core.util.StrUtil;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/**
 * Interface for managing transaction nonces, specifically designed for Ethereum-like blockchains.
 * <p>
 * This interface defines the contract for handling nonce lifecycle management, including:
 * <ul>
 *     <li>Generating distributed lock and storage keys.</li>
 *     <li>Retrieving the next valid nonce for a transaction.</li>
 *     <li>Manually incrementing the nonce.</li>
 *     <li>Querying the current local nonce.</li>
 *     <li>Determining if a nonce reset is required based on transaction results.</li>
 *     <li>Resetting the local nonce to match the on-chain state.</li>
 * </ul>
 * </p>
 */
public interface INonceManager {

    /**
     * The key prefix used for nonce-related entries in the distributed storage.
     */
    String RELAYER_ETH_NONCE_KEY_PREFIX = "RELAYER_ETH_NONCE@";

    /**
     * Generates the distributed lock key for nonce operations.
     * <p>
     * This key is used to acquire a lock ensuring that nonce updates for a specific account
     * on a specific chain are serialized, preventing race conditions.
     * </p>
     *
     * @param chainId the ID of the blockchain network.
     * @param account the address of the account.
     * @return the formatted lock key string.
     */
    default String getEthNonceLockKey(long chainId, String account) {
        return StrUtil.format("{}lock@{}-{}", RELAYER_ETH_NONCE_KEY_PREFIX, chainId, account);
    }

    /**
     * Generates the storage key for the nonce value.
     * <p>
     * This key identifies the location where the current nonce for a specific account
     * on a specific chain is stored.
     * </p>
     *
     * @param chainId the ID of the blockchain network.
     * @param account the address of the account.
     * @return the formatted value key string.
     */
    default String getEthNonceValKey(long chainId, String account) {
        return StrUtil.format("{}val@{}-{}", RELAYER_ETH_NONCE_KEY_PREFIX, chainId, account);
    }

    /**
     * Retrieves the next available nonce for a new transaction.
     * <p>
     * This method should return the value to be used for the next transaction, ensuring strict ordering.
     * </p>
     *
     * @return the next nonce as a {@link BigInteger}.
     */
    BigInteger getNextNonce();

    /**
     * Manually increments the current local nonce.
     * <p>
     * This is typically used when a transaction has been successfully submitted or confirmed,
     * and the local state needs to be advanced explicitly.
     * Note: Support for this operation depends on the specific implementation.
     * </p>
     */
    void incrementNonce();

    /**
     * Determines whether the local nonce should be reset based on the transaction submission result.
     * <p>
     * This method inspects the {@link EthSendTransaction} response (e.g., checking for "nonce too low" errors)
     * to decide if the local nonce cache has become desynchronized with the chain.
     * </p>
     *
     * @param result the result of the Ethereum transaction submission.
     * @return {@code true} if the nonce should be reset; {@code false} otherwise.
     */
    boolean ifResetNonce(EthSendTransaction result);

    /**
     * Resets the local nonce cache by synchronizing with the on-chain state.
     * <p>
     * This method typically queries the blockchain node for the actual transaction count
     * of the account and updates the local storage to match it.
     * </p>
     */
    void resetNonce();
}