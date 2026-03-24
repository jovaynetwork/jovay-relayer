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

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip4844GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticGasPriceProvider implements IGasPriceProvider {

    private BigInteger gasPrice;

    private BigInteger maxFeePerGas;

    private BigInteger maxPriorityFeePerGas;

    @Override
    public BigInteger getGasPrice(String contractFunc) {
        return gasPrice;
    }

    @Override
    public BigInteger getGasPrice() {
        return gasPrice;
    }

    @Override
    public Eip1559GasPrice getEip1559GasPrice() {
        return new Eip1559GasPrice(maxFeePerGas, maxPriorityFeePerGas, BigInteger.ZERO);
    }

    @Override
    public IGasPrice getEip4844GasPrice() {
        return new Eip4844GasPrice(maxFeePerGas, maxPriorityFeePerGas, BigInteger.valueOf(20_000_000_000L), BigInteger.ZERO);
    }
}
