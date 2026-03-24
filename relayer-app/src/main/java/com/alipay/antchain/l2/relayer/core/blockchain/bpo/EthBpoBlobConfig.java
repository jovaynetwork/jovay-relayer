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

package com.alipay.antchain.l2.relayer.core.blockchain.bpo;

import java.math.BigInteger;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration class for Ethereum BPO (Blob Parameters Only) blob parameters.
 * <p>
 * This class encapsulates the configuration parameters for EIP-4844 blob transactions,
 * including blob sidecar versioning and base fee calculation parameters. It provides
 * functionality to calculate blob base fees using a fake exponential function similar
 * to the one used in Web3j but adapted for BPO's update fraction.
 * </p>
 * <p>
 * The blob base fee mechanism helps regulate the cost of blob transactions based on
 * network demand, using an exponential pricing model.
 * </p>
 *
 * @author Aone Copilot
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EthBpoBlobConfig {

    /**
     * Minimum blob base fee in wei.
     * <p>
     * This constant defines the floor value for blob base fees to ensure
     * they never drop below 1 wei.
     * </p>
     */
    private static final BigInteger MIN_BLOB_BASE_FEE = BigInteger.ONE;

    /**
     * The name identifier for this BPO blob configuration.
     * <p>
     * Used to distinguish between different blob configuration profiles.
     * </p>
     */
    @JSONField(name = "name")
    private String name;

    /**
     * The version number of the blob sidecar format.
     * <p>
     * This version indicates the structure and encoding format of the blob sidecar
     * data associated with EIP-4844 transactions.
     * </p>
     */
    @JSONField(name = "blob_sidecar_version")
    private int blobSidecarVersion;

    /**
     * The update fraction parameter for base fee calculations.
     * <p>
     * This value is used in the fake exponential function to determine how quickly
     * the blob base fee adjusts in response to network demand. A larger value results
     * in slower fee adjustments, while a smaller value leads to more rapid changes.
     * </p>
     */
    @JSONField(name = "base_fee_update_fraction")
    private BigInteger updateFraction;

    /**
     * Calculates the fake exponential function for blob base fee computation.
     * <p>
     * This method is adapted from Web3j's {@code fakeExponential} function but uses
     * the BPO-specific {@code updateFraction} parameter. The fake exponential provides
     * an approximation of exponential growth that is computationally efficient for
     * on-chain and off-chain calculations.
     * </p>
     * <p>
     * The function computes: {@code (MIN_BLOB_BASE_FEE * updateFraction * e^(numerator/updateFraction)) / updateFraction}
     * using a Taylor series approximation to avoid expensive exponential operations.
     * </p>
     *
     * @param numerator the numerator value in the exponential calculation, typically representing
     *                  the excess blob gas or demand factor
     * @return the calculated blob base fee adjustment factor
     */
    public BigInteger fakeExponential(BigInteger numerator) {
        var i = BigInteger.ONE;
        var output = BigInteger.ZERO;
        var numeratorAccum = MIN_BLOB_BASE_FEE.multiply(updateFraction);
        while (numeratorAccum.compareTo(BigInteger.ZERO) > 0) {
            output = output.add(numeratorAccum);
            numeratorAccum =
                    numeratorAccum
                            .multiply(numerator)
                            .divide(updateFraction.multiply(i));
            i = i.add(BigInteger.ONE);
        }
        return output.divide(updateFraction);
    }
}
