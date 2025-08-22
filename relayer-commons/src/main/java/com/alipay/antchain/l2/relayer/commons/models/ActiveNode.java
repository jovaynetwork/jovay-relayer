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
