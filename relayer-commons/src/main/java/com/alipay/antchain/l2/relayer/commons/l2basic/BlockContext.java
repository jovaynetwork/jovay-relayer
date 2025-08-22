package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.ByteOrder;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockContext {

    public static final int BLOCK_CONTEXT_SIZE = 40;

    public static BlockContext deserializeFrom(byte[] raw) {
        BlockContext context = new BlockContext();

        int offset = 0;
        context.setSpecVersion(BytesUtils.getUint32(raw, offset));
        offset += 4;

        context.setBlockNumber(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setTimestamp(ByteUtil.bytesToLong(raw, offset, ByteOrder.BIG_ENDIAN));
        offset += 8;

        context.setBaseFee(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setGasLimit(BytesUtils.getUint64(raw, offset));
        offset += 8;

        context.setNumTransactions(BytesUtils.getUint16(raw, offset));
        offset += 2;

        context.setNumL1Messages(BytesUtils.getUint16(raw, offset));

        return context;
    }

    private long specVersion;

    private BigInteger blockNumber;

    private long timestamp;

    private BigInteger baseFee;

    private BigInteger gasLimit;

    private int numTransactions;

    private int numL1Messages;

    @SneakyThrows
    public byte[] serialize() {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var stream = new DataOutputStream(byteArrayOutputStream);

        stream.writeInt((int) specVersion);
        stream.write(BytesUtils.fromUint64(blockNumber));
        stream.writeLong(timestamp);
        stream.write(BytesUtils.fromUint64(baseFee));
        stream.write(BytesUtils.fromUint64(gasLimit));
        stream.writeShort(numTransactions);
        stream.writeShort(numL1Messages);

        return byteArrayOutputStream.toByteArray();
    }
}
