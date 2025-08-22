package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.Comparator;

import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class L1MsgTransactionInfo implements Comparable<L1MsgTransactionInfo> {

    private L1MsgTransaction l1MsgTransaction;

    private BigInteger sourceBlockHeight;

    private String sourceTxHash;

    @Override
    public int compareTo(@NotNull L1MsgTransactionInfo o) {
        return Comparator.<L1MsgTransactionInfo, BigInteger>comparing(value -> value.getL1MsgTransaction().getNonce()).compare(this, o);
    }
}
