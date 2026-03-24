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

package com.alipay.antchain.l2.relayer.commons.specs.forks;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidRollupSpecsException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForkInfo {

    @JSONField(name = "batch_version")
    private BatchVersionEnum batchVersion;

    public ForkInfo validate() throws InvalidRollupSpecsException {
        if (ObjectUtil.isNull(batchVersion)) {
            throw new InvalidRollupSpecsException("batch version is null");
        }
        return this;
    }
}
