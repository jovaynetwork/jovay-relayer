package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class L1GasPriceTooHighException extends L2RelayerException {
    public L1GasPriceTooHighException(BigInteger baseFee, BigInteger maxPriorityFee, BigInteger priceLimit) {
        super(
                L2RelayerErrorCodeEnum.L1_GAS_PRICE_TOO_HIGH,
                "gas price from L1 too high: base-fee: {}, max-priority: {}, limit: {}",
                baseFee, maxPriorityFee, priceLimit
        );
    }
}
