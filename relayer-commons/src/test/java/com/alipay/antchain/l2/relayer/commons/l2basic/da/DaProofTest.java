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

package com.alipay.antchain.l2.relayer.commons.l2basic.da;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for DaProof record
 */
public class DaProofTest {

    @Test
    public void testConstructorWithProof() {
        byte[] proofData = new byte[]{1, 2, 3, 4, 5};
        DaProof daProof = new DaProof(proofData);

        assertArrayEquals(proofData, daProof.proof());
    }

    @Test
    public void testConstructorWithEmptyArray() {
        byte[] emptyProof = new byte[0];
        DaProof daProof = new DaProof(emptyProof);

        assertArrayEquals(emptyProof, daProof.proof());
        assertEquals(0, daProof.proof().length);
    }

    @Test
    public void testConstructorWithNull() {
        DaProof daProof = new DaProof(null);

        assertNull(daProof.proof());
    }

    @Test
    public void testEquals() {
        byte[] proofData = new byte[]{1, 2, 3};

        DaProof daProof1 = new DaProof(proofData);
        DaProof daProof2 = new DaProof(proofData);

        // record equals for byte[] uses reference equality
        assertEquals(daProof1, daProof2);

        // different references are not equal even with same content
        DaProof daProof3 = new DaProof(new byte[]{1, 2, 3});
        assertNotEquals(daProof1, daProof3);
    }

    @Test
    public void testEqualsWithNull() {
        byte[] proofData = new byte[]{1, 2, 3};
        DaProof daProof = new DaProof(proofData);

        assertNotEquals(null, daProof);
    }

    @Test
    public void testHashCode() {
        byte[] proofData = new byte[]{1, 2, 3};

        DaProof daProof1 = new DaProof(proofData);
        DaProof daProof2 = new DaProof(proofData);

        // same reference produces same hashCode
        assertEquals(daProof1.hashCode(), daProof2.hashCode());
    }

    @Test
    public void testToString() {
        byte[] proofData = new byte[]{1, 2, 3};
        DaProof daProof = new DaProof(proofData);

        String toString = daProof.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("DaProof"));
    }

    @Test
    public void testToStringWithNull() {
        DaProof daProof = new DaProof(null);

        String toString = daProof.toString();
        assertNotNull(toString);
    }
}
