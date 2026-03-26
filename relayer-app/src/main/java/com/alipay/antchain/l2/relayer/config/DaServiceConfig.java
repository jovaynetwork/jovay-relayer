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

package com.alipay.antchain.l2.relayer.config;

import com.alipay.antchain.l2.relayer.commons.enums.DasType;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.IDaService;
import com.alipay.antchain.l2.relayer.config.auto.DaServiceProperties;
import com.alipay.antchain.l2.relayer.core.layer2.RelayerLocalDaService;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class DaServiceConfig {

    @Resource
    private DaServiceProperties properties;

    @ConditionalOnProperty(name = "l2-relayer.rollup.da-type", havingValue = "DAS")
    @Bean("daService")
    public IDaService daService(IRollupRepository rollupRepository) {
        if (properties.getType() == DasType.LOCAL) {
            return new RelayerLocalDaService(rollupRepository);
        }
        throw new RuntimeException("Only local da service is supported for now 🙇");
    }
}
