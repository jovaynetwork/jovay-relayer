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

package com.alipay.antchain.l2.relayer.dal;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgRawTransactionWrapper;
import com.alipay.antchain.l2.relayer.commons.l2basic.L1MsgTransaction;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.commons.models.L2MsgDO;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import jakarta.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false", "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class MailboxRepositoryTest extends TestBase {

    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @MockitoBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @Resource
    private IMailboxRepository mailboxRepository;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @Before
    public void initMock() {
        when(l1Client.l1BlobNumLimit()).thenReturn(4L);
        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
    }

    @Test
    public void testInterBlockchainMessage() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        L1MsgRawTransactionWrapper wrapper = new L1MsgRawTransactionWrapper(tx);
        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(BigInteger.valueOf(100), Numeric.toHexString(RandomUtil.randomBytes(32)), tx);
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);

        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        List<InterBlockchainMessageDO> messageDOS = mailboxRepository.peekReadyMessages(InterBlockchainMessageTypeEnum.L1_MSG, 10);
        Assert.assertEquals(1, messageDOS.size());
        Assert.assertArrayEquals(wrapper.calcHash(), new L1MsgRawTransactionWrapper(messageDOS.get(0).toL1MsgTransaction()).calcHash());
        Assert.assertEquals(messageDO.getSourceTxHash(), messageDOS.get(0).getSourceTxHash());
        Assert.assertEquals(messageDO.getSourceBlockHeight(), messageDOS.get(0).getSourceBlockHeight());
        Assert.assertEquals(messageDO.getNonce(), messageDOS.get(0).getNonce());
        Assert.assertArrayEquals(messageDO.getRawMessage(), messageDOS.get(0).getRawMessage());
        Assert.assertArrayEquals(messageDO.getMsgHash(), messageDOS.get(0).getMsgHash());
        Assert.assertEquals(messageDO.getState(), messageDOS.get(0).getState());
        Assert.assertEquals(messageDO.getSender(), messageDOS.get(0).getSender());
        Assert.assertEquals(messageDO.getReceiver(), messageDOS.get(0).getReceiver());
        Assert.assertEquals(messageDO.getType(), messageDOS.get(0).getType());

        mailboxRepository.updateMessageState(InterBlockchainMessageTypeEnum.L1_MSG, tx.getNonce().longValue(), InterBlockchainMessageStateEnum.MSG_COMMITTED);

        InterBlockchainMessageDO actual = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L1_MSG, tx.getNonce().longValue());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, actual.getState());
    }

    @Test
    public void testSaveL2MsgProofs() {
        var l2msg = new L2MsgDO(BigInteger.ONE, BigInteger.ONE, RandomUtil.randomBytes(32), "123");
        var messageDO = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(100), l2msg);
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        var data = new HashMap<BigInteger, byte[]>();
        data.put(BigInteger.valueOf(1), RandomUtil.randomBytes(32));
        mailboxRepository.saveL2MsgProofs(data);
        var proof = mailboxRepository.getL2MsgProof(BigInteger.valueOf(1));
        Assert.assertNotNull(proof);
        Assert.assertEquals(BigInteger.ONE, proof.getMsgNonce());
        Assert.assertArrayEquals(data.get(BigInteger.ONE), proof.getMerkleProof());
    }

    @Test
    public void testGetMsgHashes() {
        var l2msg = new L2MsgDO(BigInteger.ONE, BigInteger.ONE, RandomUtil.randomBytes(32), "123");
        var messageDO = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(100), l2msg);
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        var hashes = mailboxRepository.getMsgHashes(InterBlockchainMessageTypeEnum.L2_MSG, BigInteger.ONE);
        Assert.assertNotNull(hashes);
        Assert.assertEquals(1, hashes.size());
        Assert.assertArrayEquals(l2msg.getMsgHash(), hashes.get(0));
    }
}
