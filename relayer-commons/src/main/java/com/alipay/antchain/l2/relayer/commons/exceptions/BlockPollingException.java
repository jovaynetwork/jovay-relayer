package com.alipay.antchain.l2.relayer.commons.exceptions;

public class BlockPollingException extends L2RelayerException {
    public BlockPollingException(String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.SERVICE_BLOCK_POLLING_ERROR, detailMsgFormat, args);
    }

    public BlockPollingException(Throwable t, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.SERVICE_BLOCK_POLLING_ERROR, t, detailMsgFormat, args);
    }

    public BlockPollingException(String detailMsg) {
        super(L2RelayerErrorCodeEnum.SERVICE_BLOCK_POLLING_ERROR, detailMsg);
    }

    public BlockPollingException(Throwable t, String detailMsg) {
        super(L2RelayerErrorCodeEnum.SERVICE_BLOCK_POLLING_ERROR, t, detailMsg);
    }
}
