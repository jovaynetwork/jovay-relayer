package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;

public interface IBatchPayload {

    byte[] serialize();

    BigInteger getStartBlockNumber();

    BigInteger getEndBlockNumber();

    void validate() throws InvalidBatchException;

    long getRawTxTotalSize();

    int getL2TxCount();
}
