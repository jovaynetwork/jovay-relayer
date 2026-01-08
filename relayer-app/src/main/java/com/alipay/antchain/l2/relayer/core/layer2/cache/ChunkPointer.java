package com.alipay.antchain.l2.relayer.core.layer2.cache;

import java.math.BigInteger;

import com.alipay.antchain.l2.relayer.commons.models.ChunkWrapper;
import lombok.NonNull;

public record ChunkPointer(@NonNull BigInteger batchIndex, long chunkIndex, @NonNull BigInteger endBlockNumber, int offset, int size) {

    public static ChunkPointer from(ChunkWrapper chunk, int offset, int size) {
        return new ChunkPointer(chunk.getBatchIndex(), chunk.getChunkIndex(), chunk.getEndBlockNumber(), offset, size);
    }
}
