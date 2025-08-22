package com.alipay.antchain.l2.relayer.commons.exceptions;

public class L1ContractSeriousException extends L1ContractFatalException {

    public L1ContractSeriousException(String errMsg, String revertReason) {
        super(L2RelayerErrorCodeEnum.CALL_WITH_SERIOUS_ERROR, errMsg, revertReason);
    }
}
