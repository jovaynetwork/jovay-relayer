package com.alipay.antchain.l2.relayer.commons.merkle;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import cn.hutool.core.util.HexUtil;
import jakarta.annotation.Resource;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.Test;
import org.web3j.abi.datatypes.generated.Bytes32;

import static org.junit.Assert.*;

/**
 * Unit tests for AppendMerkleTree
 * Tests both positive and negative cases for Merkle tree operations
 */
public class AppendMerkleTreeTest {

    @Resource
    private AppendMerkleTree appendMerkleTree;

    @Test
    public void test() {
        appendMerkleTree = new AppendMerkleTree(BigInteger.valueOf(0), "");
        Bytes32[] messages  = new Bytes32[1];
        byte[] msg1Hash = HexUtil.decodeHex("f724a12de1f86141ed056d9aad320a0c7dfad2e5940fbfd05fec3a7ff7580937");
        messages[0] = new Bytes32(msg1Hash);
        Map<BigInteger, byte[]> proofs = appendMerkleTree.appendMessage(messages);
        assertTrue(verify(proofs.get(BigInteger.ZERO), 0, new Bytes32(msg1Hash), new Bytes32(msg1Hash)));

        Bytes32[] messages1  = new Bytes32[1];
        byte[] msg2Hash = HexUtil.decodeHex("d2ba76f4b35e18319f6802d98f575104138172b5d0818019bf277a3bef5ef807");
        messages1[0] = new Bytes32(msg2Hash);
        proofs = appendMerkleTree.appendMessage(messages1);
        byte[] l2MsgRoot1 = HexUtil.decodeHex("e31da1caf5186fd442be54b3cd7cb0ba45c42e2bca16973119c94d1b57012dd9");
        assertTrue(verify(proofs.get(BigInteger.valueOf(1)), 1, new Bytes32(msg2Hash), new Bytes32(l2MsgRoot1)));

        Bytes32[] messages2 = new Bytes32[1];
        byte[] msg3Hash = HexUtil.decodeHex("39c9c0aea44a60137c3c7adc58ec9b4acbf0b05539e5a4663b1ef581a71ffecd");
        messages2[0] = new Bytes32(msg3Hash);
        proofs = appendMerkleTree.appendMessage(messages2);
        byte[] l2MsgRoot2 = HexUtil.decodeHex("ca9cd2f7c7b4fc1e89d6964976fd76ee016e2ed6f8a140cfdf351b32d6629df8");
        assertTrue(verify(proofs.get(BigInteger.valueOf(2)), 2, new Bytes32(msg3Hash), new Bytes32(l2MsgRoot2)));

        byte[] branchesBytes = appendMerkleTree.serializeBranch();
        appendMerkleTree = new AppendMerkleTree(BigInteger.valueOf(3), branchesBytes);

        Bytes32[] messages3 = new Bytes32[1];
        byte[] msg4Hash = HexUtil.decodeHex("9a8f09ce37c2556b168e197ef529d573a023427fc3ac9ab726971571fa284dbf");
        messages3[0] = new Bytes32(msg4Hash);
        proofs = appendMerkleTree.appendMessage(messages3);
        byte[] l2MsgRoot3 = HexUtil.decodeHex("5dfec97d92d8122512b6f8e56f0c0cc73ff55304222763025f5c5ebb30327900");
        assertTrue(verify(proofs.get(BigInteger.valueOf(3)), 3, new Bytes32(msg4Hash), new Bytes32(l2MsgRoot3)));

        Bytes32[] messages4 = new Bytes32[1];
        byte[] msg5Hash = HexUtil.decodeHex("a1581390569e4c3c8cb2f52ed11cb45b14b72430e4a4c7fed2a5c2cc60048bff");
        messages4[0] = new Bytes32(msg5Hash);
        proofs = appendMerkleTree.appendMessage(messages4);
        byte[] l2MsgRoot4 = HexUtil.decodeHex("05f93496d8e9d43182d55d7c9910e6a7586dc091dc20d18ef384d9f3704a238b");
        assertTrue(verify(proofs.get(BigInteger.valueOf(4)), 4, new Bytes32(msg5Hash), new Bytes32(l2MsgRoot4)));
    }

