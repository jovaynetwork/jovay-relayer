package com.alipay.antchain.l2.relayer.metrics.selfreport;

import java.util.concurrent.CompletableFuture;

public class DummySelfReportMetric implements ISelfReportMetric {

    @Override
    public CompletableFuture<Void> reportAsync(String recordKey) {
        return null;
    }

    @Override
    public CompletableFuture<Void> recordEndAndReportAsync(String recordKey) {
        return null;
    }

    @Override
    public void recordStart(RollupMetricRecord record) {
    }

    @Override
    public void recordEnd(RollupMetricRecord record) {
    }
}
