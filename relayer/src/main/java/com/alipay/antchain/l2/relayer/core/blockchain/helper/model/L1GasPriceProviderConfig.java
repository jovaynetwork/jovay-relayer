package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("l1-gasprice-provider-conf")
@ConfigurationProperties(prefix = "l2-relayer.l1-client.gas-price-provider-conf")
public class L1GasPriceProviderConfig extends GasPriceProviderConfig {
}
