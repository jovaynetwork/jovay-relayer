package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderSupplierEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class GasPriceProviderConfig implements IGasPriceProviderConfig {
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

    private double eip4844PriorityFeePerGasIncreasedPercentage = 1;

    private BigInteger maxPriceLimit = BigInteger.ZERO;

    /**
     * Only for legacy tx now.
     */
    @Deprecated(since = "0.8")
    private BigInteger extraGasPrice = BigInteger.valueOf(2_000_000_000L);

    /**
     * The eip4844 tx minimum priority price
     */
    private BigInteger minimumEip4844PriorityPrice = BigInteger.ZERO;

    /**
     * The eip1559 tx minimum priority price
     */
    private BigInteger minimumEip1559PriorityPrice = BigInteger.ZERO;

    public double getBlobFeeMultiplier(BigInteger maxFeeFromNode) {
        return maxFeeFromNode.compareTo(feePerBlobGasDividingVal) > 0 ? largerFeePerBlobGasMultiplier : smallerFeePerBlobGasMultiplier;
    }
}
