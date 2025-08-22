package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class BatchNotReadyException extends L2RelayerException {
    public BatchNotReadyException(BigInteger batchIndex) {
        super(L2RelayerErrorCodeEnum.L2_BATCH_NOT_READY, "batch not ready: " + batchIndex);
    }
}
