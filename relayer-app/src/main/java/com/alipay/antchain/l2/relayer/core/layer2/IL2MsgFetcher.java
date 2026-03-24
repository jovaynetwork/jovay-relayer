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

package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;

import com.alipay.antchain.l2.trace.BasicBlockTrace;

/**
 * L2 message fetcher interface for extracting and processing L2 to L1 messages.
 * <p>
 * This interface is responsible for scanning L2 block traces to identify and extract
 * L2 to L1 cross-chain messages, which will later be aggregated into Merkle trees
 * for proof generation and L1 verification.
 * </p>
 */
public interface IL2MsgFetcher {

    /**
     * Process a block trace to extract L2 to L1 messages.
     * <p>
     * This method scans the block trace for L2 to L1 message events, extracts the message data,
     * and stores them for later Merkle tree aggregation. The messages are associated with
     * the current batch index for proper organization.
     * </p>
     *
     * @param blockTrace the block trace containing transaction execution results
     * @param currBatchIndex the current batch index that this block belongs to
     */
    void process(BasicBlockTrace blockTrace, BigInteger currBatchIndex);
}
