package com.alipay.antchain.l2.relayer.config;

import javax.annotation.Resource;

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
