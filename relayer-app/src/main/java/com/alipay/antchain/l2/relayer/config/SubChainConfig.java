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

package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;
import java.util.Objects;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPricePolicyEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.signservice.inject.JovayTxSignService;
import com.alipay.antchain.l2.relayer.utils.BlockchainUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.service.TxSignService;

/**
 * Configuration class for Layer 2 (Sub-chain) blockchain client components.
 * <p>
 * This class configures all necessary components for interacting with the Layer 2 network,
 * which is the rollup chain that the relayer monitors and processes. The L2 client is primarily
 * used for reading L2 blocks and transactions that need to be rolled up to Layer 1.
 * </p>
 * <p>
 * Key configuration areas:
 * <ul>
 *   <li><b>Network Connection</b>: Web3j client setup with RPC endpoint configuration</li>
 *   <li><b>Gas Price Management</b>: Configurable gas pricing strategies (static or API-based)
 *       supporting both legacy and EIP-1559 transaction types</li>
 *   <li><b>Nonce Management</b>: Transaction nonce tracking with support for both normal
 *       and high-throughput (FAST) modes using Redis-based caching</li>
 *   <li><b>Transaction Management</b>: Transaction signing and submission capabilities
 *       for any L2 operations the relayer needs to perform</li>
 * </ul>
 * </p>
 * <p>
 * The configuration supports flexible deployment scenarios through externalized properties,
 * allowing different gas price policies and nonce management strategies based on network
 * conditions and performance requirements.
 * </p>
 *
 * @see ParentChainConfig for Layer 1 configuration
 * @see RollupConfig for rollup-specific parameters
 */
@Configuration
@Slf4j
@Lazy
public class SubChainConfig {

    /**
     * The RPC endpoint URL for the Layer 2 network.
     * <p>
     * This URL is used to establish connection to the L2 blockchain node
     * for reading L2 blocks and transactions to be rolled up.
     * </p>
     */
    @Value("${l2-relayer.l2-client.rpc-url}")
    private String l2RpcUrl;

    /**
     * Gas price policy for Layer 2 transactions.
     * <p>
     * Determines how gas prices are calculated for L2 transactions:
     * <ul>
     *   <li>FROM_API: Fetch gas prices dynamically from the network</li>
     *   <li>STATIC: Use predefined static gas price values</li>
     * </ul>
     * Default: FROM_API
     * </p>
     */
    @Value("${l2-relayer.l2-client.gas-price-policy:FROM_API}")
    private GasPricePolicyEnum l2GasPricePolicy;

