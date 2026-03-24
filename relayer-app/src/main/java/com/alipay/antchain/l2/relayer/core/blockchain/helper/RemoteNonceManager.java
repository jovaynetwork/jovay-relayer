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

import lombok.SneakyThrows;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public record RemoteNonceManager(String account, Web3j web3j) implements INonceManager {

    @Override
    @SneakyThrows
    public BigInteger getNextNonce() {
        return getNonceFromChain();
    }

    @Override
    public boolean ifResetNonce(EthSendTransaction result) {
        return false;
    }

    @Override
    public void resetNonce() {
        throw new RuntimeException("resetNonce not implemented");
    }

    @Override
    public void incrementNonce() {
        throw new RuntimeException("incrementNonce not implemented");
    }

    @SneakyThrows
    private BigInteger getNonceFromChain() {
        var ethGetTransactionCount =
                web3j.ethGetTransactionCount(this.account, DefaultBlockParameterName.PENDING).send();
        if (ethGetTransactionCount.hasError()) {
            throw new RuntimeException("failed to query tx count: " + ethGetTransactionCount.getError().getMessage());
        }
        return ethGetTransactionCount.getTransactionCount();
    }
}
