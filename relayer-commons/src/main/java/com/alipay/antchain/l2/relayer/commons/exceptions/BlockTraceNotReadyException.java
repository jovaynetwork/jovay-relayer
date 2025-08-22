package com.alipay.antchain.l2.relayer.commons.exceptions;

import com.alipay.antchain.l2.status.L2ErrorCode;

public class BlockTraceNotReadyException extends RemoteServiceRetryException {

    public BlockTraceNotReadyException(String detailMsg) {
        super(L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER, "block strace not ready", detailMsg);
    }

    public BlockTraceNotReadyException(String detailMsgFormat, Object... args) {
        super(L2ErrorCode.L2_TRACER_ERROR_INVALID_BLOCK_NUMBER, "block strace not ready", detailMsgFormat, args);
    }
}
