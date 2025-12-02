package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchStableEvent extends BaseEvent {

    public BatchStableEvent(BigInteger batchIndex) {
        super("BATCH_STABLE", batchIndex);
    }
}