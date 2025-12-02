package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticGasLimitProvider implements IGasLimitProvider {

    private BigInteger gasLimit;

    public BigInteger getGasLimit(String contractFunc) {
        return gasLimit;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }
}
