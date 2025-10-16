package com.alipay.antchain.l2.relayer.commons.l2basic;

public interface IDaCompressor {

    byte[] compress(byte[] payload);

    byte[] decompress(byte[] payload);
}
