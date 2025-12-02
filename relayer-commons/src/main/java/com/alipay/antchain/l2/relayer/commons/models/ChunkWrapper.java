package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSONObject;
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
        BigInteger batchIndex = new BigInteger(jsonObject.getString("batch_index"));
        long chunkIndex = jsonObject.getLong("chunk_index");
        byte[] chunkHash = HexUtil.decodeHex(jsonObject.getString("chunk_hash"));
        byte[] rawChunk = Base64.decode(jsonObject.getString("raw_chunk"));
        long zkCycleSum = jsonObject.getLong("zk_cycle_sum");
        return new ChunkWrapper(batchIndex, chunkIndex, chunkHash, zkCycleSum, rawChunk);
    }

    public ChunkWrapper(BigInteger batchIndex, long chunkIndex, List<BasicBlockTrace> traces, int maxTxsInChunks) {
        this.chunk = new Chunk(traces, maxTxsInChunks);
        this.zkCycleSum = 0;
        traces.forEach(
                t -> {
                    this.zkCycleSum += t.getZkCycles();
                }
        );
        this.batchIndex = batchIndex;
        this.chunkIndex = chunkIndex;
    }

    public ChunkWrapper(BigInteger batchIndex, long chunkIndex, byte @NonNull [] chunkHash, long zkCycleSum, byte[] rawChunk) {
        if (chunkHash.length != 32) {
            throw new IllegalArgumentException("chunk hash length is not 32");
        }
        this.batchIndex = batchIndex;
        this.chunkIndex = chunkIndex;
        this.zkCycleSum = zkCycleSum;
        this.chunk = Chunk.deserializeFrom(rawChunk);
        this.chunk.setHash(chunkHash);
    }

    private BigInteger batchIndex;

    private long chunkIndex;

    private Chunk chunk;

    private long zkCycleSum;

    public BigInteger getStartBlockNumber() {
        return chunk.getStartBlockNumber();
    }

    public BigInteger getEndBlockNumber() {
        return chunk.getEndBlockNumber();
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("batch_index", batchIndex.toString());
        jsonObject.put("chunk_index", chunkIndex);
        jsonObject.put("chunk_hash", HexUtil.encodeHexStr(Objects.requireNonNull(chunk.getHash())));
        jsonObject.put("raw_chunk", Base64.encode(chunk.serialize()));
        jsonObject.put("zk_cycle_sum", zkCycleSum);
        return jsonObject.toJSONString();
    }
}
