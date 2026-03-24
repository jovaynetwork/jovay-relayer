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

    @MockitoBean
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
}
