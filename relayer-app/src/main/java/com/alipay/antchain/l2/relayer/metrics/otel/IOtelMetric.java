package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;

public interface IOtelMetric {

    void recordBatchConstructedEvent(BatchWrapper batchWrapper);

    void recordBatchCommitEvent(BigInteger batchIndex);

    void recordBatchTeeVerifyEvent(BigInteger batchIndex);

    void recordBatchStableEvent(BigInteger batchIndex);
}
