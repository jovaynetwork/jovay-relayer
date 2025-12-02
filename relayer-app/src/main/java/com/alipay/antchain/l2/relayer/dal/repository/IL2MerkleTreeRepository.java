package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;

public interface IL2MerkleTreeRepository {
    void saveMerkleTree(AppendMerkleTree merkleTree, BigInteger batchIndex);

    AppendMerkleTree getMerkleTree(BigInteger batchIndex);
}
