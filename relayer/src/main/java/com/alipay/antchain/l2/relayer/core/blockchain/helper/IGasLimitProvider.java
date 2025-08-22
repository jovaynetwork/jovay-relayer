package com.alipay.antchain.l2.relayer.core.blockchain.helper;

import java.math.BigInteger;

public interface IGasLimitProvider {

    BigInteger getGasLimit(String contractFunc);

    @Deprecated
    BigInteger getGasLimit();
}
