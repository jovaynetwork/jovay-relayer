package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public class L1ContractFatalException extends L2RelayerException {

    private final String revertReason;

    public L1ContractFatalException(L2RelayerErrorCodeEnum errorCode, String errMsg, String revertReason) {
        super(errorCode, StrUtil.format("code: {}, err_msg: {}", errorCode.getErrorCode(), errMsg));
        this.revertReason = revertReason;
    }
}
