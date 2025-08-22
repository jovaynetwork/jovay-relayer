package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class ProofCommitFailedException extends L2RelayerException {
    public ProofCommitFailedException(BigInteger batchIndex) {
        super(L2RelayerErrorCodeEnum.BATCH_PROOF_COMMIT_FAILED, "batch proof commit failed: " + batchIndex);
    }
}
