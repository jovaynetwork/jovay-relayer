package com.alipay.antchain.l2.relayer.commons.specs;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.specs.forks.ForkInfo;

public interface IRollupSpecs {

    /**
     * Get the network.
     *
     * @return {@link RollupSpecsNetwork }
     */
    RollupSpecsNetwork getNetwork();

    /**
     * Get the Jovay chain id.
     *
     * @return {@link BigInteger }
     */
    BigInteger getLayer2ChainId();

    /**
     * Get the Ethereum chain id.
     * @return {@link BigInteger }
     */
    BigInteger getLayer1ChainId();

    /**
     * Get the fork info by current batch index.
     * @param currBatchIndex curr batch index
     * @return {@link ForkInfo }
     */
    ForkInfo getFork(BigInteger currBatchIndex);
}
