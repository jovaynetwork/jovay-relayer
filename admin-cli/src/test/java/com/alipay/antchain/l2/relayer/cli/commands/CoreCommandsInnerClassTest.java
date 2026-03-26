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

package com.alipay.antchain.l2.relayer.cli.commands;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for inner static classes in CoreCommands.
 * This test class validates the TxInfo and DaInfo inner classes
 * which are POJOs with Lombok annotations.
 */
public class CoreCommandsInnerClassTest {

    // ==================== TxInfo Tests ====================

    /**
     * Test TxInfo builder pattern.
     */
    @Test
    public void testTxInfoBuilder() {
        CoreCommands.TxInfo txInfo = CoreCommands.TxInfo.builder()
                .type("BATCH_COMMIT_TX")
                .originalTx("0x123")
                .latestTx("0x456")
                .latestSendDate("2026-03-26 12:00:00")
                .state("TX_SUCCESS")
                .retryCount(0)
                .revertReason("")
                .build();

        assertEquals("BATCH_COMMIT_TX", txInfo.getType());
        assertEquals("0x123", txInfo.getOriginalTx());
        assertEquals("0x456", txInfo.getLatestTx());
        assertEquals("2026-03-26 12:00:00", txInfo.getLatestSendDate());
        assertEquals("TX_SUCCESS", txInfo.getState());
        assertEquals(0, txInfo.getRetryCount());
        assertEquals("", txInfo.getRevertReason());
    }

    /**
     * Test TxInfo all getters.
     */
    @Test
    public void testTxInfoGetters() {
        CoreCommands.TxInfo txInfo = CoreCommands.TxInfo.builder()
                .type("BATCH_TEE_PROOF_COMMIT_TX")
                .originalTx("0xabc")
                .latestTx("0xdef")
                .latestSendDate("2026-03-26 13:00:00")
                .state("TX_PENDING")
                .retryCount(2)
                .revertReason("insufficient gas")
                .build();

        assertEquals("BATCH_TEE_PROOF_COMMIT_TX", txInfo.getType());
        assertEquals("0xabc", txInfo.getOriginalTx());
        assertEquals("0xdef", txInfo.getLatestTx());
        assertEquals("2026-03-26 13:00:00", txInfo.getLatestSendDate());
        assertEquals("TX_PENDING", txInfo.getState());
        assertEquals(2, txInfo.getRetryCount());
        assertEquals("insufficient gas", txInfo.getRevertReason());
    }

    /**
     * Test TxInfo no-args constructor.
     */
    @Test
    public void testTxInfoNoArgsConstructor() {
        CoreCommands.TxInfo txInfo = new CoreCommands.TxInfo();

        assertNull(txInfo.getType());
        assertNull(txInfo.getOriginalTx());
        assertNull(txInfo.getLatestTx());
        assertNull(txInfo.getLatestSendDate());
        assertNull(txInfo.getState());
        assertEquals(0, txInfo.getRetryCount());
        assertNull(txInfo.getRevertReason());
    }

    /**
     * Test TxInfo all-args constructor.
     */
    @Test
    public void testTxInfoAllArgsConstructor() {
        CoreCommands.TxInfo txInfo = new CoreCommands.TxInfo(
                "BATCH_ZK_PROOF_COMMIT_TX",
                "0x789",
                "0x012",
                "2026-03-26 14:00:00",
                "TX_FAILED",
                3,
                "revert"
        );

        assertEquals("BATCH_ZK_PROOF_COMMIT_TX", txInfo.getType());
        assertEquals("0x789", txInfo.getOriginalTx());
        assertEquals("0x012", txInfo.getLatestTx());
        assertEquals("2026-03-26 14:00:00", txInfo.getLatestSendDate());
        assertEquals("TX_FAILED", txInfo.getState());
        assertEquals(3, txInfo.getRetryCount());
        assertEquals("revert", txInfo.getRevertReason());
    }

    /**
     * Test TxInfo with null values.
     */
    @Test
    public void testTxInfoWithNullValues() {
        CoreCommands.TxInfo txInfo = CoreCommands.TxInfo.builder()
                .type(null)
                .originalTx(null)
                .latestTx(null)
                .latestSendDate(null)
                .state(null)
                .retryCount(0)
                .revertReason(null)
                .build();

        assertNull(txInfo.getType());
        assertNull(txInfo.getOriginalTx());
        assertNull(txInfo.getLatestTx());
        assertNull(txInfo.getLatestSendDate());
        assertNull(txInfo.getState());
        assertEquals(0, txInfo.getRetryCount());
        assertNull(txInfo.getRevertReason());
    }

    /**
     * Test TxInfo with empty strings.
     */
    @Test
    public void testTxInfoWithEmptyStrings() {
        CoreCommands.TxInfo txInfo = CoreCommands.TxInfo.builder()
                .type("")
                .originalTx("")
                .latestTx("")
                .latestSendDate("")
                .state("")
                .retryCount(0)
                .revertReason("")
                .build();

        assertEquals("", txInfo.getType());
        assertEquals("", txInfo.getOriginalTx());
        assertEquals("", txInfo.getLatestTx());
        assertEquals("", txInfo.getLatestSendDate());
        assertEquals("", txInfo.getState());
        assertEquals(0, txInfo.getRetryCount());
        assertEquals("", txInfo.getRevertReason());
    }

