package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class L2MsgProofData {

    private BigInteger batchIndex;

    private BigInteger msgNonce;

    private byte[] merkleProof;
}