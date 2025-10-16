package com.alipay.antchain.l2.relayer.commons.models;

import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigInteger;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class OracleRequestDO {
    private OracleTypeEnum oracleType;

    private OracleRequestTypeEnum oracleTaskType;

    private BigInteger requestIndex;

    private byte[] rawData;

    private OracleTransactionStateEnum txState;

    private Date gmtCreate;

    private Date gmtModified;
}
