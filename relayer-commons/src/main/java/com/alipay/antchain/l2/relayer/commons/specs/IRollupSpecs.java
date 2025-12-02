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
     * @param currTimestamp the timestamp of the first block for the curr batch
     * @return {@link ForkInfo }
     */
    ForkInfo getFork(long currTimestamp);
}
