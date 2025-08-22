package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEvent implements IEvent {

    public BaseEvent(String stage, BigInteger batchIndex) {
        this.stage = stage;
        this.batchIndex = batchIndex;
    }

    private String stage;

    private BigInteger batchIndex;

    private long timestamp = System.currentTimeMillis();

    @Override
    public void fillSpan(Span span) {
        span.updateName(SPAN_PREFIX);
        span.setAttribute(ATTR_STAGE_KEY, stage);
        span.setAttribute(ATTR_BATCH_IDX_KEY, batchIndex.longValue());
        span.setAttribute(ATTR_TIMESTAMP_KEY, timestamp);
    }
}
