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
