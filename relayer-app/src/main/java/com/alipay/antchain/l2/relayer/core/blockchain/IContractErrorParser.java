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

import com.alipay.antchain.l2.relayer.commons.abi.AbiCustomerError;

public interface IContractErrorParser {

    /**
     * Parse the raw hex error data to {@link AbiCustomerError } from eth call result.
     *
     * @param hexError the raw hex error data
     * @return {@link AbiCustomerError }
     */
    AbiCustomerError parse(String hexError);
}
