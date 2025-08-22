package com.alipay.antchain.l2.relayer.commons.exceptions;

public class FetchL1MsgException extends L2RelayerException {
    public FetchL1MsgException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_FETCH_L1MSG_ERROR, detailMsgFormat, args);
    }

    public FetchL1MsgException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_FETCH_L1MSG_ERROR, t, detailMsgFormat, args);
    }

    public FetchL1MsgException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_FETCH_L1MSG_ERROR, detailMsg);
    }

    public FetchL1MsgException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_FETCH_L1MSG_ERROR, t, detailMsg);
    }
}
