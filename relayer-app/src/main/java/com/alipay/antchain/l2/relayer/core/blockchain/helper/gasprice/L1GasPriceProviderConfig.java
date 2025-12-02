package com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice;

import java.util.concurrent.ScheduledExecutorService;

import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("l1-gasprice-provider-conf")
@Lazy
public class L1GasPriceProviderConfig extends DynamicGasPriceProviderConfig {

    public L1GasPriceProviderConfig(
            @Value("${l2-relayer.l1-client.gas-price-provider-conf.persist-interval:60}") int persistInterval,
            @Autowired RedissonClient redisson,
            @Autowired ISystemConfigRepository systemConfigRepository,
            @Autowired ScheduledExecutorService dynamicPersisterScheduledExecutors,
            @Qualifier("l1-gasprice-provider-default-conf") GasPriceProviderConfig defaultConfig
    ) {
        super(
                persistInterval,
                redisson,
                "l1-gasprice-provider-conf",
                systemConfigRepository,
                dynamicPersisterScheduledExecutors,
                defaultConfig
        );
    }
}
