package com.alipay.antchain.l2.relayer.commons.exceptions;

import lombok.Getter;

@Getter
public class NoNeedToSpeedUpException extends L2RelayerException {

    public NoNeedToSpeedUpException(L2RelayerErrorCodeEnum errorCode, String formatStr, Object... objects) {
        super(errorCode, formatStr, objects);
    }
}
