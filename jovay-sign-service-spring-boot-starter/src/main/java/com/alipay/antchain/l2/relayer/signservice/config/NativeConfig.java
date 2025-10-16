package com.alipay.antchain.l2.relayer.signservice.config;

import lombok.Getter;
import lombok.Setter;
import org.web3j.crypto.Credentials;

@Getter
@Setter
public class NativeConfig {

    private String privateKey;

    public Credentials toCredentials() {
        return Credentials.create(privateKey);
    }
}
