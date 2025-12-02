package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

public class RollupEconomicStrategyNotAllowedException extends L2RelayerException {

    public RollupEconomicStrategyNotAllowedException(BigInteger baseFee, BigInteger priorityFee) {
        super(
                L2RelayerErrorCodeEnum.ROLLUP_ECONOMIC_STRATEGY_NOT_ALLOWED,
                "rollup economic strategy not allowed: baseFee {} and priority {}",
                baseFee, priorityFee
        );
    }
}
