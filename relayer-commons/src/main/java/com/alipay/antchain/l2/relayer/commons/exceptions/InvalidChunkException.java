package com.alipay.antchain.l2.relayer.commons.exceptions;

public class InvalidChunkException extends L2RelayerException {

    public InvalidChunkException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.CORE_CHUNK_INVALID, detailMsgFormat, args);
    }
}
