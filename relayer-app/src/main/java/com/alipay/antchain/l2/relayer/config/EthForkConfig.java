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

import com.alipay.antchain.l2.relayer.commons.enums.DaType;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;

/**
 * Configuration class for Ethereum fork-specific settings.
 * <p>
 * This class manages Ethereum network fork configurations, particularly for
 * EIP-4844 blob transactions. It conditionally creates the blob fork configuration
 * based on the data availability type.
 * </p>
 */
@Configuration
@Lazy
@Slf4j
public class EthForkConfig {

    /**
     * The data availability type for the rollup.
     */
    @Value("${l2-relayer.rollup.da-type:BLOBS}")
    private DaType daType;

    /**
     * Creates the Ethereum blob fork configuration bean.
     * <p>
     * This configuration is only created when the data availability type is BLOBS.
     * It manages fork-specific parameters for EIP-4844 blob transactions on different
     * Ethereum networks (mainnet, testnet, or custom networks).
     * </p>
     *
     * @param l1ChainId                          the Layer 1 chain ID
     * @param unknownEthNetworkForkBlobConfigFile optional configuration file for unknown networks
     * @return the Ethereum blob fork configuration, or null if DA type is not BLOBS
     */
    @Bean
    public EthBlobForkConfig ethBlobForkConfig(
            @Qualifier("l1ChainId") BigInteger l1ChainId,
            @Value("${l2-relayer.l1-client.eth-network-fork.unknown-network-config-file:null}") Resource unknownEthNetworkForkBlobConfigFile
    ) {
        if (daType != DaType.BLOBS) {
            log.info("daType is not BLOBS, skip and set ethBlobForkConfig empty");
            return null;
        }
        return new EthBlobForkConfig(l1ChainId, unknownEthNetworkForkBlobConfigFile);
    }
}
