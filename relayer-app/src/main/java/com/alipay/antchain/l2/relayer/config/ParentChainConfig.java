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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ParentChainType;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
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
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.service.TxSignService;

/**
 * Configuration class for Layer 1 (Parent Chain) blockchain client components.
 * <p>
 * This class configures all necessary components for interacting with the Layer 1 network,
 * which is the parent chain where the L2 rollup batches are committed and finalized.
 * The L1 client is used for submitting batch commitments, proof verifications, and reading
 * L1 state including cross-layer messages.
 * </p>
 * <p>
 * Key configuration areas:
 * <ul>
 *   <li><b>Network Connection</b>: Web3j client setup with configurable HTTP timeouts
 *       and protocol support for optimal L1 communication</li>
 *   <li><b>Dual Transaction Pools</b>: Separate configurations for blob pool (EIP-4844)
 *       and legacy pool transactions, each with dedicated signing services and nonce managers</li>
 *   <li><b>Gas Price Management</b>: Configurable gas pricing strategies (static or API-based)
 *       supporting legacy, EIP-1559, and EIP-4844 transaction types with blob gas pricing</li>
 *   <li><b>Nonce Management</b>: Transaction nonce tracking with support for both normal
 *       and high-throughput (FAST) modes using Redis-based caching, with chain-specific
 *       nonce reset detection</li>
 *   <li><b>Transaction Management</b>: Separate transaction managers for blob and legacy
 *       transactions, handling batch commitments and proof submissions respectively</li>
 * </ul>
 * </p>
 * <p>
 * The configuration supports multiple parent chain types (Ethereum, JOVAY) with
 * chain-specific optimizations and nonce reset detection strategies. It provides
 * flexible deployment scenarios through externalized properties, allowing different
 * gas price policies and nonce management strategies based on network conditions
 * and performance requirements.
 * </p>
 *
 * @see SubChainConfig for Layer 2 configuration
 * @see RollupConfig for rollup-specific parameters
 * @see EthForkConfig for Ethereum fork-specific settings
 */
@Configuration
@Slf4j
@Lazy
public class ParentChainConfig {

    /**
     * The RPC endpoint URL for the Layer 1 network.
     * <p>
     * This URL is used to establish connection to the L1 blockchain node
     * for submitting batch commitments and reading L1 state.
     * </p>
     */
    @Value("${l2-relayer.l1-client.rpc-url}")
    private String l1RpcUrl;

    /**
     * Gas price policy for Layer 1 transactions.
     * <p>
     * Determines how gas prices are calculated for L1 transactions:
     * <ul>
     *   <li>FROM_API: Fetch gas prices dynamically from the network</li>
     *   <li>STATIC: Use predefined static gas price values</li>
     * </ul>
     * Default: FROM_API
     * </p>
     */
    @Value("${l2-relayer.l1-client.gas-price-policy:FROM_API}")
    private GasPricePolicyEnum l1GasPricePolicy;

    /**
     * Static gas price for Layer 1 legacy transactions (in wei).
     * <p>
     * Used when {@code l1GasPricePolicy} is set to STATIC.
     * This value applies to legacy (non-EIP-1559) transactions.
     * </p>
     * <p>
     * Default: 4,100,000,000 wei (4.1 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l1-client.static-gas-price:4100000000}")
    private BigInteger l1StaticGasPrice;

    /**
     * Static maximum priority fee per gas for Layer 1 EIP-1559 transactions (in wei).
     * <p>
     * Used when {@code l1GasPricePolicy} is set to STATIC.
     * This is the tip paid to miners/validators for transaction inclusion.
     * </p>
     * <p>
     * Default: 2,000,000,000 wei (2 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l1-client.static-max-priority-fee-per-gas:2000000000}")
    private BigInteger l1StaticMaxPriorityFeePerGas;

    /**
     * Static maximum fee per gas for Layer 1 EIP-1559 transactions (in wei).
     * <p>
     * Used when {@code l1GasPricePolicy} is set to STATIC.
     * This is the maximum total fee (base fee + priority fee) willing to pay.
     * </p>
     * <p>
     * Default: 50,000,000,000 wei (50 Gwei)
     * </p>
     */
    @Value("${l2-relayer.l1-client.static-max-fee-per-gas:50000000000}")
    private BigInteger l1StaticMaxFeePerGas;

