package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;

import cn.hutool.json.JSONObject;

public record Eip1559GasPrice(BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas) {

    public Eip1559GasPrice validate() {
        if (maxFeePerGas.compareTo(maxPriorityFeePerGas) < 0) {
            throw new IllegalArgumentException("max priority fee per gas higher than max fee per gas");
        }
        return this;
    }

    public String toJson() {
        var obj = new JSONObject();
        obj.set("maxFeePerGas", maxFeePerGas);
        obj.set("maxPriorityFeePerGas", maxPriorityFeePerGas);
        return obj.toString();
    }
}
