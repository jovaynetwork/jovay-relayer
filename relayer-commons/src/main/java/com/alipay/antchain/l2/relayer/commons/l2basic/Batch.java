package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Setter
@Builder
@AllArgsConstructor
@Slf4j
public class Batch {

    /**
     * Create a batch with existing blobs
     *
     * @param daData            eip4844 blobs from chunks
     * @param batchVersion      version of the batch
     * @param batchIndex        index of the batch
     * @param parentBatchHeader header of the parent batch
     * @param l1MsgRollingHash  l1MsgRollingHash from the last block trace of this batch
     * @param chunks            chunks of the batch, every chunk inside must have hash calculated.
     * @return batch
     */
    public static Batch createBatch(BatchVersionEnum batchVersion, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, IDaData daData) {
        return new Batch(batchVersion, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, daData);
    }

    @Getter
    private BatchHeader batchHeader;

    @Getter
    private IBatchPayload payload;

    private IDaData daData;

    public Batch(BatchVersionEnum version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks) {
        this(version, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, null);
    }

    /**
     * Create a v0 batch
     *
     * @param batchIndex        index of the batch
     * @param parentBatchHeader header of the parent batch
     * @param l1MsgRollingHash  l1MsgRollingHash from block trace
     * @param chunks            chunks of the batch, every chunk inside must have hash calculated.
     * @param daData            blobs from chunks
     */
    public Batch(BatchVersionEnum version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, IDaData daData) {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        this.payload = new ChunksPayload(chunks);
        if (ObjectUtil.isNull(daData)) {
            this.daData = BlobsDaData.buildFrom(version, this.payload);
        }

        this.batchHeader = BatchHeader.builder()
                .version(version)
                .batchIndex(batchIndex)
                .l1MsgRollingHash(l1MsgRollingHash)
                .parentBatchHash(parentBatchHeader.getHash())
                .dataHash(this.daData.dataHash())
                .build();
    }

    public long getBatchTxsLength() {
        return this.payload.getRawTxTotalSize();
    }

    public IDaData getDaData() {
        if (ObjectUtil.isNull(this.daData)) {
            this.daData = BlobsDaData.buildFrom(this.batchHeader.getVersion(), this.payload);
        }
        return this.daData;
    }

    public byte[] getBatchHash() {
        return batchHeader.getHash();
    }

    public String getBatchHashHex() {
        return HexUtil.encodeHexStr(batchHeader.getHash());
    }

    public BigInteger getBatchIndex() {
        return batchHeader.getBatchIndex();
    }

    public BigInteger getStartBlockNumber() {
        return this.payload.getStartBlockNumber();
    }

    /**
     * Get the biggest block number for this batch.
     *
     * @return included block number
     */
    public BigInteger getEndBlockNumber() {
        return this.payload.getEndBlockNumber();
    }

    public void validate() throws InvalidBatchException {
        this.payload.validate();
    }

    public EthBlobs getEthBlobs() {
        if (ObjectUtil.isNull(this.daData) || 0 == this.daData.getDataLen()) {
            this.daData = BlobsDaData.buildFrom(this.batchHeader.getVersion(), this.payload);
        }
        // for now, we only have eip4844 blobs as DA
        return ((BlobsDaData) this.daData).getBlobs();
    }
}
