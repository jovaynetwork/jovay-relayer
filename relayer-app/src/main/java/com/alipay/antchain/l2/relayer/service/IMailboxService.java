package com.alipay.antchain.l2.relayer.service;

import java.math.BigInteger;

public interface IMailboxService {

    void initService(BigInteger startBlockNumber);

    void processL1MsgBatch();

    void proveL2Msg();
}
