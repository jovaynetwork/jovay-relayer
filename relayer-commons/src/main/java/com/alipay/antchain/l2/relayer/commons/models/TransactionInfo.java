package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class TransactionInfo {

    private String senderAccount;

    private BigInteger nonce;

    private String txHash;

    private byte[] rawTx;

    private Date sendTxTime;
}
