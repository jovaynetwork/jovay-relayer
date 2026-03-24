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
