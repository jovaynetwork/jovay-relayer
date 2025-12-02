package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderSupplierEnum;

/**
 * Interface defining methods for configuring gas price provider settings.
 */
public interface IGasPriceProviderConfig {

    /**
     * Gets the supplier enum for gas price provider.
     *
     * @return the supplier enum
     */
    GasPriceProviderSupplierEnum getGasPriceProviderSupplier();

    /**
     * Gets the URL of the gas provider.
     *
     * @return the gas provider URL
     */
    String getGasProviderUrl();

    /**
     * Gets the API key for accessing the gas provider service.
     *
     * @return the API key
     */
    String getApiKey();

    /**
     * Gets the interval at which gas prices should be updated.
     *
     * @return the gas update interval
     */
    long getGasUpdateInterval();

    /**
     * Gets the percentage increase in gas price.
     *
     * @return the gas price increased percentage
     */
    double getGasPriceIncreasedPercentage();

    /**
     * Gets the dividing value for fee per blob gas.
     *
     * @return the fee per blob gas dividing value
     */
    BigInteger getFeePerBlobGasDividingVal();

    /**
     * Gets the multiplier for larger fee per blob gas.
     *
     * @return the larger fee per blob gas multiplier
     */
    double getLargerFeePerBlobGasMultiplier();

    /**
     * Gets the multiplier for smaller fee per blob gas.
     *
     * @return the smaller fee per blob gas multiplier
     */
    double getSmallerFeePerBlobGasMultiplier();

    /**
     * Gets the multiplier for base fee.
     *
     * @return the base fee multiplier
     */
    int getBaseFeeMultiplier();

    /**
     * Gets the percentage increase in priority fee per gas.
     *
     * @return the priority fee per gas increased percentage
     */
    double getPriorityFeePerGasIncreasedPercentage();

    /**
     * Gets the EIP4844 priority fee per gas increased percentage.
     *
     * @return the EIP4844 priority fee per gas increased percentage
     */
    double getEip4844PriorityFeePerGasIncreasedPercentage();

    /**
     * Gets the maximum price limit.
     *
     * @return the maximum price limit
     */
    BigInteger getMaxPriceLimit();

    /**
     * Gets the additional gas price.
     *
     * @return the extra gas price
     */
    BigInteger getExtraGasPrice();

    /**
     * Gets the minimum EIP4844 priority price.
     *
     * @return the minimum EIP4844 priority price
     */
    BigInteger getMinimumEip4844PriorityPrice();

    /**
     * Gets the minimum EIP1559 priority price.
     *
     * @return the minimum EIP1559 priority price
     */
    BigInteger getMinimumEip1559PriorityPrice();

    /**
     * Sets the supplier enum for gas price provider.
     *
     * @param gasPriceProviderSupplier the supplier enum
     */
    void setGasPriceProviderSupplier(GasPriceProviderSupplierEnum gasPriceProviderSupplier);

    /**
     * Sets the URL of the gas provider.
     *
     * @param gasProviderUrl the gas provider URL
     */
    void setGasProviderUrl(String gasProviderUrl);

    /**
     * Sets the API key for accessing the gas provider service.
     *
     * @param apiKey the API key
     */
    void setApiKey(String apiKey);

    /**
     * Sets the interval at which gas prices should be updated.
     *
     * @param gasUpdateInterval the gas update interval
     */
    void setGasUpdateInterval(long gasUpdateInterval);

    /**
     * Sets the percentage increase in gas price.
     *
     * @param gasPriceIncreasedPercentage the gas price increased percentage
     */
    void setGasPriceIncreasedPercentage(double gasPriceIncreasedPercentage);

    /**
     * Sets the dividing value for fee per blob gas.
     *
     * @param feePerBlobGasDividingVal the fee per blob gas dividing value
     */
    void setFeePerBlobGasDividingVal(BigInteger feePerBlobGasDividingVal);

    /**
     * Sets the multiplier for larger fee per blob gas.
     *
     * @param largerFeePerBlobGasMultiplier the larger fee per blob gas multiplier
     */
    void setLargerFeePerBlobGasMultiplier(double largerFeePerBlobGasMultiplier);

    /**
     * Sets the multiplier for smaller fee per blob gas.
     *
     * @param smallerFeePerBlobGasMultiplier the smaller fee per blob gas multiplier
     */
    void setSmallerFeePerBlobGasMultiplier(double smallerFeePerBlobGasMultiplier);

    /**
     * Sets the multiplier for base fee.
     *
     * @param baseFeeMultiplier the base fee multiplier
     */
    void setBaseFeeMultiplier(int baseFeeMultiplier);

    /**
     * Sets the percentage increase in priority fee per gas.
     *
     * @param priorityFeePerGasIncreasedPercentage the priority fee per gas increased percentage
     */
    void setPriorityFeePerGasIncreasedPercentage(double priorityFeePerGasIncreasedPercentage);

    /**
     * Sets the EIP4844 priority fee per gas increased percentage.
     *
     * @param eip4844PriorityFeePerGasIncreasedPercentage the EIP4844 priority fee per gas increased percentage
     */
    void setEip4844PriorityFeePerGasIncreasedPercentage(double eip4844PriorityFeePerGasIncreasedPercentage);

    /**
     * Sets the maximum price limit.
     *
     * @param maxPriceLimit the maximum price limit
     */
    void setMaxPriceLimit(BigInteger maxPriceLimit);

    /**
     * Sets the additional gas price.
     *
     * @param extraGasPrice the extra gas price
     */
    void setExtraGasPrice(BigInteger extraGasPrice);

    /**
     * Sets the minimum EIP4844 priority price.
     *
     * @param minimumEip4844PriorityPrice the minimum EIP4844 priority price
     */
    void setMinimumEip4844PriorityPrice(BigInteger minimumEip4844PriorityPrice);

    /**
     * Sets the minimum EIP1559 priority price.
     *
     * @param minimumEip1559PriorityPrice the minimum EIP1559 priority price
     */
    void setMinimumEip1559PriorityPrice(BigInteger minimumEip1559PriorityPrice);

    /**
     * Calculates the Blob fee multiplier based on the maximum fee from node.
     *
     * @param maxFeeFromNode the maximum fee from node
     * @return the blob fee multiplier
     */
    double getBlobFeeMultiplier(BigInteger maxFeeFromNode);
}