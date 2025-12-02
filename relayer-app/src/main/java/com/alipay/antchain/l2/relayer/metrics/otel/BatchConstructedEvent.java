package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchConstructedEvent extends BaseEvent {

    public BatchConstructedEvent(BatchWrapper batchWrapper) {
        super("BATCH_CONSTRUCT", batchWrapper.getBatchHeader().getBatchIndex());
        this.startBlockHeight = batchWrapper.getStartBlockNumber();
        this.endBlockHeight = batchWrapper.getEndBlockNumber();
    }

    private BigInteger startBlockHeight;

    private BigInteger endBlockHeight;

    @Override
    public void fillSpan(Span span) {
        super.fillSpan(span);
        span.setAttribute(ATTR_BATCH_START_KEY, startBlockHeight.longValue());
        span.setAttribute(ATTR_BATCH_END_KEY, endBlockHeight.longValue());
    }
}
