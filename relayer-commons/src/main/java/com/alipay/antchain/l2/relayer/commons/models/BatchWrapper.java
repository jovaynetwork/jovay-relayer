package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BatchWrapper {

    public static BatchWrapper createBatch(
            BatchVersionEnum batchVersion,
            BigInteger batchIndex,
            BatchWrapper parentBatchWrapper,
            byte[] postStateRoot,
            byte[] l1MsgRollingHash,
            byte[] l2MsgRoot,
            long finalizedL1MsgIndex,
            @NonNull List<ChunkWrapper> chunks
    ) {
        var wrapper = new BatchWrapper();
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getChunkIndex() != i) {
                throw new RuntimeException(StrUtil.format("chunk index {} not equal to the array index {}", chunks.get(i).getChunkIndex(), i));
            }
        }
        var l1MessagePopped = finalizedL1MsgIndex - parentBatchWrapper.getTotalL1MessagePopped();
        Assert.isTrue(l1MessagePopped >= 0, "finalizedL1MsgIndex becoming smaller than the one from previous batch");
        wrapper.setBatch(Batch.createBatch(
                batchVersion,
                batchIndex,
                parentBatchWrapper.getBatchHeader(),
                l1MsgRollingHash,
                chunks.stream().map(ChunkWrapper::getChunk).collect(Collectors.toList()),
                null
        ));
        wrapper.setPostStateRoot(postStateRoot);
        wrapper.setL2MsgRoot(l2MsgRoot);
        wrapper.setTotalL1MessagePopped(finalizedL1MsgIndex);
        wrapper.setL1MessagePopped(l1MessagePopped);
        return wrapper;
    }

    private Batch batch;

    private BigInteger startBlockNumber;

    private BigInteger endBlockNumber;

    private byte[] postStateRoot;

    private byte[] l2MsgRoot;

    private long totalL1MessagePopped;

    private long l1MessagePopped;

    private long gmtCreate;

    public BigInteger getBatchIndex() {
        return batch.getBatchIndex();
    }

    public BigInteger getStartBlockNumber() {
        if (ObjectUtil.isNull(startBlockNumber)) {
            return this.batch.getStartBlockNumber();
        }
        return startBlockNumber;
    }

    public BigInteger getEndBlockNumber() {
        if (ObjectUtil.isNull(endBlockNumber)) {
            return this.batch.getEndBlockNumber();
        }
        return endBlockNumber;
    }

    public BatchHeader getBatchHeader() {
        return batch.getBatchHeader();
    }

    public List<Chunk> getChunks() {
        return ((ChunksPayload) this.getBatch().getPayload()).chunks();
    }
}
