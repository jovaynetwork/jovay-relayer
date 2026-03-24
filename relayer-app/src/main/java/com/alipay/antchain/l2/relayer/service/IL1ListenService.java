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

import java.io.IOException;

/**
 * L1 listen service interface for monitoring and processing L1 blockchain events.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Polling L1 blocks to detect new L1 messages from the Mailbox contract</li>
 *     <li>Extracting and storing L1 messages for subsequent processing</li>
 *     <li>Collecting L1 block fee information for oracle updates</li>
 * </ul>
 * </p>
 */
public interface IL1ListenService {

    /**
     * Poll and process a batch of L1 messages from L1 blockchain.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves the latest finalized L1 block</li>
     *     <li>Scans L1 blocks from the last processed block to the latest block (up to max polling limit)</li>
     *     <li>Extracts L1 messages (L1Msg) from the Mailbox contract events</li>
     *     <li>Saves L1 messages to the repository for later relay to L2</li>
     *     <li>Records L1 block fee information (base fee, gas usage, blob gas, etc.) for oracle service</li>
     * </ul>
     * </p>
     *
     * @throws IOException if there is an error communicating with the L1 blockchain
     */
    void pollL1MsgBatch() throws IOException;
}
