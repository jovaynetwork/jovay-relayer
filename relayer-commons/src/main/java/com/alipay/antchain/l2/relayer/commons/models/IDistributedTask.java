package com.alipay.antchain.l2.relayer.commons.models;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;

public interface IDistributedTask {

    void setNodeId(String nodeId);

    void setStartTime(long startTime);

    void setTimeSliceLength(long timeSliceLength);

    boolean ifFinish();

    BizTaskTypeEnum getTaskType();
}
