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
import java.util.Collections;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.DaProof;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import com.alipay.antchain.l2.relayer.engine.DistributedTaskEngine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RelayerLocalDaService
 * Tests local data availability service functionality
 */
public class RelayerLocalDaServiceTest extends TestBase {

    @TestBean(name = "l1Web3j")
    private Web3j web3j;

    private static Web3j web3j() {
        return mock(Web3j.class);
    }

    @TestBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    private static BigInteger l1ChainId() {
        return BigInteger.valueOf(123);
    }

    @MockitoBean
    private L2Client l2Client;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private DistributedTaskEngine distributedTaskEngine;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private IRollupRepository rollupRepository;

    private RelayerLocalDaService daService;

    private BatchWrapper testBatchWrapper;

    @Before
    public void setUp() {
        daService = new RelayerLocalDaService(rollupRepository);
        testBatchWrapper = createTestBatchWrapper();
    }

    // ==================== Helper Methods ====================

    /**
     * Create a test batch wrapper for testing
     */
    private BatchWrapper createTestBatchWrapper() {
        byte[] postStateRoot = new byte[32];
        Arrays.fill(postStateRoot, (byte) 0x01);
        
        byte[] l1MsgRollingHash = new byte[32];
        Arrays.fill(l1MsgRollingHash, (byte) 0x02);
        
        byte[] l2MsgRoot = new byte[32];
        Arrays.fill(l2MsgRoot, (byte) 0x03);

        ChunkWrapper chunk = new ChunkWrapper(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                0,
                ListUtil.toList(BASIC_BLOCK_TRACE1, BASIC_BLOCK_TRACE2)
        );

        return BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_WRAPPER,
                postStateRoot,
                l1MsgRollingHash,
                l2MsgRoot,
                10L,
                Collections.singletonList(chunk)
        );
    }

    // ==================== uploadBatch Tests ====================

    /**
     * Test uploadBatch with valid batch wrapper
     * Should save batch to repository successfully
     */
    @Test
    public void testUploadBatch_Success() {
        doNothing().when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(testBatchWrapper);

        verify(rollupRepository, times(1)).saveBatch(testBatchWrapper);
    }

    /**
     * Test uploadBatch with null batch wrapper
     * Should handle null gracefully
     */
    @Test
    public void testUploadBatch_NullBatch() {
        doNothing().when(rollupRepository).saveBatch(any());

        daService.uploadBatch(null);

        verify(rollupRepository, times(1)).saveBatch(null);
    }

    /**
     * Test uploadBatch when repository throws exception
     * Should propagate exception
     */
    @Test(expected = RuntimeException.class)
    public void testUploadBatch_RepositoryException() {
        doThrow(new RuntimeException("Database error")).when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(testBatchWrapper);
    }

    /**
     * Test uploadBatch with multiple batches
     * Should save all batches correctly
     */
    @Test
    public void testUploadBatch_MultipleBatches() {
        BatchWrapper batch1 = createTestBatchWrapper();
        BatchWrapper batch2 = createTestBatchWrapper();
        BatchWrapper batch3 = createTestBatchWrapper();

        doNothing().when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(batch1);
        daService.uploadBatch(batch2);
        daService.uploadBatch(batch3);

        verify(rollupRepository, times(3)).saveBatch(any(BatchWrapper.class));
    }

    // ==================== endorseBatch Tests ====================

    /**
     * Test endorseBatch with valid batch wrapper
     * Should return empty proof
     */
    @Test
    public void testEndorseBatch_Success() {
        DaProof proof = daService.endorseBatch(testBatchWrapper);

        Assert.assertNotNull(proof);
        Assert.assertNotNull(proof.proof());
        Assert.assertEquals(0, proof.proof().length);
    }

    /**
     * Test endorseBatch with null batch wrapper
     * Should return empty proof
     */
    @Test
    public void testEndorseBatch_NullBatch() {
        DaProof proof = daService.endorseBatch(null);

        Assert.assertNotNull(proof);
        Assert.assertNotNull(proof.proof());
        Assert.assertEquals(0, proof.proof().length);
    }

    /**
     * Test endorseBatch returns consistent empty proof
     * Should always return empty byte array
     */
    @Test
    public void testEndorseBatch_ConsistentEmptyProof() {
        DaProof proof1 = daService.endorseBatch(testBatchWrapper);
        DaProof proof2 = daService.endorseBatch(testBatchWrapper);

        Assert.assertNotNull(proof1);
        Assert.assertNotNull(proof2);
        Assert.assertEquals(proof1.proof().length, proof2.proof().length);
        Assert.assertEquals(0, proof1.proof().length);
        Assert.assertEquals(0, proof2.proof().length);
    }

    /**
     * Test endorseBatch with different batch wrappers
     * Should return empty proof for all
     */
    @Test
    public void testEndorseBatch_DifferentBatches() {
        BatchWrapper batch1 = createTestBatchWrapper();
        BatchWrapper batch2 = createTestBatchWrapper();

        DaProof proof1 = daService.endorseBatch(batch1);
        DaProof proof2 = daService.endorseBatch(batch2);

        Assert.assertNotNull(proof1);
        Assert.assertNotNull(proof2);
        Assert.assertEquals(0, proof1.proof().length);
        Assert.assertEquals(0, proof2.proof().length);
    }

    // ==================== Integration Tests ====================

    /**
     * Test complete workflow: upload then endorse
     * Should work correctly in sequence
     */
    @Test
    public void testCompleteWorkflow_UploadThenEndorse() {
        doNothing().when(rollupRepository).saveBatch(any(BatchWrapper.class));

        // Upload batch
        daService.uploadBatch(testBatchWrapper);
        verify(rollupRepository, times(1)).saveBatch(testBatchWrapper);

        // Endorse batch
        DaProof proof = daService.endorseBatch(testBatchWrapper);
        Assert.assertNotNull(proof);
        Assert.assertEquals(0, proof.proof().length);
    }

    /**
     * Test service with batch containing multiple chunks
     * Should handle complex batches correctly
     */
    @Test
    public void testWithMultipleChunks() {
        ChunkWrapper chunk1 = new ChunkWrapper(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                0,
                ListUtil.toList(BASIC_BLOCK_TRACE1)
        );
        
        ChunkWrapper chunk2 = new ChunkWrapper(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                1,
                ListUtil.toList(BASIC_BLOCK_TRACE2)
        );

        BatchWrapper complexBatch = BatchWrapper.createBatch(
                BatchVersionEnum.BATCH_V0,
                BigInteger.valueOf(1),
                ZERO_BATCH_WRAPPER,
                new byte[32],
                new byte[32],
                Bytes32.DEFAULT.getValue(),
                0L,
                ListUtil.toList(chunk1, chunk2)
        );

        doNothing().when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(complexBatch);
        verify(rollupRepository, times(1)).saveBatch(complexBatch);

        DaProof proof = daService.endorseBatch(complexBatch);
        Assert.assertNotNull(proof);
        Assert.assertEquals(0, proof.proof().length);
    }

    // ==================== Negative Case Tests ====================

    /**
     * Test uploadBatch with repository connection error
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testUploadBatch_ConnectionError() {
        doThrow(new RuntimeException("Connection timeout")).when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(testBatchWrapper);
    }

    /**
     * Test uploadBatch with repository constraint violation
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testUploadBatch_ConstraintViolation() {
        doThrow(new RuntimeException("Duplicate batch index")).when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(testBatchWrapper);
    }

    /**
     * Test service behavior with empty batch
     * Should handle gracefully
     */
    @Test
    public void testWithEmptyBatch() {
        BatchWrapper emptyBatch = mock(BatchWrapper.class);

        doNothing().when(rollupRepository).saveBatch(any(BatchWrapper.class));

        daService.uploadBatch(emptyBatch);
        verify(rollupRepository, times(1)).saveBatch(emptyBatch);

        DaProof proof = daService.endorseBatch(emptyBatch);
        Assert.assertNotNull(proof);
        Assert.assertEquals(0, proof.proof().length);
    }
}
