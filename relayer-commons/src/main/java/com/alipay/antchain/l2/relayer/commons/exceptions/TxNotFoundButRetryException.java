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

public class TxNotFoundButRetryException extends L2RelayerException {
    public TxNotFoundButRetryException() {
        super(L2RelayerErrorCodeEnum.TX_NOT_FOUND_BUT_RETRY, "l2 tx not found but we need to retry the query");
    }
}
