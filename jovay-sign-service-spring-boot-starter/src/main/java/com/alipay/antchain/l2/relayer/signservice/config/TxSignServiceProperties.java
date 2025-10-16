package com.alipay.antchain.l2.relayer.signservice.config;

import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@Setter
public class TxSignServiceProperties {

    private TxSignServiceType type = TxSignServiceType.WEB3J_NATIVE;

    private KmsConfig kms;

    private NativeConfig web3jNative;
}
