package com.alipay.antchain.l2.relayer.commons.exceptions;

public class TxNotFoundButRetryException extends L2RelayerException {
    public TxNotFoundButRetryException() {
        super(L2RelayerErrorCodeEnum.TX_NOT_FOUND_BUT_RETRY, "l2 tx not found but we need to retry the query");
    }
}
