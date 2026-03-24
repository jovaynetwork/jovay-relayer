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

package com.alipay.antchain.l2.relayer.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import lombok.SneakyThrows;

public class RollupUtils {

    public static boolean isProveReqTypeToProcess(String batchProveReqTypes, ProveTypeEnum proveType) {
        if (StrUtil.equalsIgnoreCase(batchProveReqTypes, "all")) {
            return true;
        }
        if (proveType == ProveTypeEnum.TEE_PROOF) {
            return StrUtil.equalsIgnoreCase(batchProveReqTypes, "tee");
        }
        if (proveType == ProveTypeEnum.ZK_PROOF) {
            return StrUtil.equalsIgnoreCase(batchProveReqTypes, "zk");
        }
        return false;
    }

    @SneakyThrows
    public static byte[] serializeChunks(BatchVersionEnum batchVersion, List<Chunk> chunks) {
        var rawChunksStream = new ByteArrayOutputStream();
        var streamToWrite = new DataOutputStream(rawChunksStream);
        chunks.forEach(
                chunk -> {
                    var raw = batchVersion.getChunkCodec().serialize(chunk);
                    try {
                        streamToWrite.writeInt(raw.length);
                        streamToWrite.write(raw);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        var rawChunks = rawChunksStream.toByteArray();
        streamToWrite.close();
        return rawChunks;
    }

    public static byte[] appendToRawChunks(byte[] rawChunks, BatchVersionEnum batchVersion, Chunk chunk) {
        var raw = batchVersion.getChunkCodec().serialize(chunk);
        var newRawChunks = new byte[rawChunks.length + 4 + raw.length];
        System.arraycopy(rawChunks, 0, newRawChunks, 0, rawChunks.length);
        System.arraycopy(ByteUtil.intToBytes(raw.length, ByteOrder.BIG_ENDIAN), 0, newRawChunks, rawChunks.length, 4);
        System.arraycopy(raw, 0, newRawChunks, rawChunks.length + 4, raw.length);
        return newRawChunks;
    }

    public static List<Chunk> deserializeChunks(BatchVersionEnum batchVersion, byte[] rawChunks) {
        var res = new ArrayList<Chunk>();
        var nextLen = BytesUtils.getUint32(rawChunks, 0);
        var offset = 4;
        while (nextLen != 0) {
            res.add(batchVersion.getChunkCodec().deserialize(BytesUtils.getBytes(rawChunks, offset, (int) nextLen)));
            offset += nextLen;
            if (rawChunks.length <= offset + 4) {
                break;
            }
            nextLen = BytesUtils.getUint32(rawChunks, offset);
            offset += 4;
        }

        return res;
    }
}
