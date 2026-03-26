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
import org.springframework.test.context.bean.override.convention.TestBean;
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

    @TestBean
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


    // ==================== Negative Case Tests ====================

    /**
     * Test peek ready messages with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    public void testPeekReadyMessages_ZeroLimit() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx
        );
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        List<InterBlockchainMessageDO> result = mailboxRepository.peekReadyMessages(
                InterBlockchainMessageTypeEnum.L1_MSG,
                0
        );
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    /**
     * Test peek ready messages with non-existent message type
     * Verifies that querying non-existent type returns empty list
     */
    @Test
    public void testPeekReadyMessages_NonExistentType() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx
        );
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        // Query with wrong message type
        List<InterBlockchainMessageDO> result = mailboxRepository.peekReadyMessages(
                InterBlockchainMessageTypeEnum.L2_MSG,
                10
        );
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    /**
     * Test get message with non-existent nonce
     * Verifies that querying non-existent message returns null
     */
    @Test
    public void testGetMessage_NonExistentNonce() {
        InterBlockchainMessageDO result = mailboxRepository.getMessage(
                InterBlockchainMessageTypeEnum.L1_MSG,
                999999L
        );
        Assert.assertNull(result);
    }

    /**
     * Test update message state for non-existent message
     * Verifies that updating non-existent message does not throw exception
     */
    @Test
    public void testUpdateMessageState_NonExistentMessage() {
        // Should not throw exception when updating non-existent message
        mailboxRepository.updateMessageState(
                InterBlockchainMessageTypeEnum.L1_MSG,
                999999L,
                InterBlockchainMessageStateEnum.MSG_COMMITTED
        );

        // Verify the message still doesn't exist
        InterBlockchainMessageDO result = mailboxRepository.getMessage(
                InterBlockchainMessageTypeEnum.L1_MSG,
                999999L
        );
        Assert.assertNull(result);
    }

    /**
     * Test save duplicate messages
     * Verifies that duplicate messages throw PersistenceException
     */
    @Test
    public void testSaveMessages_DuplicateMessages() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx
        );
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);

        // Save first time
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        // Save second time with same message hash - should throw exception
        try {
            mailboxRepository.saveMessages(ListUtil.toList(messageDO));
            Assert.fail("Expected exception for duplicate message");
        } catch (Exception e) {
            // Expected exception - unique constraint violation or persistence exception
            Assert.assertTrue(
                    e instanceof org.mybatis.spring.MyBatisSystemException ||
                    e instanceof org.springframework.dao.DataIntegrityViolationException ||
                    (e.getMessage() != null && (
                            e.getMessage().contains("Error flushing statements") ||
                            e.getMessage().contains("Unique index or primary key violation") ||
                            e.getMessage().contains("duplicate")
                    ))
            );
        }
    }

    /**
     * Test save empty message list
     * Verifies that saving empty list is handled gracefully
     */
    @Test
    public void testSaveMessages_EmptyList() {
        // Should not throw exception when saving empty list
        mailboxRepository.saveMessages(ListUtil.toList());

        List<InterBlockchainMessageDO> messages = mailboxRepository.peekReadyMessages(
                InterBlockchainMessageTypeEnum.L1_MSG,
                10
        );
        Assert.assertNotNull(messages);
        Assert.assertEquals(0, messages.size());
    }

    /**
     * Test get L2 message proof with non-existent nonce
     * Verifies that querying non-existent proof returns null
     */
    @Test
    public void testGetL2MsgProof_NonExistentNonce() {
        var proof = mailboxRepository.getL2MsgProof(BigInteger.valueOf(999999));
        Assert.assertNull(proof);
    }

    /**
     * Test save L2 message proofs with empty map
     * Verifies that saving empty proofs is handled gracefully
     */
    @Test
    public void testSaveL2MsgProofs_EmptyMap() {
        var data = new HashMap<BigInteger, byte[]>();

        // Should not throw exception when saving empty map
        mailboxRepository.saveL2MsgProofs(data);

        // Verify no proofs were saved
        var proof = mailboxRepository.getL2MsgProof(BigInteger.ONE);
        Assert.assertNull(proof);
    }

    /**
     * Test save L2 message proofs with duplicate nonces
     * Verifies that duplicate proofs are handled correctly
     */
    @Test
    public void testSaveL2MsgProofs_DuplicateNonces() {
        var l2msg = new L2MsgDO(BigInteger.ONE, BigInteger.ONE, RandomUtil.randomBytes(32), "123");
        var messageDO = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(100), l2msg);
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        byte[] proof1 = RandomUtil.randomBytes(32);
        byte[] proof2 = RandomUtil.randomBytes(32);

        var data1 = new HashMap<BigInteger, byte[]>();
        data1.put(BigInteger.ONE, proof1);
        mailboxRepository.saveL2MsgProofs(data1);

        // Save again with different proof for same nonce
        var data2 = new HashMap<BigInteger, byte[]>();
        data2.put(BigInteger.ONE, proof2);
        mailboxRepository.saveL2MsgProofs(data2);

        var savedProof = mailboxRepository.getL2MsgProof(BigInteger.ONE);
        Assert.assertNotNull(savedProof);
        // Should have the latest proof
        Assert.assertArrayEquals(proof2, savedProof.getMerkleProof());
    }

    /**
     * Test get message hashes with non-existent nonce
     * Verifies that querying non-existent hashes returns empty list
     */
    @Test
    public void testGetMsgHashes_NonExistentNonce() {
        var hashes = mailboxRepository.getMsgHashes(
                InterBlockchainMessageTypeEnum.L2_MSG,
                BigInteger.valueOf(999999)
        );
        Assert.assertNotNull(hashes);
        Assert.assertEquals(0, hashes.size());
    }

    /**
     * Test get message hashes with wrong message type
     * Verifies that querying with wrong type returns empty list
     */
    @Test
    public void testGetMsgHashes_WrongMessageType() {
        var l2msg = new L2MsgDO(BigInteger.ONE, BigInteger.ONE, RandomUtil.randomBytes(32), "123");
        var messageDO = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(100), l2msg);
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        // Query with wrong message type
        var hashes = mailboxRepository.getMsgHashes(
                InterBlockchainMessageTypeEnum.L1_MSG,
                BigInteger.ONE
        );
        Assert.assertNotNull(hashes);
        Assert.assertEquals(0, hashes.size());
    }
    /**
     * Test multiple state transitions for message
     * Verifies that message state can be updated multiple times
     */
    @Test
    public void testUpdateMessageState_MultipleTransitions() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO messageDO = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx
        );
        messageDO.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(messageDO));

        long nonce = tx.getNonce().longValue();

        // Transition: MSG_READY -> MSG_PROVED
        mailboxRepository.updateMessageState(
                InterBlockchainMessageTypeEnum.L1_MSG,
                nonce,
                InterBlockchainMessageStateEnum.MSG_PROVED
        );
        InterBlockchainMessageDO result1 = mailboxRepository.getMessage(
                InterBlockchainMessageTypeEnum.L1_MSG,
                nonce
        );
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_PROVED, result1.getState());

        // Transition: MSG_PROVED -> MSG_COMMITTED
        mailboxRepository.updateMessageState(
                InterBlockchainMessageTypeEnum.L1_MSG,
                nonce,
                InterBlockchainMessageStateEnum.MSG_COMMITTED
        );
        InterBlockchainMessageDO result2 = mailboxRepository.getMessage(
                InterBlockchainMessageTypeEnum.L1_MSG,
                nonce
        );
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, result2.getState());
    }

    /**
     * Test peek ready messages with different states
     * Verifies that only MSG_READY state messages are returned
     */
    @Test
    public void testPeekReadyMessages_OnlyReadyState() {
        L1MsgTransaction tx1 = new L1MsgTransaction(BigInteger.ONE, BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO messageDO1 = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx1
        );
        messageDO1.setState(InterBlockchainMessageStateEnum.MSG_READY);

        L1MsgTransaction tx2 = new L1MsgTransaction(BigInteger.valueOf(2), BigInteger.valueOf(1_000), "456");
        InterBlockchainMessageDO messageDO2 = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(101),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx2
        );
        messageDO2.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        mailboxRepository.saveMessages(ListUtil.toList(messageDO1, messageDO2));

        List<InterBlockchainMessageDO> messages = mailboxRepository.peekReadyMessages(
                InterBlockchainMessageTypeEnum.L1_MSG,
                10
        );

        // Should only return MSG_READY messages
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, messages.get(0).getState());
    }

    // ==================== Rollback Related Method Tests ====================

    /**
     * Test resetL1MsgsAboveNonce - resets L1 messages with nonce > threshold to MSG_READY
     */
    @Test
    public void testResetL1MsgsAboveNonce() {
        // Create L1 messages with different nonces
        L1MsgTransaction tx1 = new L1MsgTransaction(BigInteger.valueOf(50), BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO msg1 = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx1
        );
        msg1.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        L1MsgTransaction tx2 = new L1MsgTransaction(BigInteger.valueOf(60), BigInteger.valueOf(1_000), "456");
        InterBlockchainMessageDO msg2 = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(101),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx2
        );
        msg2.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Reset messages with nonce > 50
        int updated = mailboxRepository.resetL1MsgsAboveNonce(50L);
        Assert.assertEquals(1, updated);

        // Verify msg1 (nonce=50) is still COMMITTED, msg2 (nonce=60) is reset to READY
        InterBlockchainMessageDO result1 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L1_MSG, 50L);
        InterBlockchainMessageDO result2 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L1_MSG, 60L);

        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, result1.getState());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, result2.getState());
    }

    /**
     * Test resetL1MsgsAboveNonce - no messages to reset
     */
    @Test
    public void testResetL1MsgsAboveNonce_NoMatchingMessages() {
        L1MsgTransaction tx = new L1MsgTransaction(BigInteger.valueOf(50), BigInteger.valueOf(1_000), "123");
        InterBlockchainMessageDO msg = InterBlockchainMessageDO.fromL1MsgTx(
                BigInteger.valueOf(100),
                Numeric.toHexString(RandomUtil.randomBytes(32)),
                tx
        );
        msg.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);
        mailboxRepository.saveMessages(ListUtil.toList(msg));

        // Reset messages with nonce > 100 - should reset nothing
        int updated = mailboxRepository.resetL1MsgsAboveNonce(100L);
        Assert.assertEquals(0, updated);

        // Verify message is still COMMITTED
        InterBlockchainMessageDO result = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L1_MSG, 50L);
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, result.getState());
    }

    // ==================== Tests for methods using CAST(... AS BIGINT) ====================
    // Note: These tests use CAST(... AS BIGINT) which is compatible with both MySQL and H2.

    /**
     * Test deleteL2MsgsForRollback - deletes L2 messages for rollback
     * L2MsgDO constructor: (nonce, batchIndex, msgHash, sourceTxHash)
     * fromL2MsgTx first param is sourceBlockHeight
     */
    @Test
    public void testDeleteL2MsgsForRollback() {
        // Create L2 messages with different source block heights
        // L2MsgDO: nonce=100, batchIndex=10
        var l2msg1 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        // sourceBlockHeight = 1000
        var msg1 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(1000), l2msg1);
        msg1.setState(InterBlockchainMessageStateEnum.MSG_READY);

        // L2MsgDO: nonce=200, batchIndex=10
        var l2msg2 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(200), RandomUtil.randomBytes(32), "tx456");
        // sourceBlockHeight = 2000
        var msg2 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(2000), l2msg2);
        msg2.setState(InterBlockchainMessageStateEnum.MSG_READY);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Delete L2 messages where batch_index >= 10 AND source_block_height >= 1500
        int deleted = mailboxRepository.deleteL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1500));
        Assert.assertEquals(1, deleted);

        // Verify msg1 still exists (block height 1000 < 1500), msg2 is deleted (block height 2000 >= 1500)
        InterBlockchainMessageDO result1 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L);
        InterBlockchainMessageDO result2 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 200L);

        Assert.assertNotNull(result1);
        Assert.assertNull(result2);
    }

    /**
     * Test deleteL2MsgsForRollback - no messages to delete
     */
    @Test
    public void testDeleteL2MsgsForRollback_NoMatchingMessages() {
        // L2MsgDO: nonce=100, batchIndex=10, sourceBlockHeight=1000
        var l2msg = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(1000), l2msg);
        msg.setState(InterBlockchainMessageStateEnum.MSG_READY);
        mailboxRepository.saveMessages(ListUtil.toList(msg));

        // Delete with higher batch index - should delete nothing (batch_index 10 < 100)
        int deleted = mailboxRepository.deleteL2MsgsForRollback(BigInteger.valueOf(100), BigInteger.valueOf(500));
        Assert.assertEquals(0, deleted);

        // Verify message still exists
        InterBlockchainMessageDO result = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L);
        Assert.assertNotNull(result);
    }

    /**
     * Test deleteL2MsgsForRollback - deletes all matching messages
     */
    @Test
    public void testDeleteL2MsgsForRollback_DeleteAll() {
        // Create multiple L2 messages all above the threshold
        var l2msg1 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg1 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(2000), l2msg1);
        msg1.setState(InterBlockchainMessageStateEnum.MSG_READY);

        var l2msg2 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(200), RandomUtil.randomBytes(32), "tx456");
        var msg2 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(3000), l2msg2);
        msg2.setState(InterBlockchainMessageStateEnum.MSG_READY);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Delete L2 messages where batch_index >= 10 AND source_block_height >= 1500
        int deleted = mailboxRepository.deleteL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1500));
        Assert.assertEquals(2, deleted);

        // Verify both messages are deleted
        Assert.assertNull(mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L));
        Assert.assertNull(mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 200L));
    }

    /**
     * Test resetL2MsgsForRollback - resets L2 messages to MSG_READY
     */
    @Test
    public void testResetL2MsgsForRollback() {
        // Create L2 messages with same batch index but different source block heights
        var l2msg1 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg1 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(1000), l2msg1);
        msg1.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        var l2msg2 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(200), RandomUtil.randomBytes(32), "tx456");
        var msg2 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(2000), l2msg2);
        msg2.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Reset L2 messages where batch_index = 10 AND source_block_height < 1500
        int updated = mailboxRepository.resetL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1500));
        Assert.assertEquals(1, updated);

        // Verify msg1 is reset to READY (block height 1000 < 1500), msg2 is still COMMITTED (block height 2000 >= 1500)
        InterBlockchainMessageDO result1 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L);
        InterBlockchainMessageDO result2 = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 200L);

        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, result1.getState());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, result2.getState());
    }

    /**
     * Test resetL2MsgsForRollback - no messages to reset
     */
    @Test
    public void testResetL2MsgsForRollback_NoMatchingMessages() {
        // sourceBlockHeight = 2000 >= 1500, so should not be reset
        var l2msg = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(2000), l2msg);
        msg.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);
        mailboxRepository.saveMessages(ListUtil.toList(msg));

        // Reset with threshold 1500 - should reset nothing (block height 2000 >= 1500)
        int updated = mailboxRepository.resetL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1500));
        Assert.assertEquals(0, updated);

        // Verify message is still COMMITTED
        InterBlockchainMessageDO result = mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L);
        Assert.assertNotNull(result);
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, result.getState());
    }

    /**
     * Test resetL2MsgsForRollback - resets all matching messages
     */
    @Test
    public void testResetL2MsgsForRollback_ResetAll() {
        // Create multiple L2 messages all below the threshold
        var l2msg1 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg1 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(500), l2msg1);
        msg1.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        var l2msg2 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(200), RandomUtil.randomBytes(32), "tx456");
        var msg2 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(800), l2msg2);
        msg2.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Reset L2 messages where batch_index = 10 AND source_block_height < 1000
        int updated = mailboxRepository.resetL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1000));
        Assert.assertEquals(2, updated);

        // Verify both messages are reset to READY
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L).getState());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 200L).getState());
    }

    /**
     * Test resetL2MsgsForRollback - only affects messages with matching batch index
     */
    @Test
    public void testResetL2MsgsForRollback_OnlyMatchingBatchIndex() {
        // Create L2 messages with different batch indices
        // L2MsgDO: batchIndex=10, nonce=100, sourceBlockHeight=500
        var l2msg1 = new L2MsgDO(BigInteger.valueOf(10), BigInteger.valueOf(100), RandomUtil.randomBytes(32), "tx123");
        var msg1 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(500), l2msg1);
        msg1.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        // L2MsgDO: batchIndex=20, nonce=200, sourceBlockHeight=500
        var l2msg2 = new L2MsgDO(BigInteger.valueOf(20), BigInteger.valueOf(200), RandomUtil.randomBytes(32), "tx456");
        var msg2 = InterBlockchainMessageDO.fromL2MsgTx(BigInteger.valueOf(500), l2msg2);  // Different batch index
        msg2.setState(InterBlockchainMessageStateEnum.MSG_COMMITTED);

        mailboxRepository.saveMessages(ListUtil.toList(msg1, msg2));

        // Reset L2 messages where batch_index = 10 AND source_block_height < 1000
        int updated = mailboxRepository.resetL2MsgsForRollback(BigInteger.valueOf(10), BigInteger.valueOf(1000));
        Assert.assertEquals(1, updated);

        // Verify only msg1 is reset, msg2 is still COMMITTED (different batch index)
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_READY, mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 100L).getState());
        Assert.assertEquals(InterBlockchainMessageStateEnum.MSG_COMMITTED, mailboxRepository.getMessage(InterBlockchainMessageTypeEnum.L2_MSG, 200L).getState());
    }
}
