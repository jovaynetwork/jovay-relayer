package com.alipay.antchain.l2.relayer.commons.models;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OracleFeeInfo {
    // EIP-1559's prior knowledge
    private final BigInteger baseFeeChangeDenominator = BigInteger.valueOf(8);

    private BigInteger startBatchIndex = BigInteger.ONE;

    private BigInteger lastBatchDaFee;

    private BigInteger lastBatchExecFee;

    private BigInteger lastBatchByteLength;

    private BigInteger lastL1BaseFee = BigInteger.valueOf(1000000000); // WEI

    private BigInteger lastL1BlobBaseFee = BigInteger.valueOf(1000000000); // WEI

    private BigInteger blobBaseFeeScala;

    private BigInteger baseFeeScala;

    private BigInteger l1FixedProfit;

    private BigInteger totalScala;
}
