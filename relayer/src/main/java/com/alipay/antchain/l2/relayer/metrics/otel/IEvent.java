package com.alipay.antchain.l2.relayer.metrics.otel;

import io.opentelemetry.api.trace.Span;

public interface IEvent {

    String SPAN_PREFIX = "ANTCHAIN_ROLLUP";

    String ATTR_STAGE_KEY = "antchain.rollup.stage";

    String ATTR_BATCH_IDX_KEY = "antchain.rollup.batch.index";

    String ATTR_BATCH_START_KEY = "antchain.rollup.batch.block.index.start";

    String ATTR_BATCH_END_KEY = "antchain.rollup.batch.block.index.end";

    String ATTR_TIMESTAMP_KEY = "antchain.rollup.timestamp";

    void fillSpan(Span span);
}
