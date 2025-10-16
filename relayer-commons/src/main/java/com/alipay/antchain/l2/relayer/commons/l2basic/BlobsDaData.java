package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.web3j.crypto.Blob;
import org.web3j.crypto.BlobUtils;

@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlobsDaData implements IDaData {

    public static final int CAPACITY_BYTE_LEN_PER_WORD = 31;

    public static final int CAPACITY_BYTE_PER_BLOB = CAPACITY_BYTE_LEN_PER_WORD * EthBlobs.WORDS_NUM_PER_BLOB;

    public static final int DA_DATA_META_LEN_SIZE = 4;

    public static final int DATA_LEN_SIZE_OF_DA_META = 3;

    public static BlobsDaData buildFrom(BatchVersionEnum version, IBatchPayload payload) {
        return new BlobsDaData(version, payload);
    }

    public static BlobsDaData buildFrom(EthBlobs blobs) {
        return new BlobsDaData(blobs);
    }

    public static BlobsDaData lazyBuildFrom(EthBlobs blobs) {
        var daData = new BlobsDaData();
        daData.blobs = blobs;
        return daData;
    }

    private DaVersion daVersion;

    private BatchVersionEnum batchVersion;

    private int dataLen;
    
    private IBatchPayload batchPayload;

    private EthBlobs blobs;

    /**
     * Use this constructor to decode the batch from DA data.
     * 
     * @param blobs
     */
    private BlobsDaData(EthBlobs blobs) {
        this.blobs = blobs;
        this.batchPayload = toBatchPayload();
    }

    /**
     * Use this constructor to sink the batch into DA data.
     * 
     * @param version batch version
     * @param payload batch payload
     */
    private BlobsDaData(BatchVersionEnum version, IBatchPayload payload) {
        this.batchVersion = version;
        var rawPayload = payload.serialize();
        this.daVersion = DaVersion.DA_0;

        log.info("try to build the blob DA data for batch of version {} and blocks from {} to {}",
                version, payload.getStartBlockNumber(), payload.getEndBlockNumber());
        // If batch version supports batch data compression, try to compress the batch body
        if (batchVersion == BatchVersionEnum.BATCH_V1) {
            var compressed = batchVersion.getDaCompressor().compress(rawPayload);
            byte[] data;
            if (compressed.length < rawPayload.length) {
                log.info("choose to use compressed batch body for compression ratio: {}/{} = {}",
                        rawPayload.length, compressed.length, rawPayload.length / (double) compressed.length);
                // if compressed is smaller than rawChunks, use compressed
                data = compressed;
                // mark codecVersion as DA_2, means that compression on
                this.daVersion = DaVersion.DA_2;
            } else {
                log.info("compression ratio is not improved, use raw batch payload");
                // use uncompressed as data
                data = rawPayload;
                // it's da version one
                this.daVersion = DaVersion.DA_1;
            }
            this.dataLen = data.length;
            rawPayload = new byte[DA_DATA_META_LEN_SIZE + this.dataLen];
            rawPayload[0] = batchVersion.getValue();
            // copy the data length into first 3 bytes
            System.arraycopy(
                    BytesUtils.toUnsignedByteArray(DATA_LEN_SIZE_OF_DA_META, BigInteger.valueOf(this.dataLen)),
                    0, rawPayload, 1, DATA_LEN_SIZE_OF_DA_META
            );
            // copy the data into the rest of the bytes
            System.arraycopy(data, 0, rawPayload, DA_DATA_META_LEN_SIZE, this.dataLen);
        } else if (batchVersion == BatchVersionEnum.BATCH_V0) {
            this.dataLen = rawPayload.length;
        }

        this.blobs = new EthBlobs(sinkIntoBlobs(rawPayload));
    }

    @Override
    public byte[] dataHash() {
        var versionedHashesStream = new ByteArrayOutputStream();
        blobs.blobs().stream().map(BlobUtils::getCommitment).map(BlobUtils::kzgToVersionedHash)
                .forEach(versionedHash -> versionedHashesStream.writeBytes(versionedHash.toArray()));
        return new Keccak.Digest256().digest(versionedHashesStream.toByteArray());
    }

    @Override
    public IBatchPayload toBatchPayload() {
        if (ObjectUtil.isNotNull(batchPayload)) {
            return batchPayload;
        }
        if (ObjectUtil.isEmpty(blobs)) {
            throw new IllegalArgumentException("blobs is empty");
        }

        var buf = ByteBuffer.allocate(blobs.blobs().size() * EthBlobs.WORDS_NUM_PER_BLOB * CAPACITY_BYTE_LEN_PER_WORD);
        // first byte of blobs is the DA codec version.
        this.daVersion = DaVersion.from(blobs.blobs().get(0).getData().get(0));
        for (var blob : blobs.blobs()) {
            var data = blob.getData().toArray();
            Assert.isTrue(data.length % 32 == 0, "blob data must be multiple of 32");
            for (int i = 0; i < data.length / 32; i++) {
                var realData = new byte[CAPACITY_BYTE_LEN_PER_WORD];
                System.arraycopy(
                        ArrayUtil.sub(data, i * 32, (i + 1) * 32), 1,
                        realData, 0,
                        CAPACITY_BYTE_LEN_PER_WORD
                );
                buf.put(realData);
            }
        }
        this.batchPayload = new ChunksPayload(RollupUtils.deserializeChunks(
                switch (this.daVersion) {
                    case DA_0 -> {
                        this.batchVersion = BatchVersionEnum.BATCH_V0;
                        yield buf.array();
                    }
                    case DA_1 -> {
                        var rawDaData = buf.array();
                        this.batchVersion = BatchVersionEnum.from(rawDaData[0]);
                        this.dataLen = BytesUtils.getUint24(rawDaData, 1);
                        yield ArrayUtil.sub(rawDaData, DA_DATA_META_LEN_SIZE, DA_DATA_META_LEN_SIZE + this.dataLen);
                    }
                    case DA_2 -> {
                        var rawDaData = buf.array();
                        this.batchVersion = BatchVersionEnum.from(rawDaData[0]);
                        this.dataLen = BytesUtils.getUint24(rawDaData, 1);
                        yield this.batchVersion.getDaCompressor().decompress(ArrayUtil.sub(rawDaData, DA_DATA_META_LEN_SIZE, DA_DATA_META_LEN_SIZE + this.dataLen));
                    }
                }
        ));
        return batchPayload;
    }

    private List<Blob> sinkIntoBlobs(byte[] rawPayload) {
        var blobs = new ArrayList<Blob>();
        var wordsNum = (int) Math.ceil(rawPayload.length / (double) CAPACITY_BYTE_LEN_PER_WORD);
        var buf = ByteBuffer.allocate(wordsNum * 32);

        for (int i = 0; i < wordsNum; i += 1) {
            var rawWord = new byte[32];
            if (i == 0) {
                // first byte of the blobs is supposed to be the DA codec version field
                rawWord[0] = this.daVersion.toByte();
            }
            System.arraycopy(
                    rawPayload,
                    i * CAPACITY_BYTE_LEN_PER_WORD,
                    rawWord,
                    1,
                    i == wordsNum - 1 ? rawPayload.length - i * CAPACITY_BYTE_LEN_PER_WORD : CAPACITY_BYTE_LEN_PER_WORD
            );
            buf.put(rawWord);
        }

        var raw = buf.array();
        for (int i = 0; i < raw.length; i += EthBlobs.BLOB_SIZE) {
            var blobData = new byte[EthBlobs.BLOB_SIZE];
            System.arraycopy(raw, i, blobData, 0, Math.min(EthBlobs.BLOB_SIZE, raw.length - i));
            blobs.add(new Blob(blobData));
        }
        return blobs;
    }
}
