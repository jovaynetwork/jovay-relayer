package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;

public interface IRollupService {

    void setAnchorBatch(BatchHeader anchorBatch, AppendMerkleTree anchorBatchMerkleTree);

    void setAnchorBatch(BigInteger batchIndex);

    void pollL2Blocks();

    void proveTeeL2Batch();

    void proveZkL2Batch();

    void commitL2Batch();

    void commitL2TeeProof();

    void commitL2ZkProof();
}
