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

import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;

/**
 * Oracle service interface for managing L2 gas price oracle updates.
 * <p>
 * This service is responsible for feeding L1 gas price information to the L2 Gas Oracle contract,
 * enabling L2 to accurately calculate transaction costs based on L1 gas prices.
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 *     <li>Processing L1 block fee updates (base fee and blob base fee)</li>
 *     <li>Processing L2 batch fee updates (DA fee and execution fee)</li>
 *     <li>Updating fee scalars and profit parameters in the L2 Gas Oracle contract</li>
 * </ul>
 * </p>
 */
public interface IOracleService {

    /**
     * Process L1 block oracle requests.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves pending L1 block update requests from the repository</li>
     *     <li>Calculates next L1 block's base fee and blob base fee</li>
     *     <li>Updates fee scalars in the L2 Gas Oracle contract if thresholds are exceeded</li>
     *     <li>Creates reliable transaction records for tracking</li>
     * </ul>
     * </p>
     */
    void processBlockOracle();

    /**
     * Process L2 batch oracle requests.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves pending L2 batch prove requests from the repository</li>
     *     <li>Calculates batch DA fee and execution fee based on transaction receipts</li>
     *     <li>Updates batch rollup fees in the L2 Gas Oracle contract</li>
     *     <li>Creates reliable transaction records for tracking</li>
     * </ul>
     * </p>
     */
    void processBatchOracle();

    /**
     * Update batch blob fee and transaction fee for a specific oracle request.
     * <p>
     * This method calculates the DA (Data Availability) fee and execution fee for a batch
     * based on L1 transaction receipts, then updates the L2 Gas Oracle contract.
     * </p>
     *
     * @param oracleRequestDO the oracle request containing batch information
     * @throws Exception if there is an error updating the fees
     */
    void updateBatchBlobFeeAndTxFee(OracleRequestDO oracleRequestDO) throws Exception;

    /**
     * Update blob base fee scalar and transaction fee scalar.
     * <p>
     * This method:
     * <ul>
     *     <li>Predicts next L1 block's base fee and blob base fee</li>
     *     <li>Calculates fee scalars based on configured thresholds</li>
     *     <li>Updates the L2 Gas Oracle contract if scalars exceed thresholds</li>
     *     <li>Calibrates fees to prevent extreme values</li>
     * </ul>
     * </p>
     *
     * @param oracleRequestDO the oracle request containing L1 block fee information
     */
    void updateBlobBaseFeeScalaAndTxFeeScala(OracleRequestDO oracleRequestDO);

    /**
     * Update the fixed profit parameter in the L2 Gas Oracle contract.
     * <p>
     * This is typically called through admin CLI to adjust the profit margin.
     * </p>
     *
     * @param profit the new fixed profit value
     * @return transaction information of the update transaction
     */
    TransactionInfo updateFixedProfit(BigInteger profit);

    /**
     * Update the total scalar parameter in the L2 Gas Oracle contract.
     * <p>
     * This is typically called through admin CLI to adjust the overall fee multiplier.
     * </p>
     *
     * @param totalScala the new total scalar value
     * @return transaction information of the update transaction
     */
    TransactionInfo updateTotalScala(BigInteger totalScala);

    /**
     * Withdraw funds from the vault in the L2 Gas Oracle contract.
     * <p>
     * This is typically called through admin CLI for fund management.
     * </p>
     *
     * @param address the recipient address
     * @param amount the amount to withdraw
     * @return transaction information of the withdrawal transaction
     */
    TransactionInfo withdrawVault(String address, BigInteger amount);
}
