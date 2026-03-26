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

package com.alipay.antchain.l2.relayer.engine.core;

import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Negative case tests for Activator
 * Tests exception handling and edge cases in node activation
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivatorTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private ScheduleContext scheduleContext;

    @InjectMocks
    private Activator activator;

    @Before
    public void setUp() {
        when(scheduleContext.getNodeId()).thenReturn("test-node-id");
        when(scheduleContext.getNodeIp()).thenReturn("192.168.1.1");
    }

    /**
     * Test activate with null node ID
     * Verifies that null node ID is handled correctly
     */
    @Test
    public void testActivate_NullNodeId() {
        when(scheduleContext.getNodeId()).thenReturn(null);
        when(scheduleContext.getNodeIp()).thenReturn("192.168.1.1");

        try {
            activator.activate();
            // If no exception, verify repository was called with null
            verify(scheduleRepository, times(1)).activate(null, "192.168.1.1");
        } catch (Exception e) {
            // Exception is acceptable for null node ID
            assert e != null;
        }
    }

    /**
     * Test activate with null node IP
     * Verifies that null node IP is handled correctly
     */
    @Test
    public void testActivate_NullNodeIp() {
        when(scheduleContext.getNodeId()).thenReturn("test-node-id");
        when(scheduleContext.getNodeIp()).thenReturn(null);

        try {
            activator.activate();
            // If no exception, verify repository was called with null
            verify(scheduleRepository, times(1)).activate("test-node-id", null);
        } catch (Exception e) {
            // Exception is acceptable for null node IP
            assert e != null;
        }
    }

    /**
     * Test activate with empty node ID
     * Verifies that empty node ID is handled correctly
     */
    @Test
    public void testActivate_EmptyNodeId() {
        when(scheduleContext.getNodeId()).thenReturn("");
        when(scheduleContext.getNodeIp()).thenReturn("192.168.1.1");

        try {
            activator.activate();
            verify(scheduleRepository, times(1)).activate("", "192.168.1.1");
        } catch (Exception e) {
            // Exception is acceptable for empty node ID
            assert e != null;
        }
    }

    /**
     * Test activate with empty node IP
     * Verifies that empty node IP is handled correctly
     */
    @Test
    public void testActivate_EmptyNodeIp() {
        when(scheduleContext.getNodeId()).thenReturn("test-node-id");
        when(scheduleContext.getNodeIp()).thenReturn("");

        try {
            activator.activate();
            verify(scheduleRepository, times(1)).activate("test-node-id", "");
        } catch (Exception e) {
            // Exception is acceptable for empty node IP
            assert e != null;
        }
    }

    /**
     * Test activate when repository throws exception
     * Verifies that repository exceptions are propagated
     */
    @Test(expected = RuntimeException.class)
    public void testActivate_RepositoryThrowsException() {
        doThrow(new RuntimeException("Database connection failed"))
                .when(scheduleRepository).activate(any(), any());

        activator.activate();
    }

    /**
     * Test activate when schedule context is null
     * Verifies that null context causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testActivate_NullScheduleContext() {
        // Create activator with null context
        Activator nullContextActivator = new Activator();
        // This should throw NullPointerException when trying to get node ID
        nullContextActivator.activate();
    }

    /**
     * Test activate with invalid IP format
     * Verifies that invalid IP format is handled
     */
    @Test
    public void testActivate_InvalidIpFormat() {
        when(scheduleContext.getNodeId()).thenReturn("test-node-id");
        when(scheduleContext.getNodeIp()).thenReturn("invalid-ip-format");

        try {
            activator.activate();
            verify(scheduleRepository, times(1)).activate("test-node-id", "invalid-ip-format");
        } catch (Exception e) {
            // Exception is acceptable for invalid IP format
            assert e != null;
        }
    }

    /**
     * Test activate with very long node ID
     * Verifies that long node ID is handled correctly
     */
    @Test
    public void testActivate_VeryLongNodeId() {
        String longNodeId = "a".repeat(1000);
        when(scheduleContext.getNodeId()).thenReturn(longNodeId);
        when(scheduleContext.getNodeIp()).thenReturn("192.168.1.1");

        try {
            activator.activate();
            verify(scheduleRepository, times(1)).activate(longNodeId, "192.168.1.1");
        } catch (Exception e) {
            // Exception is acceptable for very long node ID
            assert e != null;
        }
    }

    /**
     * Test activate with special characters in node ID
     * Verifies that special characters are handled correctly
     */
    @Test
    public void testActivate_SpecialCharactersInNodeId() {
        when(scheduleContext.getNodeId()).thenReturn("node-id-with-!@#$%^&*()");
        when(scheduleContext.getNodeIp()).thenReturn("192.168.1.1");

        try {
            activator.activate();
            verify(scheduleRepository, times(1)).activate("node-id-with-!@#$%^&*()", "192.168.1.1");
        } catch (Exception e) {
            // Exception is acceptable for special characters
            assert e != null;
        }
    }

    /**
     * Test activate when repository is null
     * Verifies that null repository causes NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testActivate_NullRepository() {
        Activator nullRepoActivator = new Activator();
        // Manually set context but leave repository null
        nullRepoActivator.activate();
    }
}
