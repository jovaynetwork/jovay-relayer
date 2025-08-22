package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.status.L2ErrorCode;

public class RemoteServiceRetryException extends L2RelayerException {

    public RemoteServiceRetryException(L2ErrorCode errorCode, String errMsg, String detailMsg) {
        super(L2RelayerErrorCodeEnum.CORE_REMOTE_SERVICE_ERROR, StrUtil.format("(code: {}, err_msg: {}) : {}", errorCode.getNumber(), errMsg, detailMsg));
    }

    public RemoteServiceRetryException(L2ErrorCode errorCode, String errMsg, String detailMsgFormat, Object... args) {
        super(L2RelayerErrorCodeEnum.CORE_REMOTE_SERVICE_ERROR, StrUtil.format("(code: {}, err_msg: {}) : {}", errorCode.getNumber(), errMsg, StrUtil.format(detailMsgFormat, args)));
    }
}
