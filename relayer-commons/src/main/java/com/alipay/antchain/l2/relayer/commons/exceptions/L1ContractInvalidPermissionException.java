package com.alipay.antchain.l2.relayer.commons.exceptions;

public class L1ContractInvalidPermissionException extends L1ContractFatalException {

    public L1ContractInvalidPermissionException(String errMsg, String revertReason) {
        super(L2RelayerErrorCodeEnum.CALL_WITH_INVALID_PERMISSION, errMsg, revertReason);
    }
}