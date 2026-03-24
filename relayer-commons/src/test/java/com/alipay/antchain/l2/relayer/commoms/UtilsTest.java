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

import java.nio.charset.Charset;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.trace.*;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.crypto.transaction.type.Transaction2930;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.utils.Numeric;

public class UtilsTest {

    @Test
    public void testEip1559() {
        var expected = EIP1559Transaction.newBuilder()
                .setChainId(100)
                .setNonce(100)
                .setGas(100)
                .setTo(ByteString.copyFrom(RandomUtil.randomBytes(20)))
                .setValue(U256.newBuilder().setValue(ByteString.copyFrom(new byte[]{1})))
                .setData(ByteString.copyFrom("123".getBytes()))
                .setMaxFeePerGas(10)
                .setMaxPriorityFeePerGas(1)
                .setV(0)
                .setR(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                .setS(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                .build();
        var actual = Utils.convertFromEip1559ProtoTx(expected);
        Assert.assertEquals(expected.getChainId(), ((Transaction1559) actual.getTransaction()).getChainId());
        Assert.assertArrayEquals(expected.getData().toByteArray(), Numeric.hexStringToByteArray(actual.getData()));
        Assert.assertArrayEquals(expected.getR().getValue().toByteArray(), actual.getSignatureData().getR());
    }

    @Test
    public void testEip2930() {
        var expected = EIP2930Transaction.newBuilder()
                .setChainId(100)
                .setNonce(100)
                .setGas(100)
                .setTo(ByteString.copyFrom(RandomUtil.randomBytes(20)))
                .setValue(U256.newBuilder().setValue(ByteString.copyFrom(new byte[]{1})))
                .setData(ByteString.copyFrom("123".getBytes()))
                .setV(0)
                .setR(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                .setS(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                .setAccessList(
                        AccessList.newBuilder().addAccessListItems(AccessListItem.newBuilder()
                                .setAddress(ByteString.copyFrom(RandomUtil.randomBytes(20)))
                                .addStorageKeys(U256.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
                                .build()
                        )
                ).build();
        var actual = Utils.convertFromEip2930ProtoTx(expected);
        Assert.assertEquals(expected.getChainId(), ((Transaction2930) actual.getTransaction()).getChainId());
        Assert.assertArrayEquals(expected.getData().toByteArray(), Numeric.hexStringToByteArray(actual.getData()));
        Assert.assertArrayEquals(expected.getR().getValue().toByteArray(), actual.getSignatureData().getR());
        Assert.assertArrayEquals(
                expected.getAccessList().getAccessListItemsList().get(0).getAddress().toByteArray(),
                Numeric.hexStringToByteArray(((Transaction2930) actual.getTransaction()).getAccessList().get(0).getAddress())
        );
        Assert.assertArrayEquals(
                expected.getAccessList().getAccessListItemsList().get(0).getStorageKeys(0).getValue().toByteArray(),
                Numeric.hexStringToByteArray(((Transaction2930) actual.getTransaction()).getAccessList().get(0).getStorageKeys().get(0))
        );
    }

    @Test
    public void testEip4844TxHashCalc() {
        var rawTx = (SignedRawTransaction) EthTxDecoder.decode(FileUtil.readString("raw_eip4844_tx", Charset.defaultCharset()));
        Assert.assertEquals(
                "0xf45e5c2311de27e225397d50ab65f10083cc2c7899a4ffb4bd8236b4581352fa",
                Utils.calcEip4844TxHash((Transaction4844) rawTx.getTransaction(), rawTx.getSignatureData())
        );

        rawTx = (SignedRawTransaction) EthTxDecoder.decode(FileUtil.readString("raw_eip4844_fusaka_tx", Charset.defaultCharset()));
        Assert.assertEquals(
                "0xe869464f6fdf1382a26dc2e4a1fb45d3cd85a0de3af7d20d1fccad85d75ef466",
                Utils.calcEip4844TxHash((Transaction4844) rawTx.getTransaction(), rawTx.getSignatureData())
        );
    }
}
