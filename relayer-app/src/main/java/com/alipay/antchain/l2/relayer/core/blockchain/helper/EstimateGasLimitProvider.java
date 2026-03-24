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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.TxSimulateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

@AllArgsConstructor
@Builder
public class EstimateGasLimitProvider implements IGasLimitProvider {

    private Web3j web3j;

    private String fromAddress;

    private String toAddress;

    private String dataHex;

    private BigInteger extraGasLimit;

    @Override
    public BigInteger getGasLimit(String contractFunc) {
        return getGasLimitLogic(contractFunc);
    }

    @Override
    public BigInteger getGasLimit() {
        return getGasLimitLogic("");
    }

    @SneakyThrows
    private BigInteger getGasLimitLogic(String contractFunc) {
        EthEstimateGas ethEstimateGas;
        if (StrUtil.equals(contractFunc, "deploy")) {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        } else {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        }
        if (ethEstimateGas.hasError()) {
            throw new TxSimulateException(ethEstimateGas.getError());
        }

        return ethEstimateGas.getAmountUsed().add(extraGasLimit);
    }
}
