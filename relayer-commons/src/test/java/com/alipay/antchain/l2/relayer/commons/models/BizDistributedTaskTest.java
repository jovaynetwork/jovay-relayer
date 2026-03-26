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

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BizDistributedTask class
 */
public class BizDistributedTaskTest {

    @Test
    public void testNoArgConstructor() {
        BizDistributedTask task = new BizDistributedTask();
        assertEquals("", task.getNodeId());
        assertNull(task.getTaskType());
        assertEquals("", task.getExt());
        assertEquals(0, task.getStartTime());
        assertEquals(0, task.getTimeSliceLength());
    }

    @Test
    public void testSingleParameterConstructor() {
        BizDistributedTask task = new BizDistributedTask(BizTaskTypeEnum.BATCH_COMMIT_TASK);
        assertEquals(BizTaskTypeEnum.BATCH_COMMIT_TASK, task.getTaskType());
        assertEquals("", task.getNodeId());
        assertEquals("", task.getExt());
        assertEquals(0, task.getStartTime());
        assertEquals(0, task.getTimeSliceLength());
    }

    @Test
    public void testFourParameterConstructor() {
        String nodeId = "node123";
        BizTaskTypeEnum taskType = BizTaskTypeEnum.BATCH_COMMIT_TASK;
        String ext = "extension data";
        long startTime = 123456789L;

        BizDistributedTask task = new BizDistributedTask(nodeId, taskType, ext, startTime);

        assertEquals(nodeId, task.getNodeId());
        assertEquals(taskType, task.getTaskType());
        assertEquals(ext, task.getExt());
        assertEquals(startTime, task.getStartTime());
        assertEquals(0, task.getTimeSliceLength());
    }

    @Test
    public void testSettersAndGetters() {
        BizDistributedTask task = new BizDistributedTask();

        task.setNodeId("node456");
        assertEquals("node456", task.getNodeId());

        task.setTaskType(BizTaskTypeEnum.BATCH_PROVE_TASK);
        assertEquals(BizTaskTypeEnum.BATCH_PROVE_TASK, task.getTaskType());

        task.setExt("new extension");
        assertEquals("new extension", task.getExt());

        task.setStartTime(987654321L);
        assertEquals(987654321L, task.getStartTime());

        task.setTimeSliceLength(5000L);
        assertEquals(5000L, task.getTimeSliceLength());
    }

    @Test
    public void testIfFinishReturnsTrueWhenTimeSlicePassed() {
        BizDistributedTask task = new BizDistributedTask();
        task.setStartTime(System.currentTimeMillis() - 2000);
        task.setTimeSliceLength(1000);

        assertTrue(task.ifFinish());
    }

    @Test
    public void testIfFinishReturnsFalseWhenTimeSliceNotPassed() {
        BizDistributedTask task = new BizDistributedTask();
        task.setStartTime(System.currentTimeMillis());
        task.setTimeSliceLength(10000);

        assertFalse(task.ifFinish());
    }

    @Test
    public void testGetEndTimestamp() {
        BizDistributedTask task = new BizDistributedTask();
        task.setStartTime(1000L);
        task.setTimeSliceLength(5000L);

        assertEquals(6000L, task.getEndTimestamp());
    }

    @Test
    public void testGetEndTimestampWithZeroValues() {
        BizDistributedTask task = new BizDistributedTask();
        task.setStartTime(0L);
        task.setTimeSliceLength(0L);

        assertEquals(0L, task.getEndTimestamp());
    }
}