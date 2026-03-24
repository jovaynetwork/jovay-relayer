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

import java.math.BigInteger;

/**
 * Mailbox service interface for handling cross-chain message relay between L1 and L2.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Forwarding L1 messages (L1Msg) from L1 to L2, such as deposit transactions</li>
 *     <li>Generating and recording Merkle proofs for L2 messages (L2Msg) sent from L2 to L1, such as withdrawal transactions</li>
 * </ul>
 * </p>
 */
public interface IMailboxService {

    /**
     * Initialize the mailbox service with the starting L1 block number.
     * <p>
     * This method sets up the initial state for the mailbox service to begin
     * scanning and processing L1 messages from the specified block height.
     * </p>
     *
     * @param startBlockNumber the L1 block number to start scanning from
     * @throws com.alipay.antchain.l2.relayer.commons.exceptions.InitMailboxServiceException if the service has already been initialized
     */
    void initService(BigInteger startBlockNumber);

    /**
     * Process a batch of L1 messages.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves pending and ready L1 messages from the repository</li>
     *     <li>Checks if messages have been finalized on L2</li>
     *     <li>Sends ready L1 messages to L2 network</li>
     *     <li>Updates message states accordingly</li>
     * </ul>
     * </p>
     */
    void processL1MsgBatch();

    /**
     * Generate Merkle proofs for L2 messages in the next batch.
     * <p>
     * This method:
     * <ul>
     *     <li>Identifies the next batch that needs L2 message proof generation</li>
     *     <li>Verifies the batch is ready (TEE or ZK proof has been committed)</li>
     *     <li>Aggregates L2 messages and generates Merkle proofs</li>
     *     <li>Saves the proofs to the repository for external query (e.g., withdrawal process)</li>
     * </ul>
     * </p>
     */
    void proveL2Msg();
}
