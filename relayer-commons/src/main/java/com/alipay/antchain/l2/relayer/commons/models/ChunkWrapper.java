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
import java.util.List;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ChunkWrapper {

    public static ChunkWrapper decodeFromJson(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        var batchVersion = jsonObject.containsKey("batch_version") ? BatchVersionEnum.from(jsonObject.getByte("batch_version")) : null;
        BigInteger batchIndex = new BigInteger(jsonObject.getString("batch_index"));
        long chunkIndex = jsonObject.getLong("chunk_index");
        byte[] rawChunk = Base64.decode(jsonObject.getString("raw_chunk"));
        var gasSum = jsonObject.getLong("gas_sum");
        if (ObjectUtil.isNull(gasSum)) {
            gasSum = -1L;
        }
        return new ChunkWrapper(batchVersion, batchIndex, chunkIndex, gasSum, rawChunk);
    }

    public ChunkWrapper(@NonNull BatchVersionEnum batchVersion, BigInteger batchIndex, long chunkIndex, List<BasicBlockTrace> traces) {
        this.batchVersion = batchVersion;
        this.chunk = new Chunk(traces);
        this.gasSum = 0;
        traces.forEach(
                t -> {
                    this.gasSum += t.getHeader().getGasUsed();
                }
        );
        this.batchIndex = batchIndex;
        this.chunkIndex = chunkIndex;
    }

    public ChunkWrapper(BatchVersionEnum batchVersion, BigInteger batchIndex, long chunkIndex, long gasSum, byte[] rawChunk) {
        this.batchVersion = batchVersion;
        this.batchIndex = batchIndex;
        this.chunkIndex = chunkIndex;
        this.gasSum = gasSum;
        // chunks codec upgrade from batch v2
        // so if no batch version variable, use batch v1
        this.chunk = ObjectUtil.isNull(batchVersion) ? BatchVersionEnum.BATCH_V1.getChunkCodec().deserialize(rawChunk)
                : batchVersion.getChunkCodec().deserialize(rawChunk);
    }

    private BatchVersionEnum batchVersion;

    private BigInteger batchIndex;

    private long chunkIndex;

    private Chunk chunk;

    /**
     * Has to warn you bro, some old version data has value -1 on this field
     */
    private long gasSum;

    public BigInteger getStartBlockNumber() {
        return chunk.getStartBlockNumber();
    }

    public BigInteger getEndBlockNumber() {
        return chunk.getEndBlockNumber();
    }

    public byte[] serializeChunk() {
        return batchVersion.getChunkCodec().serialize(chunk);
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("batch_version", batchVersion.getValueAsUint8());
        jsonObject.put("batch_index", batchIndex.toString());
        jsonObject.put("chunk_index", chunkIndex);
        jsonObject.put("raw_chunk", Base64.encode(batchVersion.getChunkCodec().serialize(chunk)));
        jsonObject.put("gas_sum", gasSum);
        return jsonObject.toJSONString();
    }
}
