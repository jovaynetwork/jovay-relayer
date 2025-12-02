package com.alipay.antchain.l2.relayer.commons.exceptions;

public class CommitL2BatchZkProofException extends L2RelayerException {
    public CommitL2BatchZkProofException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsgFormat, args);
    }

    public CommitL2BatchZkProofException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsgFormat, args);
    }

    public CommitL2BatchZkProofException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, detailMsg);
    }

    public CommitL2BatchZkProofException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.COMMIT_L2_BATCH_ERROR, t, detailMsg);
    }
}
