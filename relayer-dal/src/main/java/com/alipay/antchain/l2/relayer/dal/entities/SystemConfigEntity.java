package com.alipay.antchain.l2.relayer.dal.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@TableName("system_config")
public class SystemConfigEntity extends BaseEntity {
    @TableField("conf_key")
    private String confKey;

    @TableField("conf_value")
    private String confValue;
}