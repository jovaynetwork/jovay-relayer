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
 * Unit tests for DasType enum
 */
public class DasTypeTest {

    @Test
    public void testValues() {
        DasType[] values = DasType.values();
        assertEquals(1, values.length);
        assertEquals(DasType.LOCAL, values[0]);
    }

    @Test
    public void testValueOfLocal() {
        assertEquals(DasType.LOCAL, DasType.valueOf("LOCAL"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalid() {
        DasType.valueOf("INVALID");
    }
}
