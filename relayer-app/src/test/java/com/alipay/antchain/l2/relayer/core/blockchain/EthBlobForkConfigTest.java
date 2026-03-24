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

import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBlobForkConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.bpo.EthBpoBlobConfig;
import org.junit.Assert;
import org.junit.Test;

public class EthBlobForkConfigTest {

    @Test
    public void testGetCurrConfig() {
        var mainnetFork = new EthBlobForkConfig(BigInteger.ONE, null);
        var curr = mainnetFork.getCurrConfig(1746612311000L);

        Assert.assertEquals("Prague", curr.getName());
        Assert.assertEquals(0, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(5007716), curr.getUpdateFraction());

        curr = mainnetFork.getCurrConfig(1764798551000L);
        Assert.assertEquals("Osaka", curr.getName());
        Assert.assertEquals(1, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(5007716), curr.getUpdateFraction());

        curr = mainnetFork.getCurrConfig(1765290071000L);
        Assert.assertEquals("BPO1", curr.getName());
        Assert.assertEquals(1, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(8346193), curr.getUpdateFraction());

        curr = mainnetFork.getCurrConfig(1767747671000L);
        Assert.assertEquals("BPO2", curr.getName());
        Assert.assertEquals(1, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(11684671), curr.getUpdateFraction());

        var sepoliaFork = new EthBlobForkConfig(BigInteger.valueOf(11155111), null);
        curr = sepoliaFork.getCurrConfig(1761607008000L);
        Assert.assertEquals("BPO2", curr.getName());
        Assert.assertEquals(1, curr.getBlobSidecarVersion());
        Assert.assertEquals(BigInteger.valueOf(11684671), curr.getUpdateFraction());
    }

    @Test
    public void testFakeExponential() {
        var conf = new EthBpoBlobConfig("test", 1, BigInteger.valueOf(5007716));

        BigInteger excessBlobGas = BigInteger.valueOf(100000000);
        BigInteger nextBlobBaseFee = conf.fakeExponential(excessBlobGas);
        Assert.assertEquals(BigInteger.valueOf(470442149), nextBlobBaseFee);

        BigInteger excessBlobGas1 = BigInteger.valueOf(50000000);
        BigInteger nextBlobBaseFee1 = conf.fakeExponential(excessBlobGas1);
        Assert.assertEquals(BigInteger.valueOf(21689), nextBlobBaseFee1);

        BigInteger excessBlobGas2 = BigInteger.valueOf(5000000);
        BigInteger nextBlobBaseFee2 = conf.fakeExponential(excessBlobGas2);
        Assert.assertEquals(BigInteger.valueOf(2), nextBlobBaseFee2);

        BigInteger excessBlobGas3 = BigInteger.valueOf(500000);
        BigInteger nextBlobBaseFee3 = conf.fakeExponential(excessBlobGas3);
        Assert.assertEquals(BigInteger.valueOf(1), nextBlobBaseFee3);

        BigInteger excessBlobGas4 = BigInteger.valueOf(1000000000);
        BigInteger nextBlobBaseFe4 = conf.fakeExponential(excessBlobGas4);
        Assert.assertEquals("530960562297742761328174384460574518866979472367699220445419521976236776610989157807452", nextBlobBaseFe4.toString());
    }
}
