package com.alipay.antchain.l2.relayer.commons.exceptions;

import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;

public class SpeedupAsyncNowException extends NoNeedToSpeedUpException {

    public SpeedupAsyncNowException(ReliableTransactionDO tx) {
        super(L2RelayerErrorCodeEnum.SPEEDUP_TX_ASYNC, "{}-{}-{} tx is speeding up by other thread", tx.getChainType(), tx.getTransactionType(), tx.getBatchIndex());
    }
}
