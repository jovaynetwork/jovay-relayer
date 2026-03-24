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

package com.alipay.antchain.l2.relayer.signservice.core;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.signservice.config.NativeConfig;
import lombok.NonNull;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.service.TxSignService;

public class Web3jTxSignService implements TxSignService {

    private final Credentials credentials;

    public Web3jTxSignService(@NonNull NativeConfig config) {
        this.credentials = config.toCredentials();
    }

    @Override
    public byte[] sign(RawTransaction rawTransaction, long chainId) {
        final byte[] signedMessage;

        if (rawTransaction instanceof L1MsgRawTransactionWrapper) {
            byte[] encodedTransaction = ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithoutSig();
            Sign.SignatureData signatureData =
                    Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

            Sign.SignatureData eip155SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, chainId);
            return ((L1MsgRawTransactionWrapper) rawTransaction).encodeWithSig(eip155SignatureData);
        } else {
            if (chainId > -1 && rawTransaction.getType().isLegacy()) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            }
        }

        return signedMessage;
    }

    @Override
    public String getAddress() {
        return credentials.getAddress();
    }
}