    /**
     * Test TxInfo with maximum retry count.
     */
    @Test
    public void testTxInfoMaxRetryCount() {
        CoreCommands.TxInfo txInfo = CoreCommands.TxInfo.builder()
                .type("BATCH_COMMIT_TX")
                .originalTx("0x123")
                .latestTx("0x456")
                .latestSendDate("2026-03-26 12:00:00")
                .state("TX_FAILED")
                .retryCount(Integer.MAX_VALUE)
                .revertReason("max retries reached")
                .build();

        assertEquals(Integer.MAX_VALUE, txInfo.getRetryCount());
    }

    // ==================== DaInfo Tests ====================

    /**
     * Test DaInfo builder pattern.
     */
    @Test
    public void testDaInfoBuilder() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(1)
                .compressed(true)
                .compressionRatio(0.75)
                .txCount(100)
                .blobSize(131072)
                .validBlobBytesSize(98304)
                .build();

        assertEquals(1, daInfo.getDaVersion());
        assertTrue(daInfo.isCompressed());
        assertEquals(0.75, daInfo.getCompressionRatio(), 0.001);
        assertEquals(100, daInfo.getTxCount());
        assertEquals(131072, daInfo.getBlobSize());
        assertEquals(98304, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo all getters.
     */
    @Test
    public void testDaInfoGetters() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(2)
                .compressed(false)
                .compressionRatio(1.0)
                .txCount(50)
                .blobSize(65536)
                .validBlobBytesSize(65536)
                .build();

        assertEquals(2, daInfo.getDaVersion());
        assertFalse(daInfo.isCompressed());
        assertEquals(1.0, daInfo.getCompressionRatio(), 0.001);
        assertEquals(50, daInfo.getTxCount());
        assertEquals(65536, daInfo.getBlobSize());
        assertEquals(65536, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo no-args constructor.
     */
    @Test
    public void testDaInfoNoArgsConstructor() {
        CoreCommands.DaInfo daInfo = new CoreCommands.DaInfo();

        assertEquals(0, daInfo.getDaVersion());
        assertFalse(daInfo.isCompressed());
        assertEquals(0.0, daInfo.getCompressionRatio(), 0.001);
        assertEquals(0, daInfo.getTxCount());
        assertEquals(0, daInfo.getBlobSize());
        assertEquals(0, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo all-args constructor.
     */
    @Test
    public void testDaInfoAllArgsConstructor() {
        CoreCommands.DaInfo daInfo = new CoreCommands.DaInfo(
                3,
                true,
                0.85,
                200,
                262144,
                222822
        );

        assertEquals(3, daInfo.getDaVersion());
        assertTrue(daInfo.isCompressed());
        assertEquals(0.85, daInfo.getCompressionRatio(), 0.001);
        assertEquals(200, daInfo.getTxCount());
        assertEquals(262144, daInfo.getBlobSize());
        assertEquals(222822, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo with zero values.
     */
    @Test
    public void testDaInfoWithZeroValues() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(0)
                .compressed(false)
                .compressionRatio(0.0)
                .txCount(0)
                .blobSize(0)
                .validBlobBytesSize(0)
                .build();

        assertEquals(0, daInfo.getDaVersion());
        assertFalse(daInfo.isCompressed());
        assertEquals(0.0, daInfo.getCompressionRatio(), 0.001);
        assertEquals(0, daInfo.getTxCount());
        assertEquals(0, daInfo.getBlobSize());
        assertEquals(0, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo with maximum values.
     */
    @Test
    public void testDaInfoMaxValues() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(Integer.MAX_VALUE)
                .compressed(true)
                .compressionRatio(1.0)
                .txCount(Long.MAX_VALUE)
                .blobSize(Integer.MAX_VALUE)
                .validBlobBytesSize(Integer.MAX_VALUE)
                .build();

        assertEquals(Integer.MAX_VALUE, daInfo.getDaVersion());
        assertTrue(daInfo.isCompressed());
        assertEquals(1.0, daInfo.getCompressionRatio(), 0.001);
        assertEquals(Long.MAX_VALUE, daInfo.getTxCount());
        assertEquals(Integer.MAX_VALUE, daInfo.getBlobSize());
        assertEquals(Integer.MAX_VALUE, daInfo.getValidBlobBytesSize());
    }

    /**
     * Test DaInfo compression ratio precision.
     */
    @Test
    public void testDaInfoCompressionRatioPrecision() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(1)
                .compressed(true)
                .compressionRatio(0.123456789)
                .txCount(100)
                .blobSize(100000)
                .validBlobBytesSize(12345)
                .build();

        assertEquals(0.123456789, daInfo.getCompressionRatio(), 0.000000001);
    }

    /**
     * Test DaInfo with valid blob bytes size greater than blob size (edge case).
     */
    @Test
    public void testDaInfoValidBytesGreaterThanBlobSize() {
        CoreCommands.DaInfo daInfo = CoreCommands.DaInfo.builder()
                .daVersion(1)
                .compressed(false)
                .compressionRatio(1.0)
                .txCount(10)
                .blobSize(1000)
                .validBlobBytesSize(1000)
                .build();

        assertEquals(1000, daInfo.getBlobSize());
        assertEquals(1000, daInfo.getValidBlobBytesSize());
    }

    // ==================== needAdminServer Test ====================

    /**
     * Test needAdminServer returns true.
     * This method is inherited from BaseCommands and should always return true
     * for CoreCommands as it requires admin server connection.
     */
    @Test
    public void testNeedAdminServerReturnsTrue() {
        CoreCommands coreCommands = new CoreCommands();
        assertTrue("needAdminServer should return true for CoreCommands", coreCommands.needAdminServer());
    }
}
