package com.alipay.antchain.l2.relayer.dal.entities;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.BatchProveRequestStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("batch_prove_request")
public class BatchProveRequestEntity extends BaseEntity {

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("prove_type")
    private ProveTypeEnum proveType;

    @TableField("proof")
    private byte[] proof;

    @TableField("state")
    private BatchProveRequestStateEnum state;
}
