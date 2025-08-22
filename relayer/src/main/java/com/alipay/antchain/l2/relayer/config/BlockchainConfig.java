package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@Slf4j
@Lazy
public class BlockchainConfig {

    @Value("${l2-relayer.l1-client.rpc-url}")
    private String l1RpcUrl;

    @Value("${l2-relayer.l1-client.gas-price-policy:FROM_API}")
    private GasPricePolicyEnum l1GasPricePolicy;

    @Value("${l2-relayer.l1-client.static-gas-price:4100000000}")
    private BigInteger l1StaticGasPrice;

    @Value("${l2-relayer.l1-client.static-max-priority-fee-per-gas:2000000000}")
    private BigInteger l1StaticMaxPriorityFeePerGas;

    @Value("${l2-relayer.l1-client.static-max-fee-per-gas:50000000000}")
    private BigInteger l1StaticMaxFeePerGas;

    @Value("${l2-relayer.l1-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l1NoncePolicy;

    @Value("${l2-relayer.l2-client.rpc-url}")
    private String l2RpcUrl;

    @Value("${l2-relayer.l2-client.gas-price-policy:FROM_API}")
    private GasPricePolicyEnum l2GasPricePolicy;

    @Value("${l2-relayer.l2-client.static-gas-price:4100000000}")
    private BigInteger l2StaticGasPrice;

    @Value("${l2-relayer.l2-client.static-max-priority-fee-per-gas:2000000000}")
    private BigInteger l2StaticMaxPriorityFeePerGas;

    @Value("${l2-relayer.l2-client.static-max-fee-per-gas:50000000000}")
    private BigInteger l2StaticMaxFeePerGas;

    @Value("${l2-relayer.l2-client.nonce-policy:NORMAL}")
    private EthNoncePolicyEnum l2NoncePolicy;

    @Bean("l1Web3j")
    public Web3j l1Web3j() {
        return Web3j.build(new HttpService(l1RpcUrl, new OkHttpClient.Builder().build()));
    }

    @Bean("l1ChainId")
    @SneakyThrows
    public BigInteger l1ChainId(@Qualifier("l1Web3j") Web3j l1Web3j) {
        return l1Web3j.ethChainId().send().getChainId();
    }

    @Bean("l1BlobPoolTxCredentials")
    public Credentials l1BlobPoolTxCredentials(@Value("${l2-relayer.l1-client.blob-pool-tx-account.private-key}") String l1PrivateKey) {
        return Credentials.create(l1PrivateKey);
    }

    @Bean("l1LegacyPoolTxCredentials")
    public Credentials l1LegacyPoolTxCredentials(@Value("${l2-relayer.l1-client.legacy-pool-tx-account.private-key}") String l1PrivateKey) {
        return Credentials.create(l1PrivateKey);
    }

    @Bean("l1BlobPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1BlobPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            @Qualifier("l1BlobPoolTxCredentials") Credentials l1Credentials,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l1NoncePolicy, l1Web3j, l1Credentials, redisson);
    }

    @Bean("l1LegacyPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1LegacyPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            @Qualifier("l1LegacyPoolTxCredentials") Credentials l1Credentials,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l1NoncePolicy, l1Web3j, l1Credentials, redisson);
    }

    @Bean("l1GasPriceProvider")
    public IGasPriceProvider l1GasPriceProvider(
            @Qualifier("l1Web3j") Web3j l1Web3j,
            @Qualifier("l1-gasprice-provider-conf") GasPriceProviderConfig l1GasPriceProviderConfig
    ) {
        return createGasPriceProvider(l1Web3j, l1GasPricePolicy, l1StaticGasPrice, l1StaticMaxFeePerGas, l1StaticMaxPriorityFeePerGas, l1GasPriceProviderConfig);
    }

    @Bean("l2Web3j")
    public Web3j l2Web3j() {
        return Web3j.build(new HttpService(l2RpcUrl));
    }

    @Bean("l2ChainId")
    @SneakyThrows
    public BigInteger l2ChainId(@Qualifier("l2Web3j") Web3j l2Web3j) {
        return l2Web3j.ethChainId().send().getChainId();
    }

    @Bean("l2Credentials")
    public Credentials l2Credentials(@Value("${l2-relayer.l2-client.private-key}") String l2PrivateKey) {
        return Credentials.create(l2PrivateKey);
    }

    @Bean("l2TransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l2TransactionManager(
            @Qualifier("l2ChainId") BigInteger chainId,
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2Credentials") Credentials l2Credentials,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l2NoncePolicy, l2Web3j, l2Credentials, redisson);
    }

    @Bean("l2GasPriceProvider")
    public IGasPriceProvider l2GasPriceProvider(
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2-gasprice-provider-conf") GasPriceProviderConfig l2GasPriceProviderConfig
    ) {
        return createGasPriceProvider(l2Web3j, l2GasPricePolicy, l2StaticGasPrice, l2StaticMaxFeePerGas, l2StaticMaxPriorityFeePerGas, l2GasPriceProviderConfig);
    }

    private BaseRawTransactionManager createTransactionManager(BigInteger chainId, EthNoncePolicyEnum noncePolicy, Web3j web3j, Credentials credentials, RedissonClient redisson) {
        log.info("create tx manager by {}-{}", chainId, noncePolicy);
        return noncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(web3j, credentials, chainId.longValue(), redisson)
                : new AcbRawTransactionManager(web3j, credentials, chainId.longValue(), redisson);
    }

    private IGasPriceProvider createGasPriceProvider(
            Web3j web3j,
            GasPricePolicyEnum gasPricePolicy,
            BigInteger staticGasPrice,
            BigInteger staticMaxFeePerGas,
            BigInteger staticMaxPriorityFeePerGas,
            GasPriceProviderConfig gasPriceProviderConfig
    ) {
        log.info("create gas price provider by policy {}", gasPricePolicy);
        switch (gasPricePolicy) {
            case FROM_API:
                return ApiGasPriceProvider.create(web3j, gasPriceProviderConfig);
            case STATIC:
            default:
                return new StaticGasPriceProvider(staticGasPrice, staticMaxFeePerGas, staticMaxPriorityFeePerGas);
        }
    }
}
