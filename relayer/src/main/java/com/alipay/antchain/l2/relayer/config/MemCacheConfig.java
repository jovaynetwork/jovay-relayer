
package com.alipay.antchain.l2.relayer.config;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemCacheConfig {

    @Value("${relayer.cache.system_conf.ttl:30000}")
    private long systemConfigCacheTTL;

    @Bean(name = "systemConfigCache")
    public Cache<String, String> systemConfigCache() {
        return CacheUtil.newLRUCache(10, systemConfigCacheTTL);
    }
}