    /**
     * Nonce management policy for Layer 1 transactions.
     * <p>
     * Determines how transaction nonces are managed:
     * <ul>
     *   <li>NORMAL: Standard nonce management with sequential ordering</li>
     *   <li>FAST: Optimized nonce management for higher throughput</li>
     * </ul>
     * Default: NORMAL
     * </p>
     */
    @Value("${l2-relayer.l1-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l1NoncePolicy;

    /**
     * Transaction signing service for Layer 1 legacy pool transactions.
     * <p>
     * This service handles the signing of legacy (non-blob) transactions
     * that are submitted to the L1 network, such as finalization transactions.
     * </p>
     */
    @Getter
    @JovayTxSignService("l1LegacyPoolTxSignService")
    private TxSignService l1LegacyPoolTxSignService;

    /**
     * Transaction signing service for Layer 1 blob pool transactions.
     * <p>
     * This service handles the signing of EIP-4844 blob transactions
     * that are submitted to the L1 network for batch commitments.
     * </p>
     */
    @Getter
    @JovayTxSignService(
            value = "l1BlobPoolTxSignService",
            conditionalProperty = "l2-relayer.rollup.da-type",
            conditionalPropertyHavingValue = "BLOBS",
            conditionalPropertyMatchIfMissing = true
    )
    private TxSignService l1BlobPoolTxSignService;

    /**
     * Creates the Web3j client bean for Layer 1 network.
     * <p>
     * Configures an HTTP-based Web3j client with optional custom HTTP protocols.
     * The client is used for all L1 blockchain interactions including reading
     * state and submitting transactions.
     * </p>
     *
     * @return the configured Web3j client for L1
     */
    @Bean("l1Web3j")
    public Web3j l1Web3j(
            @Value("${l2-relayer.l1-client.http-client.write-timeout:60}") long writeTimeout,
            @Value("${l2-relayer.l1-client.http-client.read-timeout:60}") long readTimeout,
            @Value("${l2-relayer.l1-client.http-client.protocols:}") List<Protocol> l1HttpClientProtocols
    ) {
        var httpClientBuilder = new OkHttpClient.Builder();
        if (ObjectUtil.isNotEmpty(l1HttpClientProtocols)) {
            log.info("l1 http client uses specified protocols: {}", l1HttpClientProtocols);
            httpClientBuilder.protocols(l1HttpClientProtocols);
            httpClientBuilder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
            httpClientBuilder.readTimeout(readTimeout, TimeUnit.SECONDS);
        }
        return Web3j.build(new HttpService(l1RpcUrl, httpClientBuilder.build()));
    }

    /**
     * Retrieves the chain ID of the Layer 1 network.
     * <p>
     * Queries the L1 network via RPC to obtain its chain ID, which is used
     * for transaction signing and validation.
     * </p>
     *
     * @param l1Web3j the Web3j client for L1
     * @return the L1 chain ID
     * @throws Exception if the RPC call fails
     */
    @Bean("l1ChainId")
    @SneakyThrows
    public BigInteger l1ChainId(@Qualifier("l1Web3j") Web3j l1Web3j) {
        return l1Web3j.ethChainId().send().getChainId();
    }

    /**
     * Creates the transaction manager for Layer 1 blob pool transactions.
     * <p>
     * This manager handles EIP-4844 blob transactions used for batch commitments.
     * It manages nonce allocation, transaction signing, and submission to the L1
     * blob transaction pool.
     * </p>
     *
     * @param chainId           the L1 chain ID
     * @param l1Web3j           the Web3j client for L1
     * @param redisson          the Redis client for distributed nonce management
     * @param ethBlobForkConfig the blob fork configuration
     * @return the configured transaction manager for L1 blob transactions
     * @throws Exception if initialization fails
     */
    @Bean("l1BlobPoolTxTransactionManager")
    @ConditionalOnProperty(name = "l2-relayer.rollup.da-type", havingValue = "BLOBS", matchIfMissing = true)
    @SneakyThrows
    public BaseRawTransactionManager l1BlobPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson,
            EthBlobForkConfig ethBlobForkConfig,
            @Qualifier("l1BlobNonceManager") INonceManager l1BlobNonceManager
    ) {
        return BlockchainUtils.createTransactionManager(chainId, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1BlobPoolTxSignService),
                redisson, ethBlobForkConfig, l1BlobNonceManager);
    }

    /**
     * Creates the transaction manager for Layer 1 legacy pool transactions.
     * <p>
     * This manager handles legacy (non-blob) transactions such as finalization
     * transactions. It manages nonce allocation, transaction signing, and
     * submission to the L1 legacy transaction pool.
     * </p>
     *
     * @param chainId           the L1 chain ID
     * @param l1Web3j           the Web3j client for L1
     * @param redisson          the Redis client for distributed nonce management
     * @param ethBlobForkConfig the blob fork configuration
     * @return the configured transaction manager for L1 legacy transactions
     * @throws Exception if initialization fails
     */
    @Bean("l1LegacyPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1LegacyPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson,
            @Autowired(required = false) EthBlobForkConfig ethBlobForkConfig,
            @Qualifier("l1LegacyNonceManager") INonceManager l1LegacyNonceManager
    ) {
        return BlockchainUtils.createTransactionManager(chainId, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1LegacyPoolTxSignService),
                redisson, ethBlobForkConfig, l1LegacyNonceManager);
    }

    /**
     * Creates the gas price provider for Layer 1 transactions.
     * <p>
     * Provides gas price estimates for L1 transactions based on the configured
     * policy (static or API-based). Supports both legacy and EIP-1559 gas pricing.
     * </p>
     *
     * @param l1Web3j                  the Web3j client for L1
     * @param l1GasPriceProviderConfig the gas price provider configuration
     * @param ethBlobForkConfig        the blob fork configuration
     * @return the configured gas price provider for L1
     */
    @Bean("l1GasPriceProvider")
    public IGasPriceProvider l1GasPriceProvider(
            @Qualifier("l1Web3j") Web3j l1Web3j,
            @Qualifier("l1-gasprice-provider-conf") IGasPriceProviderConfig l1GasPriceProviderConfig,
            @Autowired(required = false) EthBlobForkConfig ethBlobForkConfig
    ) {
        return BlockchainUtils.createGasPriceProvider(l1Web3j, l1GasPricePolicy, l1StaticGasPrice, l1StaticMaxFeePerGas,
                l1StaticMaxPriorityFeePerGas, l1GasPriceProviderConfig, ethBlobForkConfig);
    }

    /**
     * Creates the nonce manager for Layer 1 blob pool transactions.
     * <p>
     * The nonce manager is responsible for tracking and allocating transaction nonces
     * for L1 blob transactions (EIP-4844). It supports two modes based on the configured
     * nonce policy:
     * </p>
     * <ul>
     *   <li><b>FAST mode</b>: Uses {@link CachedNonceManager} with Redis-based caching
     *       for optimized nonce management in high-throughput scenarios. Includes nonce
     *       reset detection based on the parent chain type.</li>
     *   <li><b>NORMAL mode</b>: Uses {@link RemoteNonceManager} which queries nonces
     *       directly from the blockchain node for each transaction.</li>
     * </ul>
     *
     * @param chainId             the L1 chain ID
     * @param l1Web3j             the Web3j client for L1
     * @param redisson            the Redis client for distributed nonce caching (used in FAST mode)
     * @param rollupRepository    the repository for rollup data access (used in FAST mode)
     * @param l1NonceResetChecker the nonce reset checker for detecting chain reorganizations
     * @return the configured nonce manager for L1 blob transactions
     */
    @ConditionalOnProperty(name = "l2-relayer.rollup.da-type", havingValue = "BLOBS", matchIfMissing = true)
    @Bean("l1BlobNonceManager")
    public INonceManager l1BlobNonceManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson,
            IRollupRepository rollupRepository,
            INonceResetChecker l1NonceResetChecker
    ) {
        if (l1NoncePolicy == EthNoncePolicyEnum.FAST) {
            return new CachedNonceManager(redisson, l1Web3j, chainId.longValue(), l1BlobPoolTxSignService.getAddress(),
                    ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        }
        return new RemoteNonceManager(l1BlobPoolTxSignService.getAddress(), l1Web3j);
    }

    /**
     * Creates the nonce manager for Layer 1 legacy pool transactions.
     * <p>
     * The nonce manager is responsible for tracking and allocating transaction nonces
     * for L1 legacy (non-blob) transactions. It supports two modes based on the configured
     * nonce policy:
     * </p>
     * <ul>
     *   <li><b>FAST mode</b>: Uses {@link CachedNonceManager} with Redis-based caching
     *       for optimized nonce management in high-throughput scenarios. Includes nonce
     *       reset detection based on the parent chain type.</li>
     *   <li><b>NORMAL mode</b>: Uses {@link RemoteNonceManager} which queries nonces
     *       directly from the blockchain node for each transaction.</li>
     * </ul>
     *
     * @param chainId             the L1 chain ID
     * @param l1Web3j             the Web3j client for L1
     * @param redisson            the Redis client for distributed nonce caching (used in FAST mode)
     * @param rollupRepository    the repository for rollup data access (used in FAST mode)
     * @param l1NonceResetChecker the nonce reset checker for detecting chain reorganizations
     * @return the configured nonce manager for L1 legacy transactions
     */
    @Bean("l1LegacyNonceManager")
    public INonceManager l1LegacyNonceManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson,
            IRollupRepository rollupRepository,
            INonceResetChecker l1NonceResetChecker
    ) {
        if (l1NoncePolicy == EthNoncePolicyEnum.FAST) {
            return new CachedNonceManager(redisson, l1Web3j, chainId.longValue(), l1LegacyPoolTxSignService.getAddress(),
                    ChainTypeEnum.LAYER_ONE, rollupRepository, l1NonceResetChecker);
        }
        return new RemoteNonceManager(l1LegacyPoolTxSignService.getAddress(), l1Web3j);
    }

    /**
     * Creates the nonce reset checker for Layer 1 transactions.
     * <p>
     * The nonce reset checker is used to detect when nonces need to be reset due to
     * chain reorganizations or other network events. Different parent chain types have
     * different reset detection strategies:
     * </p>
     * <ul>
     *   <li><b>ETHEREUM</b>: Uses Ethereum Geth-specific nonce reset detection logic</li>
     *   <li><b>JOVAY</b>: Uses JOVAY-specific nonce reset detection logic</li>
     * </ul>
     *
     * @param parentChainType the type of parent chain (L1)
     * @return the configured nonce reset checker for the parent chain type
     */
    @Bean
    public INonceResetChecker l1NonceResetChecker(
            @Value("${l2-relayer.rollup.config.parent-chain-type:ETHEREUM}") ParentChainType parentChainType
    ) {
        return switch (parentChainType) {
            case ETHEREUM -> NonceResetChecker.ETHEREUM_GETH;
            case JOVAY -> NonceResetChecker.JOVAY;
        };
    }
}
