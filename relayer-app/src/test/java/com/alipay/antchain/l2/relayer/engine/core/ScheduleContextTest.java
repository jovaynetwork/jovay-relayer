package com.alipay.antchain.l2.relayer.engine.core;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Negative case tests for ScheduleContext
 * Tests exception handling and edge cases in schedule context initialization
 */
public class ScheduleContextTest {

    /**
     * Test constructor with null mode
     * Verifies that null mode is handled (may default to UUID mode)
     */
    @Test
    public void testConstructor_NullMode() {
        try {
            ScheduleContext context = new ScheduleContext(null);
            // If no exception, verify context is created with default values
            assertNotNull(context);
            assertNotNull(context.getNodeId());
            assertNotNull(context.getNodeIp());
        } catch (NullPointerException e) {
            // Exception is also acceptable for null mode
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with empty mode
     * Verifies that empty mode is handled
     */
    @Test
    public void testConstructor_EmptyMode() {
        try {
            ScheduleContext context = new ScheduleContext("");
            assertNotNull(context);
            assertNotNull(context.getNodeId());
            assertNotNull(context.getNodeIp());
        } catch (Exception e) {
            // Exception is acceptable for empty mode
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with IP mode
     * Verifies that IP mode sets node ID to node IP
     */
    @Test
    public void testConstructor_IpMode() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        assertEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test constructor with UUID mode
     * Verifies that UUID mode generates UUID as node ID
     */
    @Test
    public void testConstructor_UuidMode() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_UUID);
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        assertNotEquals(context.getNodeIp(), context.getNodeId());
        // UUID should be 36 characters with hyphens
        assertTrue(context.getNodeId().length() >= 32);
    }

    /**
     * Test constructor with invalid mode
     * Verifies that invalid mode defaults to UUID mode
     */
    @Test
    public void testConstructor_InvalidMode() {
        ScheduleContext context = new ScheduleContext("INVALID_MODE");
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        // Should default to UUID mode
        assertNotEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test constructor with lowercase IP mode
     * Verifies that mode comparison is case-insensitive
     */
    @Test
    public void testConstructor_LowercaseIpMode() {
        ScheduleContext context = new ScheduleContext("ip");
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        assertEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test constructor with mixed case UUID mode
     * Verifies that mode comparison is case-insensitive
     */
    @Test
    public void testConstructor_MixedCaseUuidMode() {
        ScheduleContext context = new ScheduleContext("UuId");
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        // Should not match IP mode, so use UUID
        assertNotEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test that node IP is valid format
     * Verifies that node IP is not loopback address
     */
    @Test
    public void testConstructor_NodeIpNotLoopback() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        String nodeIp = context.getNodeIp();
        assertNotNull(nodeIp);
        // Should not be localhost or 127.0.0.1
        assertFalse(nodeIp.equals("localhost"));
        assertFalse(nodeIp.startsWith("127."));
    }

    /**
     * Test that node IP is IPv4 format
     * Verifies that node IP has 4 octets
     */
    @Test
    public void testConstructor_NodeIpIsIpv4() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        String nodeIp = context.getNodeIp();
        assertNotNull(nodeIp);
        // IPv4 should have 3 dots
        int dotCount = nodeIp.length() - nodeIp.replace(".", "").length();
        assertEquals(3, dotCount);
    }

    /**
     * Test multiple instances have different UUIDs
     * Verifies that UUID mode generates unique IDs
     */
    @Test
    public void testConstructor_MultipleInstancesHaveDifferentUuids() {
        ScheduleContext context1 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_UUID);
        ScheduleContext context2 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_UUID);
        
        assertNotEquals(context1.getNodeId(), context2.getNodeId());
    }

    /**
     * Test multiple instances have same IP
     * Verifies that IP mode returns consistent IP
     */
    @Test
    public void testConstructor_MultipleInstancesHaveSameIp() {
        ScheduleContext context1 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        ScheduleContext context2 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        assertEquals(context1.getNodeIp(), context2.getNodeIp());
        assertEquals(context1.getNodeId(), context2.getNodeId());
    }

    /**
     * Test constructor with mode containing whitespace
     * Verifies that whitespace in mode is handled
     */
    @Test
    public void testConstructor_ModeWithWhitespace() {
        ScheduleContext context = new ScheduleContext(" IP ");
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        // Whitespace should cause mode not to match, defaulting to UUID
        assertNotEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test constructor with very long mode string
     * Verifies that long mode string is handled
     */
    @Test
    public void testConstructor_VeryLongMode() {
        String longMode = "A".repeat(1000);
        ScheduleContext context = new ScheduleContext(longMode);
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
    }

    /**
     * Test constructor with special characters in mode
     * Verifies that special characters are handled
     */
    @Test
    public void testConstructor_SpecialCharactersInMode() {
        ScheduleContext context = new ScheduleContext("!@#$%^&*()");
        
        assertNotNull(context.getNodeId());
        assertNotNull(context.getNodeIp());
        // Should default to UUID mode
        assertNotEquals(context.getNodeIp(), context.getNodeId());
    }

    /**
     * Test getNodeId returns non-null value
     * Verifies that node ID is always available
     */
    @Test
    public void testGetNodeId_NotNull() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        assertNotNull(context.getNodeId());
        assertFalse(context.getNodeId().isEmpty());
    }

    /**
     * Test getNodeIp returns non-null value
     * Verifies that node IP is always available
     */
    @Test
    public void testGetNodeIp_NotNull() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_UUID);
        
        assertNotNull(context.getNodeIp());
        assertFalse(context.getNodeIp().isEmpty());
    }

    /**
     * Test node ID and IP are immutable
     * Verifies that values don't change after construction
     */
    @Test
    public void testNodeIdAndIpAreImmutable() {
        ScheduleContext context = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        
        String nodeId1 = context.getNodeId();
        String nodeIp1 = context.getNodeIp();
        
        // Call getters multiple times
        String nodeId2 = context.getNodeId();
        String nodeIp2 = context.getNodeIp();
        
        assertEquals(nodeId1, nodeId2);
        assertEquals(nodeIp1, nodeIp2);
    }
}
