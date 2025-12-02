package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OtelMetricImpl implements IOtelMetric {

    private static Span startDefaultSpan(BigInteger batchIndex) {
        return GlobalOpenTelemetry.get()
                .getTracer("l2relayer-batch", batchIndex.toString())
                .spanBuilder("")
                .setParent(Context.current())
                .startSpan();
    }

    @Override
    public void recordBatchConstructedEvent(BatchWrapper batchWrapper) {
        CompletableFuture.runAsync(
                () -> {
                    log.debug("recordBatchConstructedEvent: {}", batchWrapper.getBatchHeader().getBatchIndex());
                    Span span = startDefaultSpan(batchWrapper.getBatchHeader().getBatchIndex());
                    new BatchConstructedEvent(batchWrapper).fillSpan(span);
                    span.end();
                }
        ).exceptionally(throwable -> {
            log.error("unexpected otel metric recordBatchConstructedEvent exception: ", throwable);
            return null;
        });
    }

    @Override
    public void recordBatchCommitEvent(BigInteger batchIndex) {
        CompletableFuture.runAsync(
                () -> {
                    log.debug("recordBatchCommitEvent: {}", batchIndex);
                    Span span = startDefaultSpan(batchIndex);
                    new BatchCommitEvent(batchIndex).fillSpan(span);
                    span.end();
                }
        ).exceptionally(throwable -> {
            log.error("unexpected otel metric recordBatchCommitEvent exception: ", throwable);
            return null;
        });
    }

    @Override
    public void recordBatchTeeVerifyEvent(BigInteger batchIndex) {
        CompletableFuture.runAsync(
                () -> {
                    log.debug("recordBatchVerifyEvent: {}", batchIndex);
                    Span span = startDefaultSpan(batchIndex);
                    new BatchTeeVerifyEvent(batchIndex).fillSpan(span);
                    span.end();
                }
        ).exceptionally(throwable -> {
            log.error("unexpected otel metric recordBatchVerifyEvent exception: ", throwable);
            return null;
        });
    }

    @Override
    public void recordBatchStableEvent(BigInteger batchIndex) {
        CompletableFuture.runAsync(
                () -> {
                    log.debug("recordBatchStableEvent: {}", batchIndex);
                    Span span = startDefaultSpan(batchIndex);
                    new BatchStableEvent(batchIndex).fillSpan(span);
                    span.end();
                }
        ).exceptionally(throwable -> {
            log.error("unexpected otel metric recordBatchStableEvent exception: ", throwable);
            return null;
        });
    }
}
