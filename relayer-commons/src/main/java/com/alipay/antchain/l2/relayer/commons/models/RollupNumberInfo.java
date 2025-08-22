package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RollupNumberInfo {

    private BigInteger number;

    private Date gmtModified;
}
