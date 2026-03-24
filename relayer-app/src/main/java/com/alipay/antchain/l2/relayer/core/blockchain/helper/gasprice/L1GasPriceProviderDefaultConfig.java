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
