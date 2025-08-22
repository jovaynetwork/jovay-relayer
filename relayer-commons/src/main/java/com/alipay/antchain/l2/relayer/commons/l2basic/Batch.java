package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidBatchException;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.web3j.crypto.BlobUtils;

@Setter
@Builder
@AllArgsConstructor
@Slf4j
public class Batch {

    /**
     * Create a v0 batch
     *
     * @param batchIndex      index of the batch
     * @param parentBatchHeader header of the parent batch
     * @param chunks          chunks of the batch, every chunk inside must have hash calculated.
     * @return batch
     */
    public static Batch createBatchV0(BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks) {
        return new Batch(0, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks);
    }

    /**
     * Create a v0 batch with existing blobs
     *
     * @param batchIndex      index of the batch
     * @param parentBatchHeader header of the parent batch
     * @param chunks          chunks of the batch, every chunk inside must have hash calculated.
     * @return batch
     */
    public static Batch createBatchV0(BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, EthBlobs blobs) {
        return new Batch(0, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, blobs);
    }

    @Getter
    private BatchHeader batchHeader;

    @Getter
    private List<Chunk> chunks;

    private EthBlobs blobs;

    public Batch(int version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks) {
        this(version, batchIndex, parentBatchHeader, l1MsgRollingHash, chunks, null);
    }

    /**
     * Create a v0 batch
     *
     * @param batchIndex        index of the batch
     * @param parentBatchHeader header of the parent batch
     * @param l1MsgRollingHash  l1MsgRollingHash from block trace
     * @param chunks            chunks of the batch, every chunk inside must have hash calculated.
     * @param blobs             blobs from chunks
     */
    public Batch(int version, BigInteger batchIndex, BatchHeader parentBatchHeader, byte[] l1MsgRollingHash, @NonNull List<Chunk> chunks, EthBlobs blobs) {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("batch has no chunk");
        }
        if (ObjectUtil.isNull(blobs)) {
            blobs = EthBlobs.extractBlobsFromChunks(chunks);
            if (ObjectUtil.isEmpty(blobs)) {
                throw new RuntimeException("empty blobs");
            }
        }

        var versionedHashesStream = new ByteArrayOutputStream();
        blobs.blobs().stream().map(BlobUtils::getCommitment).map(BlobUtils::kzgToVersionedHash)
                .forEach(versionedHash -> versionedHashesStream.writeBytes(versionedHash.toArray()));

        this.batchHeader = BatchHeader.builder()
                .version(ByteUtil.intToByte(version))
                .batchIndex(batchIndex)
                .l1MsgRollingHash(l1MsgRollingHash)
                .parentBatchHash(parentBatchHeader.getHash())
                .dataHash(new Keccak.Digest256().digest(versionedHashesStream.toByteArray()))
                .build();
        this.chunks = chunks;
        this.blobs = blobs;
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
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("block has no chunk");
        }
        return this.chunks.stream().map(Chunk::getStartBlockNumber).min(BigInteger::compareTo).get();
    }

    /**
     * Get the biggest block number for this batch.
     *
     * @return included block number
     */
    public BigInteger getEndBlockNumber() {
        if (ObjectUtil.isEmpty(chunks)) {
            throw new RuntimeException("block has no chunk");
        }
        return this.chunks.stream().map(Chunk::getEndBlockNumber).max(BigInteger::compareTo).get();
    }

    public void validate() throws InvalidBatchException {
        BigInteger currEnd = null;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (ObjectUtil.isNotNull(currEnd)) {
                if (!currEnd.add(BigInteger.ONE).equals(chunk.getStartBlockNumber())) {
                    throw new InvalidBatchException("discontinuous chunks block numbers: (batch: {}, chunk: {})", this.getBatchIndex().toString(), i);
                }
            }
            currEnd = chunk.getEndBlockNumber();
        }
    }

    public EthBlobs getEthBlobs() {
        if (ObjectUtil.isNotEmpty(this.blobs)) {
            return this.blobs;
        }
        return EthBlobs.extractBlobsFromChunks(this.chunks);
    }
}
