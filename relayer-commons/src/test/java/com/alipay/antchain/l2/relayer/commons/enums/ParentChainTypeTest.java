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

package com.alipay.antchain.l2.relayer.commons.enums;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ParentChainType enum
 */
public class ParentChainTypeTest {

    @Test
    public void testJovaySupportedDaTypes() {
        assertEquals(1, ParentChainType.JOVAY.supportedDaTypes().size());
        assertEquals(DaType.DAS, ParentChainType.JOVAY.supportedDaTypes().get(0));
    }

    @Test
    public void testEthereumSupportedDaTypes() {
        assertEquals(1, ParentChainType.ETHEREUM.supportedDaTypes().size());
        assertEquals(DaType.BLOBS, ParentChainType.ETHEREUM.supportedDaTypes().get(0));
    }

    @Test
    public void testJovayNeedRollupFeeFeed() {
        assertFalse(ParentChainType.JOVAY.needRollupFeeFeed());
    }

    @Test
    public void testEthereumNeedRollupFeeFeed() {
        assertTrue(ParentChainType.ETHEREUM.needRollupFeeFeed());
    }

    @Test
    public void testValues() {
        ParentChainType[] values = ParentChainType.values();
        assertEquals(2, values.length);
        assertEquals(ParentChainType.JOVAY, values[0]);
        assertEquals(ParentChainType.ETHEREUM, values[1]);
    }

    @Test
    public void testValueOf() {
        assertEquals(ParentChainType.JOVAY, ParentChainType.valueOf("JOVAY"));
        assertEquals(ParentChainType.ETHEREUM, ParentChainType.valueOf("ETHEREUM"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalid() {
        ParentChainType.valueOf("INVALID");
    }
}
