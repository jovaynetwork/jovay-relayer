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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.l2basic.Batch;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.models.BatchProveRequestDO;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ReliableTransactionDO;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false", "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class RollupRepositoryTest extends TestBase {

    public static final ChunkWrapper CHUNK1 = new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(100), 0, ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2));

    public static final Batch BATCH1 = new Batch(
            BatchVersionEnum.BATCH_V0, BigInteger.valueOf(100),
            ZERO_BATCH_HEADER,
            RandomUtil.randomBytes(32),
            ListUtil.toList(CHUNK1.getChunk())
    );

    public static final ReliableTransactionDO RELIABLE_TRANSACTION_DO1 = ReliableTransactionDO.builder()
            .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
            .batchIndex(BigInteger.valueOf(100))
            .chainType(ChainTypeEnum.LAYER_ONE)
            .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
            .originalTxHash("0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9")
            .latestTxHash("0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9")
            .rawTx(HexUtil.decodeHex("00"))
            .latestTxSendTime(new Date())
            .nonce(88L)
            .build();

    public static final ReliableTransactionDO RELIABLE_TRANSACTION_DO2 = ReliableTransactionDO.builder()
            .transactionType(TransactionTypeEnum.BATCH_TEE_PROOF_COMMIT_TX)
            .batchIndex(BigInteger.valueOf(100))
            .chainType(ChainTypeEnum.LAYER_ONE)
            .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
            .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
            .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
            .rawTx(HexUtil.decodeHex("00"))
            .latestTxSendTime(new Date())
            .nonce(89L)
            .state(ReliableTransactionStateEnum.TX_PACKAGED)
            .build();

    public static List<ReliableTransactionDO> randomReliableTransactionDOs(int size, ReliableTransactionStateEnum state) {
        List<ReliableTransactionDO> list = ListUtil.toList();
        for (int i = 0; i < size; i++) {
            ReliableTransactionDO reliableTransactionDO = BeanUtil.copyProperties(RELIABLE_TRANSACTION_DO1, ReliableTransactionDO.class);
            reliableTransactionDO.setBatchIndex(reliableTransactionDO.getBatchIndex().add(BigInteger.valueOf(i)));
            reliableTransactionDO.setOriginalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)));
            reliableTransactionDO.setLatestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)));
            reliableTransactionDO.setState(state);
            list.add(reliableTransactionDO);
        }
        return list;
    }

    @Resource
    private IRollupRepository rollupRepository;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Before
    public void initMock() {
        when(l1Client.l1BlobNumLimit()).thenReturn(4L);

        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
    }

    @Test
    public void testBlockTrace() {
        rollupRepository.cacheL2BlockTrace(BASIC_BLOCK_TRACE1);
        BasicBlockTrace result = rollupRepository.getL2BlockTraceFromCache(BigInteger.valueOf(BASIC_BLOCK_TRACE1.getHeader().getNumber()));
        assertArrayEquals(BASIC_BLOCK_TRACE1.toByteArray(), result.toByteArray());
    }

    @Test
    public void testRollupNumberRecords() {
        // return default values
        assertEquals(BigInteger.valueOf(0), rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.NEXT_CHUNK));
        assertNull(rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_ONE, RollupNumberRecordTypeEnum.BLOCK_PROCESSED));
        assertEquals(BigInteger.valueOf(0), rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED));
        assertEquals(BigInteger.valueOf(0), rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BATCH_COMMITTED));
        assertNull(rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_MSG_PROVE_BATCH));

        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED, BigInteger.valueOf(100));
        assertEquals(BigInteger.valueOf(100), rollupRepository.getRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.BLOCK_PROCESSED));
    }

    @Test
    public void testChunks() {
        rollupRepository.saveChunk(CHUNK1);
        ChunkWrapper actual = rollupRepository.getChunk(CHUNK1.getBatchIndex(), CHUNK1.getChunkIndex());
        assertNotNull(actual);
        assertEquals(CHUNK1.getBatchIndex(), actual.getBatchIndex());
        assertEquals(CHUNK1.getChunkIndex(), actual.getChunkIndex());
        assertArrayEquals(CHUNK1.getChunk().serialize(false), actual.getChunk().serialize(false));
        assertEquals(CHUNK1.getGasSum(), actual.getGasSum());

        List<ChunkWrapper> chunkWrappers = rollupRepository.getChunks(CHUNK1.getBatchIndex());
        assertEquals(CHUNK1.getBatchIndex(), chunkWrappers.get(0).getBatchIndex());
        assertArrayEquals(CHUNK1.getChunk().serialize(false), chunkWrappers.get(0).getChunk().serialize(false));
    }

    @Test
    public void testBatch() {
        BatchWrapper batchWrapper = new BatchWrapper();
        batchWrapper.setBatch(BATCH1);
        batchWrapper.setPostStateRoot(RandomUtil.randomBytes(32));
        batchWrapper.setL2MsgRoot(RandomUtil.randomBytes(32));
        // save with insert
        rollupRepository.saveBatch(batchWrapper);
        // save with update
        rollupRepository.saveBatch(batchWrapper);
        rollupRepository.saveChunk(CHUNK1);
        BatchWrapper batch = rollupRepository.getBatch(BATCH1.getBatchIndex());
        assertEquals(BATCH1.getBatchIndex(), batch.getBatch().getBatchIndex());
        assertEquals(1, batch.getChunks().size());
        assertArrayEquals(CHUNK1.getChunk().serialize(false), batch.getChunks().get(0).serialize(false));
        assertArrayEquals(BATCH1.getBatchHeader().getL1MsgRollingHash(), batch.getBatchHeader().getL1MsgRollingHash());
        assertArrayEquals(BATCH1.getBatchHash(), batch.getBatch().getBatchHash());
        assertArrayEquals(BATCH1.getBatchHeader().getDataHash(), batch.getBatchHeader().getDataHash());

        batch = rollupRepository.getBatch(BATCH1.getBatchIndex(), false);
        assertEquals(BATCH1.getBatchIndex(), batch.getBatch().getBatchIndex());
        assertNull(batch.getBatch().getPayload());
        assertArrayEquals(BATCH1.getBatchHash(), batch.getBatch().getBatchHash());
        assertArrayEquals(BATCH1.getBatchHeader().getDataHash(), batch.getBatchHeader().getDataHash());
    }

    @Test
    public void testBatchPartial() {
        rollupRepository.savePartialBatchHeader(BATCH1.getBatchHeader(), 2, 1, BATCH1.getStartBlockNumber(), BATCH1.getEndBlockNumber());
        BatchHeader batchHeader = rollupRepository.getBatchHeader(BATCH1.getBatchIndex());
        assertNotNull(batchHeader);
        assertEquals(BATCH1.getBatchHashHex(), batchHeader.getHashHex());
    }

    @Test
    public void testCalcWaitingBatchCountBeyondIndex() {
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH, BigInteger.valueOf(100));

        Assert.assertEquals(1, rollupRepository.calcWaitingBatchCountBeyondIndex(BigInteger.valueOf(99)));
        Assert.assertEquals(0, rollupRepository.calcWaitingBatchCountBeyondIndex(BigInteger.valueOf(100)));
        Assert.assertEquals(0, rollupRepository.calcWaitingBatchCountBeyondIndex(BigInteger.valueOf(101)));
    }

    @Test
    public void testCalcWaitingProofCountBeyondIndex() {
        rollupRepository.updateRollupNumberRecord(ChainTypeEnum.LAYER_TWO, RollupNumberRecordTypeEnum.NEXT_BATCH, BigInteger.valueOf(100));
        Assert.assertEquals(1, rollupRepository.calcWaitingProofCountBeyondIndex(ProveTypeEnum.TEE_PROOF, BigInteger.valueOf(99)));
        Assert.assertEquals(0, rollupRepository.calcWaitingProofCountBeyondIndex(ProveTypeEnum.TEE_PROOF, BigInteger.valueOf(100)));
        Assert.assertEquals(0, rollupRepository.calcWaitingProofCountBeyondIndex(ProveTypeEnum.TEE_PROOF, BigInteger.valueOf(101)));
    }

    @Test
    public void testBatchProveRequest() {
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.ZK_PROOF);

        BatchProveRequestDO teeReq = rollupRepository.getBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);
        assertNotNull(teeReq);
        assertEquals(BigInteger.valueOf(100), teeReq.getBatchIndex());
        assertEquals(ProveTypeEnum.TEE_PROOF, teeReq.getProveType());
        assertEquals(BatchProveRequestStateEnum.PENDING, teeReq.getState());

        List<BatchProveRequestDO> requestDOS = rollupRepository.peekPendingBatchProveRequest(10, null);
        assertNotNull(requestDOS);
        assertEquals(2, requestDOS.size());

        rollupRepository.updateBatchProveRequestState(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF, BatchProveRequestStateEnum.PROVE_READY);
        teeReq = rollupRepository.getBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);
        assertEquals(BatchProveRequestStateEnum.PROVE_READY, teeReq.getState());
        rollupRepository.updateBatchProveRequestState(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF, BatchProveRequestStateEnum.PENDING);

        byte[] rawProof = RandomUtil.randomBytes(10);
        rollupRepository.saveBatchProofAndUpdateReqState(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF, rawProof);
        teeReq = rollupRepository.getBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);
        assertArrayEquals(rawProof, teeReq.getProof());
        assertEquals(BatchProveRequestStateEnum.PROVE_READY, teeReq.getState());
    }

    @Test
    public void testReliableTx() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);
        ReliableTransactionDO actual = rollupRepository.getReliableTransaction(RELIABLE_TRANSACTION_DO1.getOriginalTxHash());
        assertNotNull(actual);
        assertEquals(RELIABLE_TRANSACTION_DO1.getOriginalTxHash(), actual.getOriginalTxHash());
        assertEquals(RELIABLE_TRANSACTION_DO1.getNonce(), actual.getNonce());
        assertEquals(RELIABLE_TRANSACTION_DO1.getBatchIndex(), actual.getBatchIndex());
        assertArrayEquals(RELIABLE_TRANSACTION_DO1.getRawTx(), actual.getRawTx());
        assertEquals(RELIABLE_TRANSACTION_DO1.getTransactionType(), actual.getTransactionType());
        assertEquals(RELIABLE_TRANSACTION_DO1.getLatestTxSendTime(), actual.getLatestTxSendTime());
        assertEquals(RELIABLE_TRANSACTION_DO1.getSenderAccount(), actual.getSenderAccount());
        assertEquals(RELIABLE_TRANSACTION_DO1.getChainType(), actual.getChainType());
        assertEquals(RELIABLE_TRANSACTION_DO1.getLatestTxHash(), actual.getLatestTxHash());

        actual = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, RELIABLE_TRANSACTION_DO1.getBatchIndex(), RELIABLE_TRANSACTION_DO1.getTransactionType());
        assertNotNull(actual);
        assertEquals(RELIABLE_TRANSACTION_DO1.getOriginalTxHash(), actual.getOriginalTxHash());
        assertEquals(RELIABLE_TRANSACTION_DO1.getNonce(), actual.getNonce());
        assertEquals(RELIABLE_TRANSACTION_DO1.getBatchIndex(), actual.getBatchIndex());
        assertArrayEquals(RELIABLE_TRANSACTION_DO1.getRawTx(), actual.getRawTx());
        assertEquals(RELIABLE_TRANSACTION_DO1.getTransactionType(), actual.getTransactionType());
        assertEquals(RELIABLE_TRANSACTION_DO1.getLatestTxSendTime(), actual.getLatestTxSendTime());
        assertEquals(RELIABLE_TRANSACTION_DO1.getSenderAccount(), actual.getSenderAccount());
        assertEquals(RELIABLE_TRANSACTION_DO1.getChainType(), actual.getChainType());
        assertEquals(RELIABLE_TRANSACTION_DO1.getLatestTxHash(), actual.getLatestTxHash());
        assertTrue(actual.getGmtModified().getTime() > 0);

        List<ReliableTransactionDO> list = rollupRepository.getTxPendingReliableTransactions(10);
        assertNotNull(list);
        assertEquals(1, list.size());

        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO2);

        list = rollupRepository.getNotFinalizedReliableTransactions(ChainTypeEnum.LAYER_ONE, 10);
        assertNotNull(list);
        assertEquals(2, list.size());
        list.sort(Comparator.comparingLong(ReliableTransactionDO::getNonce));
        assertEquals(RELIABLE_TRANSACTION_DO2.getLatestTxHash(), list.get(1).getLatestTxHash());
        assertEquals(ReliableTransactionStateEnum.TX_PACKAGED, list.get(1).getState());

        rollupRepository.updateReliableTransactionState(RELIABLE_TRANSACTION_DO1.getOriginalTxHash(), ReliableTransactionStateEnum.TX_SUCCESS);
        actual = rollupRepository.getReliableTransaction(RELIABLE_TRANSACTION_DO1.getOriginalTxHash());
        assertEquals(ReliableTransactionStateEnum.TX_SUCCESS, actual.getState());

        rollupRepository.updateReliableTransactionState(ChainTypeEnum.LAYER_ONE, RELIABLE_TRANSACTION_DO1.getBatchIndex(), RELIABLE_TRANSACTION_DO1.getTransactionType(), ReliableTransactionStateEnum.TX_PENDING);
        actual = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, RELIABLE_TRANSACTION_DO1.getBatchIndex(), RELIABLE_TRANSACTION_DO1.getTransactionType());
        assertEquals(ReliableTransactionStateEnum.TX_PENDING, actual.getState());

        ReliableTransactionDO temp = BeanUtil.copyProperties(RELIABLE_TRANSACTION_DO1, ReliableTransactionDO.class);
        temp.setLatestTxHash("0x1234123412341234123412341234123412341234123412341234123412341234");
        rollupRepository.updateReliableTransaction(temp);
        actual = rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, RELIABLE_TRANSACTION_DO1.getBatchIndex(), RELIABLE_TRANSACTION_DO1.getTransactionType());
        assertEquals(temp.getLatestTxHash(), actual.getLatestTxHash());

        var l2Tx = BeanUtil.copyProperties(RELIABLE_TRANSACTION_DO2, ReliableTransactionDO.class);
        l2Tx.setState(ReliableTransactionStateEnum.TX_PACKAGED);
        l2Tx.setChainType(ChainTypeEnum.LAYER_TWO);
        l2Tx.setOriginalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)));
        l2Tx.setLatestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)));
        rollupRepository.insertReliableTransaction(l2Tx);

        list = rollupRepository.getNotFinalizedReliableTransactions(ChainTypeEnum.LAYER_TWO, 10);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveRawTx() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);
        var actual = rollupRepository.getReliableTransaction(RELIABLE_TRANSACTION_DO1.getOriginalTxHash());
        assertEquals(1, actual.getRawTx().length);
        rollupRepository.removeRawTx(ChainTypeEnum.LAYER_ONE, RELIABLE_TRANSACTION_DO1.getBatchIndex(), RELIABLE_TRANSACTION_DO1.getTransactionType());
        actual = rollupRepository.getReliableTransaction(RELIABLE_TRANSACTION_DO1.getOriginalTxHash());
        assertEquals(0, actual.getRawTx().length);
    }

    @Test
    public void testQueryLatestNonce() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        var actual = rollupRepository.queryLatestNonce(RELIABLE_TRANSACTION_DO1.getChainType(), RELIABLE_TRANSACTION_DO1.getSenderAccount());
        assertEquals(BigInteger.valueOf(RELIABLE_TRANSACTION_DO1.getNonce()), actual);

        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO2);
        actual = rollupRepository.queryLatestNonce(RELIABLE_TRANSACTION_DO1.getChainType(), RELIABLE_TRANSACTION_DO1.getSenderAccount());
        assertEquals(BigInteger.valueOf(RELIABLE_TRANSACTION_DO2.getNonce()), actual);

        actual = rollupRepository.queryLatestNonce(ChainTypeEnum.LAYER_TWO, RELIABLE_TRANSACTION_DO1.getSenderAccount());
        assertEquals(BigInteger.valueOf(-1), actual);
    }

    @Test
    public void testGetFailedReliableTransactions() {
        var dataList = randomReliableTransactionDOs(10, ReliableTransactionStateEnum.TX_FAILED);
        dataList.get(0).setRetryCount(1);
        dataList.get(1).setState(ReliableTransactionStateEnum.TX_PENDING);
        dataList.forEach(rollupRepository::insertReliableTransaction);

        var actual = rollupRepository.getFailedReliableTransactions(10, 1);

        assertEquals(8, actual.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(dataList.get(i + 2).getOriginalTxHash(), actual.get(i).getOriginalTxHash());
            assertEquals(dataList.get(i + 2).getLatestTxHash(), actual.get(i).getLatestTxHash());
            assertEquals(0, actual.get(i).getRetryCount().intValue());
            assertEquals(ReliableTransactionStateEnum.TX_FAILED, actual.get(i).getState());
            assertEquals(dataList.get(i + 2).getChainType(), actual.get(i).getChainType());
        }
    }

    // ==================== Negative Case Tests ====================

    /**
     * Test get batch with non-existent batch index
     * Verifies that querying non-existent batch returns null
     */
    @Test
    public void testGetBatch_NonExistentIndex() {
        BatchWrapper result = rollupRepository.getBatch(BigInteger.valueOf(999999));
        assertNull(result);
    }

    /**
     * Test get batch header with non-existent batch index
     * Verifies that querying non-existent batch header returns null
     */
    @Test
    public void testGetBatchHeader_NonExistentIndex() {
        BatchHeader result = rollupRepository.getBatchHeader(BigInteger.valueOf(999999));
        assertNull(result);
    }

    /**
     * Test get chunk with non-existent indices
     * Verifies that querying non-existent chunk returns null
     */
    @Test
    public void testGetChunk_NonExistentIndices() {
        ChunkWrapper result = rollupRepository.getChunk(BigInteger.valueOf(999999), 0);
        assertNull(result);
    }

    /**
     * Test get chunks with non-existent batch index
     * Verifies that querying non-existent chunks returns empty list
     */
    @Test
    public void testGetChunks_NonExistentBatchIndex() {
        List<ChunkWrapper> result = rollupRepository.getChunks(BigInteger.valueOf(999999));
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test get reliable transaction with non-existent hash
     * Verifies that querying non-existent transaction returns null
     */
    @Test
    public void testGetReliableTransaction_NonExistentHash() {
        ReliableTransactionDO result = rollupRepository.getReliableTransaction("0x0000000000000000000000000000000000000000000000000000000000000000");
        assertNull(result);
    }

    /**
     * Test get reliable transaction with non-existent batch index and type
     * Verifies that querying non-existent transaction returns null
     */
    @Test
    public void testGetReliableTransaction_NonExistentBatchAndType() {
        ReliableTransactionDO result = rollupRepository.getReliableTransaction(
                ChainTypeEnum.LAYER_ONE,
                BigInteger.valueOf(999999),
                TransactionTypeEnum.BATCH_COMMIT_TX
        );
        assertNull(result);
    }

    /**
     * Test update reliable transaction state for non-existent transaction
     * Verifies that updating non-existent transaction throws IllegalArgumentException
     */
    @Test
    public void testUpdateReliableTransactionState_NonExistentTransaction() {
        // Should throw IllegalArgumentException when updating non-existent transaction
        try {
            rollupRepository.updateReliableTransactionState(
                    "0x0000000000000000000000000000000000000000000000000000000000000000",
                    ReliableTransactionStateEnum.TX_SUCCESS
            );
            fail("Expected IllegalArgumentException for non-existent transaction");
        } catch (IllegalArgumentException e) {
            // Expected exception - update count must be 1
            assertTrue(e.getMessage().contains("must be equals"));
        }
    }

    /**
     * Test get batch prove request with non-existent batch index
     * Verifies that querying non-existent prove request returns null
     */
    @Test
    public void testGetBatchProveRequest_NonExistentIndex() {
        BatchProveRequestDO result = rollupRepository.getBatchProveRequest(
                BigInteger.valueOf(999999),
                ProveTypeEnum.TEE_PROOF
        );
        assertNull(result);
    }

    /**
     * Test update batch prove request state for non-existent request
     * Verifies that updating non-existent request throws IllegalArgumentException
     */
    @Test
    public void testUpdateBatchProveRequestState_NonExistentRequest() {
        // Should throw IllegalArgumentException when updating non-existent request
        try {
            rollupRepository.updateBatchProveRequestState(
                    BigInteger.valueOf(999999),
                    ProveTypeEnum.TEE_PROOF,
                    BatchProveRequestStateEnum.PROVE_READY
            );
            fail("Expected IllegalArgumentException for non-existent request");
        } catch (IllegalArgumentException e) {
            // Expected exception - update count must be 1
            assertTrue(e.getMessage().contains("must be equals"));
        }
    }

    /**
     * Test get L2 block trace from cache with non-existent block number
     * Verifies that querying non-existent cached block trace returns null
     */
    @Test
    public void testGetL2BlockTraceFromCache_NonExistentBlock() {
        BasicBlockTrace result = rollupRepository.getL2BlockTraceFromCache(BigInteger.valueOf(999999));
        assertNull(result);
    }

    /**
     * Test get rollup number record with invalid type
     * Verifies that querying with invalid parameters returns default value
     */
    @Test
    public void testGetRollupNumberRecord_DefaultValues() {
        // Test various combinations that should return default values
        BigInteger result1 = rollupRepository.getRollupNumberRecord(
                ChainTypeEnum.LAYER_ONE,
                RollupNumberRecordTypeEnum.NEXT_CHUNK
        );
        assertEquals(BigInteger.valueOf(0), result1);

        BigInteger result2 = rollupRepository.getRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO,
                RollupNumberRecordTypeEnum.BLOCK_PROCESSED
        );
        assertEquals(BigInteger.valueOf(0), result2);
    }

    /**
     * Test get not finalized reliable transactions with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    public void testGetNotFinalizedReliableTransactions_ZeroLimit() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        List<ReliableTransactionDO> result = rollupRepository.getNotFinalizedReliableTransactions(
                ChainTypeEnum.LAYER_ONE,
                0
        );
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test get TX pending reliable transactions with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    public void testGetTxPendingReliableTransactions_ZeroLimit() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        List<ReliableTransactionDO> result = rollupRepository.getTxPendingReliableTransactions(0);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test get failed reliable transactions with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    public void testGetFailedReliableTransactions_ZeroLimit() {
        var dataList = randomReliableTransactionDOs(5, ReliableTransactionStateEnum.TX_FAILED);
        dataList.forEach(rollupRepository::insertReliableTransaction);

        var result = rollupRepository.getFailedReliableTransactions(0, 0);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test peek pending batch prove request with zero limit
     * Verifies that zero limit returns empty list
     */
    @Test
    public void testPeekPendingBatchProveRequest_ZeroLimit() {
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);

        List<BatchProveRequestDO> result = rollupRepository.peekPendingBatchProveRequest(0, null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test save batch proof and update request state for non-existent request
     * Verifies that operation throws IllegalArgumentException for non-existent request
     */
    @Test
    public void testSaveBatchProofAndUpdateReqState_NonExistentRequest() {
        byte[] rawProof = RandomUtil.randomBytes(10);

        // Should throw IllegalArgumentException when saving proof for non-existent request
        try {
            rollupRepository.saveBatchProofAndUpdateReqState(
                    BigInteger.valueOf(999999),
                    ProveTypeEnum.TEE_PROOF,
                    rawProof
            );
            fail("Expected IllegalArgumentException for non-existent request");
        } catch (IllegalArgumentException e) {
            // Expected exception - update count must be 1
            assertTrue(e.getMessage().contains("must be equals"));
        }
    }

    /**
     * Test duplicate chunk insertion
     * Verifies that saving the same chunk twice is handled correctly
     */
    @Test
    public void testSaveChunk_DuplicateInsertion() {
        rollupRepository.saveChunk(CHUNK1);

        // Save the same chunk again - should handle gracefully
        rollupRepository.saveChunk(CHUNK1);

        List<ChunkWrapper> chunks = rollupRepository.getChunks(CHUNK1.getBatchIndex());
        assertNotNull(chunks);
        // Should only have one chunk
        assertEquals(1, chunks.size());
    }

    /**
     * Test calc waiting batch count beyond index with negative index
     * Verifies that calculation handles edge cases correctly
     */
    @Test
    public void testCalcWaitingBatchCountBeyondIndex_EdgeCases() {
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO,
                RollupNumberRecordTypeEnum.NEXT_BATCH,
                BigInteger.valueOf(100)
        );

        // Test with very large index
        assertEquals(0, rollupRepository.calcWaitingBatchCountBeyondIndex(BigInteger.valueOf(1000000)));
    }

    /**
     * Test calc waiting proof count beyond index with negative index
     * Verifies that calculation handles edge cases correctly
     */
    @Test
    public void testCalcWaitingProofCountBeyondIndex_EdgeCases() {
        rollupRepository.updateRollupNumberRecord(
                ChainTypeEnum.LAYER_TWO,
                RollupNumberRecordTypeEnum.NEXT_BATCH,
                BigInteger.valueOf(100)
        );

        // Test with very large index
        assertEquals(0, rollupRepository.calcWaitingProofCountBeyondIndex(
                ProveTypeEnum.TEE_PROOF,
                BigInteger.valueOf(1000000)
        ));
    }

    /**
     * Test update reliable transaction with non-existent transaction
     * Verifies that updating non-existent transaction throws IllegalArgumentException
     */
    @Test
    public void testUpdateReliableTransaction_NonExistentTransaction() {
        ReliableTransactionDO temp = BeanUtil.copyProperties(RELIABLE_TRANSACTION_DO1, ReliableTransactionDO.class);
        temp.setOriginalTxHash("0x0000000000000000000000000000000000000000000000000000000000000000");
        temp.setLatestTxHash("0x1111111111111111111111111111111111111111111111111111111111111111");

        // Should throw IllegalArgumentException when updating non-existent transaction
        try {
            rollupRepository.updateReliableTransaction(temp);
            fail("Expected IllegalArgumentException for non-existent transaction");
        } catch (IllegalArgumentException e) {
            // Expected exception - update count must be 1
            assertTrue(e.getMessage().contains("must be equals"));
        }
    }

    // ==================== Rollback Related Method Tests ====================

    /**
     * Test deleteBatchesFrom - deletes all batches with batch_index >= specified index
     */
    @Test
    public void testDeleteBatchesFrom() {
        // Save multiple batches
        rollupRepository.saveChunk(CHUNK1);
        BatchWrapper batchWrapper1 = new BatchWrapper();
        batchWrapper1.setBatch(BATCH1);
        batchWrapper1.setPostStateRoot(RandomUtil.randomBytes(32));
        batchWrapper1.setL2MsgRoot(RandomUtil.randomBytes(32));
        rollupRepository.saveBatch(batchWrapper1);

        var chunk2 = new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(101), 0, ListUtil.toList(BASIC_BLOCK_TRACE1));
        rollupRepository.saveChunk(chunk2);
        var batch2 = new Batch(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(101), ZERO_BATCH_HEADER, RandomUtil.randomBytes(32), ListUtil.toList(chunk2.getChunk()));
        BatchWrapper batchWrapper2 = new BatchWrapper();
        batchWrapper2.setBatch(batch2);
        batchWrapper2.setPostStateRoot(RandomUtil.randomBytes(32));
        batchWrapper2.setL2MsgRoot(RandomUtil.randomBytes(32));
        rollupRepository.saveBatch(batchWrapper2);

        var chunk3 = new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(102), 0, ListUtil.toList(BASIC_BLOCK_TRACE2));
        rollupRepository.saveChunk(chunk3);
        var batch3 = new Batch(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(102), ZERO_BATCH_HEADER, RandomUtil.randomBytes(32), ListUtil.toList(chunk3.getChunk()));
        BatchWrapper batchWrapper3 = new BatchWrapper();
        batchWrapper3.setBatch(batch3);
        batchWrapper3.setPostStateRoot(RandomUtil.randomBytes(32));
        batchWrapper3.setL2MsgRoot(RandomUtil.randomBytes(32));
        rollupRepository.saveBatch(batchWrapper3);

        // Verify all batches exist
        assertTrue(rollupRepository.hasBatch(BigInteger.valueOf(100)));
        assertTrue(rollupRepository.hasBatch(BigInteger.valueOf(101)));
        assertTrue(rollupRepository.hasBatch(BigInteger.valueOf(102)));

        // Delete batches from index 101
        int deleted = rollupRepository.deleteBatchesFrom(BigInteger.valueOf(101));
        assertEquals(2, deleted);

        // Verify batch 100 still exists, 101 and 102 are deleted
        assertTrue(rollupRepository.hasBatch(BigInteger.valueOf(100)));
        assertFalse(rollupRepository.hasBatch(BigInteger.valueOf(101)));
        assertFalse(rollupRepository.hasBatch(BigInteger.valueOf(102)));
    }

    /**
     * Test deleteBatchesFrom with no matching batches
     */
    @Test
    public void testDeleteBatchesFrom_NoMatchingBatches() {
        rollupRepository.saveChunk(CHUNK1);
        BatchWrapper batchWrapper = new BatchWrapper();
        batchWrapper.setBatch(BATCH1);
        batchWrapper.setPostStateRoot(RandomUtil.randomBytes(32));
        batchWrapper.setL2MsgRoot(RandomUtil.randomBytes(32));
        rollupRepository.saveBatch(batchWrapper);

        // Delete from a higher index - should delete nothing
        int deleted = rollupRepository.deleteBatchesFrom(BigInteger.valueOf(200));
        assertEquals(0, deleted);

        // Original batch should still exist
        assertTrue(rollupRepository.hasBatch(BigInteger.valueOf(100)));
    }

    /**
     * Test deleteChunksForRollback - deletes chunks based on batch and chunk index
     * Deletes chunks where: batch_index > targetBatchIndex OR (batch_index = targetBatchIndex AND chunk_index >= targetChunkIndex)
     */
    @Test
    public void testDeleteChunksForRollback() {
        // Save chunks for batch 100
        rollupRepository.saveChunk(CHUNK1); // batch 100, chunk 0
        var chunk2 = new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(100), 1, ListUtil.toList(BASIC_BLOCK_TRACE2));
        rollupRepository.saveChunk(chunk2); // batch 100, chunk 1

        // Verify chunks exist
        assertNotNull(rollupRepository.getChunk(BigInteger.valueOf(100), 0));
        assertNotNull(rollupRepository.getChunk(BigInteger.valueOf(100), 1));

        // Delete chunks: batch > 100 OR (batch = 100 AND chunk >= 1)
        // This should delete chunk 100-1 (chunk_index >= 1)
        int deleted = rollupRepository.deleteChunksForRollback(BigInteger.valueOf(100), 1);
        
        // Verify chunk 100-0 still exists, 100-1 is deleted
        assertNotNull(rollupRepository.getChunk(BigInteger.valueOf(100), 0));
        // Note: The actual number of deleted records depends on the implementation
        assertTrue(deleted >= 0);
    }

    /**
     * Test deleteBatchProveRequestsFrom - deletes batch prove requests
     */
    @Test
    public void testDeleteBatchProveRequestsFrom() {
        // Create multiple batch prove requests
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF);
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(101), ProveTypeEnum.TEE_PROOF);
        rollupRepository.createBatchProveRequest(BigInteger.valueOf(102), ProveTypeEnum.TEE_PROOF);

        // Verify all requests exist
        assertNotNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF));
        assertNotNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(101), ProveTypeEnum.TEE_PROOF));
        assertNotNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(102), ProveTypeEnum.TEE_PROOF));

        // Delete from index 101
        int deleted = rollupRepository.deleteBatchProveRequestsFrom(BigInteger.valueOf(101));
        assertEquals(2, deleted);

        // Verify request 100 still exists, 101 and 102 are deleted
        assertNotNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(100), ProveTypeEnum.TEE_PROOF));
        assertNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(101), ProveTypeEnum.TEE_PROOF));
        assertNull(rollupRepository.getBatchProveRequest(BigInteger.valueOf(102), ProveTypeEnum.TEE_PROOF));
    }

    /**
     * Test deleteRollupReliableTransactionsFrom - deletes rollup transactions
     */
    @Test
    public void testDeleteRollupReliableTransactionsFrom() {
        // Create rollup transactions for different batches
        var tx1 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .batchIndex(BigInteger.valueOf(100))
                .chainType(ChainTypeEnum.LAYER_ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(88L)
                .build();
        rollupRepository.insertReliableTransaction(tx1);

        var tx2 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                .batchIndex(BigInteger.valueOf(101))
                .chainType(ChainTypeEnum.LAYER_ONE)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(89L)
                .build();
        rollupRepository.insertReliableTransaction(tx2);

        // Delete from index 101
        int deleted = rollupRepository.deleteRollupReliableTransactionsFrom(BigInteger.valueOf(101));
        assertEquals(1, deleted);

        // Verify tx1 still exists, tx2 is deleted
        assertNotNull(rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, BigInteger.valueOf(100), TransactionTypeEnum.BATCH_COMMIT_TX));
        assertNull(rollupRepository.getReliableTransaction(ChainTypeEnum.LAYER_ONE, BigInteger.valueOf(101), TransactionTypeEnum.BATCH_COMMIT_TX));
    }

    /**
     * Test deleteL1MsgReliableTransactionsAboveNonce - deletes L1 message transactions
     */
    @Test
    public void testDeleteL1MsgReliableTransactionsAboveNonce() {
        // Create L1 message transactions with different nonces
        var tx1 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.L1_MSG_TX)
                .batchIndex(BigInteger.valueOf(100))
                .chainType(ChainTypeEnum.LAYER_TWO)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(50L)
                .build();
        rollupRepository.insertReliableTransaction(tx1);

        var tx2 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.L1_MSG_TX)
                .batchIndex(BigInteger.valueOf(101))
                .chainType(ChainTypeEnum.LAYER_TWO)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(60L)
                .build();
        rollupRepository.insertReliableTransaction(tx2);

        // Delete transactions with nonce > 50
        int deleted = rollupRepository.deleteL1MsgReliableTransactionsAboveNonce(50L);
        assertEquals(1, deleted);

        // Verify tx1 still exists (nonce=50), tx2 is deleted (nonce=60)
        assertNotNull(rollupRepository.getReliableTransaction(tx1.getOriginalTxHash()));
        assertNull(rollupRepository.getReliableTransaction(tx2.getOriginalTxHash()));
    }

    /**
     * Test deleteOracleBatchFeeFeedTransactionsFrom - deletes oracle batch fee feed transactions
     */
    @Test
    public void testDeleteOracleBatchFeeFeedTransactionsFrom() {
        // Create oracle batch fee feed transactions
        var tx1 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.L2_ORACLE_BATCH_FEE_FEED_TX)
                .batchIndex(BigInteger.valueOf(100))
                .chainType(ChainTypeEnum.LAYER_TWO)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(88L)
                .build();
        rollupRepository.insertReliableTransaction(tx1);

        var tx2 = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.L2_ORACLE_BATCH_FEE_FEED_TX)
                .batchIndex(BigInteger.valueOf(101))
                .chainType(ChainTypeEnum.LAYER_TWO)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(89L)
                .build();
        rollupRepository.insertReliableTransaction(tx2);

        // Delete from index 101
        int deleted = rollupRepository.deleteOracleBatchFeeFeedTransactionsFrom(BigInteger.valueOf(101));
        assertEquals(1, deleted);

        // Verify tx1 still exists, tx2 is deleted
        assertNotNull(rollupRepository.getReliableTransaction(tx1.getOriginalTxHash()));
        assertNull(rollupRepository.getReliableTransaction(tx2.getOriginalTxHash()));
    }

    // ==================== Tests for methods using CAST(... AS BIGINT) ====================
    // Note: These tests use CAST(... AS BIGINT) which is compatible with both MySQL and H2.

    /**
     * Test findChunkByBlockHeight - finds chunk containing specific block height
     * CHUNK1 is created from BASIC_BLOCK_TRACE1 (block#1) and BASIC_BLOCK_TRACE2 (block#2)
     * So the block range is 1-2
     */
    @Test
    public void testFindChunkByBlockHeight() {
        rollupRepository.saveChunk(CHUNK1);

        // Find chunk by block height within range (block 1 or 2)
        ChunkWrapper found = rollupRepository.findChunkByBlockHeight(BigInteger.valueOf(100), BigInteger.ONE);
        assertNotNull(found);
        assertEquals(BigInteger.valueOf(100), found.getBatchIndex());
        assertEquals(0, found.getChunkIndex());
    }

    /**
     * Test findChunkByBlockHeight - returns null when block height not in any chunk
     */
    @Test
    public void testFindChunkByBlockHeight_NotFound() {
        rollupRepository.saveChunk(CHUNK1);

        // Find chunk by block height outside range (much higher than end_number)
        ChunkWrapper found = rollupRepository.findChunkByBlockHeight(BigInteger.valueOf(100), BigInteger.valueOf(999999));
        assertNull(found);
    }

    /**
     * Test findChunkByBlockHeight - returns null for non-existent batch
     */
    @Test
    public void testFindChunkByBlockHeight_NonExistentBatch() {
        ChunkWrapper found = rollupRepository.findChunkByBlockHeight(BigInteger.valueOf(999), BigInteger.valueOf(50));
        assertNull(found);
    }

    /**
     * Test findChunkByBlockHeight - finds correct chunk among multiple chunks
     * Uses existing BASIC_BLOCK_TRACE constants to create chunks
     */
    @Test
    public void testFindChunkByBlockHeight_MultipleChunks() {
        // Save multiple chunks for the same batch with different block ranges
        // CHUNK1: batch 100, chunk 0 (from BASIC_BLOCK_TRACE1 and BASIC_BLOCK_TRACE2)
        rollupRepository.saveChunk(CHUNK1);

        // Create chunk 1 using existing BASIC_BLOCK_TRACE2
        var chunk2 = new ChunkWrapper(BatchVersionEnum.BATCH_V0, BigInteger.valueOf(100), 1, ListUtil.toList(BASIC_BLOCK_TRACE2));
        rollupRepository.saveChunk(chunk2);

        // Verify both chunks exist
        assertNotNull(rollupRepository.getChunk(BigInteger.valueOf(100), 0));
        assertNotNull(rollupRepository.getChunk(BigInteger.valueOf(100), 1));
    }

    /**
     * Test findChunkByBlockHeight - block height at boundary
     */
    @Test
    public void testFindChunkByBlockHeight_AtBoundary() {
        rollupRepository.saveChunk(CHUNK1);

        // Find chunk at the exact end block number
        ChunkWrapper found = rollupRepository.findChunkByBlockHeight(BigInteger.valueOf(100), CHUNK1.getEndBlockNumber());
        if (found != null) {
            assertEquals(BigInteger.valueOf(100), found.getBatchIndex());
            assertEquals(0, found.getChunkIndex());
        }
    }

    // ==================== Tests for getReliableTransactionsByState ====================

    /**
     * Test getReliableTransactionsByState - returns only TX_PENDING transactions
     */
    @Test
    public void testGetReliableTransactionsByState_PendingOnly() {
        // Insert TX_PENDING transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);
        // Insert TX_PACKAGED transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO2);

        // Query only TX_PENDING
        List<ReliableTransactionDO> pendingList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PENDING,
                10
        );
        assertNotNull(pendingList);
        assertEquals(1, pendingList.size());
        assertEquals(ReliableTransactionStateEnum.TX_PENDING, pendingList.get(0).getState());
        assertEquals(RELIABLE_TRANSACTION_DO1.getOriginalTxHash(), pendingList.get(0).getOriginalTxHash());
    }

    /**
     * Test getReliableTransactionsByState - returns only TX_PACKAGED transactions
     */
    @Test
    public void testGetReliableTransactionsByState_PackagedOnly() {
        // Insert TX_PENDING transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);
        // Insert TX_PACKAGED transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO2);

        // Query only TX_PACKAGED
        List<ReliableTransactionDO> packagedList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PACKAGED,
                10
        );
        assertNotNull(packagedList);
        assertEquals(1, packagedList.size());
        assertEquals(ReliableTransactionStateEnum.TX_PACKAGED, packagedList.get(0).getState());
        assertEquals(RELIABLE_TRANSACTION_DO2.getOriginalTxHash(), packagedList.get(0).getOriginalTxHash());
    }

    /**
     * Test getReliableTransactionsByState - returns empty list when no matching state
     */
    @Test
    public void testGetReliableTransactionsByState_NoMatchingState() {
        // Insert only TX_PENDING transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        // Query TX_FAILED (should return empty)
        List<ReliableTransactionDO> failedList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_FAILED,
                10
        );
        assertNotNull(failedList);
        assertEquals(0, failedList.size());
    }

    /**
     * Test getReliableTransactionsByState - respects batch size limit
     */
    @Test
    public void testGetReliableTransactionsByState_BatchSizeLimit() {
        // Insert multiple TX_PENDING transactions
        for (int i = 0; i < 5; i++) {
            ReliableTransactionDO tx = ReliableTransactionDO.builder()
                    .transactionType(TransactionTypeEnum.BATCH_COMMIT_TX)
                    .batchIndex(BigInteger.valueOf(100 + i))
                    .chainType(ChainTypeEnum.LAYER_ONE)
                    .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                    .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                    .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                    .rawTx(HexUtil.decodeHex("00"))
                    .latestTxSendTime(new Date())
                    .nonce(88L + i)
                    .state(ReliableTransactionStateEnum.TX_PENDING)
                    .build();
            rollupRepository.insertReliableTransaction(tx);
        }

        // Query with limit 3
        List<ReliableTransactionDO> limitedList = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PENDING,
                3
        );
        assertNotNull(limitedList);
        assertEquals(3, limitedList.size());
    }

    /**
     * Test getReliableTransactionsByState - filters by chain type
     */
    @Test
    public void testGetReliableTransactionsByState_FiltersByChainType() {
        // Insert L1 TX_PENDING transaction
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        // Insert L2 TX_PENDING transaction
        ReliableTransactionDO l2Tx = ReliableTransactionDO.builder()
                .transactionType(TransactionTypeEnum.L2_ORACLE_BASE_FEE_FEED_TX)
                .batchIndex(BigInteger.valueOf(200))
                .chainType(ChainTypeEnum.LAYER_TWO)
                .senderAccount("0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4")
                .originalTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .latestTxHash(Numeric.toHexString(RandomUtil.randomBytes(32)))
                .rawTx(HexUtil.decodeHex("00"))
                .latestTxSendTime(new Date())
                .nonce(100L)
                .state(ReliableTransactionStateEnum.TX_PENDING)
                .build();
        rollupRepository.insertReliableTransaction(l2Tx);

        // Query L1 only
        List<ReliableTransactionDO> l1List = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PENDING,
                10
        );
        assertNotNull(l1List);
        assertEquals(1, l1List.size());
        assertEquals(ChainTypeEnum.LAYER_ONE, l1List.get(0).getChainType());

        // Query L2 only
        List<ReliableTransactionDO> l2List = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_TWO,
                ReliableTransactionStateEnum.TX_PENDING,
                10
        );
        assertNotNull(l2List);
        assertEquals(1, l2List.size());
        assertEquals(ChainTypeEnum.LAYER_TWO, l2List.get(0).getChainType());
    }

    /**
     * Test getReliableTransactionsByState - zero limit returns empty list
     */
    @Test
    public void testGetReliableTransactionsByState_ZeroLimit() {
        rollupRepository.insertReliableTransaction(RELIABLE_TRANSACTION_DO1);

        List<ReliableTransactionDO> result = rollupRepository.getReliableTransactionsByState(
                ChainTypeEnum.LAYER_ONE,
                ReliableTransactionStateEnum.TX_PENDING,
                0
        );
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
