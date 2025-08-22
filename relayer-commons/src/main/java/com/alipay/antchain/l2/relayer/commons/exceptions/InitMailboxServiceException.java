package com.alipay.antchain.l2.relayer.commons.exceptions;

public class InitMailboxServiceException extends L2RelayerException {
    public InitMailboxServiceException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.INIT_MAILBOX_SERVICE_ERROR, detailMsgFormat, args);
    }

    public InitMailboxServiceException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.INIT_MAILBOX_SERVICE_ERROR, t, detailMsgFormat, args);
    }

    public InitMailboxServiceException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.INIT_MAILBOX_SERVICE_ERROR, detailMsg);
    }

    public InitMailboxServiceException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.INIT_MAILBOX_SERVICE_ERROR, t, detailMsg);
    }
}
