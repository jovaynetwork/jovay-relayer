package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import com.alipay.antchain.l2.relayer.utils.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration("l2-gasprice-provider-default-conf")
@ConfigurationProperties(prefix = "l2-relayer.l2-client.gas-price-provider-conf")
@PropertySource(
        value = "classpath:/gasprice-default-conf/jovay/${l2-relayer.rollup.specs.network}-default-config.yml",
        factory = YamlPropertySourceFactory.class,
        ignoreResourceNotFound = true
)
public class L2GasPriceProviderDefaultConfig extends GasPriceProviderConfig {
}
