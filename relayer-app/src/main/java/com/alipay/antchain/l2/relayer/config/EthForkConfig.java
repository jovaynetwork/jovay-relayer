package com.alipay.antchain.l2.relayer.config;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;

@Configuration
@Lazy
@Slf4j
public class EthForkConfig {

    @Bean
    public EthBlobForkConfig ethBlobForkConfig(
            @Qualifier("l1ChainId") BigInteger l1ChainId,
            @Value("${l2-relayer.l1-client.eth-network-fork.unknown-network-config-file:null}") Resource unknownEthNetworkForkBlobConfigFile
    ) {
        return new EthBlobForkConfig(l1ChainId, unknownEthNetworkForkBlobConfigFile);
    }
}
