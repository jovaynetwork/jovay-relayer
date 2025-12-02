package com.alipay.antchain.l2.relayer.service;

import java.io.IOException;

public interface IL1ListenService {
    void pollL1MsgBatch() throws IOException;
}
