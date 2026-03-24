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
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class FusakaTransaction4844Test {

    private static final String HEX_RAW_TX = FileUtil.readString("raw_eip4844_fusaka_tx", Charset.defaultCharset());

    @Test
    public void test() {
        var rawTx = (SignedRawTransaction) EthTxDecoder.decode(HEX_RAW_TX);
        Assert.assertArrayEquals(
                Numeric.hexStringToByteArray(HEX_RAW_TX),
                TransactionEncoder.encode(rawTx, rawTx.getSignatureData())
        );
    }
}
