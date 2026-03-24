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

package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class L2MsgDO {

    private BigInteger batchIndex;

    private BigInteger msgNonce;

    private byte[] msgHash;

    private String sourceTxHash;

    public String getMsgHashHex() {
        return HexUtil.encodeHexStr(msgHash);
    }
}
