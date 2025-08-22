package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;

public class L1ContractWarnException extends L2RelayerException {

    public L1ContractWarnException(L2RelayerErrorCodeEnum errorCode, String errMsg) {
        super(errorCode, StrUtil.format("(code: {}, err_msg: {})", errorCode.getErrorCode(), errMsg));
    }
}
