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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.io.IOException;
import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.TransactionInfo;
import org.web3j.protocol.core.methods.response.Transaction;

/**
 * L2 client interface for interacting with Layer 2 blockchain and L2 Gas Oracle contract.
 * <p>
 * This interface provides methods for:
 * <ul>
 *     <li>Sending and managing L1 to L2 message transactions</li>
 *     <li>Querying and updating L2 Gas Oracle contract state</li>
 *     <li>Managing gas fee parameters and vault operations</li>
 * </ul>
 * </p>
 */
public interface L2ClientInterface {

    /**
     * Send an L1 to L2 message transaction to the L2 blockchain.
     * <p>
     * This relays a cross-chain message from L1 to L2 by sending it to the L2 Mailbox contract.
     * </p>
     *
     * @param l1MsgTransaction the L1 message transaction to send
     * @return transaction information of the sent transaction
     */
    TransactionInfo sendL1MsgTx(L1MsgTransaction l1MsgTransaction);

    /**
     * Resend an L1 to L2 message transaction.
     * <p>
     * This is used to retry sending a failed or stuck L1 message transaction.
     * </p>
     *
     * @param l1MsgTransaction the L1 message transaction to resend
     * @return transaction information of the resent transaction
     */
    TransactionInfo resendL1MsgTx(L1MsgTransaction l1MsgTransaction);

    /**
     * Resend a gas feed transaction to the L2 Gas Oracle contract.
     *
     * @param encodedFunc the encoded function call data
     * @return transaction information of the resent transaction
     */
    TransactionInfo resendGasFeedTx(String encodedFunc);

    /**
     * Query the last batch DA (Data Availability) fee from the L2 Gas Oracle contract.
     *
     * @return the last batch DA fee
     */
    BigInteger queryL2GasOracleLastBatchDaFee();

    /**
     * Query the last batch execution fee from the L2 Gas Oracle contract.
     *
     * @return the last batch execution fee
     */
    BigInteger queryL2GasOracleLastBatchExecFee();

    /**
     * Query the last batch byte length from the L2 Gas Oracle contract.
     *
     * @return the last batch byte length
     */
    BigInteger queryL2GasOracleLastBatchByteLength();

    /**
     * Update batch rollup fee parameters in the L2 Gas Oracle contract.
     * <p>
     * This updates the DA fee, execution fee, and byte length for the latest batch.
     * </p>
     *
     * @param lastBatchDaFee the DA fee for the last batch
     * @param lastBatchExecFee the execution fee for the last batch
     * @param lastBatchByteLength the byte length of the last batch
     * @return transaction information of the update transaction
     */
    TransactionInfo updateBatchRollupFee(BigInteger lastBatchDaFee, BigInteger lastBatchExecFee, BigInteger lastBatchByteLength);

    /**
     * Update base fee scalars in the L2 Gas Oracle contract.
     * <p>
     * This updates the L1 base fee scalar and blob base fee scalar used for gas price calculation.
     * </p>
     *
     * @param baseFeeScala the base fee scalar
     * @param blobBaseFeeScala the blob base fee scalar
     * @return transaction information of the update transaction
     * @throws IOException if there is an I/O error during the operation
     */
    TransactionInfo updateBaseFeeScala(BigInteger baseFeeScala, BigInteger blobBaseFeeScala) throws IOException;

    /**
     * Update the fixed profit parameter in the L2 Gas Oracle contract.
     *
     * @param fixedProfit the new fixed profit value
     * @return transaction information of the update transaction
     */
    TransactionInfo updateFixedProfit(BigInteger fixedProfit);

    /**
     * Update the total scalar parameter in the L2 Gas Oracle contract.
     *
     * @param totalScala the new total scalar value
     * @return transaction information of the update transaction
     */
    TransactionInfo updateTotalScala(BigInteger totalScala);

    /**
     * Withdraw funds from the vault in the L2 Gas Oracle contract.
     *
     * @param account the recipient account address
     * @param amount the amount to withdraw
     * @return transaction information of the withdrawal transaction
     */
    TransactionInfo withdrawVault(String account, BigInteger amount);

    /**
     * Query transaction details with retry mechanism.
     * <p>
     * This queries a transaction by hash with automatic retries for reliability.
     * </p>
     *
     * @param from the sender address
     * @param txHash the transaction hash
     * @param nonce the transaction nonce
     * @return the transaction details, or null if not found
     */
    Transaction queryTxWithRetry(String from, String txHash, BigInteger nonce);

    /**
     * Query the finalized L1 message nonce from the L2 Mailbox contract.
     * <p>
     * This returns the nonce of the last L1 message that has been finalized on L2.
     * </p>
     *
     * @return the finalized L1 message nonce
     */
    BigInteger queryFinalizeL1MsgNonce();
}
