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

import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.L1BlockFeeInfo;
import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Repository interface for managing oracle-related data and operations.
 * <p>This interface provides methods for handling oracle requests, transaction receipts,
 * and L1 block fee information. Oracles are critical components that provide external
 * data to the rollup system, particularly for gas price estimation and transaction
 * cost calculations.</p>
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Oracle request management with state tracking</li>
 *   <li>Transaction receipt storage for rollup operations</li>
 *   <li>L1 block fee information tracking for cost optimization</li>
 *   <li>Support for multiple oracle types and request types</li>
 * </ul>
 */
public interface IOracleRepository {
    /**
     * Retrieves oracle requests matching specific criteria.
     * <p>This method fetches a batch of oracle requests filtered by oracle type,
     * request type, and transaction state. This is useful for processing pending
     * requests or monitoring request status.</p>
     *
     * @param oracleType the type of oracle (e.g., gas price oracle, data oracle)
     * @param requestType the type of request being made
     * @param state the current transaction state of the requests
     * @param size the maximum number of requests to retrieve
     * @return a list of oracle request data objects matching the criteria
     */
    List<OracleRequestDO> peekRequests(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state, int size);

    /**
     * Retrieves the most recent oracle request matching specific criteria.
     * <p>This method returns the latest oracle request of a given type and state,
     * which is useful for determining the current status or getting the most
     * up-to-date oracle data.</p>
     *
     * @param oracleType the type of oracle
     * @param requestType the type of request
     * @param state the current transaction state
     * @return the latest oracle request data object, or null if none found
     */
    OracleRequestDO peekLatestRequest(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);

    /**
     * Retrieves the index of the latest oracle request with specific state.
     * <p>This method returns the index (typically a sequence number) of the most
     * recent oracle request matching the given criteria. This is useful for
     * tracking request progression and identifying gaps.</p>
     *
     * @param oracleType the type of oracle
     * @param requestType the type of request
     * @param state the current transaction state
     * @return the index of the latest request, or null if none found
     */
    BigInteger peekLatestRequestIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);

    /**
     * Retrieves the index of the latest oracle request regardless of state.
     * <p>This method returns the index of the most recent oracle request of a
     * given type, without filtering by state. This provides the absolute latest
     * request index for the specified oracle and request type.</p>
     *
     * @param oracleType the type of oracle
     * @param requestType the type of request
     * @return the index of the latest request, or null if none found
     */
    BigInteger peekLatestRequestIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType);

    /**
     * Retrieves a specific oracle request by type and index.
     * <p>This method looks up an oracle request using its oracle type, request type,
     * and unique index. This allows for precise retrieval of historical or specific
     * oracle requests.</p>
     *
     * @param oracleType the type of oracle
     * @param requestTypeEnum the type of request
     * @param index the unique index of the request
     * @return the oracle request data object, or null if not found
     */
    OracleRequestDO peekRequestByTypeAndIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestTypeEnum, String index);

    /**
     * Saves L1 block fee information.
     * <p>This method persists fee information from L1 blocks, which is essential
     * for calculating transaction costs and optimizing gas usage in the rollup
     * system. This data is typically provided by gas price oracles.</p>
     *
     * @param blockFeeInfo the L1 block fee information to save
     */
    void saveBlockFeeInfo(L1BlockFeeInfo blockFeeInfo);

    /**
     * Saves a transaction receipt for a rollup operation.
     * <p>This method stores the transaction receipt from a rollup-related transaction
     * on L1, indexed by batch number and oracle information. These receipts are
     * important for tracking transaction status and verifying successful execution.</p>
     *
     * @param batchIndex the batch index associated with the transaction
     * @param oracleType the type of oracle involved
     * @param requestTypeEnum the type of request
     * @param txReceipt the transaction receipt from the blockchain
     */
    void saveRollupTxReceipt(BigInteger batchIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestTypeEnum, TransactionReceipt txReceipt);

    /**
     * Updates the state of an oracle request.
     * <p>This method changes the transaction state of a specific oracle request,
     * tracking its lifecycle from creation through processing to completion or
     * failure. State updates are crucial for request management and retry logic.</p>
     *
     * @param requestIndex the unique index of the request
     * @param oracleType the type of oracle
     * @param requestType the type of request
     * @param state the new transaction state to set
     */
    void updateRequestState(String requestIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state);
}
