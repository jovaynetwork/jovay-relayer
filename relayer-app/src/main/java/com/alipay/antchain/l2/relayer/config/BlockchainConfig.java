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
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.IGasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.EthNoncePolicyEnum;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPricePolicyEnum;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.signservice.inject.JovayTxSignService;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
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
 * Configuration class for blockchain client connections and transaction management.
 * <p>
 * This class configures Web3j clients for both Layer 1 (L1) and Layer 2 (L2) networks,
 * including RPC connections, gas price strategies, nonce management policies, and
 * transaction signing services. It provides separate configurations for different
 * transaction pools (legacy and blob) on L1.
 * </p>
 * <p>
 * Key configuration areas:
 * <ul>
 *   <li>L1 and L2 RPC endpoint connections</li>
 *   <li>Gas price policies (static or API-based)</li>
 *   <li>Nonce management strategies (normal or fast)</li>
 *   <li>Transaction managers for different transaction types</li>
 *   <li>Transaction signing services</li>
 * </ul>
 * </p>
 *
 * @author Aone Copilot
 * @since 1.0
 */
@Configuration
@Slf4j
@Lazy
public class BlockchainConfig {

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
    @JovayTxSignService("l1BlobPoolTxSignService")
    private TxSignService l1BlobPoolTxSignService;

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
     * @param rollupRepository  the rollup repository
     * @return the configured transaction manager for L1 blob transactions
     * @throws Exception if initialization fails
     */
    @Bean("l1BlobPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1BlobPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson,
            EthBlobForkConfig ethBlobForkConfig,
            IRollupRepository rollupRepository
    ) {
        return createTransactionManager(chainId, ChainTypeEnum.LAYER_ONE, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1BlobPoolTxSignService),
                redisson, ethBlobForkConfig, rollupRepository);
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
            EthBlobForkConfig ethBlobForkConfig,
            IRollupRepository rollupRepository
    ) {
        return createTransactionManager(chainId, ChainTypeEnum.LAYER_ONE, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1LegacyPoolTxSignService),
                redisson, ethBlobForkConfig, rollupRepository);
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
            EthBlobForkConfig ethBlobForkConfig
    ) {
        return createGasPriceProvider(l1Web3j, l1GasPricePolicy, l1StaticGasPrice, l1StaticMaxFeePerGas,
                l1StaticMaxPriorityFeePerGas, l1GasPriceProviderConfig, ethBlobForkConfig);
    }

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
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, ChainTypeEnum.LAYER_TWO, l2NoncePolicy, l2Web3j, Objects.requireNonNull(l2TxSignService), redisson, null, null);
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
        return createGasPriceProvider(l2Web3j, l2GasPricePolicy, l2StaticGasPrice, l2StaticMaxFeePerGas, l2StaticMaxPriorityFeePerGas, l2GasPriceProviderConfig, null);
    }

    /**
     * Creates a transaction manager based on the specified nonce policy.
     * <p>
     * This factory method instantiates either a fast or normal transaction manager
     * depending on the nonce policy. Fast managers optimize for higher throughput
     * while normal managers ensure strict sequential nonce ordering.
     * </p>
     *
     * @param chainId           the chain ID for transaction signing
     * @param chainType         the chain type
     * @param noncePolicy       the nonce management policy (NORMAL or FAST)
     * @param web3j             the Web3j client instance
     * @param txSignService     the transaction signing service
     * @param redisson          the Redis client for distributed nonce management
     * @param ethBlobForkConfig the blob fork configuration (null for L2)
     * @return the configured transaction manager
     */
    private BaseRawTransactionManager createTransactionManager(
            BigInteger chainId, ChainTypeEnum chainType, EthNoncePolicyEnum noncePolicy, Web3j web3j, TxSignService txSignService,
            RedissonClient redisson, EthBlobForkConfig ethBlobForkConfig, IRollupRepository rollupRepository) {
        log.info("create tx manager by {}-{}", chainId, noncePolicy);
        return noncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, ethBlobForkConfig, chainType, rollupRepository)
                : new AcbRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, ethBlobForkConfig);
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
    private IGasPriceProvider createGasPriceProvider(
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
