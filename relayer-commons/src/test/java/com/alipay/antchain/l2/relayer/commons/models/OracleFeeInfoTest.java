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

package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OracleFeeInfo
 */
public class OracleFeeInfoTest {

    private OracleFeeInfo oracleFeeInfo;

    @Before
    public void setUp() {
        oracleFeeInfo = new OracleFeeInfo();
    }

    // ==================== Positive Tests ====================

    /**
     * Test default constructor
     * Should create instance with default values
     */
    @Test
    public void testDefaultConstructor() {
        OracleFeeInfo info = new OracleFeeInfo();

        assertNotNull(info);
        assertEquals(BigInteger.valueOf(8), info.getBaseFeeChangeDenominator());
        assertEquals(BigInteger.ONE, info.getStartBatchIndex());
        assertEquals(BigInteger.valueOf(1000000000), info.getLastL1BaseFee());
        assertEquals(BigInteger.valueOf(1000000000), info.getLastL1BlobBaseFee());
    }

    /**
     * Test builder pattern
     * Should create instance with specified values
     */
    @Test
    public void testBuilder() {
        OracleFeeInfo info = OracleFeeInfo.builder()
                .startBatchIndex(BigInteger.TEN)
                .lastBatchDaFee(BigInteger.valueOf(1000))
                .lastBatchExecFee(BigInteger.valueOf(2000))
                .lastBatchByteLength(BigInteger.valueOf(500))
                .lastL1BaseFee(BigInteger.valueOf(3000000000L))
                .lastL1BlobBaseFee(BigInteger.valueOf(2000000000L))
                .blobBaseFeeScala(BigInteger.valueOf(100))
                .baseFeeScala(BigInteger.valueOf(200))
                .l1FixedProfit(BigInteger.valueOf(300))
                .totalScala(BigInteger.valueOf(400))
                .build();

        assertNotNull(info);
        assertEquals(BigInteger.TEN, info.getStartBatchIndex());
        assertEquals(BigInteger.valueOf(1000), info.getLastBatchDaFee());
        assertEquals(BigInteger.valueOf(2000), info.getLastBatchExecFee());
        assertEquals(BigInteger.valueOf(500), info.getLastBatchByteLength());
        assertEquals(BigInteger.valueOf(3000000000L), info.getLastL1BaseFee());
        assertEquals(BigInteger.valueOf(2000000000L), info.getLastL1BlobBaseFee());
        assertEquals(BigInteger.valueOf(100), info.getBlobBaseFeeScala());
        assertEquals(BigInteger.valueOf(200), info.getBaseFeeScala());
        assertEquals(BigInteger.valueOf(300), info.getL1FixedProfit());
        assertEquals(BigInteger.valueOf(400), info.getTotalScala());
    }

    /**
     * Test all args constructor
     * Should create instance with all specified values
     */
    @Test
    public void testAllArgsConstructor() {
        OracleFeeInfo info = new OracleFeeInfo(
                BigInteger.valueOf(5),
                BigInteger.valueOf(100),
                BigInteger.valueOf(200),
                BigInteger.valueOf(300),
                BigInteger.valueOf(1500000000L),
                BigInteger.valueOf(1200000000L),
                BigInteger.valueOf(10),
                BigInteger.valueOf(20),
                BigInteger.valueOf(30),
                BigInteger.valueOf(40)
        );

        assertNotNull(info);
        assertEquals(BigInteger.valueOf(5), info.getStartBatchIndex());
        assertEquals(BigInteger.valueOf(100), info.getLastBatchDaFee());
        assertEquals(BigInteger.valueOf(200), info.getLastBatchExecFee());
        assertEquals(BigInteger.valueOf(300), info.getLastBatchByteLength());
        assertEquals(BigInteger.valueOf(1500000000L), info.getLastL1BaseFee());
        assertEquals(BigInteger.valueOf(1200000000L), info.getLastL1BlobBaseFee());
    }

