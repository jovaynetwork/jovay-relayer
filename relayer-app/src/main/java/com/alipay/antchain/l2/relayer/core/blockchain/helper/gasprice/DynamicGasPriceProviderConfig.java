/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderSupplierEnum;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.l2.relayer.engine.dynamicconf.PrefixedDynamicConfig;
import org.redisson.api.RedissonClient;

public class DynamicGasPriceProviderConfig extends PrefixedDynamicConfig implements IGasPriceProviderConfig {

    private static final List<String> SENSITIVE_KEYS_TO_HIDE_VALUES = ListUtil.toList(GasPriceProviderConfig.Fields.apiKey);

    private final GasPriceProviderConfig defaultConfig;

    public DynamicGasPriceProviderConfig(
            int persistInterval,
            RedissonClient redisson,
            String prefix,
            ISystemConfigRepository systemConfigRepository,
            ScheduledExecutorService dynamicPersisterScheduledExecutors,
            GasPriceProviderConfig defaultConfig
    ) {
        super(prefix, persistInterval, redisson, systemConfigRepository, SENSITIVE_KEYS_TO_HIDE_VALUES, dynamicPersisterScheduledExecutors);
        this.defaultConfig = defaultConfig;
    }

    @Override
    public GasPriceProviderSupplierEnum getGasPriceProviderSupplier() {
        var val = super.get(GasPriceProviderConfig.Fields.gasPriceProviderSupplier);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getGasPriceProviderSupplier();
        }
        return GasPriceProviderSupplierEnum.valueOf(val);
    }

    @Override
    public String getGasProviderUrl() {
        var val = super.get(GasPriceProviderConfig.Fields.gasProviderUrl);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getGasProviderUrl();
        }
        return val;
    }

    @Override
    public String getApiKey() {
        var val = super.get(GasPriceProviderConfig.Fields.apiKey);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getApiKey();
        }
        return val;
    }

    @Override
    public long getGasUpdateInterval() {
        var val = super.get(GasPriceProviderConfig.Fields.gasUpdateInterval);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getGasUpdateInterval();
        }
        return Long.parseLong(val);
    }

    @Override
    public double getGasPriceIncreasedPercentage() {
        var val = super.get(GasPriceProviderConfig.Fields.gasPriceIncreasedPercentage);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getGasPriceIncreasedPercentage();
        }
        return Double.parseDouble(val);
    }

    @Override
    public BigInteger getFeePerBlobGasDividingVal() {
        var val = super.get(GasPriceProviderConfig.Fields.feePerBlobGasDividingVal);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getFeePerBlobGasDividingVal();
        }
        return new BigInteger(val);
    }

    @Override
    public double getLargerFeePerBlobGasMultiplier() {
        var val = super.get(GasPriceProviderConfig.Fields.largerFeePerBlobGasMultiplier);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getLargerFeePerBlobGasMultiplier();
        }
        return Double.parseDouble(val);
    }

    @Override
    public double getSmallerFeePerBlobGasMultiplier() {
        var val = super.get(GasPriceProviderConfig.Fields.smallerFeePerBlobGasMultiplier);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getSmallerFeePerBlobGasMultiplier();
        }
        return Double.parseDouble(val);
    }

    @Override
    public int getBaseFeeMultiplier() {
        var val = super.get(GasPriceProviderConfig.Fields.baseFeeMultiplier);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getBaseFeeMultiplier();
        }
        return Integer.parseInt(val);
    }

    @Override
    public double getPriorityFeePerGasIncreasedPercentage() {
        var val = super.get(GasPriceProviderConfig.Fields.priorityFeePerGasIncreasedPercentage);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getPriorityFeePerGasIncreasedPercentage();
        }
        return Double.parseDouble(val);
    }

    @Override
    public double getEip4844PriorityFeePerGasIncreasedPercentage() {
        var val = super.get(GasPriceProviderConfig.Fields.eip4844PriorityFeePerGasIncreasedPercentage);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getEip4844PriorityFeePerGasIncreasedPercentage();
        }
        return Double.parseDouble(val);
    }

    @Override
    public BigInteger getMaxPriceLimit() {
        var val = super.get(GasPriceProviderConfig.Fields.maxPriceLimit);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getMaxPriceLimit();
        }
        return new BigInteger(val);
    }

    @Override
    public BigInteger getExtraGasPrice() {
        var val = super.get(GasPriceProviderConfig.Fields.extraGasPrice);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getExtraGasPrice();
        }
        return new BigInteger(val);
    }

    @Override
    public BigInteger getMinimumEip4844PriorityPrice() {
        var val = super.get(GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getMinimumEip4844PriorityPrice();
        }
        return new BigInteger(val);
    }

    @Override
    public BigInteger getMinimumEip1559PriorityPrice() {
        var val = super.get(GasPriceProviderConfig.Fields.minimumEip1559PriorityPrice);
        if (StrUtil.isEmpty(val)) {
            return defaultConfig.getMinimumEip1559PriorityPrice();
        }
        return new BigInteger(val);
    }

    @Override
    public void setGasPriceProviderSupplier(GasPriceProviderSupplierEnum gasPriceProviderSupplier) {
        super.set(GasPriceProviderConfig.Fields.gasPriceProviderSupplier, gasPriceProviderSupplier.name());
    }

    @Override
    public void setGasProviderUrl(String gasProviderUrl) {
        super.set(GasPriceProviderConfig.Fields.gasProviderUrl, gasProviderUrl);
    }

    @Override
    public void setApiKey(String apiKey) {
        super.set(GasPriceProviderConfig.Fields.apiKey, apiKey);
    }

    @Override
    public void setGasUpdateInterval(long gasUpdateInterval) {
        super.set(GasPriceProviderConfig.Fields.gasUpdateInterval, String.valueOf(gasUpdateInterval));
    }

    @Override
    public void setGasPriceIncreasedPercentage(double gasPriceIncreasedPercentage) {
        super.set(GasPriceProviderConfig.Fields.gasPriceIncreasedPercentage, String.valueOf(gasPriceIncreasedPercentage));
    }

    @Override
    public void setFeePerBlobGasDividingVal(BigInteger feePerBlobGasDividingVal) {
        super.set(GasPriceProviderConfig.Fields.feePerBlobGasDividingVal, feePerBlobGasDividingVal.toString());
    }

    @Override
    public void setLargerFeePerBlobGasMultiplier(double largerFeePerBlobGasMultiplier) {
        super.set(GasPriceProviderConfig.Fields.largerFeePerBlobGasMultiplier, String.valueOf(largerFeePerBlobGasMultiplier));
    }

    @Override
    public void setSmallerFeePerBlobGasMultiplier(double smallerFeePerBlobGasMultiplier) {
        super.set(GasPriceProviderConfig.Fields.smallerFeePerBlobGasMultiplier, String.valueOf(smallerFeePerBlobGasMultiplier));
    }

    @Override
    public void setBaseFeeMultiplier(int baseFeeMultiplier) {
        super.set(GasPriceProviderConfig.Fields.baseFeeMultiplier, String.valueOf(baseFeeMultiplier));
    }

    @Override
    public void setPriorityFeePerGasIncreasedPercentage(double priorityFeePerGasIncreasedPercentage) {
        super.set(GasPriceProviderConfig.Fields.priorityFeePerGasIncreasedPercentage, String.valueOf(priorityFeePerGasIncreasedPercentage));
    }

    @Override
    public void setEip4844PriorityFeePerGasIncreasedPercentage(double eip4844PriorityFeePerGasIncreasedPercentage) {
        super.set(GasPriceProviderConfig.Fields.eip4844PriorityFeePerGasIncreasedPercentage, String.valueOf(eip4844PriorityFeePerGasIncreasedPercentage));
    }

    @Override
    public void setMaxPriceLimit(BigInteger maxPriceLimit) {
        super.set(GasPriceProviderConfig.Fields.maxPriceLimit, maxPriceLimit.toString());
    }

    @Override
    public void setExtraGasPrice(BigInteger extraGasPrice) {
        super.set(GasPriceProviderConfig.Fields.extraGasPrice, extraGasPrice.toString());
    }

    @Override
    public void setMinimumEip4844PriorityPrice(BigInteger minimumEip4844PriorityPrice) {
        super.set(GasPriceProviderConfig.Fields.minimumEip4844PriorityPrice, minimumEip4844PriorityPrice.toString());
    }

    @Override
    public void setMinimumEip1559PriorityPrice(BigInteger minimumEip1559PriorityPrice) {
        super.set(GasPriceProviderConfig.Fields.minimumEip1559PriorityPrice, minimumEip1559PriorityPrice.toString());
    }

    @Override
    public double getBlobFeeMultiplier(BigInteger maxFeeFromNode) {
        return maxFeeFromNode.compareTo(getFeePerBlobGasDividingVal()) > 0 ? getLargerFeePerBlobGasMultiplier() : getSmallerFeePerBlobGasMultiplier();
    }
}