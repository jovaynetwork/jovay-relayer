package com.alipay.antchain.l2.relayer.commons.exceptions;

public class CommitL2BatchTeeProofException extends L2RelayerException {
    public CommitL2BatchTeeProofException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsgFormat, args);
    }

    public CommitL2BatchTeeProofException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsgFormat, args);
    }

    public CommitL2BatchTeeProofException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsg);
    }

    public CommitL2BatchTeeProofException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsg);
    }
}
