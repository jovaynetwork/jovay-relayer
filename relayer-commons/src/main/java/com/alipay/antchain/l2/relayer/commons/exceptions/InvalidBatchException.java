package com.alipay.antchain.l2.relayer.commons.exceptions;

public class InvalidBatchException extends L2RelayerException {

    public InvalidBatchException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.CORE_BATCH_INVALID, detailMsgFormat, args);
    }
}
