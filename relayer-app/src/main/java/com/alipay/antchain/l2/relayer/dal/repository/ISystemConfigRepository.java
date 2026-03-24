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

package com.alipay.antchain.l2.relayer.dal.repository;

import java.util.Map;

/**
 * Repository interface for managing system configuration data.
 * <p>This interface provides methods for storing, retrieving, and managing
 * system-wide configuration parameters. It supports both individual key-value
 * operations and batch operations for configuration management.</p>
 * <p>Special functionality includes anchor batch tracking to ensure proper
 * initialization of the rollup system.</p>
 */
public interface ISystemConfigRepository {

    /**
     * Retrieves a system configuration value by its key.
     * <p>This method looks up the configuration value associated with the specified key
     * from the system configuration storage.</p>
     *
     * @param key the configuration key to retrieve
     * @return the configuration value associated with the key, or null if not found
     */
    String getSystemConfig(String key);

    /**
     * Checks if a system configuration key exists.
     * <p>This method verifies whether a configuration entry exists for the given key
     * without retrieving its value.</p>
     *
     * @param key the configuration key to check
     * @return true if the configuration key exists, false otherwise
     */
    boolean hasSystemConfig(String key);

    /**
     * Sets multiple system configuration entries in batch.
     * <p>This method allows updating multiple configuration key-value pairs
     * in a single operation, which is more efficient than individual updates.</p>
     *
     * @param configs a map containing configuration key-value pairs to set
     */
    void setSystemConfig(Map<String, String> configs);

    /**
     * Retrieves all system configurations with keys matching the specified prefix.
     * <p>This method is useful for retrieving related configuration entries
     * that share a common prefix, such as all configurations for a specific module.</p>
     *
     * @param prefix the prefix to match against configuration keys
     * @return a map of configuration key-value pairs where keys start with the specified prefix
     */
    Map<String, String> getPrefixedSystemConfig(String prefix);

    /**
     * Sets a single system configuration entry.
     * <p>This method updates or creates a configuration entry with the specified
     * key-value pair.</p>
     *
     * @param key the configuration key
     * @param value the configuration value to set
     */
    void setSystemConfig(String key, String value);

    /**
     * Checks if the anchor batch has been set.
     * <p>The anchor batch is a critical initialization parameter for the rollup system.
     * This method verifies whether this initialization has been completed.</p>
     *
     * @return true if the anchor batch has been set, false otherwise
     */
    boolean isAnchorBatchSet();

    /**
     * Marks the anchor batch as having been set.
     * <p>This method should be called once during system initialization to indicate
     * that the anchor batch configuration has been properly established. This is
     * typically a one-time operation.</p>
     */
    void markAnchorBatchHasBeenSet();
}
