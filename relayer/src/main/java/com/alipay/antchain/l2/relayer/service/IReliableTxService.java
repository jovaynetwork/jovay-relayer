package com.alipay.antchain.l2.relayer.service;

public interface IReliableTxService {

    void processL1NotFinalizedTx();

    void processL2NotFinalizedTx();

    void retryFailedTx();
}
