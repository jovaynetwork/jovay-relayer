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

/**
 * Gas limit provider interface for obtaining gas limits for contract function calls.
 * <p>
 * This interface provides methods to retrieve appropriate gas limits for different
 * contract functions, allowing for function-specific gas limit strategies to ensure
 * transactions have sufficient gas while avoiding excessive costs.
 * </p>
 */
public interface IGasLimitProvider {

    /**
     * Get gas limit for a specific contract function call.
     * <p>
     * This allows for function-specific gas limit strategies, which can be optimized
     * based on the complexity and gas consumption patterns of different operations.
     * </p>
     *
     * @param contractFunc the contract function name
     * @return the gas limit
     */
    BigInteger getGasLimit(String contractFunc);

    /**
     * Get the default gas limit.
     * <p>
     * This method is deprecated. Use {@link #getGasLimit(String)} with specific
     * function names for more accurate gas limit estimation.
     * </p>
     *
     * @return the default gas limit
     * @deprecated Use {@link #getGasLimit(String)} instead
     */
    @Deprecated
    BigInteger getGasLimit();
}
