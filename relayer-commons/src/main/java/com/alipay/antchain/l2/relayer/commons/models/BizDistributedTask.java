

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
