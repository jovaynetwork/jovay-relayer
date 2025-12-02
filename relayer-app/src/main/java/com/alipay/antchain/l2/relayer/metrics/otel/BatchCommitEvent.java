package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchCommitEvent extends BaseEvent {

    public BatchCommitEvent(BigInteger batchIndex) {
        super("COMMIT_BATCH", batchIndex);
    }
}