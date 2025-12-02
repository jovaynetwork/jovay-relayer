package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;

public interface IGasPriceProvider {

    BigInteger getGasPrice(String contractFunc);

    @Deprecated
    BigInteger getGasPrice();

    IGasPrice getEip1559GasPrice();

    IGasPrice getEip4844GasPrice();
}