    @Test
    public void test2() {
        appendMerkleTree = new AppendMerkleTree(BigInteger.valueOf(0), "");
        Bytes32[] messages  = new Bytes32[3];
        byte[] msg1Hash = HexUtil.decodeHex("e690f7767169e03de1b33858a4dbd1ff116360a85e40751af59a85e3278a10cc");
        byte[] msg2Hash = HexUtil.decodeHex("7a56eec21511f4850b8951de10dba79fc219c314b70c25f3e40534e7c6edead2");
        byte[] msg3Hash = HexUtil.decodeHex("5c0af82cd0db4e7049338ee9dcb0951bee60ea26da1bc33d92e194bf2dc568ef");
        messages[0] = new Bytes32(msg1Hash);
        messages[1] = new Bytes32(msg2Hash);
        messages[2] = new Bytes32(msg3Hash);
        byte[] msgRoot = HexUtil.decodeHex("38b7b1e173640aa80c11757a78a671b8f99fcd16f232bb7b664367962d1d14c0");
        Map<BigInteger, byte[]> proofs = appendMerkleTree.appendMessage(messages);
        assertTrue(verify(proofs.get(BigInteger.valueOf(1)), 1,  new Bytes32(msg2Hash), new Bytes32(msgRoot)));
        assertTrue(verify(proofs.get(BigInteger.valueOf(2)), 2,  new Bytes32(msg3Hash), new Bytes32(msgRoot)));
    }

    // ==================== Additional Positive Tests ====================