    /**
     * Test getter and setter for startBatchIndex
     */
    @Test
    public void testStartBatchIndexGetterSetter() {
        BigInteger value = BigInteger.valueOf(100);
        oracleFeeInfo.setStartBatchIndex(value);

        assertEquals(value, oracleFeeInfo.getStartBatchIndex());
    }

    /**
     * Test getter and setter for lastBatchDaFee
     */
    @Test
    public void testLastBatchDaFeeGetterSetter() {
        BigInteger value = BigInteger.valueOf(5000);
        oracleFeeInfo.setLastBatchDaFee(value);

        assertEquals(value, oracleFeeInfo.getLastBatchDaFee());
    }

    /**
     * Test getter and setter for lastBatchExecFee
     */
    @Test
    public void testLastBatchExecFeeGetterSetter() {
        BigInteger value = BigInteger.valueOf(8000);
        oracleFeeInfo.setLastBatchExecFee(value);

        assertEquals(value, oracleFeeInfo.getLastBatchExecFee());
    }

    /**
     * Test getter and setter for lastBatchByteLength
     */
    @Test
    public void testLastBatchByteLengthGetterSetter() {
        BigInteger value = BigInteger.valueOf(1024);
        oracleFeeInfo.setLastBatchByteLength(value);

        assertEquals(value, oracleFeeInfo.getLastBatchByteLength());
    }

    /**
     * Test getter and setter for lastL1BaseFee
     */
    @Test
    public void testLastL1BaseFeeGetterSetter() {
        BigInteger value = BigInteger.valueOf(2000000000L);
        oracleFeeInfo.setLastL1BaseFee(value);

        assertEquals(value, oracleFeeInfo.getLastL1BaseFee());
    }

    /**
     * Test getter and setter for lastL1BlobBaseFee
     */
    @Test
    public void testLastL1BlobBaseFeeGetterSetter() {
        BigInteger value = BigInteger.valueOf(1500000000L);
        oracleFeeInfo.setLastL1BlobBaseFee(value);

        assertEquals(value, oracleFeeInfo.getLastL1BlobBaseFee());
    }

    /**
     * Test getter and setter for blobBaseFeeScala
     */
    @Test
    public void testBlobBaseFeeScalaGetterSetter() {
        BigInteger value = BigInteger.valueOf(150);
        oracleFeeInfo.setBlobBaseFeeScala(value);

        assertEquals(value, oracleFeeInfo.getBlobBaseFeeScala());
    }

    /**
     * Test getter and setter for baseFeeScala
     */
    @Test
    public void testBaseFeeScalaGetterSetter() {
        BigInteger value = BigInteger.valueOf(250);
        oracleFeeInfo.setBaseFeeScala(value);

        assertEquals(value, oracleFeeInfo.getBaseFeeScala());
    }

    /**
     * Test getter and setter for l1FixedProfit
     */
    @Test
    public void testL1FixedProfitGetterSetter() {
        BigInteger value = BigInteger.valueOf(500);
        oracleFeeInfo.setL1FixedProfit(value);

        assertEquals(value, oracleFeeInfo.getL1FixedProfit());
    }

    /**
     * Test getter and setter for totalScala
     */
    @Test
    public void testTotalScalaGetterSetter() {
        BigInteger value = BigInteger.valueOf(1000);
        oracleFeeInfo.setTotalScala(value);

        assertEquals(value, oracleFeeInfo.getTotalScala());
    }

    /**
     * Test baseFeeChangeDenominator is final and has correct value
     */
    @Test
    public void testBaseFeeChangeDenominator() {
        assertEquals(BigInteger.valueOf(8), oracleFeeInfo.getBaseFeeChangeDenominator());
        
        // Create new instance to verify it's consistent
        OracleFeeInfo newInfo = new OracleFeeInfo();
        assertEquals(BigInteger.valueOf(8), newInfo.getBaseFeeChangeDenominator());
    }

