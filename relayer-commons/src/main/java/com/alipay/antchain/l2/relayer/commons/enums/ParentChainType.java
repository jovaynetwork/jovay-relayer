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

package com.alipay.antchain.l2.relayer.commons.enums;

import java.util.List;

/**
 * Enum representing the types of parent chains supported by the L2 relayer.
 * Each parent chain type has different characteristics and supported DA (Data Availability) types.
 */
public enum ParentChainType {

    /**
     * JOVAY parent chain type
     */
    JOVAY,

    /**
     * Ethereum parent chain type
     */
    ETHEREUM;

    /**
     * Returns the list of supported Data Availability (DA) types for this parent chain.
     *
     * @return a list of supported {@link DaType} for this parent chain type
     */
    public List<DaType> supportedDaTypes() {
        return switch (this) {
            case JOVAY -> List.of(DaType.DAS);
            case ETHEREUM -> List.of(DaType.BLOBS);
        };
    }

    /**
     * Determines whether this parent chain type requires rollup fee feed.
     *
     * @return {@code true} if this parent chain type is ETHEREUM and requires rollup fee feed,
     *         {@code false} otherwise
     */
    public boolean needRollupFeeFeed() {
        return this == ParentChainType.ETHEREUM;
    }
}
