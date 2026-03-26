/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlobsDaData;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.utils.RollupUtils;
import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.crypto.Blob;

import static java.util.stream.IntStream.range;

public class EthBlobsTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static byte[] createRandomBlob() {
        final byte[][] blob =
                range(0, CKZG4844JNI.FIELD_ELEMENTS_PER_BLOB)
                        .mapToObj(__ -> randomBLSFieldElement())
                        .map(fieldElement -> fieldElement.toArray(ByteOrder.BIG_ENDIAN))
                        .toArray(byte[][]::new);
        return flatten(blob);
    }

    private static UInt256 randomBLSFieldElement() {
        final BigInteger attempt = new BigInteger(CKZG4844JNI.BLS_MODULUS.bitLength(), RANDOM);
        if (attempt.compareTo(CKZG4844JNI.BLS_MODULUS) < 0) {
            return UInt256.valueOf(attempt);
        }
        return randomBLSFieldElement();
    }

    private static byte[] flatten(final byte[]... bytes) {
        final int capacity = Arrays.stream(bytes).mapToInt(b -> b.length).sum();
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);
        Arrays.stream(bytes).forEach(buffer::put);
        return buffer.array();
    }

    @Test
    public void testSerialization() {
        var ethBlobs = new EthBlobs(range(0, 4).mapToObj(__ -> new Blob(createRandomBlob())).toList());
        var bytes = ethBlobs.toBytes();
        Assert.assertEquals(4 * EthBlobs.BLOB_SIZE, bytes.length);

        var actual = EthBlobs.fromBytes(bytes);
        Assert.assertEquals(4, actual.blobs().size());
        for (int i = 0; i < 4; i++) {
            Assert.assertArrayEquals(ethBlobs.blobs().get(i).getData().toArray(), actual.blobs().get(i).getData().toArray());
        }
    }

    @Test
    public void testExtractBlobsFromHeavyChunks() {
        var inFile = FileUtil.readString("batch-heavy", Charset.defaultCharset());
        var jObj = JSON.parseObject(inFile);
        var chunkJsonArr = jObj.getJSONArray("chunks");
        var chunks = chunkJsonArr.stream().map(x -> Chunk.fromJson(x.toString())).toList();

        var rawChunks = RollupUtils.serializeChunks(BatchVersionEnum.BATCH_V0, chunks);

        var ethBlobs = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, new ChunksPayload(BatchVersionEnum.BATCH_V0, chunks));
        Assert.assertEquals(3, ethBlobs.getBlobs().blobs().size());

        var actual = ethBlobs.toBatchPayload();
        Assert.assertArrayEquals(
                DigestUtil.sha256(rawChunks),
                DigestUtil.sha256(actual.serialize())
        );
        Assert.assertEquals(2, ethBlobs.getBlobs().blobs().get(0).getData().get(0));
        Assert.assertEquals(1, ethBlobs.getBlobs().blobs().get(0).getData().get(1));

        // batch v0
        ethBlobs = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V0, new ChunksPayload(BatchVersionEnum.BATCH_V0, chunks));
        Assert.assertEquals(6, ethBlobs.getBlobs().blobs().size());

        actual = ethBlobs.toBatchPayload();
        Assert.assertArrayEquals(
                DigestUtil.sha256(rawChunks),
                DigestUtil.sha256(actual.serialize())
        );
        Assert.assertEquals(0, ethBlobs.getBlobs().blobs().get(0).getData().get(0));
    }

    @Test
    public void testExtractBlobsFromLightChunks() {
        var inFile = FileUtil.readString("batch-light", Charset.defaultCharset());
        var jObj = JSON.parseObject(inFile);
        var chunkJsonArr = jObj.getJSONArray("chunks");
        var chunks = chunkJsonArr.stream().map(x -> Chunk.fromJson(x.toString())).toList();

        var rawChunks = RollupUtils.serializeChunks(BatchVersionEnum.BATCH_V0, chunks);

        var ethBlobs = BlobsDaData.buildFrom(BatchVersionEnum.BATCH_V1, new ChunksPayload(BatchVersionEnum.BATCH_V0, chunks));
        Assert.assertEquals(1, ethBlobs.getBlobs().blobs().size());

        var actual = ethBlobs.toBatchPayload();
        Assert.assertArrayEquals(
                DigestUtil.sha256(rawChunks),
                DigestUtil.sha256(actual.serialize())
        );

        var obj = BlobsDaData.buildFrom(ethBlobs.getBlobs());
        Assert.assertEquals(obj.toBatchPayload(), obj.toBatchPayload());
    }

    @Test
    public void testRollupUtilsAppendToRawChunks() {
        var inFile = FileUtil.readString("batch-heavy", Charset.defaultCharset());
        var jObj = JSON.parseObject(inFile);
        var chunkJsonArr = jObj.getJSONArray("chunks");
        var chunks = chunkJsonArr.stream().map(x -> Chunk.fromJson(x.toString())).toList();

        var rawChunks = RollupUtils.appendToRawChunks(
                RollupUtils.serializeChunks(BatchVersionEnum.BATCH_V0, chunks.subList(0, chunks.size() - 1)),
                BatchVersionEnum.BATCH_V0,
                chunks.get(chunks.size() - 1)
        );
        var actual= RollupUtils.deserializeChunks(BatchVersionEnum.BATCH_V0, rawChunks);
        Assert.assertEquals(chunks.size(), actual.size());
        for (int i = 0; i < chunks.size(); i++) {
            Assert.assertEquals(chunks.get(i).getStartBlockNumber(), actual.get(i).getStartBlockNumber());
            Assert.assertEquals(chunks.get(i).getEndBlockNumber(), actual.get(i).getEndBlockNumber());
            Assert.assertArrayEquals(chunks.get(i).getL2Transactions(), actual.get(i).getL2Transactions());
        }
    }
}
