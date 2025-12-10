package com.alipay.antchain.l2.relayer.engine.dynamicconf;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.dal.repository.ISystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

@Slf4j
public class PrefixedDynamicConfig implements IDynamicConfig {

    private final static String REDIS_FOR_DYNAMIC_CONFIG_PREFIX = "relayer-dynamic-config@";

    private final static String REDIS_SCHEDULED_EXECUTOR_SERVICE_LOCK_PREFIX = "persist_config@";

    private final int persistInterval;

    private final String prefix;

    private final RMap<String, String> configCacheMap;

    private final RLock persistLock;

    private final ISystemConfigRepository systemConfigRepository;

    private final ValueDesensitizeFilter valueDesensitizeFilter;

    public PrefixedDynamicConfig(
            String prefix,
            int persistInterval,
            RedissonClient redisson,
            ISystemConfigRepository systemConfigRepository,
            List<String> sensitiveKeys,
            ScheduledExecutorService dynamicPersisterScheduledExecutors
    ) {
        this.prefix = prefix;
        this.systemConfigRepository = systemConfigRepository;
        this.configCacheMap = redisson.getMap(redisMapKey());
        this.persistLock = redisson.getLock(REDIS_SCHEDULED_EXECUTOR_SERVICE_LOCK_PREFIX + prefix);
        this.persistInterval = persistInterval;
        this.valueDesensitizeFilter = new ValueDesensitizeFilter(ObjectUtil.isEmpty(sensitiveKeys) ? sensitiveKeys
                : sensitiveKeys.stream().map(this::keyWithPrefix).toList());
        initCacheFromStorage();
        dynamicPersisterScheduledExecutors.scheduleWithFixedDelay(this::persistCurrConfig, persistInterval, persistInterval, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return configCacheMap.getOrDefault(keyWithPrefix(key), null);
    }

    @Override
    public void set(String key, String value) {
        configCacheMap.put(keyWithPrefix(key), value);
    }

    private String keyWithPrefix(String key) {
        return prefix + "@" + key;
    }

    private String redisMapKey() {
        return REDIS_FOR_DYNAMIC_CONFIG_PREFIX + prefix;
    }

    private void initCacheFromStorage() {
        var configs = systemConfigRepository.getPrefixedSystemConfig(prefix + "@");
        for (var entry : configs.entrySet()) {
            configCacheMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        log.info("Init dynamic config cache for {} from storage with: {}", prefix, JSON.toJSONString(configs, valueDesensitizeFilter));
    }

    private void persistCurrConfig() {
        try {
            if (!persistLock.tryLock(0, persistInterval, TimeUnit.SECONDS)) {
                return;
            }
            for (var entry : configCacheMap.entrySet()) {
                if (ObjectUtil.isNull(entry.getValue())) {
                    configCacheMap.remove(entry.getKey());
                    log.error("Remove null value from config cache map: {}", entry.getKey());
                    continue;
                }
                log.debug("Persist current config: {} - {}", entry.getKey(), valueDesensitizeFilter.desensitize(entry.getValue()));
                systemConfigRepository.setSystemConfig(entry.getKey(), entry.getValue());
            }
            log.info("Persist current config for {} with {} entries successfully", prefix, configCacheMap.size());
        } catch (Throwable t) {
            log.error("Persist current config failed: ", t);
        }
    }
}
