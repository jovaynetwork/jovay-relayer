package com.alipay.antchain.l2.relayer.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
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
    public static byte[] serializeChunks(List<Chunk> chunks) {
        var rawChunksStream = new ByteArrayOutputStream();
        var streamToWrite = new DataOutputStream(rawChunksStream);
        chunks.forEach(
                chunk -> {
                    var raw = chunk.serialize();
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

    public static List<Chunk> deserializeChunks(byte[] rawChunks) {
        var res = new ArrayList<Chunk>();
        var nextLen = BytesUtils.getUint32(rawChunks, 0);
        var offset = 4;
        while (nextLen != 0) {
            res.add(Chunk.deserializeFrom(BytesUtils.getBytes(rawChunks, offset, (int) nextLen)));
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
