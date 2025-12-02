package com.alipay.antchain.l2.relayer.core.layer2.economic;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;

public class NopeChecker implements IRollupCostChecker {

    public static final NopeChecker INSTANCE = new NopeChecker();

    @Override
    public void check(IGasPrice gasPrice) {
    }
}
