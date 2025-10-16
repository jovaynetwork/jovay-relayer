package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import lombok.*;
import org.bouncycastle.jcajce.provider.digest.Keccak;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHeader {

    public static BatchHeader deserializeFrom(byte[] raw) {
        BatchHeader header = new BatchHeader();

        int offset = 0;
        header.setVersion(BatchVersionEnum.from(BytesUtils.getUint8(raw, offset++)));

        header.setBatchIndex(BytesUtils.getUint64(raw, offset));
        offset += 8;

        header.setL1MsgRollingHash(BytesUtils.getBytes32(raw, offset));
        offset += 32;

        header.setDataHash(BytesUtils.getBytes32(raw, offset));
        offset += 32;

        header.setParentBatchHash(BytesUtils.getBytes32(raw, offset));

        return header;
    }

    private BatchVersionEnum version;

    private BigInteger batchIndex;

    private byte[] l1MsgRollingHash;

    private byte[] dataHash;

    private byte[] parentBatchHash;

    private byte[] hash;

    public byte[] getHash() {
        if (ObjectUtil.isEmpty(hash)) {
            this.hash = new Keccak.Digest256().digest(this.serialize());
        }
        return hash;
    }

    public String getHashHex() {
        return HexUtil.encodeHexStr(getHash());
    }

    @SneakyThrows
    public byte[] serialize() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        stream.writeByte(version.getValue());
        stream.write(BytesUtils.fromUint64(batchIndex));
        stream.write(l1MsgRollingHash);
        stream.write(dataHash);
        stream.write(parentBatchHash);

        return byteArrayOutputStream.toByteArray();
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        jsonObject.put("batchIndex", batchIndex.toString());
        jsonObject.put("l1MsgRollingHash", l1MsgRollingHash);
        jsonObject.put("dataHash", HexUtil.encodeHexStr(dataHash));
        jsonObject.put("parentBatchHash", HexUtil.encodeHexStr(parentBatchHash));
        jsonObject.put("hash", HexUtil.encodeHexStr(getHash()));
        return jsonObject.toString();
    }
}
