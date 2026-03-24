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

package com.alipay.antchain.l2.relayer.engine.dynamicconf;

/**
 * Dynamic configuration interface for runtime configuration management.
 * <p>
 * This interface provides methods to get and set configuration values dynamically
 * without requiring application restart. It is typically backed by a distributed
 * configuration service or database.
 * </p>
 */
public interface IDynamicConfig {

    /**
     * Get a configuration value by key.
     *
     * @param key the configuration key
     * @return the configuration value, or null if not found
     */
    String get(String key);

    /**
     * Set a configuration value.
     * <p>
     * This updates the configuration value for the specified key,
     * making it available to all instances in a distributed environment.
     * </p>
     *
     * @param key the configuration key
     * @param value the configuration value to set
     */
    void set(String key, String value);
}
