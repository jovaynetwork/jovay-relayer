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

package com.alipay.antchain.l2.relayer.core.blockchain;

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.signservice.config.NativeConfig;
import com.alipay.antchain.l2.relayer.signservice.core.Web3jTxSignService;
import org.junit.Assert;
import org.junit.Test;

public class RelayerTxSignServiceImplTest {

    @Test
    public void testSignL1MsgTx() {
        var nativeConfig = new NativeConfig();
        nativeConfig.setPrivateKey("0xfcfc69bd0056a2592e1f46cfba8264d8918fe98ecf5a2ef43aaa4ed1463725e1");
        var relayerTxSignService = new Web3jTxSignService(nativeConfig);
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(tx);

        Assert.assertEquals("7ff84b018203e882012381eba07961e440ae49628601f2925505bcb2352fb8bdeb688836e758542b638ee7795ea03abd8a847a6f8a8b1386f7ca4db73a72893bb486612e3e4bac19cf47bfa90c99", HexUtil.encodeHexStr(relayerTxSignService.sign(wrapper, 100)));
    }
}
