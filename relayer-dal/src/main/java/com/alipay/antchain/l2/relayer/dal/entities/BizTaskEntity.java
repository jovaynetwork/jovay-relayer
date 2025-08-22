package com.alipay.antchain.l2.relayer.dal.entities;

import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@TableName("biz_task")
public class BizTaskEntity extends BaseEntity {

    @TableField("node_id")
    private String nodeId;

    @TableField("task_type")
    private BizTaskTypeEnum taskType;

    @TableField("start_timestamp")
    private Date startTimestamp;
}
