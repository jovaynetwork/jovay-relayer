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

package com.alipay.antchain.l2.relayer.commons.models;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OracleFeeInfo {
    // EIP-1559's prior knowledge
    private final BigInteger baseFeeChangeDenominator = BigInteger.valueOf(8);

    private BigInteger startBatchIndex = BigInteger.ONE;

    private BigInteger lastBatchDaFee;

    private BigInteger lastBatchExecFee;

    private BigInteger lastBatchByteLength;

    private BigInteger lastL1BaseFee = BigInteger.valueOf(1000000000); // WEI

    private BigInteger lastL1BlobBaseFee = BigInteger.valueOf(1000000000); // WEI

    private BigInteger blobBaseFeeScala;

    private BigInteger baseFeeScala;

    private BigInteger l1FixedProfit;

    private BigInteger totalScala;
}
