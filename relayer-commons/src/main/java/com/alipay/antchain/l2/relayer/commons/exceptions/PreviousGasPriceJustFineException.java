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

package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public class PreviousGasPriceJustFineException extends NoNeedToSpeedUpException {

    private final BigInteger currentMaxFeePerGas;

    private final BigInteger currentMaxPriorityFeePerGas;

    private final BigInteger previousMaxFeePerGas;

    private final BigInteger previousMaxPriorityFeePerGas;

    public PreviousGasPriceJustFineException(
            BigInteger previousMaxFeePerGas,
            BigInteger previousMaxPriorityFeePerGas,
            BigInteger currentMaxFeePerGas,
            BigInteger currentMaxPriorityFeePerGas
    ) {
        super(L2RelayerErrorCodeEnum.NO_NEED_TO_SPEED_UP_TX,
                StrUtil.format("Current net gas price is not higher than previous gas price, no need to speed up:" +
                               " net price is (maxFee: {}, maxPriorityFee: {}) now and previous in tx is (maxFee: {}, maxPriorityFee: {})",
                        currentMaxFeePerGas, currentMaxPriorityFeePerGas, previousMaxFeePerGas, previousMaxPriorityFeePerGas));
        this.currentMaxFeePerGas = currentMaxFeePerGas;
        this.currentMaxPriorityFeePerGas = currentMaxPriorityFeePerGas;
        this.previousMaxFeePerGas = previousMaxFeePerGas;
        this.previousMaxPriorityFeePerGas = previousMaxPriorityFeePerGas;
    }
}
