package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;
import org.web3j.protocol.core.Response;

public class TxSimulateException extends L2RelayerException {
    public TxSimulateException(Response.Error error) {
        super(L2RelayerErrorCodeEnum.ROLLUP_SEND_TX_ERROR,
                StrUtil.format("{}: {}", error.getCode(), error.getMessage()));
    }
}
