package com.alipay.antchain.l2.relayer.service;

public interface IReliableTxService {

    void processNotFinalizedTx();

    void retryFailedTx();
}
