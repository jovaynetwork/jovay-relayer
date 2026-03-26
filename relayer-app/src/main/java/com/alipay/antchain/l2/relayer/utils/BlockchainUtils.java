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

package com.alipay.antchain.l2.relayer.utils;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPricePolicyEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.web3j.protocol.Web3j;
import org.web3j.service.TxSignService;

@Slf4j
public class BlockchainUtils {

    /**
     * Creates a transaction manager based on the specified nonce policy.
     * <p>
     * This factory method instantiates either a fast or normal transaction manager
     * depending on the nonce policy. Fast managers optimize for higher throughput
     * while normal managers ensure strict sequential nonce ordering.
     * </p>
     *
     * @param chainId           the chain ID for transaction signing
     * @param noncePolicy       the nonce management policy (NORMAL or FAST)
     * @param web3j             the Web3j client instance
     * @param txSignService     the transaction signing service
     * @param redisson          the Redis client for distributed nonce management
     * @param ethBlobForkConfig the blob fork configuration (null for L2)
     * @return the configured transaction manager
     */
    public static BaseRawTransactionManager createTransactionManager(
            BigInteger chainId, EthNoncePolicyEnum noncePolicy, Web3j web3j, TxSignService txSignService,
            RedissonClient redisson, EthBlobForkConfig ethBlobForkConfig, INonceManager nonceManager) {
        log.info("create tx manager by {}-{}", chainId, noncePolicy);
        return noncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, ethBlobForkConfig, nonceManager)
                : new AcbRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, ethBlobForkConfig, nonceManager);
    }

    /**
     * Creates a gas price provider based on the specified policy.
     * <p>
     * This factory method instantiates either an API-based or static gas price
     * provider. API-based providers fetch real-time gas prices from the network,
     * while static providers use predefined values.
     * </p>
     *
     * @param web3j                      the Web3j client instance
     * @param gasPricePolicy             the gas price policy (FROM_API or STATIC)
     * @param staticGasPrice             the static gas price for legacy transactions
     * @param staticMaxFeePerGas         the static max fee per gas for EIP-1559
     * @param staticMaxPriorityFeePerGas the static max priority fee for EIP-1559
     * @param gasPriceProviderConfig     the gas price provider configuration
     * @param ethBlobForkConfig          the blob fork configuration (null for L2)
     * @return the configured gas price provider
     */
    public static IGasPriceProvider createGasPriceProvider(
            Web3j web3j,
            GasPricePolicyEnum gasPricePolicy,
            BigInteger staticGasPrice,
            BigInteger staticMaxFeePerGas,
            BigInteger staticMaxPriorityFeePerGas,
            IGasPriceProviderConfig gasPriceProviderConfig,
            EthBlobForkConfig ethBlobForkConfig
    ) {
        log.info("create gas price provider by policy {}", gasPricePolicy);
        return switch (gasPricePolicy) {
            case FROM_API -> ApiGasPriceProvider.create(web3j, gasPriceProviderConfig, ethBlobForkConfig);
            default -> new StaticGasPriceProvider(staticGasPrice, staticMaxFeePerGas, staticMaxPriorityFeePerGas);
        };
    }
}
