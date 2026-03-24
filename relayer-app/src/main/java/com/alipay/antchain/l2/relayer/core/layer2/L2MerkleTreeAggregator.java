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

package com.alipay.antchain.l2.relayer.core.layer2;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.dal.repository.IL2MerkleTreeRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IMailboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.datatypes.generated.Bytes32;

@Component
@Slf4j
public class L2MerkleTreeAggregator implements IL2MerkleTreeAggregator {

    @Resource
    private IMailboxRepository mailboxRepository;

    @Resource
    private IL2MerkleTreeRepository l2MerkleTreeRepository;

    @Override
    public Map<BigInteger, byte[]> aggregate(BigInteger currBatchIndex) {
        log.info("start to aggregate l2 merkle tree for batch {}", currBatchIndex);

        AppendMerkleTree lastMerkleTree = l2MerkleTreeRepository.getMerkleTree(currBatchIndex.subtract(BigInteger.ONE));
        if (ObjectUtil.isNull(lastMerkleTree)) {
            throw new RuntimeException("can't find last batch's merkle tree for curr batch#" + currBatchIndex);
        }

        Map<BigInteger, byte[]> proofResult = null;
        List<byte[]> msgHashes = mailboxRepository.getMsgHashes(InterBlockchainMessageTypeEnum.L2_MSG, currBatchIndex);
        if (ObjectUtil.isNotEmpty(msgHashes)) {
            log.info("append {} l2 msg hashes to last batch's merkle tree", msgHashes.size());
            proofResult = lastMerkleTree.appendMessage(msgHashes.stream().map(Bytes32::new).toArray(Bytes32[]::new));
        }

        l2MerkleTreeRepository.saveMerkleTree(lastMerkleTree, currBatchIndex);

        return proofResult;
    }
}
