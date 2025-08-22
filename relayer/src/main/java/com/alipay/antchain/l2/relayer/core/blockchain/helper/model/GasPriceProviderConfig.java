package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.GasPriceProviderSupplierEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GasPriceProviderConfig {
    private GasPriceProviderSupplierEnum gasPriceProviderSupplier = GasPriceProviderSupplierEnum.ETHEREUM;

    private String gasProviderUrl;

    private String apiKey;

    private long gasUpdateInterval;

    private double gasPriceIncreasedPercentage = 0.1;

    private BigInteger feePerBlobGasDividingVal = BigInteger.valueOf(10_000_000L);

    private double largerFeePerBlobGasMultiplier = 2;

    private double smallerFeePerBlobGasMultiplier = 1000;

    private int baseFeeMultiplier = 2;

    private double priorityFeePerGasIncreasedPercentage = 0.1;

    private BigInteger extraGasPrice = BigInteger.valueOf(2_000_000_000L);

    public double getBlobFeeMultiplier(BigInteger maxFeeFromNode) {
        return maxFeeFromNode.compareTo(feePerBlobGasDividingVal) > 0 ? largerFeePerBlobGasMultiplier : smallerFeePerBlobGasMultiplier;
    }
}
