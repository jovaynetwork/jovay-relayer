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

import jakarta.annotation.Resource;

import com.alipay.antchain.l2.relayer.metrics.selfreport.DummySelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.ISelfReportMetric;
import com.alipay.antchain.l2.relayer.metrics.selfreport.SelfReportMetric;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MetricsConfig {

    @Value("${l2-relayer.metrics.self-report.url:}")
    private String selfReportUrl;

    @Value("${l2-relayer.metrics.self-report.cache-ttl:180}")
    private int cacheTTL;

    @Resource
    private RedissonClient redisson;

    @Bean
    @ConditionalOnProperty(name = "l2-relayer.metrics.self-report.switch", havingValue = "true")
    public ISelfReportMetric selfReportMetric() {
        return new SelfReportMetric(selfReportUrl, redisson, cacheTTL);
    }

    @Bean
    @ConditionalOnProperty(name = "l2-relayer.metrics.self-report.switch", havingValue = "false", matchIfMissing = true)
    public ISelfReportMetric dummySelfReportMetric() {
        return new DummySelfReportMetric();
    }
}
