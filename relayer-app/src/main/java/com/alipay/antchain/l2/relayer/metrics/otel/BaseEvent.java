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
