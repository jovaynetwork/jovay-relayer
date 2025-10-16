package com.alipay.antchain.l2.relayer.commons.l2basic;

public interface IDaData {

    DaVersion getDaVersion();

    BatchVersionEnum getBatchVersion();

    int getDataLen();

    byte[] dataHash();

    IBatchPayload toBatchPayload();
}
