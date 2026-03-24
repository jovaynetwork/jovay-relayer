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

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;

@Configuration
@Lazy
@Slf4j
public class EthForkConfig {

    @Bean
    public EthBlobForkConfig ethBlobForkConfig(
            @Qualifier("l1ChainId") BigInteger l1ChainId,
            @Value("${l2-relayer.l1-client.eth-network-fork.unknown-network-config-file:null}") Resource unknownEthNetworkForkBlobConfigFile
    ) {
        return new EthBlobForkConfig(l1ChainId, unknownEthNetworkForkBlobConfigFile);
    }
}
