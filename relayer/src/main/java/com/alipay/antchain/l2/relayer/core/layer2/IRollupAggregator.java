package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;

import com.alipay.antchain.l2.trace.BasicBlockTrace;

public interface IRollupAggregator {

    BigInteger process(BasicBlockTrace blockTrace, BigInteger currHeight);
}
