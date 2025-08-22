package com.alipay.antchain.l2.relayer.query.dal.repo;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.query.commons.BatchMeta;

public interface IBatchDataRepository {

    BatchMeta getBatchMeta(BigInteger batchIndex);
}
