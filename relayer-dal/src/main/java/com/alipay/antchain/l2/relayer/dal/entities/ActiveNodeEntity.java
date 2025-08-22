package com.alipay.antchain.l2.relayer.dal.entities;

import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("active_node")
public class ActiveNodeEntity extends BaseEntity{

    @TableField("node_id")
    private String nodeId;

    @TableField("node_ip")
    private String nodeIp;

    @TableField(value = "last_active_time")
    private Date lastActiveTime;

    @TableField("status")
    private ActiveNodeStatusEnum status;
}
