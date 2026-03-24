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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.abi.AbiCustomerError;
import com.alipay.antchain.l2.relayer.commons.abi.AbiDecoder;

public class ContractErrorParserImpl implements IContractErrorParser {

    private final List<AbiDecoder> decoders = new ArrayList<>();

    public void addContractAbi(String contractName, String abi) {
        decoders.add(new AbiDecoder(contractName, abi));
    }

    @Override
    public AbiCustomerError parse(String hexError) {
        hexError = StrUtil.replace(hexError, "\"", "");
        for (var decoder : decoders) {
            var err = decoder.decodeError(hexError);
            if (err != null) {
                return err;
            }
        }
        return null;
    }
}
