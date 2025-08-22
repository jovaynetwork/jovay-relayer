package com.alipay.antchain.l2.relayer.query.commons;

import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMeta {

    private BigInteger batchIndex;

    private BigInteger startBlock;

    private BigInteger endBlock;
}
