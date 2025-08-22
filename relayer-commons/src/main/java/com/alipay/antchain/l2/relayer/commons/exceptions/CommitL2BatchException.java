package com.alipay.antchain.l2.relayer.commons.exceptions;

public class CommitL2BatchException extends L2RelayerException {
    public CommitL2BatchException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsgFormat, args);
    }

    public CommitL2BatchException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsgFormat, args);
    }

    public CommitL2BatchException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsg);
    }

    public CommitL2BatchException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsg);
    }
}
