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

import java.math.BigInteger;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Basic blockchain client interface providing fundamental blockchain interaction capabilities.
 * <p>
 * This interface defines common operations for interacting with Ethereum-compatible blockchains,
 * including querying blocks, transactions, and sending raw transactions.
 * </p>
 */
public interface BasicBlockchainClient {

    /**
     * Query the latest block header.
     *
     * @param blockParameterName the block parameter (e.g., LATEST, FINALIZED, SAFE)
     * @return the block header information
     */
    EthBlock queryLatestBlockHeader(DefaultBlockParameterName blockParameterName);

    /**
     * Query the latest block number.
     *
     * @param blockParameterName the block parameter (e.g., LATEST, FINALIZED, SAFE)
     * @return the latest block number
     */
    BigInteger queryLatestBlockNumber(DefaultBlockParameterName blockParameterName);

    /**
     * Query block information by block number.
     *
     * @param height the block height/number
     * @return the block information
     */
    EthBlock queryBlockByNumber(BigInteger height);

    /**
     * Query transaction receipt by transaction hash.
     *
     * @param txhash the transaction hash
     * @return the transaction receipt, or null if not found
     */
    TransactionReceipt queryTxReceipt(String txhash);

    /**
     * Query transaction details by transaction hash.
     *
     * @param txhash the transaction hash
     * @return the transaction details, or null if not found
     */
    Transaction queryTx(String txhash);

    /**
     * Send a raw signed transaction to the blockchain.
     *
     * @param rawSignedTx the raw signed transaction bytes
     * @return the transaction send result containing the transaction hash
     */
    EthSendTransaction sendRawTx(byte[] rawSignedTx);

    /**
     * Query the transaction count (nonce) for an address.
     *
     * @param address the account address
     * @param name the block parameter (e.g., LATEST, PENDING)
     * @return the transaction count (nonce)
     */
    BigInteger queryTxCount(String address, DefaultBlockParameterName name);

    /**
     * Query the account balance at a specific block.
     *
     * @param address the account address
     * @param blockParameter the block parameter specifying which block to query
     * @return the account balance in wei
     */
    BigInteger queryAccountBalance(String address, DefaultBlockParameter blockParameter);
}
