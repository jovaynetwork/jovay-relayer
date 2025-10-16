package com.alipay.antchain.l2.relayer.dal.entities;

import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@TableName("oracle_request")
public class OracleRequestEntity extends BaseEntity {
    @TableField("oracle_type")
    private OracleTypeEnum oracleType;

    @TableField("oracle_task_type")
    private OracleRequestTypeEnum oracleTaskType;

    @TableField("request_index")
    private BigInteger requestIndex;

    @TableField("raw_data")
    private byte[] rawData;

    @TableField("tx_state")
    private OracleTransactionStateEnum txState;
}
