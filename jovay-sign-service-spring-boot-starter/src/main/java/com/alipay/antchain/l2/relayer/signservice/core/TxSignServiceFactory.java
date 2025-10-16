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
