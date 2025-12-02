package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.Map;

public interface IL2MerkleTreeAggregator {

    Map<BigInteger, byte[]> aggregate(BigInteger currBatchIndex);
}
