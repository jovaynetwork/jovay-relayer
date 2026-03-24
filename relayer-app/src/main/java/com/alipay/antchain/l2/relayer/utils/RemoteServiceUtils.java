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

package com.alipay.antchain.l2.relayer.utils;

import com.alipay.antchain.l2.status.L2ErrorCode;
import com.alipay.antchain.l2.status.L2Status;
import com.alipay.antchain.l2.status.Retriable;

public class RemoteServiceUtils {

    public static boolean isL2StatusRetryable(L2Status l2Status) {
        return l2Status.getNeedRetry() == Retriable.RETRIABLE_YES
               || l2Status.getErrorCode() == L2ErrorCode.L2_TIMEOUT
               || l2Status.getErrorCode() == L2ErrorCode.L2_SERVER_BUSY
               || l2Status.getErrorCode() == L2ErrorCode.L2_RESOURCE_EXHAUSTED;
    }
}
