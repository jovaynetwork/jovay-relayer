package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public class PreviousGasPriceJustFineException extends NoNeedToSpeedUpException {

    private final BigInteger currentMaxFeePerGas;

    private final BigInteger currentMaxPriorityFeePerGas;

    private final BigInteger previousMaxFeePerGas;

    private final BigInteger previousMaxPriorityFeePerGas;

    public PreviousGasPriceJustFineException(
            BigInteger previousMaxFeePerGas,
            BigInteger previousMaxPriorityFeePerGas,
            BigInteger currentMaxFeePerGas,
            BigInteger currentMaxPriorityFeePerGas
    ) {
        super(L2RelayerErrorCodeEnum.NO_NEED_TO_SPEED_UP_TX,
                StrUtil.format("Current net gas price is not higher than previous gas price, no need to speed up:" +
                               " net price is (maxFee: {}, maxPriorityFee: {}) now and previous in tx is (maxFee: {}, maxPriorityFee: {})",
                        currentMaxFeePerGas, currentMaxPriorityFeePerGas, previousMaxFeePerGas, previousMaxPriorityFeePerGas));
        this.currentMaxFeePerGas = currentMaxFeePerGas;
        this.currentMaxPriorityFeePerGas = currentMaxPriorityFeePerGas;
        this.previousMaxFeePerGas = previousMaxFeePerGas;
        this.previousMaxPriorityFeePerGas = previousMaxPriorityFeePerGas;
    }
}
