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

package com.alipay.antchain.l2.relayer.commons.specs;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RollupSpecsNetwork {

    /**
     * For the Jovay mainnet
     */
    MAINNET("mainnet"),

    /**
     * For the Jovay testnet
     */
    TESTNET("testnet"),

    /**
     * For the Jovay private net
     */
    PRIVATE_NET("private_net");

    @JSONField
    private final String name;

    @JSONCreator
    public RollupSpecsNetwork from(String name) {
        return switch (name) {
            case "mainnet" -> MAINNET;
            case "testnet" -> TESTNET;
            case "private_net" -> PRIVATE_NET;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
