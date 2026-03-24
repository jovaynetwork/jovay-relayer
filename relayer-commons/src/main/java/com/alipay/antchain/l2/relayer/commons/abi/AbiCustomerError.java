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

import com.esaulpaugh.headlong.abi.ContractError;
import com.esaulpaugh.headlong.abi.Tuple;
import org.web3j.utils.Numeric;

public record AbiCustomerError(String contractName, ContractError<Tuple> error, Tuple parameters) {

    public String getErrorName() {
        return error.getName();
    }

    public String getReason() {
        return error.getName() + ":" + toString(parameters);
    }

    public String toString(Tuple tuple) {
        StringBuilder sb = new StringBuilder("[");
        if (!tuple.isEmpty()) {
            for (Object o : tuple) {
                parse(sb, o);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    private void parse(StringBuilder sb, Object element) {
        if (element instanceof byte[]) {
            sb.append(Numeric.toHexString((byte[]) element));
        } else if (element instanceof Tuple) {
            sb.append(toString((Tuple) element));
        } else {
            String str = element.toString();
            sb.append(element instanceof String
                    ? '"' + str + '"'
                    : "_".equals(str)
                    ? "\\_"
                    : element);
        }
    }
}
