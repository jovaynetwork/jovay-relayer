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

package com.alipay.antchain.l2.relayer.core.blockchain.bpo;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

/**
 * Configuration manager for Ethereum blob fork parameters across different network upgrades.
 * <p>
 * This class manages time-based blob configuration parameters that may change across
 * different Ethereum network forks (e.g., Cancun, Deneb). It loads network-specific
 * configurations from JSON files and provides the appropriate configuration based on
 * the current timestamp.
 * </p>
 * <p>
 * Supported networks:
 * <ul>
 *   <li>Ethereum Mainnet (chain ID: 1)</li>
 *   <li>Sepolia Testnet (chain ID: 11155111)</li>
 *   <li>Custom/Unknown networks (via configuration file)</li>
 * </ul>
 * </p>
 * <p>
 * The configuration is organized as a time-ordered map where each timestamp represents
 * a fork activation time, and the associated configuration applies from that time forward
 * until the next fork.
 * </p>
 *
 * @author Aone Copilot
 * @since 1.0
 */
@Slf4j
public class EthBlobForkConfig {

    /**
     * Map of fork activation timestamps to their corresponding blob configurations.
     * <p>
     * Keys are Unix timestamps (in milliseconds) representing when each fork activates.
     * Values are the blob configuration parameters for that fork.
     * The map is sorted in reverse order (newest first) for efficient lookup.
     * </p>
     */
    private Map<BigInteger, EthBpoBlobConfig> configs;

    /**
     * Constructs a new EthBlobForkConfig instance.
     * <p>
     * Initializes the fork configuration by loading the appropriate configuration file
     * based on the L1 chain ID. For known networks (Mainnet, Sepolia), uses bundled
     * configuration files. For unknown networks, uses the provided custom configuration file.
     * </p>
     *
     * @param l1ChainId                           the Layer 1 chain ID
     * @param unknownEthNetworkForkBlobConfigFile the configuration file for unknown networks
     */
    public EthBlobForkConfig(BigInteger l1ChainId, Resource unknownEthNetworkForkBlobConfigFile) {
        initConfigsFromResources(l1ChainId, unknownEthNetworkForkBlobConfigFile);
    }

    /**
     * Initializes the fork configurations from resource files.
     * <p>
     * Loads the appropriate JSON configuration file based on the chain ID:
     * <ul>
     *   <li>Chain ID 1: Ethereum Mainnet configuration (bpo/mainnet.json)</li>
     *   <li>Chain ID 11155111: Sepolia Testnet configuration (bpo/sepolia.json)</li>
     *   <li>Other: Custom network configuration from provided file</li>
     * </ul>
     * </p>
     * <p>
     * The configuration file should be a JSON object where keys are Unix timestamps
     * (in milliseconds) and values are blob configuration objects. After loading,
     * the configurations are sorted in reverse chronological order for efficient lookup.
     * </p>
     *
     * @param chainId                             the Layer 1 chain ID
     * @param unknownEthNetworkForkBlobConfigFile the configuration file for unknown networks
     * @throws Exception if the configuration file cannot be read or parsed
     */
    @SneakyThrows
    private void initConfigsFromResources(BigInteger chainId, Resource unknownEthNetworkForkBlobConfigFile) {
        this.configs = new ConcurrentHashMap<>();
        String raw;
        if (chainId.equals(BigInteger.ONE)) {
            log.info("loading Ethereum blob config: mainnet");
            raw = ResourceUtil.readStr("bpo/mainnet.json", Charset.defaultCharset());
        } else if (chainId.equals(BigInteger.valueOf(11155111))) {
            log.info("loading Ethereum blob config: sepolia testnet");
            raw = ResourceUtil.readStr("bpo/sepolia.json", Charset.defaultCharset());
        } else {
            log.info("loading Ethereum blob config: unknown net");
            raw = unknownEthNetworkForkBlobConfigFile.getContentAsString(Charset.defaultCharset());
        }
        JSON.parseObject(raw).getInnerMap().forEach((k, v) -> {
            log.info("loading Ethereum blob config entry: ({} => {})", k, v);
            this.configs.put(new BigInteger(k), JSON.parseObject(v.toString(), EthBpoBlobConfig.class));
        });
        this.configs = MapUtil.sort(configs, Comparator.reverseOrder());
    }

    /**
     * Retrieves the blob configuration applicable at the specified timestamp.
     * <p>
     * Searches through the fork configurations to find the most recent fork that
     * was active at or before the given timestamp. The configurations are sorted
     * in reverse chronological order, so the first matching entry is returned.
     * </p>
     *
     * @param currTimestamp the timestamp (in milliseconds) to query
     * @return the blob configuration active at the specified timestamp
     * @throws RuntimeException if no configuration is found for the given timestamp
     */
    public EthBpoBlobConfig getCurrConfig(long currTimestamp) {
        for (var entry : configs.entrySet()) {
            if (entry.getKey().longValue() <= currTimestamp) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("No BPO config found");
    }

    /**
     * Retrieves the current blob configuration based on the system time.
     * <p>
     * This is a convenience method that calls {@link #getCurrConfig(long)} with
     * the current system timestamp.
     * </p>
     *
     * @return the blob configuration active at the current time
     * @throws RuntimeException if no configuration is found for the current time
     */
    public EthBpoBlobConfig getCurrConfig() {
        return getCurrConfig(System.currentTimeMillis());
    }
}
