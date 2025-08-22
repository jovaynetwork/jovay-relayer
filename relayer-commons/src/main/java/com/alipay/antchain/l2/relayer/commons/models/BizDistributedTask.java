

package com.alipay.antchain.l2.relayer.commons.models;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BizDistributedTask implements IDistributedTask {

    public static String getUniqueKeyForApplication(String domainSpace, String domain) {
        return StrUtil.format("Application_{}-{}", domainSpace, domain);
    }

    private String nodeId = StrUtil.EMPTY;

    private BizTaskTypeEnum taskType;

    private String ext = StrUtil.EMPTY;

    private long startTime = 0;

    private long timeSliceLength = 0;

    public BizDistributedTask(BizTaskTypeEnum taskType) {
        this.taskType = taskType;
    }

    public BizDistributedTask(
            String nodeId,
            BizTaskTypeEnum taskType,
            String ext,
            long startTime
    ) {
        this.nodeId = nodeId;
        this.taskType = taskType;
        this.ext = ext;
        this.startTime = startTime;
    }

    public boolean ifFinish() {
        return (System.currentTimeMillis() - this.startTime) > timeSliceLength;
    }

    public long getEndTimestamp() {
        return this.startTime + this.timeSliceLength;
    }
}
