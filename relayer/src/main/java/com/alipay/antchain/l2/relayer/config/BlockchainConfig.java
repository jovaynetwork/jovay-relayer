package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.*;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.model.GasPriceProviderConfig;
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

    @Value("${l2-relayer.l1-client.http-client.protocols:}")
    private List<Protocol> l1HttpClientProtocols;

    @Value("${l2-relayer.l1-client.blob-sidecar-version:0}")
    private int blobSidecarVersion;

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

    @Getter
    @JovayTxSignService("l1LegacyPoolTxSignService")
    private TxSignService l1LegacyPoolTxSignService;

    @Getter
    @JovayTxSignService("l1BlobPoolTxSignService")
    private TxSignService l1BlobPoolTxSignService;

    @Getter
    @JovayTxSignService("l2TxSignService")
    private TxSignService l2TxSignService;

    @Bean("l1Web3j")
    public Web3j l1Web3j() {
        var httpClientBuilder = new OkHttpClient.Builder();
        if (ObjectUtil.isNotEmpty(l1HttpClientProtocols)) {
            log.info("l1 http client uses specified protocols: {}", l1HttpClientProtocols);
            httpClientBuilder.protocols(l1HttpClientProtocols);
        }
        return Web3j.build(new HttpService(l1RpcUrl, httpClientBuilder.build()));
    }

    @Bean("l1ChainId")
    @SneakyThrows
    public BigInteger l1ChainId(@Qualifier("l1Web3j") Web3j l1Web3j) {
        return l1Web3j.ethChainId().send().getChainId();
    }

    @Bean("l1BlobPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1BlobPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1BlobPoolTxSignService), redisson, blobSidecarVersion);
    }

    @Bean("l1LegacyPoolTxTransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l1LegacyPoolTxTransactionManager(
            @Qualifier("l1ChainId") BigInteger chainId,
            @Qualifier("l1Web3j") Web3j l1Web3j,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l1NoncePolicy, l1Web3j, Objects.requireNonNull(l1LegacyPoolTxSignService), redisson, blobSidecarVersion);
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

    @Bean("l2TransactionManager")
    @SneakyThrows
    public BaseRawTransactionManager l2TransactionManager(
            @Qualifier("l2ChainId") BigInteger chainId,
            @Qualifier("l2Web3j") Web3j l2Web3j,
            RedissonClient redisson
    ) {
        return createTransactionManager(chainId, l2NoncePolicy, l2Web3j, Objects.requireNonNull(l2TxSignService), redisson, 0);
    }

    @Bean("l2GasPriceProvider")
    public IGasPriceProvider l2GasPriceProvider(
            @Qualifier("l2Web3j") Web3j l2Web3j,
            @Qualifier("l2-gasprice-provider-conf") GasPriceProviderConfig l2GasPriceProviderConfig
    ) {
        return createGasPriceProvider(l2Web3j, l2GasPricePolicy, l2StaticGasPrice, l2StaticMaxFeePerGas, l2StaticMaxPriorityFeePerGas, l2GasPriceProviderConfig);
    }

    private BaseRawTransactionManager createTransactionManager(
                BigInteger chainId, EthNoncePolicyEnum noncePolicy, Web3j web3j, TxSignService txSignService,
                RedissonClient redisson, int blobSidecarVersion) {
        log.info("create tx manager by {}-{}", chainId, noncePolicy);
        return noncePolicy == EthNoncePolicyEnum.FAST ?
                new AcbFastRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, blobSidecarVersion)
                : new AcbRawTransactionManager(web3j, txSignService, chainId.longValue(), redisson, blobSidecarVersion);
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
        return switch (gasPricePolicy) {
            case FROM_API -> ApiGasPriceProvider.create(web3j, gasPriceProviderConfig);
            default -> new StaticGasPriceProvider(staticGasPrice, staticMaxFeePerGas, staticMaxPriorityFeePerGas);
        };
    }
}
