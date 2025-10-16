package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;

import cn.hutool.json.JSONObject;

public record Eip4844GasPrice(BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerBlobGas) implements IGasPrice {

    public IGasPrice validate() {
        if (maxFeePerGas.compareTo(maxPriorityFeePerGas) < 0) {
            throw new IllegalArgumentException("max priority fee per gas higher than max fee per gas");
        }
        if (maxFeePerBlobGas.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("max fee per blob gas must be positive");
        }
        return this;
    }

    public String toJson() {
        var obj = new JSONObject();
        obj.set("maxFeePerGas", maxFeePerGas);
        obj.set("maxPriorityFeePerGas", maxPriorityFeePerGas);
        obj.set("maxFeePerBlobGas", maxFeePerBlobGas);
        return obj.toString();
    }
}
