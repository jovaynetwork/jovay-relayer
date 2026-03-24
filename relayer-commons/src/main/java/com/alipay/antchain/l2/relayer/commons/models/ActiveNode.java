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

import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ActiveNode {

    public ActiveNode(String nodeId, String node_ip, long last_active_time) {
        this.nodeId = nodeId;
        this.nodeIp = node_ip;
        this.lastActiveTime = last_active_time;
    }

    private String nodeId;
    private String nodeIp;
    private long lastActiveTime;
    private ActiveNodeStatusEnum status;

    public boolean ifActive(long activateTimeLength) {
        return (System.currentTimeMillis() - this.lastActiveTime) <= activateTimeLength;
    }
}
