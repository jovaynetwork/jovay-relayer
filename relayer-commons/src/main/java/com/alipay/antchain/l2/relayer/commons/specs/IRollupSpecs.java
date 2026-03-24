/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
