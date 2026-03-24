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

import java.util.concurrent.ScheduledExecutorService;

import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("l2-gasprice-provider-conf")
@Lazy
public class L2GasPriceProviderConfig extends DynamicGasPriceProviderConfig {

    public L2GasPriceProviderConfig(
            @Value("${l2-relayer.l2-client.gas-price-provider-conf.persist-interval:60}") int persistInterval,
            @Autowired RedissonClient redisson,
            @Autowired ISystemConfigRepository systemConfigRepository,
            @Autowired ScheduledExecutorService dynamicPersisterScheduledExecutors,
            @Qualifier("l2-gasprice-provider-default-conf") GasPriceProviderConfig defaultConfig
    ) {
        super(
                persistInterval,
                redisson,
                "l2-gasprice-provider-conf",
                systemConfigRepository,
                dynamicPersisterScheduledExecutors,
                defaultConfig
        );
    }
}
