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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RollupSpecs implements IRollupSpecs {

    public static RollupSpecs fromJson(String json) {
        return JSON.parseObject(json, RollupSpecs.class);
    }

    @JSONField(name = "network")
    private RollupSpecsNetwork network;

    @JSONField(name = "layer2_chain_id")
    private BigInteger layer2ChainId;

    @JSONField(name = "layer1_chain_id")
    private BigInteger layer1ChainId;

    @JSONField(name = "forks")
    private Map<BigInteger, ForkInfo> forks;

    @Override
    public ForkInfo getFork(long currTimestamp) {
        for (var entry : MapUtil.sort(forks, Comparator.reverseOrder()).entrySet()) {
            if (entry.getKey().longValue() <= currTimestamp) {
                return entry.getValue().validate();
            }
        }
        throw new RuntimeException("No fork found for batch start timestamp " + currTimestamp);
    }
}
