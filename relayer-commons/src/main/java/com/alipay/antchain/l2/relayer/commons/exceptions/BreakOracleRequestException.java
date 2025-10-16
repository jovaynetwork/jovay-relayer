package com.alipay.antchain.l2.relayer.commons.exceptions;

public class BreakOracleRequestException extends L2RelayerException {

    public BreakOracleRequestException(String msgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.BREAK_ORACLE_REQUEST, msgFormat, args);
    }
}
