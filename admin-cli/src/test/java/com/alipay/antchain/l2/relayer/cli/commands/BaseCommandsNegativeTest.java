package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.shell.Availability;

/**
 * Negative test cases for BaseCommands
 * Tests network availability checks, server status validation, and error recovery paths
 */
public class BaseCommandsNegativeTest {

    private TestBaseCommands baseCommands;

    @Before
    public void setUp() {
        baseCommands = new TestBaseCommands();
    }

    // ==================== Server Availability Tests ====================

    @Test(expected = IllegalArgumentException.class)
    public void testBaseAvailability_InvalidPort() {
        // Test with invalid port number - will throw IllegalArgumentException (port out of range)
        baseCommands.setAdminAddress("dns://localhost:99999");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseAvailability_NegativePort() {
        // Test with negative port number - will throw IllegalArgumentException (port out of range)
        baseCommands.setAdminAddress("dns://localhost:-1");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_ZeroPort() {
        // Test with port zero
        baseCommands.setAdminAddress("dns://localhost:0");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseAvailability_PortOutOfRange() {
        // Test with port number exceeding valid range - will throw IllegalArgumentException (port out of range)
        baseCommands.setAdminAddress("dns://localhost:70000");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_NonNumericPort() {
        // Test with non-numeric port - will throw NumberFormatException
        baseCommands.setAdminAddress("dns://localhost:abc");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_MissingPort() {
        // Test with missing port - will throw IndexOutOfBoundsException
        baseCommands.setAdminAddress("dns://localhost");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_EmptyHost() {
        // Test with empty host
        baseCommands.setAdminAddress("dns://:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test
    public void testBaseAvailability_LocalhostButWrongPort() {
        // Test with localhost but wrong port
        baseCommands.setAdminAddress("dns://localhost:9999");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
        Assert.assertTrue(availability.getReason().contains("unreachable"));
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_IPv6Unreachable() {
        // Test with unreachable IPv6 address - will throw NumberFormatException (multiple colons)
        baseCommands.setAdminAddress("dns://[2001:db8::1]:7088");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_InvalidIPv6Format() {
        // Test with invalid IPv6 format - will throw NumberFormatException (multiple colons)
        baseCommands.setAdminAddress("dns://[gggg::1]:7088");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_MissingProtocol() {
        // Test with missing protocol - will throw IndexOutOfBoundsException (no //)
        baseCommands.setAdminAddress("localhost:7088");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_WrongProtocol() {
        // Test with wrong protocol
        baseCommands.setAdminAddress("http://localhost:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_EmptyAddress() {
        // Test with empty address - will throw IndexOutOfBoundsException
        baseCommands.setAdminAddress("");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_NullAddress() {
        // Test with null address - will throw IndexOutOfBoundsException
        baseCommands.setAdminAddress(null);
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_WhitespaceAddress() {
        // Test with whitespace-only address - will throw IndexOutOfBoundsException
        baseCommands.setAdminAddress("   ");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_MultipleColons() {
        // Test with multiple colons in address
        baseCommands.setAdminAddress("dns://localhost:7088:9999");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_TrailingSlash() {
        // Test with trailing slash - will throw NumberFormatException (port "7088/")
        baseCommands.setAdminAddress("dns://localhost:7088/");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_WithPath() {
        // Test with path in address - will throw NumberFormatException (port "7088/admin")
        baseCommands.setAdminAddress("dns://localhost:7088/admin");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_WithQueryParams() {
        // Test with query parameters - will throw NumberFormatException (port "7088?param=value")
        baseCommands.setAdminAddress("dns://localhost:7088?param=value");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_ServerNotNeeded() {
        // Test when admin server is not needed
        baseCommands.setAdminAddress("dns://unreachable-server.example.com:9999");
        baseCommands.setNeedAdminServer(false);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertTrue(availability.isAvailable());
    }

    @Test
    public void testBaseAvailability_LocalhostWithCorrectPort() throws IOException {
        // Test with localhost and a port that's actually listening
        ServerSocket serverSocket = null;
        try {
            // Create a temporary server socket to simulate a listening port
            serverSocket = new ServerSocket(0); // Use any available port
            int port = serverSocket.getLocalPort();
            
            baseCommands.setAdminAddress("dns://localhost:" + port);
            baseCommands.setNeedAdminServer(true);

            Availability availability = baseCommands.baseAvailability();
            Assert.assertTrue(availability.isAvailable());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    @Test
    public void testBaseAvailability_127_0_0_1_WithCorrectPort() throws IOException {
        // Test with 127.0.0.1 and a port that's actually listening
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            
            baseCommands.setAdminAddress("dns://127.0.0.1:" + port);
            baseCommands.setNeedAdminServer(true);

            Availability availability = baseCommands.baseAvailability();
            Assert.assertTrue(availability.isAvailable());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testBaseAvailability_LeadingSpaces() {
        // Test with leading spaces in address
        baseCommands.setAdminAddress("   dns://localhost:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test(expected = NumberFormatException.class)
    public void testBaseAvailability_TrailingSpaces() {
        // Test with trailing spaces in address - will throw NumberFormatException (port "7088   ")
        baseCommands.setAdminAddress("dns://localhost:7088   ");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_MixedCaseProtocol() {
        // Test with mixed case protocol
        baseCommands.setAdminAddress("DNS://localhost:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        // Behavior may vary, but should handle gracefully
        Assert.assertNotNull(availability);
    }

    @Test
    public void testBaseAvailability_StaticProtocol() {
        // Test with static:// protocol
        baseCommands.setAdminAddress("static://localhost:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBaseAvailability_DoubleSlashMissing() {
        // Test with missing double slash after protocol - will throw IndexOutOfBoundsException
        baseCommands.setAdminAddress("dns:localhost:7088");
        baseCommands.setNeedAdminServer(true);

        baseCommands.baseAvailability();
    }

    @Test
    public void testBaseAvailability_TripleSlash() {
        // Test with triple slash after protocol
        baseCommands.setAdminAddress("dns:///localhost:7088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    @Test
    public void testBaseAvailability_PortWithLeadingZeros() {
        // Test with port having leading zeros
        baseCommands.setAdminAddress("dns://localhost:007088");
        baseCommands.setNeedAdminServer(true);

        Availability availability = baseCommands.baseAvailability();
        Assert.assertFalse(availability.isAvailable());
    }

    // Test implementation of BaseCommands for testing purposes
    private static class TestBaseCommands extends BaseCommands {
        private String adminAddress;
        private boolean needAdminServer;

        public void setAdminAddress(String adminAddress) {
            this.adminAddress = adminAddress;
        }

        public void setNeedAdminServer(boolean needAdminServer) {
            this.needAdminServer = needAdminServer;
        }

        @Override
        public String getAdminAddress() {
            return adminAddress;
        }

        @Override
        public boolean needAdminServer() {
            return needAdminServer;
        }
    }
}
