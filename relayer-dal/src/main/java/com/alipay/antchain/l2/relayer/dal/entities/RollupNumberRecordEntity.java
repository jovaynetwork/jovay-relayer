package com.alipay.antchain.l2.relayer.dal.entities;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.RollupNumberRecordTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rollup_number_record")
public class RollupNumberRecordEntity extends BaseEntity {

    @TableField("chain_type")
    private ChainTypeEnum chainType;

    @TableField("record_type")
    private RollupNumberRecordTypeEnum recordType;

    @TableField("number")
    private String number;

    public BigInteger getNumberValue() {
        return new BigInteger(number);
    }
}
