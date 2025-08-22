package com.alipay.antchain.l2.relayer.dal.entities;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 数据创建时间
     */
    @TableField("gmt_create")
    private Date gmtCreate;

    /**
     * 数据更新时间
     */
    @TableField(value = "gmt_modified", update = "now()", updateStrategy = FieldStrategy.ALWAYS)
    private Date gmtModified;

    public void init() {
        Date now = new Date();
        this.setGmtCreate(now);
        this.setGmtModified(now);
    }

    public void update() {
        this.setGmtModified(new Date());
    }
}
