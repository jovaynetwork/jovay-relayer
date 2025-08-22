package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.Date;

import com.alipay.antchain.l2.relayer.commons.enums.BatchProveRequestStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchProveRequestDO {

    private BigInteger batchIndex;

    private ProveTypeEnum proveType;

    private byte[] proof;

    private BatchProveRequestStateEnum state;

    private Date gmtModified;
}
