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

package com.alipay.antchain.l2.relayer.service;

/**
 * Reliable transaction service interface for tracking and managing transaction lifecycle.
 * <p>
 * This service ensures that all transactions sent by the Relayer are eventually committed on-chain
 * by monitoring their status and taking appropriate actions (resend, speed up, etc.) when needed.
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 *     <li>Tracking pending transactions on both L1 and L2</li>
 *     <li>Detecting transaction timeout and performing speed-up operations</li>
 *     <li>Handling transaction failures and retrying when appropriate</li>
 *     <li>Updating transaction states based on on-chain confirmation</li>
 * </ul>
 * </p>
 */
public interface IReliableTxService {

    /**
     * Process not-finalized transactions on L1.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves pending L1 transactions from the repository</li>
     *     <li>Checks if transactions are packaged on-chain</li>
     *     <li>For missing transactions: resends the raw signed transaction</li>
     *     <li>For timeout transactions: speeds up by increasing gas price</li>
     *     <li>For confirmed transactions: updates state and creates oracle gas feed requests</li>
     *     <li>Handles transaction receipts and updates final states (success/failed)</li>
     * </ul>
     * </p>
     */
    void processL1NotFinalizedTx();

    /**
     * Process not-finalized transactions on L2.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves pending L2 transactions from the repository</li>
     *     <li>Checks if transactions are packaged on-chain</li>
     *     <li>For missing transactions: resends the raw signed transaction</li>
     *     <li>For confirmed transactions: updates state based on receipt status</li>
     * </ul>
     * </p>
     */
    void processL2NotFinalizedTx();

    /**
     * Retry failed transactions on both L1 and L2.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves failed transactions that haven't exceeded retry limit</li>
     *     <li>Sorts transactions by batch index and transaction type</li>
     *     <li>Checks if retry conditions are met (e.g., previous transaction succeeded)</li>
     *     <li>Resends transactions and updates retry count</li>
     *     <li>Updates state to pending if retry is successful</li>
     * </ul>
     * </p>
     */
    void retryFailedTx();
}
