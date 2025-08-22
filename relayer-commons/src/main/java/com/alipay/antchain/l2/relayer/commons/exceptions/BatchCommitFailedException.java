package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class BatchCommitFailedException extends L2RelayerException {
    public BatchCommitFailedException(BigInteger batchIndex) {
        super(L2RelayerErrorCodeEnum.L2_BATCH_COMMIT_FAILED, "batch commit failed: " + batchIndex);
    }
}
