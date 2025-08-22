package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("l2-gasprice-provider-conf")
@ConfigurationProperties(prefix = "l2-relayer.l2-client.gas-price-provider-conf")
public class L2GasPriceProviderConfig extends GasPriceProviderConfig {
}