    /**
     * Static gas price for Layer 2 legacy transactions (in wei).
     * <p>
     * Used when {@code l2GasPricePolicy} is set to STATIC.
     * This value applies to legacy (non-EIP-1559) transactions.
     * </p>
     * <p>
     * Default: 4,100,000,000 wei (4.1 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l2-client.static-gas-price:4100000000}")
    private BigInteger l2StaticGasPrice;

    /**
     * Static maximum priority fee per gas for Layer 2 EIP-1559 transactions (in wei).
     * <p>
     * Used when {@code l2GasPricePolicy} is set to STATIC.
     * This is the tip paid to validators for transaction inclusion.
     * </p>
     * <p>
     * Default: 2,000,000,000 wei (2 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l2-client.static-max-priority-fee-per-gas:2000000000}")
    private BigInteger l2StaticMaxPriorityFeePerGas;

    /**
     * Static maximum fee per gas for Layer 2 EIP-1559 transactions (in wei).
     * <p>
     * Used when {@code l2GasPricePolicy} is set to STATIC.
     * This is the maximum total fee (base fee + priority fee) willing to pay.
     * </p>
     * <p>
     * Default: 50,000,000,000 wei (50 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l2-client.static-max-fee-per-gas:50000000000}")
    private BigInteger l2StaticMaxFeePerGas;

    /**
     * Nonce management policy for Layer 2 transactions.
     * <p>
     * Determines how transaction nonces are managed:
     * <ul>
     *   <li>NORMAL: Standard nonce management with sequential ordering</li>
     *   <li>FAST: Optimized nonce management for higher throughput</li>
     * </ul>
     * Default: NORMAL
     * </p>
     */
    @Value("${l2-relayer.l2-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l2NoncePolicy;

    /**
     * Transaction signing service for Layer 2 transactions.
     * <p>
     * This service handles the signing of transactions on the L2 network,
     * if the relayer needs to submit any transactions to L2.
     * </p>
     */
    @Getter
    @JovayTxSignService("l2TxSignService")
    private TxSignService l2TxSignService;

    /**
     * Creates the Web3j client bean for Layer 2 network.
     * <p>
     * Configures an HTTP-based Web3j client for L2 blockchain interactions,
     * primarily used for reading L2 blocks and transactions to be rolled up.
     * </p>
     *
     * @return the configured Web3j client for L2
     */
    @Bean("l2Web3j")
    public Web3j l2Web3j() {
        return Web3j.build(new HttpService(l2RpcUrl));
    }

    /**
     * Retrieves the chain ID of the Layer 2 network.
     * <p>
     * Queries the L2 network via RPC to obtain its chain ID, which is used
     * for validation and ensuring correct network connection.
     * </p>
     *
     * @param l2Web3j the Web3j client for L2
     * @return the L2 chain ID
     * @throws Exception if the RPC call fails
     */
    @Bean("l2ChainId")
    @SneakyThrows
    public BigInteger l2ChainId(@Qualifier("l2Web3j") Web3j l2Web3j) {
        return l2Web3j.ethChainId().send().getChainId();
    }

    /**
     * Creates the transaction manager for Layer 2 transactions.
     * <p>
     * This manager handles L2 transactions if the relayer needs to submit
     * any transactions to the L2 network. It manages nonce allocation and
     * transaction signing.
     * </p>
     *
     * @param chainId  the L2 chain ID
     * @param l2Web3j  the Web3j client for L2
     * @param redisson the Redis client for distributed nonce management
     * @return the configured transaction manager for L2
     * @throws Exception if initialization fails
     */
    @Bean("l2TransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l2TransactionManager(
            @Qualifier("l2ChainId") BigInteger chainId,
            @Qualifier("l2Web3j") Web3j l2Web3j,
            RedissonClient redisson,
            @Qualifier("l2NonceManager") INonceManager l2NonceManager
    ) {
        return BlockchainUtils.createTransactionManager(chainId, l2NoncePolicy, l2Web3j, Objects.requireNonNull(l2TxSignService), redisson, null, l2NonceManager);
    }

    /**
     * Creates the gas price provider for Layer 2 transactions.
     * <p>
     * Provides gas price estimates for L2 transactions based on the configured
     * policy (static or API-based). Supports both legacy and EIP-1559 gas pricing.
     * </p>
     *
     * @param l2Web3j                  the Web3j client for L2
     * @param l2GasPriceProviderConfig the gas price provider configuration
     * @return the configured gas price provider for L2
     */
    @Bean("l2GasPriceProvider")
    public IGasPriceProvider l2GasPriceProvider(
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2-gasprice-provider-conf") IGasPriceProviderConfig l2GasPriceProviderConfig
    ) {
        return BlockchainUtils.createGasPriceProvider(l2Web3j, l2GasPricePolicy, l2StaticGasPrice, l2StaticMaxFeePerGas, l2StaticMaxPriorityFeePerGas, l2GasPriceProviderConfig, null);
    }

    /**
     * Creates the nonce manager for Layer 2 transactions.
     * <p>
     * The nonce manager is responsible for tracking and allocating transaction nonces
     * for L2 transactions. It supports two modes based on the configured nonce policy:
     * </p>
     * <ul>
     *   <li><b>FAST mode</b>: Uses {@link CachedNonceManager} with Redis-based caching
     *       for optimized nonce management in high-throughput scenarios. Includes nonce
     *       reset detection using JOVAY-specific checker.</li>
     *   <li><b>NORMAL mode</b>: Uses {@link RemoteNonceManager} which queries nonces
     *       directly from the blockchain node for each transaction.</li>
     * </ul>
     *
     * @param chainId          the L2 chain ID
     * @param l2Web3j          the Web3j client for L2
     * @param redisson         the Redis client for distributed nonce caching (used in FAST mode)
     * @param rollupRepository the repository for rollup data access (used in FAST mode)
     * @return the configured nonce manager for L2 transactions
     */
    @Bean("l2NonceManager")
    public INonceManager l2NonceManager(
            @Qualifier("l2ChainId") BigInteger chainId,
            @Qualifier("l2Web3j") Web3j l2Web3j,
            RedissonClient redisson,
            IRollupRepository rollupRepository
    ) {
        if (l2NoncePolicy == EthNoncePolicyEnum.FAST) {
            return new CachedNonceManager(redisson, l2Web3j, chainId.longValue(), l2TxSignService.getAddress(),
                    ChainTypeEnum.LAYER_TWO, rollupRepository, NonceResetChecker.JOVAY);
        }
        return new RemoteNonceManager(l2TxSignService.getAddress(), l2Web3j);
    }
}
