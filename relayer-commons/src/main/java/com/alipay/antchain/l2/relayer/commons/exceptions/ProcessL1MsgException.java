package com.alipay.antchain.l2.relayer.commons.exceptions;

public class ProcessL1MsgException extends L2RelayerException {
    public ProcessL1MsgException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_PROCESS_L1MSG_ERROR, detailMsgFormat, args);
    }

    public ProcessL1MsgException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_PROCESS_L1MSG_ERROR, t, detailMsgFormat, args);
    }

    public ProcessL1MsgException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_PROCESS_L1MSG_ERROR, detailMsg);
    }

    public ProcessL1MsgException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.MAILBOX_SERVICE_PROCESS_L1MSG_ERROR, t, detailMsg);
    }
}
