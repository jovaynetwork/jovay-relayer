package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;

public interface IGasPrice {

    BigInteger maxFeePerGas();

    BigInteger maxPriorityFeePerGas();

    BigInteger maxFeePerBlobGas();

    BigInteger baseFee();

    IGasPrice validate();

    String toJson();
}
