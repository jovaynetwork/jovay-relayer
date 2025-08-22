package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;

public interface IGasPriceProvider {

    BigInteger getGasPrice(String contractFunc);

    @Deprecated
    BigInteger getGasPrice();

    Eip1559GasPrice getEip1559GasPrice();

    BigInteger getMaxFeePerBlobGas();
}
