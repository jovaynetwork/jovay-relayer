package com.alipay.antchain.l2.relayer.core.layer2.economic;

import com.alipay.antchain.l2.relayer.commons.exceptions.RollupEconomicStrategyNotAllowedException;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.IGasPrice;

public interface IRollupCostChecker {

    void check(IGasPrice gasPrice) throws RollupEconomicStrategyNotAllowedException;
}
