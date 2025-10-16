/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.l2.relayer.signservice.autocofigure;

import com.alipay.antchain.l2.relayer.signservice.config.TxSignServicesProperties;
import com.alipay.antchain.l2.relayer.signservice.core.TxSignServiceFactory;
import com.alipay.antchain.l2.relayer.signservice.inject.TxSignServiceBeanPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TxSignServicesProperties.class})
@Slf4j
public class TxSignServiceAutoConfiguration {

    @Bean
    public static TxSignServiceBeanPostProcessor txSignServiceBeanPostProcessor(
            TxSignServicesProperties txSignServicesProperties,
            TxSignServiceFactory txSignServiceFactory
    ) {
        return new TxSignServiceBeanPostProcessor(txSignServicesProperties, txSignServiceFactory);
    }

    @Bean
    public static TxSignServiceFactory txSignServiceFactory() {
        return new TxSignServiceFactory();
    }
}
