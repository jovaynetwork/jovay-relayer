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

package com.alipay.antchain.l2.relayer.commons.utils;

import java.math.BigInteger;
import java.nio.ByteOrder;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ByteUtil;

public class BytesUtils {

    public static byte[] fromUint64(BigInteger val) {
        Assert.isTrue(val.compareTo(BigInteger.ZERO) >= 0, "uint64 less than zero");
        return toUnsignedByteArray(8, val);
    }

    public static byte[] fromUint256(BigInteger val) {
        Assert.isTrue(val.compareTo(BigInteger.ZERO) >= 0, "uint256 less than zero");
        return toUnsignedByteArray(32, val);
    }

    public static byte getUint8(byte[] raw, int offset) {
        return raw[offset];
    }

    public static int getUint8AsInteger(byte[] raw, int offset) {
        return ByteUtil.bytesToShort(new byte[]{0, raw[offset]}, ByteOrder.BIG_ENDIAN);
    }

    public static BigInteger getUint64(byte[] raw, int offset) {
        byte[] rawNum = new byte[8];
        System.arraycopy(raw, offset, rawNum, 0, 8);
        return new BigInteger(1, rawNum);
    }

    public static BigInteger getUint256(byte[] raw, int offset) {
        return new BigInteger(1, getBytes32(raw, offset));
    }

    public static int getUint16(byte[] raw, int offset) {
        byte[] rawNum = new byte[4];
        System.arraycopy(raw, offset, rawNum, 2, 2);
        return ByteUtil.bytesToInt(rawNum, ByteOrder.BIG_ENDIAN);
    }

    public static int getUint24(byte[] raw, int offset) {
        byte[] rawNum = new byte[4];
        System.arraycopy(raw, offset, rawNum, 1, 3);
        return ByteUtil.bytesToInt(rawNum, ByteOrder.BIG_ENDIAN);
    }

    public static long getUint32(byte[] raw, int offset) {
        byte[] rawNum = new byte[8];
        System.arraycopy(raw, offset, rawNum, 4, 4);
        return ByteUtil.bytesToLong(rawNum, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] getBytes32(byte[] raw, int offset) {
        byte[] rawVal = new byte[32];
        System.arraycopy(raw, offset, rawVal, 0, 32);
        return rawVal;
    }

    public static byte[] getBytes(byte[] raw, int offset, int len) {
        byte[] rawVal = new byte[len];
        System.arraycopy(raw, offset, rawVal, 0, len);
        return rawVal;
    }

    public static byte[] toUnsignedByteArray(int length, BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == length) {
            return bytes;
        }

        int start = bytes[0] == 0 ? 1 : 0;
        int count = bytes.length - start;

        if (count > length) {
            throw new IllegalArgumentException("standard length exceeded for value");
        }

        byte[] tmp = new byte[length];
        System.arraycopy(bytes, start, tmp, tmp.length - count, count);
        return tmp;
    }

    public static int calcBytesInEvmWord(int l) {
        return calcEvmWordNum(l) << 5;
    }

    public static int calcEvmWordNum(int l) {
        return (l >> 5) + ((l & 31) != 0 ? 1 : 0);
    }
}
