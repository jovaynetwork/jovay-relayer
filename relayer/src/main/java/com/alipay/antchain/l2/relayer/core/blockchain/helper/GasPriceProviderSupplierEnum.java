package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;

@Getter
public enum GasPriceProviderSupplierEnum {
    ETHERSCAN("etherscan"),

    OWLRACLE("owlracle"),

    ETHEREUM("ethereum");

    private String name;

    GasPriceProviderSupplierEnum(String name) {
        this.name = name;
    }

    @JSONField
    public String getName() {
        return name;
    }
}
