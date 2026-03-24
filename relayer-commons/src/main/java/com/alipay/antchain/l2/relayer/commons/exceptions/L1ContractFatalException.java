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

package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public class L1ContractFatalException extends L2RelayerException {

    private final String revertReason;

    public L1ContractFatalException(L2RelayerErrorCodeEnum errorCode, String errMsg, String revertReason) {
        super(errorCode, StrUtil.format("code: {}, err_msg: {}", errorCode.getErrorCode(), errMsg));
        this.revertReason = revertReason;
    }
}
