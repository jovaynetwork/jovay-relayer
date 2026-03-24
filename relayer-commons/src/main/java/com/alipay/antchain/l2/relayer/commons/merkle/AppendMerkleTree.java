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

package com.alipay.antchain.l2.relayer.commons.merkle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.web3j.abi.datatypes.generated.Bytes32;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class AppendMerkleTree {
    public static final int MAX_TREE_HEIGHT = 40;

    public static final AppendMerkleTree EMPTY_TREE = new AppendMerkleTree(new Bytes32[]{}, new Bytes32[]{}, BigInteger.ZERO);

    private Bytes32[] branches;

    private Bytes32[] zeroHashes;

    private BigInteger nextMessageNonce;

    public AppendMerkleTree(BigInteger nextMessageNonce, String serializedBranches) {
        this(nextMessageNonce, HexUtil.decodeHex(serializedBranches));
    }

    public AppendMerkleTree(BigInteger nextMessageNonce, byte[] serializedBranches) {
        branches = new Bytes32[MAX_TREE_HEIGHT];
        zeroHashes = new Bytes32[MAX_TREE_HEIGHT];
        for (int i = 0; i < MAX_TREE_HEIGHT; i++) {
            branches[i] = Bytes32.DEFAULT;
        }
        // Compute hashes in empty sparse Merkle tree
        zeroHashes[0] = Bytes32.DEFAULT;
        for (int height = 0; height + 1 < MAX_TREE_HEIGHT; height++) {
            ByteBuffer buffer = ByteBuffer.allocate(64);
            buffer.put(zeroHashes[height].getValue());
            buffer.put(zeroHashes[height].getValue());
            Bytes32 tmpHash = new Bytes32(new Keccak.Digest256().digest(buffer.array()));
            zeroHashes[height + 1] = tmpHash;
        }
        if (serializedBranches != null) {
            Assert.isTrue(serializedBranches.length % 32 == 0);
            for (int i = 0; i < serializedBranches.length / 32; i++) {
                branches[i] = new Bytes32(Arrays.copyOfRange(serializedBranches, i * 32, (i + 1) * 32));
            }
        }
        this.nextMessageNonce = nextMessageNonce;
    }

    public byte[] serializeBranch() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);
        for (Bytes32 bytes32 : branches) {
            try {
                stream.write(bytes32.getValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public Map<BigInteger, byte[]> appendMessage(Bytes32[] messages) {
        Assert.isTrue(messages.length != 0);
        try {
            List<Map<BigInteger,Bytes32>> merkleTree = constructMerkleTree(messages);

            updateBranch(merkleTree, messages.length);

            return getAndStoreProof(merkleTree, messages.length);
        } catch (Exception e) {
            throw new RuntimeException("failed to append message to merkle tree", e);
        }
    }

    private List<Map<BigInteger,Bytes32>> constructMerkleTree(Bytes32[] messages) {
        List<Map<BigInteger,Bytes32>> merkleTree = new ArrayList<>();
        BigInteger leafCount = nextMessageNonce.add(BigInteger.valueOf(messages.length - 1));
        merkleTree.add(new HashMap<>());
        while (leafCount.compareTo(BigInteger.ZERO) > 0) {
            merkleTree.add(new HashMap<>());
            leafCount = leafCount.shiftRight(1);
        }

        long cur = nextMessageNonce.longValue();
        int height = 0; // construct merkle begin height is 0;
        while(cur > 0) {
            if (cur % 2 == 1) {
                merkleTree.get(height).put(BigInteger.valueOf(cur^1), branches[height]);
            }
            cur >>= 1;
            height++;
        }

        for (int i = 0; i < messages.length; i++) {
            log.debug("append l2Msg {} to merkle tree", HexUtil.encodeHexStr(messages[i].getValue()));
            merkleTree.get(0).put(nextMessageNonce.add(BigInteger.valueOf(i)), messages[i]);
        }

        long updateBegin = nextMessageNonce.longValue();
        long updateEnd = nextMessageNonce.longValue() + messages.length - 1;
         // update with new message begin height is 1;
        for (height = 1; height + 1 <= merkleTree.size(); height++) {
            if (updateBegin % 2 == 1) {
                updateBegin--;
            }
            for (long i = updateBegin; i <= updateEnd; i += 2) {
                ByteBuffer buffer = ByteBuffer.allocate(64);
                buffer.put(merkleTree.get(height - 1).get(BigInteger.valueOf(i)).getValue());
                merkleTree.get(height - 1).putIfAbsent(BigInteger.valueOf(i + 1), zeroHashes[height - 1]);
                buffer.put(merkleTree.get(height - 1).get(BigInteger.valueOf(i+1)).getValue());
                Bytes32 tmpHash = new Bytes32(new Keccak.Digest256().digest(buffer.array()));
                merkleTree.get(height).put(BigInteger.valueOf(i/2), tmpHash);
            }
            updateBegin >>= 1;
            updateEnd >>= 1;
        }

        return merkleTree;
    }

    private void updateBranch(List<Map<BigInteger,Bytes32>> merkleTree, int newMessageCount) throws Exception {
        long newNonce = newMessageCount + nextMessageNonce.longValue();
        int height = 0;
        while (newNonce > 0) {
            if (newNonce % 2 == 1) {
                branches[height] = merkleTree.get(height).get(BigInteger.valueOf(newNonce^1));
            }
            newNonce >>= 1;
            height++;
        }
        nextMessageNonce = nextMessageNonce.add(BigInteger.valueOf(newMessageCount));
    }

    private Map<BigInteger, byte[]> getAndStoreProof(List<Map<BigInteger,Bytes32>> merkleTree, int newMessageCount) throws Exception {
        Map<BigInteger, byte[]> proof = new HashMap<>();
        long beginNonce = nextMessageNonce.longValue() - newMessageCount;
        for (long nonce = beginNonce; nonce < nextMessageNonce.longValue(); nonce++) {
            long cur = nonce;
            int height = 0;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);
            while(height + 1 < merkleTree.size()) {
                stream.write(merkleTree.get(height).get(BigInteger.valueOf(cur^1)).getValue());
                cur >>>= 1;
                height++;
            }
            proof.put(BigInteger.valueOf(nonce), byteArrayOutputStream.toByteArray());
        }
        return proof;
    }
}
