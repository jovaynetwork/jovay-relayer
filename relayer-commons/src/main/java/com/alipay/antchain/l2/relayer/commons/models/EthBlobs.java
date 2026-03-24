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

import java.nio.ByteBuffer;
import java.util.List;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Blob;

@Slf4j
public record EthBlobs(List<Blob> blobs) {

    public static final int WORDS_NUM_PER_BLOB = 4096;

    public static final int BLOB_SIZE = WORDS_NUM_PER_BLOB * 32;

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
}