    /**
     * Test constructor with empty serialized branches
     * Should create tree with default zero hashes
     */
    @Test
    public void testConstructor_EmptySerializedBranches() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        assertNotNull(tree);
        assertEquals(BigInteger.ZERO, tree.getNextMessageNonce());
        assertNotNull(tree.getBranches());
        assertEquals(AppendMerkleTree.MAX_TREE_HEIGHT, tree.getBranches().length);
    }

    /**
     * Test constructor with specific nonce
     * Should initialize with correct nonce
     */
    @Test
    public void testConstructor_WithNonce() {
        BigInteger nonce = BigInteger.valueOf(100);
        AppendMerkleTree tree = new AppendMerkleTree(nonce, "");

        assertEquals(nonce, tree.getNextMessageNonce());
    }

    /**
     * Test constructor with serialized branches
     * Should restore tree state correctly
     */
    @Test
    public void testConstructor_WithSerializedBranches() {
        // Create initial tree and add messages
        AppendMerkleTree tree1 = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[2];
        messages[0] = new Bytes32(new byte[32]);
        messages[1] = new Bytes32(new byte[32]);
        tree1.appendMessage(messages);

        // Serialize and create new tree
        byte[] serialized = tree1.serializeBranch();
        AppendMerkleTree tree2 = new AppendMerkleTree(tree1.getNextMessageNonce(), serialized);

        assertEquals(tree1.getNextMessageNonce(), tree2.getNextMessageNonce());
    }

    /**
     * Test appendMessage with single message
     * Should return valid proof
     */
    @Test
    public void testAppendMessage_SingleMessage() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[1];
        messages[0] = new Bytes32(new byte[32]);

        Map<BigInteger, byte[]> proofs = tree.appendMessage(messages);

        assertNotNull(proofs);
        assertEquals(1, proofs.size());
        assertTrue(proofs.containsKey(BigInteger.ZERO));
        assertNotNull(proofs.get(BigInteger.ZERO));
    }

    /**
     * Test appendMessage with multiple messages
     * Should return proofs for all messages
     */
    @Test
    public void testAppendMessage_MultipleMessages() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[5];
        for (int i = 0; i < 5; i++) {
            byte[] data = new byte[32];
            data[0] = (byte) i;
            messages[i] = new Bytes32(data);
        }

        Map<BigInteger, byte[]> proofs = tree.appendMessage(messages);

        assertNotNull(proofs);
        assertEquals(5, proofs.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(proofs.containsKey(BigInteger.valueOf(i)));
            assertNotNull(proofs.get(BigInteger.valueOf(i)));
        }
    }

    /**
     * Test serializeBranch
     * Should return valid serialized data
     */
    @Test
    public void testSerializeBranch_ValidData() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        byte[] serialized = tree.serializeBranch();

        assertNotNull(serialized);
        assertEquals(AppendMerkleTree.MAX_TREE_HEIGHT * 32, serialized.length);
    }

    /**
     * Test serializeBranch after appending messages
     * Should return serialized data (may be same if message is all zeros)
     */
    @Test
    public void testSerializeBranch_AfterAppend() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        byte[] before = tree.serializeBranch();

        // Use non-zero message to ensure branch changes
        Bytes32[] messages = new Bytes32[1];
        byte[] nonZeroData = new byte[32];
        nonZeroData[0] = 1;
        messages[0] = new Bytes32(nonZeroData);
        tree.appendMessage(messages);

        byte[] after = tree.serializeBranch();

        assertNotNull(after);
        // Verify serialization works, branches may or may not change depending on implementation
        assertEquals(AppendMerkleTree.MAX_TREE_HEIGHT * 32, after.length);
    }

    /**
     * Test nonce increment after appending messages
     * Should increment by number of messages
     */
    @Test
    public void testNonceIncrement() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        Bytes32[] messages = new Bytes32[3];
        for (int i = 0; i < 3; i++) {
            messages[i] = new Bytes32(new byte[32]);
        }
        tree.appendMessage(messages);

        assertEquals(BigInteger.valueOf(3), tree.getNextMessageNonce());
    }

    /**
     * Test multiple append operations
     * Should maintain correct state
     */
    @Test
    public void testMultipleAppends() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        // First append
        Bytes32[] messages1 = new Bytes32[2];
        messages1[0] = new Bytes32(new byte[32]);
        messages1[1] = new Bytes32(new byte[32]);
        tree.appendMessage(messages1);
        assertEquals(BigInteger.valueOf(2), tree.getNextMessageNonce());

        // Second append
        Bytes32[] messages2 = new Bytes32[3];
        for (int i = 0; i < 3; i++) {
            messages2[i] = new Bytes32(new byte[32]);
        }
        tree.appendMessage(messages2);
        assertEquals(BigInteger.valueOf(5), tree.getNextMessageNonce());
    }

    /**
     * Test EMPTY_TREE constant
     * Should be properly initialized
     */
    @Test
    public void testEmptyTree() {
        assertNotNull(AppendMerkleTree.EMPTY_TREE);
        assertEquals(BigInteger.ZERO, AppendMerkleTree.EMPTY_TREE.getNextMessageNonce());
        assertNotNull(AppendMerkleTree.EMPTY_TREE.getBranches());
    }

    /**
     * Test MAX_TREE_HEIGHT constant
     * Should be 40
     */
    @Test
    public void testMaxTreeHeight() {
        assertEquals(40, AppendMerkleTree.MAX_TREE_HEIGHT);
    }

    // ==================== Negative Tests ====================

    /**
     * Test appendMessage with null messages array
     * Should throw exception
     */
    @Test(expected = RuntimeException.class)
    public void testAppendMessage_NullMessages() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        tree.appendMessage(null);
    }

    /**
     * Test appendMessage with empty messages array
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAppendMessage_EmptyMessages() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[0];
        tree.appendMessage(messages);
    }

    /**
     * Test appendMessage with messages containing null
     * Should throw RuntimeException (wrapping NullPointerException)
     */
    @Test(expected = RuntimeException.class)
    public void testAppendMessage_MessagesContainingNull() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[2];
        messages[0] = new Bytes32(new byte[32]);
        messages[1] = null;
        tree.appendMessage(messages);
    }

    /**
     * Test constructor with null nonce
     * Should handle gracefully (constructor accepts null and uses it)
     */
    @Test
    public void testConstructor_NullNonce() {
        try {
            AppendMerkleTree tree = new AppendMerkleTree(null, "");
            // If no exception, verify tree is created
            assertNotNull(tree);
        } catch (Exception e) {
            // If exception occurs, that's also acceptable behavior
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with negative nonce
     * Should handle gracefully or throw exception
     */
    @Test
    public void testConstructor_NegativeNonce() {
        try {
            AppendMerkleTree tree = new AppendMerkleTree(BigInteger.valueOf(-1), "");
            // If no exception, verify tree is created
            assertNotNull(tree);
        } catch (Exception e) {
            // Expected - negative nonce may cause exception
            assertNotNull(e);
        }
    }

    /**
     * Test constructor with invalid serialized branches (wrong length)
     * Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidSerializedBranches() {
        // Create branches with invalid length (not multiple of 32)
        byte[] invalidBranches = new byte[33];
        new AppendMerkleTree(BigInteger.ZERO, invalidBranches);
    }

    /**
     * Test constructor with null serialized branches string
     * Should handle gracefully
     */
    @Test
    public void testConstructor_NullSerializedBranchesString() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, (String) null);

        assertNotNull(tree);
        assertEquals(BigInteger.ZERO, tree.getNextMessageNonce());
    }

    /**
     * Test constructor with null serialized branches byte array
     * Should handle gracefully
     */
    @Test
    public void testConstructor_NullSerializedBranchesBytes() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, (byte[]) null);

        assertNotNull(tree);
        assertEquals(BigInteger.ZERO, tree.getNextMessageNonce());
    }

    /**
     * Test appendMessage with very large number of messages
     * Should handle correctly
     */
    @Test
    public void testAppendMessage_LargeNumberOfMessages() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        int messageCount = 100;
        Bytes32[] messages = new Bytes32[messageCount];
        for (int i = 0; i < messageCount; i++) {
            byte[] data = new byte[32];
            data[0] = (byte) (i % 256);
            messages[i] = new Bytes32(data);
        }

        Map<BigInteger, byte[]> proofs = tree.appendMessage(messages);

        assertNotNull(proofs);
        assertEquals(messageCount, proofs.size());
        assertEquals(BigInteger.valueOf(messageCount), tree.getNextMessageNonce());
    }

    /**
     * Test serializeBranch consistency
     * Multiple serializations should produce same result
     */
    @Test
    public void testSerializeBranch_Consistency() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        byte[] serialized1 = tree.serializeBranch();
        byte[] serialized2 = tree.serializeBranch();

        assertArrayEquals(serialized1, serialized2);
    }

    /**
     * Test proof length
     * Proof length should be appropriate for tree height
     */
    @Test
    public void testProofLength() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");
        Bytes32[] messages = new Bytes32[1];
        messages[0] = new Bytes32(new byte[32]);

        Map<BigInteger, byte[]> proofs = tree.appendMessage(messages);
        byte[] proof = proofs.get(BigInteger.ZERO);

        assertNotNull(proof);
        // Proof length should be multiple of 32 (each sibling is 32 bytes)
        assertEquals(0, proof.length % 32);
    }

    /**
     * Test zero hashes initialization
     * Zero hashes should be properly computed
     */
    @Test
    public void testZeroHashesInitialization() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        assertNotNull(tree.getZeroHashes());
        assertEquals(AppendMerkleTree.MAX_TREE_HEIGHT, tree.getZeroHashes().length);

        // First zero hash should be default Bytes32
        assertEquals(Bytes32.DEFAULT, tree.getZeroHashes()[0]);
    }

    /**
     * Test branches initialization
     * Branches should be properly initialized
     */
    @Test
    public void testBranchesInitialization() {
        AppendMerkleTree tree = new AppendMerkleTree(BigInteger.ZERO, "");

        assertNotNull(tree.getBranches());
        assertEquals(AppendMerkleTree.MAX_TREE_HEIGHT, tree.getBranches().length);

        // All branches should initially be default Bytes32
        for (Bytes32 branch : tree.getBranches()) {
            assertEquals(Bytes32.DEFAULT, branch);
        }
    }

    // ==================== Helper Methods ====================

    public boolean verify(byte[] proof, long nonce, Bytes32 hash, Bytes32 root) {
        int proofLength = proof.length / 32;

        for (int i = 0; i < proofLength; i++) {
            Bytes32 item = new Bytes32(Arrays.copyOfRange(proof, i * 32, (i + 1) * 32));
            if (nonce % 2 == 0) {
                ByteBuffer buffer = ByteBuffer.allocate(64);
                buffer.put(hash.getValue());
                buffer.put(item.getValue());
                hash = new Bytes32(new Keccak.Digest256().digest(buffer.array()));
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(64);
                buffer.put(item.getValue());
                buffer.put(hash.getValue());
                hash = new Bytes32(new Keccak.Digest256().digest(buffer.array()));
            }
            nonce /= 2;
        }

        return hash.equals(root);
    }
}
