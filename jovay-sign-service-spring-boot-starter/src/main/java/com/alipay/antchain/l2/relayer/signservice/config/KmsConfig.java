package com.alipay.antchain.l2.relayer.signservice.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KmsConfig {

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String privateKeyId;

    private String privateKeyVersionId;

    private String publicKey;

    private String ca;
}
