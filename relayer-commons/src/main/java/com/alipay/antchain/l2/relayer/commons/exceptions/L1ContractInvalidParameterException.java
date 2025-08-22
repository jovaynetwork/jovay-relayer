package com.alipay.antchain.l2.relayer.commons.exceptions;

public class L1ContractInvalidParameterException extends L1ContractFatalException {

    public L1ContractInvalidParameterException(String errMsg, String revertReason) {
        super(L2RelayerErrorCodeEnum.CALL_WITH_INVALID_PARAMETER, errMsg, revertReason);
    }
}
