package com.alipay.antchain.l2.relayer.commons.exceptions;

public class OracleException extends L2RelayerException {
    public OracleException(String code, String msg, String message) {
        super(L2RelayerErrorCodeEnum.valueOf(code), msg, message);
    }
}
