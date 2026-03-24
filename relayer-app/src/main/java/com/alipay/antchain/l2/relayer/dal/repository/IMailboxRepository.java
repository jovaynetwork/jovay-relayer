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

package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;

/**
 * Repository interface for managing cross-chain message mailbox operations.
 * <p>This interface provides methods for handling inter-blockchain messages,
 * including message storage, state management, and proof data handling. It serves
 * as the data layer for the mailbox system that facilitates communication between
 * L1 and L2 chains.</p>
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Message queue management with state tracking (ready, pending, etc.)</li>
 *   <li>Message proof generation and storage for cross-chain verification</li>
 *   <li>Batch message operations for efficient processing</li>
 *   <li>Support for both L1-to-L2 and L2-to-L1 message types</li>
 * </ul>
 */
public interface IMailboxRepository {

    /**
     * Retrieves ready-to-process messages of a specific type.
     * <p>This method fetches messages that are in the "ready" state and can be
     * processed immediately. Messages are returned in order up to the specified
     * batch size for efficient batch processing.</p>
     *
     * @param type the type of inter-blockchain message (L1-to-L2 or L2-to-L1)
     * @param batchSize the maximum number of messages to retrieve
     * @return a list of ready inter-blockchain messages
     */
    List<InterBlockchainMessageDO> peekReadyMessages(InterBlockchainMessageTypeEnum type, int batchSize);

    /**
     * Retrieves pending messages of a specific type.
     * <p>This method fetches messages that are in the "pending" state, typically
     * waiting for confirmation or further processing. This is useful for monitoring
     * and retry mechanisms.</p>
     *
     * @param type the type of inter-blockchain message (L1-to-L2 or L2-to-L1)
     * @param batchSize the maximum number of messages to retrieve
     * @return a list of pending inter-blockchain messages
     */
    List<InterBlockchainMessageDO> peekPendingMessages(InterBlockchainMessageTypeEnum type, int batchSize);

    /**
     * Saves multiple inter-blockchain messages in batch.
     * <p>This method efficiently persists multiple messages to storage in a single
     * operation. This is typically used when receiving or generating multiple
     * cross-chain messages.</p>
     *
     * @param messageDOS a list of inter-blockchain message data objects to save
     */
    void saveMessages(List<InterBlockchainMessageDO> messageDOS);

    /**
     * Updates the state of a specific message.
     * <p>This method changes the processing state of a message identified by its
     * type and nonce. State transitions track the message lifecycle from creation
     * through processing to completion or failure.</p>
     *
     * @param type the type of inter-blockchain message
     * @param nonce the unique nonce identifying the message
     * @param state the new state to set for the message
     */
    void updateMessageState(InterBlockchainMessageTypeEnum type, long nonce, InterBlockchainMessageStateEnum state);

    /**
     * Retrieves a specific inter-blockchain message.
     * <p>This method looks up a message by its type and nonce, returning the
     * complete message data including content, state, and metadata.</p>
     *
     * @param type the type of inter-blockchain message
     * @param nonce the unique nonce identifying the message
     * @return the inter-blockchain message data object, or null if not found
     */
    InterBlockchainMessageDO getMessage(InterBlockchainMessageTypeEnum type, long nonce);

    /**
     * Saves multiple L2 message proofs in batch.
     * <p>This method stores cryptographic proofs for L2 messages, indexed by their
     * message nonce. These proofs are essential for verifying message authenticity
     * and inclusion when relaying messages to L1.</p>
     *
     * @param proofs a map of message nonces to their corresponding proof data
     */
    void saveL2MsgProofs(Map<BigInteger, byte[]> proofs);

    /**
     * Retrieves the proof data for a specific L2 message.
     * <p>This method fetches the cryptographic proof associated with an L2 message,
     * which can be used to verify the message's inclusion in the L2 state when
     * submitting it to L1.</p>
     *
     * @param msgNonce the nonce of the L2 message
     * @return the L2 message proof data, or null if not found
     */
    L2MsgProofData getL2MsgProof(BigInteger msgNonce);

    /**
     * Retrieves message hashes for a specific batch.
     * <p>This method returns the cryptographic hashes of all messages of a given
     * type that are included in a specific batch. These hashes are used for
     * batch verification and Merkle tree construction.</p>
     *
     * @param type the type of inter-blockchain message
     * @param batchIndex the batch index to query
     * @return a list of message hashes in the specified batch
     */
    List<byte[]> getMsgHashes(InterBlockchainMessageTypeEnum type, BigInteger batchIndex);
}
