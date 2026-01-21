package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.Sign;

public class L1MsgTransactionTest {

    @Test
    public void testEncode() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(tx);
        Assert.assertEquals("7fc7018203e8820123", HexUtil.encodeHexStr(wrapper.encodeWithoutSig()));
        Assert.assertEquals("7fce018203e882012364820102820304", HexUtil.encodeHexStr(wrapper.encodeWithSig(new Sign.SignatureData(new byte[]{100}, new byte[] {1, 2}, new byte[] {3, 4}))));
    }

    @Test
    public void testHash() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(tx);
        Assert.assertEquals("b4d2d492f5bcd6445b7c5f419bc038af0fdf7cdef1cf20dfb59fc417f606f340", HexUtil.encodeHexStr(wrapper.calcHash()));
    }
}
