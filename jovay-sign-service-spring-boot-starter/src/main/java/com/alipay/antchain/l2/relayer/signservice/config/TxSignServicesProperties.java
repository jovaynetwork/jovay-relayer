package com.alipay.antchain.l2.relayer.signservice.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jovay")
@Getter
public class TxSignServicesProperties {

    private final Map<String, TxSignServiceProperties> signService = new ConcurrentHashMap<>();
}
