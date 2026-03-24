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

package com.alipay.antchain.l2.relayer.commons.abi;

import java.util.Map;
import java.util.stream.Collectors;

import com.esaulpaugh.headlong.abi.ABIJSON;
import com.esaulpaugh.headlong.abi.ContractError;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.util.Strings;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

@Slf4j
public class AbiDecoder {

    private final String contractName;

    private final Map<String, ContractError<Tuple>> errorMap;

    public AbiDecoder(String contractName, String abi) {
        this.contractName = contractName;
        this.errorMap = ABIJSON.parseErrors(abi).stream()
                .collect(Collectors.toMap(e -> e.function().selectorHex(), e -> e));
    }

    public AbiCustomerError decodeError(String code) {
        try {
            code = Numeric.cleanHexPrefix(code);
            if (code.length() < 8) {
                return null;
            }
            var error = errorMap.get(code.substring(0, 8));
            if (error == null) {
                return null;
            }
            var function = error.function();
            var tuple = function.decodeCall(Strings.decode(code));
            return new AbiCustomerError(contractName, error, tuple);
        } catch (Throwable t) {
            log.warn("decodeError failed for {}", code, t);
            return null;
        }
    }
}
