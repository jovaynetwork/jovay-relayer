package com.alipay.antchain.l2.relayer.commons.l2basic;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.github.luben.zstd.Zstd;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BatchVersionEnum {

    /**
     * <p>Batch version 0. We start from here. </p>
     */
    BATCH_V0((byte) 0, null),

    /**
     * <p>Batch version 1 supports: </p>
     * <ol>
     *     <li>Batch data compression in zstd algo with default parameters </li>
     * </ol>
     */
    BATCH_V1((byte) 1, new IDaCompressor() {
        @Override
        public byte[] compress(byte[] payload) {
            return Zstd.compress(payload);
        }

        @Override
        public byte[] decompress(byte[] payload) {
            return Zstd.decompress(payload);
        }
    });

    @JSONField
    private final byte value;

    private final IDaCompressor daCompressor;

    public boolean isBatchDataCompressionSupport() {
        return ObjectUtil.isNotNull(daCompressor);
    }

    @JSONCreator
    public static BatchVersionEnum from(byte value) {
        for (BatchVersionEnum version : BatchVersionEnum.values()) {
            if (version.getValue() == value) {
                return version;
            }
        }
        return null;
    }

    public static BatchVersionEnum from(int val) {
        return from((byte) val);
    }
}
