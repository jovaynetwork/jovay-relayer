package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;

public class InvalidRollupSpecsException extends L2RelayerException {

    public InvalidRollupSpecsException(String errMsg, Object... params) {
        super(L2RelayerErrorCodeEnum.INVALID_ROLLUP_SPECS, StrUtil.format("invalid rollup specs: {}", errMsg, params));
    }
}
