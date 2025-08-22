package com.alipay.antchain.l2.relayer.metrics.selfreport;

import java.util.concurrent.CompletableFuture;

public interface ISelfReportMetric {

    CompletableFuture<Void> reportAsync(String recordKey);

    CompletableFuture<Void> recordEndAndReportAsync(String recordKey);

    void recordStart(RollupMetricRecord record);

    void recordEnd(RollupMetricRecord record);
}
