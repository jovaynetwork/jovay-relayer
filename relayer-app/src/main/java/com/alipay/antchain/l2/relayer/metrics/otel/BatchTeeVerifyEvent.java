package com.alipay.antchain.l2.relayer.metrics.otel;

import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchTeeVerifyEvent extends BaseEvent {

    public BatchTeeVerifyEvent(BigInteger batchIndex) {
        super("TEE_VERIFY_BATCH", batchIndex);
    }
}