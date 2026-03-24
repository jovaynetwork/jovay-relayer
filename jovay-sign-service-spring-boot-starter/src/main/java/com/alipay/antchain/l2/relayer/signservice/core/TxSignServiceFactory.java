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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alipay.antchain.l2.relayer.signservice.config.TxSignServiceProperties;
import lombok.NonNull;
import org.web3j.service.TxSignService;

public class TxSignServiceFactory {

    private final Map<String, TxSignService> txSignServiceMap;

    public TxSignServiceFactory() {
        this.txSignServiceMap = new ConcurrentHashMap<>();
    }

    public TxSignService createTxSignService(@NonNull String serviceName, @NonNull TxSignServiceProperties properties) {
        if (txSignServiceMap.containsKey(serviceName)) {
            return txSignServiceMap.get(serviceName);
        }
        txSignServiceMap.put(
                serviceName,
                switch (properties.getType()) {
                    case WEB3J_NATIVE -> new Web3jTxSignService(properties.getWeb3jNative());
                    case ALIYUN_KMS -> new KmsTxSignService(properties.getKms());
                }
        );
        return txSignServiceMap.get(serviceName);
    }
}
