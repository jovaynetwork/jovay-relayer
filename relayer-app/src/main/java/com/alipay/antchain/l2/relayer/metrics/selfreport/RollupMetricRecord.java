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
