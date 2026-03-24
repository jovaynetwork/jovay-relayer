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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.SendTxResult;
import org.web3j.crypto.Blob;

/**
 * Transaction manager interface for sending and managing blockchain transactions.
 * <p>
 * This interface provides methods to:
 * <ul>
 *     <li>Send regular transactions (EIP-1559)</li>
 *     <li>Send blob transactions (EIP-4844)</li>
 *     <li>Manage transaction nonces</li>
 *     <li>Handle transaction signing and submission</li>
 * </ul>
 * </p>
 */
public interface ITransactionManager {

    /**
     * Prefix for transaction send lock keys in distributed storage.
     */
    String RELAYER_ETH_SEND_TX_KEY_PREFIX = "RELAYER_ETH_SEND_TX@";

    /**
     * Get the distributed lock key for transaction sending.
     * <p>
     * This lock ensures that only one thread/process can send transactions for an account
     * at a time, preventing nonce conflicts.
     * </p>
     *
     * @param chainId the blockchain chain ID
     * @param account the account address
     * @return the lock key string
     */
    default String getSendTxLockKey(long chainId, String account) {
        return StrUtil.format("{}lock@{}-{}", RELAYER_ETH_SEND_TX_KEY_PREFIX, chainId, account);
    }

    /**
     * Send a transaction with automatic nonce management.
     * <p>
     * The nonce will be automatically obtained from the nonce manager.
     * </p>
     *
     * @param gasPrice    the gas price parameters (EIP-1559)
     * @param gasLimit    the gas limit
     * @param to          the recipient address
     * @param data        the transaction data
     * @param value       the value to send in wei
     * @param constructor whether this is a contract constructor call
     * @return the transaction send result
     * @throws IOException if there is an I/O error during transaction sending
     */
    SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException;

    /**
     * Send a transaction with explicit nonce.
     * <p>
     * This allows manual control of the transaction nonce.
     * </p>
     *
     * @param gasPrice    the gas price parameters (EIP-1559)
     * @param gasLimit    the gas limit
     * @param to          the recipient address
     * @param data        the transaction data
     * @param nonce       the transaction nonce
     * @param value       the value to send in wei
     * @param constructor whether this is a contract constructor call
     * @return the transaction send result
     * @throws IOException if there is an I/O error during transaction sending
     */
    SendTxResult sendTx(IGasPrice gasPrice, BigInteger gasLimit, String to, String data, BigInteger nonce, BigInteger value, boolean constructor) throws IOException;

    /**
     * Send a blob transaction (EIP-4844) with automatic nonce management.
     * <p>
     * This is used for submitting transactions with blob data for data availability.
     * </p>
     *
     * @param blobs    the list of blobs to include in the transaction
     * @param gasPrice the gas price parameters including blob gas price
     * @param gasLimit the gas limit
     * @param to       the recipient address
     * @param value    the value to send in wei
     * @param data     the transaction data
     * @return the transaction send result
     * @throws IOException if there is an I/O error during transaction sending
     */
    SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data
    ) throws IOException;

    /**
     * Send a blob transaction (EIP-4844) with explicit nonce.
     * <p>
     * This allows manual control of the transaction nonce for blob transactions.
     * </p>
     *
     * @param blobs    the list of blobs to include in the transaction
     * @param gasPrice the gas price parameters including blob gas price
     * @param gasLimit the gas limit
     * @param to       the recipient address
     * @param nonce    the transaction nonce
     * @param value    the value to send in wei
     * @param data     the transaction data
     * @return the transaction send result
     * @throws IOException if there is an I/O error during transaction sending
     */
    SendTxResult sendTx(
            List<Blob> blobs,
            IGasPrice gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger nonce,
            BigInteger value,
            String data
    ) throws IOException;

    /**
     * Get the address of the account managed by this transaction manager.
     *
     * @return the account address
     */
    String getAddress();

    /**
     * Get the nonce manager associated with this transaction manager.
     *
     * @return the nonce manager instance
     */
    INonceManager getNonceManager();
}
