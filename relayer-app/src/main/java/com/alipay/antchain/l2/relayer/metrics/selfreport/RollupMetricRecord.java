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

package com.alipay.antchain.l2.relayer.metrics.selfreport;

import java.math.BigInteger;
import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RollupMetricRecord {

    public static RollupMetricRecord createCommitBatchRecord(BigInteger batchIndex) {
        RollupMetricRecord record = new RollupMetricRecord();
        record.setKey(MetricUtils.getBatchCommitMetricKey(batchIndex));
        record.setFrom("l2-relayer");
        record.setTo("layer1-commitBatch");
        record.setTimePoint(new Date());
        return record;
    }

    public static RollupMetricRecord createCommitProofRecord(ProveTypeEnum proveType, BigInteger batchIndex) {
        RollupMetricRecord record = new RollupMetricRecord();
        record.setKey(MetricUtils.getProofCommitMetricKey(proveType, batchIndex));
        record.setFrom("l2-relayer");
        record.setTo("layer1-commit" + proveType);
        record.setTimePoint(new Date());
        return record;
    }

    public RollupMetricRecord() {
        this.timePoint = new Date();
    }

    private String key;

    private String from;

    private String to;

    private Date timePoint;
}
