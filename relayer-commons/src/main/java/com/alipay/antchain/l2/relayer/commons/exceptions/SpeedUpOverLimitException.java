package com.alipay.antchain.l2.relayer.commons.exceptions;

import lombok.Getter;

@Getter
public class SpeedUpOverLimitException extends NoNeedToSpeedUpException {

    public SpeedUpOverLimitException(String formatStr, Object... objects) {
        super(L2RelayerErrorCodeEnum.SPEED_UP_OVER_LIMIT, formatStr, objects);
    }
}
