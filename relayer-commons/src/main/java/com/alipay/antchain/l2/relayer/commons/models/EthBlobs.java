package com.alipay.antchain.l2.relayer.commons.models;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Blob;

@Slf4j
public record EthBlobs(List<Blob> blobs) {

    public static final int WORDS_NUM_PER_BLOB = 4096;

    public static final int BLOB_SIZE = WORDS_NUM_PER_BLOB * 32;

    public static final int CAPACITY_BYTE_LEN_PER_WORD = 31;

    public static final int CAPACITY_BYTE_PER_BLOB = CAPACITY_BYTE_LEN_PER_WORD * 4096;

    @SneakyThrows
    public static EthBlobs extractBlobsFromChunks(List<Chunk> chunks) {
        var rawChunks = RollupUtils.serializeChunks(chunks);

        var blobs = new ArrayList<Blob>();
        var wordsNum = (int) Math.ceil(rawChunks.length / (double) CAPACITY_BYTE_LEN_PER_WORD);
        var buf = ByteBuffer.allocate(wordsNum * 32);

        for (int i = 0; i < wordsNum; i += 1) {
            var rawWord = new byte[32];
            System.arraycopy(
                    rawChunks,
                    i * CAPACITY_BYTE_LEN_PER_WORD,
                    rawWord,
                    1,
                    i == wordsNum - 1 ? rawChunks.length - i * CAPACITY_BYTE_LEN_PER_WORD : CAPACITY_BYTE_LEN_PER_WORD
            );
            buf.put(rawWord);
        }

        var raw = buf.array();
        for (int i = 0; i < raw.length; i += BLOB_SIZE) {
            var blobData = new byte[BLOB_SIZE];
            System.arraycopy(raw, i, blobData, 0, Math.min(BLOB_SIZE, raw.length - i));
            blobs.add(new Blob(blobData));
        }

        return new EthBlobs(blobs);
    }

    public static EthBlobs fromBytes(byte[] bytes) {
        var buf = ByteBuffer.wrap(bytes);
        Assert.isTrue(bytes.length % BLOB_SIZE == 0, "bytes length must be multiple of BLOB_SIZE");
        var blobs = new Blob[bytes.length / BLOB_SIZE];
        for (int i = 0; i < blobs.length; i++) {
            var blobData = new byte[BLOB_SIZE];
            buf.get(i * BLOB_SIZE, blobData);
            blobs[i] = new Blob(blobData);
        }
        return new EthBlobs(List.of(blobs));
    }

    public byte[] toBytes() {
        var buf = ByteBuffer.allocate(blobs.size() * BLOB_SIZE);
        blobs.forEach(b -> buf.put(b.getData().toArray()));
        return buf.array();
    }

    public List<Chunk> toChunks() {
        if (ObjectUtil.isEmpty(blobs)) {
            return new ArrayList<>();
        }

        var buf = ByteBuffer.allocate(blobs.size() * WORDS_NUM_PER_BLOB * CAPACITY_BYTE_LEN_PER_WORD);
        for (var blob : blobs) {
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

        return RollupUtils.deserializeChunks(buf.array());
    }
}
