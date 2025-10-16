package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip1559GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.Eip4844GasPrice;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticGasPriceProvider implements IGasPriceProvider {

    private BigInteger gasPrice;

    private BigInteger maxFeePerGas;

    private BigInteger maxPriorityFeePerGas;

    @Override
    public BigInteger getGasPrice(String contractFunc) {
        return gasPrice;
    }

    @Override
    public BigInteger getGasPrice() {
        return gasPrice;
    }

    @Override
    public Eip1559GasPrice getEip1559GasPrice() {
        return new Eip1559GasPrice(maxFeePerGas, maxPriorityFeePerGas);
    }

    @Override
    public IGasPrice getEip4844GasPrice() {
        return new Eip4844GasPrice(maxFeePerGas, maxPriorityFeePerGas, BigInteger.valueOf(20_000_000_000L));
    }
}
