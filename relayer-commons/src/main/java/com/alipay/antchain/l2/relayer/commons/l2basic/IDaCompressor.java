package com.alipay.antchain.l2.relayer.commons.l2basic;

import cn.hutool.core.util.StrUtil;
import com.github.luben.zstd.Zstd;

public interface IDaCompressor {

    /**
     * Default ZSTD compressor implementation.
     * <p>
     * This implementation uses the Zstandard (ZSTD) compression algorithm for compressing
     * and decompressing batch payload data. ZSTD provides a good balance between compression
     * ratio and speed, making it suitable for L2 batch data compression.
     * </p>
     * <p>
     * When decompression fails due to OutOfMemoryError, it throws a RuntimeException with
     * detailed information about the compressed size and expected decompressed size to help
     * diagnose memory issues.
     * </p>
     *
     * @see <a href="https://github.com/facebook/zstd">Zstandard Compression</a>
     */
    IDaCompressor ZSTD_DEFAULT_COMPRESSOR = new IDaCompressor() {
        @Override
        public byte[] compress(byte[] payload) {
            return Zstd.compress(payload);
        }

        @Override
        public byte[] decompress(byte[] payload) {
            try {
                return Zstd.decompress(payload);
            } catch (OutOfMemoryError e) {
                throw new RuntimeException(StrUtil.format(
                        "The uncompressed data too large, compressed size is {}B and decompressed data size is {}B. " +
                        "Maybe try to increase the max Java memory limit by '-Xmx', etc.",
                        payload.length, Zstd.getFrameContentSize(payload)
                ), e);
            }
        }
    };

    byte[] compress(byte[] payload);

    byte[] decompress(byte[] payload);
}