    /**
     * Test setting all fields to zero
     */
    @Test
    public void testSetAllFieldsToZero() {
        oracleFeeInfo.setStartBatchIndex(BigInteger.ZERO);
        oracleFeeInfo.setLastBatchDaFee(BigInteger.ZERO);
        oracleFeeInfo.setLastBatchExecFee(BigInteger.ZERO);
        oracleFeeInfo.setLastBatchByteLength(BigInteger.ZERO);
        oracleFeeInfo.setLastL1BaseFee(BigInteger.ZERO);
        oracleFeeInfo.setLastL1BlobBaseFee(BigInteger.ZERO);
        oracleFeeInfo.setBlobBaseFeeScala(BigInteger.ZERO);
        oracleFeeInfo.setBaseFeeScala(BigInteger.ZERO);
        oracleFeeInfo.setL1FixedProfit(BigInteger.ZERO);
        oracleFeeInfo.setTotalScala(BigInteger.ZERO);

        assertEquals(BigInteger.ZERO, oracleFeeInfo.getStartBatchIndex());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getLastBatchDaFee());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getLastBatchExecFee());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getLastBatchByteLength());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getLastL1BaseFee());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getLastL1BlobBaseFee());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getBlobBaseFeeScala());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getBaseFeeScala());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getL1FixedProfit());
        assertEquals(BigInteger.ZERO, oracleFeeInfo.getTotalScala());
    }

    /**
     * Test setting all fields to large values
     */
    @Test
    public void testSetAllFieldsToLargeValues() {
        BigInteger largeValue = new BigInteger("999999999999999999999999999");
        
        oracleFeeInfo.setStartBatchIndex(largeValue);
        oracleFeeInfo.setLastBatchDaFee(largeValue);
        oracleFeeInfo.setLastBatchExecFee(largeValue);
        oracleFeeInfo.setLastBatchByteLength(largeValue);
        oracleFeeInfo.setLastL1BaseFee(largeValue);
        oracleFeeInfo.setLastL1BlobBaseFee(largeValue);
        oracleFeeInfo.setBlobBaseFeeScala(largeValue);
        oracleFeeInfo.setBaseFeeScala(largeValue);
        oracleFeeInfo.setL1FixedProfit(largeValue);
        oracleFeeInfo.setTotalScala(largeValue);

        assertEquals(largeValue, oracleFeeInfo.getStartBatchIndex());
        assertEquals(largeValue, oracleFeeInfo.getLastBatchDaFee());
        assertEquals(largeValue, oracleFeeInfo.getLastBatchExecFee());
        assertEquals(largeValue, oracleFeeInfo.getLastBatchByteLength());
        assertEquals(largeValue, oracleFeeInfo.getLastL1BaseFee());
        assertEquals(largeValue, oracleFeeInfo.getLastL1BlobBaseFee());
        assertEquals(largeValue, oracleFeeInfo.getBlobBaseFeeScala());
        assertEquals(largeValue, oracleFeeInfo.getBaseFeeScala());
        assertEquals(largeValue, oracleFeeInfo.getL1FixedProfit());
        assertEquals(largeValue, oracleFeeInfo.getTotalScala());
    }

    /**
     * Test builder with partial fields
     */
    @Test
    public void testBuilderWithPartialFields() {
        OracleFeeInfo info = OracleFeeInfo.builder()
                .startBatchIndex(BigInteger.valueOf(50))
                .lastBatchDaFee(BigInteger.valueOf(3000))
                .build();

        assertNotNull(info);
        assertEquals(BigInteger.valueOf(50), info.getStartBatchIndex());
        assertEquals(BigInteger.valueOf(3000), info.getLastBatchDaFee());
        // Other fields should be null or default
        assertNull(info.getLastBatchExecFee());
        assertNull(info.getBlobBaseFeeScala());
    }

    // ==================== Negative Tests ====================

    /**
     * Test setting null values
     * Should accept null values for nullable fields
     */
    @Test
    public void testSetNullValues() {
        oracleFeeInfo.setStartBatchIndex(null);
        oracleFeeInfo.setLastBatchDaFee(null);
        oracleFeeInfo.setLastBatchExecFee(null);
        oracleFeeInfo.setLastBatchByteLength(null);
        oracleFeeInfo.setLastL1BaseFee(null);
        oracleFeeInfo.setLastL1BlobBaseFee(null);
        oracleFeeInfo.setBlobBaseFeeScala(null);
        oracleFeeInfo.setBaseFeeScala(null);
        oracleFeeInfo.setL1FixedProfit(null);
        oracleFeeInfo.setTotalScala(null);

        assertNull(oracleFeeInfo.getStartBatchIndex());
        assertNull(oracleFeeInfo.getLastBatchDaFee());
        assertNull(oracleFeeInfo.getLastBatchExecFee());
        assertNull(oracleFeeInfo.getLastBatchByteLength());
        assertNull(oracleFeeInfo.getLastL1BaseFee());
        assertNull(oracleFeeInfo.getLastL1BlobBaseFee());
        assertNull(oracleFeeInfo.getBlobBaseFeeScala());
        assertNull(oracleFeeInfo.getBaseFeeScala());
        assertNull(oracleFeeInfo.getL1FixedProfit());
        assertNull(oracleFeeInfo.getTotalScala());
    }

    /**
     * Test setting negative values
     * Should accept negative BigInteger values
     */
    @Test
    public void testSetNegativeValues() {
        BigInteger negativeValue = BigInteger.valueOf(-1000);
        
        oracleFeeInfo.setStartBatchIndex(negativeValue);
        oracleFeeInfo.setLastBatchDaFee(negativeValue);
        oracleFeeInfo.setLastBatchExecFee(negativeValue);

        assertEquals(negativeValue, oracleFeeInfo.getStartBatchIndex());
        assertEquals(negativeValue, oracleFeeInfo.getLastBatchDaFee());
        assertEquals(negativeValue, oracleFeeInfo.getLastBatchExecFee());
    }

    /**
     * Test builder with all null values
     */
    @Test
    public void testBuilderWithAllNullValues() {
        OracleFeeInfo info = OracleFeeInfo.builder()
                .startBatchIndex(null)
                .lastBatchDaFee(null)
                .lastBatchExecFee(null)
                .lastBatchByteLength(null)
                .lastL1BaseFee(null)
                .lastL1BlobBaseFee(null)
                .blobBaseFeeScala(null)
                .baseFeeScala(null)
                .l1FixedProfit(null)
                .totalScala(null)
                .build();

        assertNotNull(info);
        assertNull(info.getStartBatchIndex());
        assertNull(info.getLastBatchDaFee());
        assertNull(info.getLastBatchExecFee());
    }

    /**
     * Test multiple updates to same field
     */
    @Test
    public void testMultipleUpdatesToSameField() {
        oracleFeeInfo.setStartBatchIndex(BigInteger.ONE);
        assertEquals(BigInteger.ONE, oracleFeeInfo.getStartBatchIndex());

        oracleFeeInfo.setStartBatchIndex(BigInteger.TEN);
        assertEquals(BigInteger.TEN, oracleFeeInfo.getStartBatchIndex());

        oracleFeeInfo.setStartBatchIndex(BigInteger.valueOf(100));
        assertEquals(BigInteger.valueOf(100), oracleFeeInfo.getStartBatchIndex());
    }

    /**
     * Test default values after construction
     */
    @Test
    public void testDefaultValuesAfterConstruction() {
        OracleFeeInfo info = new OracleFeeInfo();

        // Check default initialized values
        assertEquals(BigInteger.ONE, info.getStartBatchIndex());
        assertEquals(BigInteger.valueOf(1000000000), info.getLastL1BaseFee());
        assertEquals(BigInteger.valueOf(1000000000), info.getLastL1BlobBaseFee());
        
        // Check null values for non-initialized fields
        assertNull(info.getLastBatchDaFee());
        assertNull(info.getLastBatchExecFee());
        assertNull(info.getLastBatchByteLength());
        assertNull(info.getBlobBaseFeeScala());
        assertNull(info.getBaseFeeScala());
        assertNull(info.getL1FixedProfit());
        assertNull(info.getTotalScala());
    }
}
