/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.commoms;

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
