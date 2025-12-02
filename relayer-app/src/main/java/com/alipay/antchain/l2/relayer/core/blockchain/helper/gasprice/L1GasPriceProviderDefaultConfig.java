package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import com.alipay.antchain.l2.relayer.utils.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component("l1-gasprice-provider-default-conf")
@ConfigurationProperties(prefix = "l2-relayer.l1-client.gas-price-provider-conf")
@PropertySource(
        value = "classpath:/gasprice-default-conf/ethereum/${l2-relayer.rollup.specs.network}-default-config.yml",
        factory = YamlPropertySourceFactory.class,
        ignoreResourceNotFound = true
)
public class L1GasPriceProviderDefaultConfig extends GasPriceProviderConfig {
}
