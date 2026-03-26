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

package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.Arrays;
import jakarta.annotation.Resource;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import com.alipay.antchain.l2.trace.*;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class L2MsgFetcherTest extends TestBase {

    private static final BasicBlockTrace BLOCK_TRACE_WITH_L2_MSG = BasicBlockTrace.newBuilder()
            .setL2Msgs(L2Msgs.newBuilder().addL2Msgs(
                    L2Msg.newBuilder().setL2MsgLog(
                            Log.newBuilder()
                                    .setAddress(ByteString.copyFrom(Numeric.hexStringToByteArray(L1MsgTransaction.L2_MAILBOX_AS_RECEIVER.toString())))
                                    .addTopics(Hash.newBuilder().setValue(ByteString.copyFrom(HexUtil.decodeHex("ae3d495073f8b68ac52caee883181e95d8a4ee28cf92341dcb2548e0e4610505"))))
                                    .setData(ByteString.copyFrom(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000640b4ddb13ebe8807d71701e36e4db5a82bed3e6b78674a9bb072069168fbdf64600000000000000000000000000000000000000000000000000000000000000043132333400000000000000000000000000000000000000000000000000000000")))
                    ).setOriginTxHash(Hash.newBuilder().setValue(ByteString.copyFrom(RandomUtil.randomBytes(32))))
            ).build())
            .setHeader(BlockHeader.newBuilder().setNumber(100L).build())
            .build();

    @Resource
    private IL2MsgFetcher l2MsgFetcher;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @MockitoBean
    private IMailboxRepository mailboxRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Before
    public void initMock() {
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
        when(rollupConfig.getBatchCommitBlobSizeLimit()).thenReturn(1000_000);
    }

    @Test
    public void testProcess() {
        l2MsgFetcher.process(BLOCK_TRACE_WITH_L2_MSG, BigInteger.ONE);
        verify(mailboxRepository, times(1)).saveMessages(argThat(
                x -> x.size() == 1
                     && Arrays.equals(x.get(0).getMsgHash(), Numeric.hexStringToByteArray("0x0b4ddb13ebe8807d71701e36e4db5a82bed3e6b78674a9bb072069168fbdf646"))
        ));
    }
}
