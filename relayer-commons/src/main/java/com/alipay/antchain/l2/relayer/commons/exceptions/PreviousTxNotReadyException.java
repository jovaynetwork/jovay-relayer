package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.ReliableTransactionStateEnum;

public class PreviousTxNotReadyException extends NoNeedToSpeedUpException {
    public PreviousTxNotReadyException(BigInteger prevBatchIndex, String previousTxHash, ReliableTransactionStateEnum state) {
        super(L2RelayerErrorCodeEnum.PREVIOUS_TX_NOT_READY, "previous tx not ready: {} {} {}", prevBatchIndex, previousTxHash, state);
    }
}
