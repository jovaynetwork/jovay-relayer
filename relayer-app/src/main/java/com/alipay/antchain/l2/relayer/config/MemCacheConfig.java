
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

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemCacheConfig {

    @Value("${l2-relayer.cache.system_conf.ttl:30000}")
    private long systemConfigCacheTTL;

    @Value("${l2-relayer.cache.l2-block-trace-for-curr-chunk.capacity:1300}")
    private int l2BlockTraceCacheTTL;

    @Bean(name = "systemConfigCache")
    public Cache<String, String> systemConfigCache() {
        return CacheUtil.newLRUCache(10, systemConfigCacheTTL);
    }

    @Bean
    Cache<BigInteger, BasicBlockTrace> l2BlockTracesCacheForCurrChunk() {
        return CacheUtil.newFIFOCache(l2BlockTraceCacheTTL);
    }
}
