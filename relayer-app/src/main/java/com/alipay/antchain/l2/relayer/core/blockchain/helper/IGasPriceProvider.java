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

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;

/**
 * Gas price provider interface for obtaining gas prices for different transaction types.
 * <p>
 * This interface provides methods to retrieve gas prices for:
 * <ul>
 *     <li>Legacy transactions (pre-EIP-1559)</li>
 *     <li>EIP-1559 transactions (with base fee and priority fee)</li>
 *     <li>EIP-4844 transactions (with blob gas pricing)</li>
 * </ul>
 * </p>
 */
public interface IGasPriceProvider {

    /**
     * Get gas price for a specific contract function call.
     * <p>
     * This allows for function-specific gas price strategies, which can be useful
     * for optimizing costs based on the importance or urgency of different operations.
     * </p>
     *
     * @param contractFunc the contract function name
     * @return the gas price in wei
     */
    BigInteger getGasPrice(String contractFunc);

    /**
     * Get the default gas price.
     * <p>
     * This method is deprecated. Use {@link #getEip1559GasPrice()} for EIP-1559 compatible chains
     * or {@link #getGasPrice(String)} for function-specific pricing.
     * </p>
     *
     * @return the gas price in wei
     * @deprecated Use {@link #getEip1559GasPrice()} instead
     */
    @Deprecated
    BigInteger getGasPrice();

    /**
     * Get EIP-1559 gas price parameters.
     * <p>
     * Returns gas price information including:
     * <ul>
     *     <li>maxFeePerGas: Maximum total fee per gas unit</li>
     *     <li>maxPriorityFeePerGas: Maximum priority fee (tip) per gas unit</li>
     * </ul>
     * </p>
     *
     * @return the EIP-1559 gas price object containing fee parameters
     */
    IGasPrice getEip1559GasPrice();

    /**
     * Get EIP-4844 gas price parameters.
     * <p>
     * Returns gas price information for blob transactions including:
     * <ul>
     *     <li>maxFeePerGas: Maximum fee per gas for execution</li>
     *     <li>maxPriorityFeePerGas: Maximum priority fee per gas</li>
     *     <li>maxFeePerBlobGas: Maximum fee per blob gas unit</li>
     * </ul>
     * </p>
     *
     * @return the EIP-4844 gas price object containing blob fee parameters
     */
    IGasPrice getEip4844GasPrice();
}
