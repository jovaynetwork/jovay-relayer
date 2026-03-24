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

package com.alipay.antchain.l2.relayer.commoms;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.Test;
import org.web3j.abi.datatypes.generated.Bytes32;

import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertTrue;

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
