package com.alipay.antchain.l2.relayer.commons.l2basic;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.l2.relayer.commons.exceptions.InvalidChunkException;
import com.alipay.antchain.l2.relayer.commons.utils.BytesUtils;
import com.alipay.antchain.l2.relayer.commons.utils.EthTxDecoder;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import com.alipay.antchain.l2.trace.Transaction;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Hash;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;

@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chunk {

    public static Chunk deserializeFrom(byte[] raw) {
        Assert.isTrue(raw.length >= 1, "raw chunk is empty");
        Chunk chunk = new Chunk();

        int offset = 0;
        chunk.setNumBlocks(BytesUtils.getUint8(raw, offset++));
        Assert.isTrue(raw.length > chunk.getNumBlocksVal() * BlockContext.BLOCK_CONTEXT_SIZE,
                "raw chunk is too short for block contexts");

        List<BlockContext> blockContexts = new ArrayList<>();
        for (int i = 0; i < chunk.getNumBlocks(); i++) {
            blockContexts.add(BlockContext.deserializeFrom(
                    ArrayUtil.sub(raw, offset, offset += BlockContext.BLOCK_CONTEXT_SIZE)
            ));
        }
        chunk.setBlocks(blockContexts);

        if (offset < raw.length - 1) {
            chunk.setL2Transactions(ArrayUtil.sub(raw, offset, raw.length));
        }

        return chunk;
    }

    public static Chunk fromJson(String rawJson) {
        var result = new Chunk();
        var jsonObj = JSON.parseObject(rawJson);
        result.setNumBlocks(jsonObj.getByte("numBlocks"));
        result.setBlocks(jsonObj.getJSONArray("blockContexts").toJavaList(BlockContext.class));
        result.setL2Transactions(HexUtil.decodeHex(jsonObj.getString("l2Transactions")));
        result.setHash(HexUtil.decodeHex(jsonObj.getString("hash")));
        return result;
    }

    @SneakyThrows
    public Chunk(@NonNull List<BasicBlockTrace> blocks, int maxTxsInChunks) {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new IllegalArgumentException("empty blocks");
        }

        blocks.sort(Comparator.comparingLong(o -> o.getHeader().getNumber()));

        this.blocks = new ArrayList<>();
        ByteArrayOutputStream l2TxStream = new ByteArrayOutputStream();
        DataOutputStream l2TxStreamToWrite = new DataOutputStream(l2TxStream);
        List<BlockHashStream> streams = new ArrayList<>();
        for (BasicBlockTrace block : blocks) {
            this.blocks.add(Utils.convertFromBlockTrace(block));
            BlockHashStream blockHashStream = new BlockHashStream(BigInteger.valueOf(block.getHeader().getNumber()));
            block.getTransactionsList().forEach(tx -> dumpTxToChunk(tx, blockHashStream, l2TxStreamToWrite));
            streams.add(blockHashStream);
        }
        byte[] txHashes = ArrayUtil.addAll(streams.stream().map(BlockHashStream::toByteArray).toArray(byte[][]::new));
        streams.forEach(BlockHashStream::close);
        if (txHashes.length % 32 != 0) {
            throw new RuntimeException(StrUtil.format("txHashes of chunk size {} not multiple of 32", txHashes.length));
        }
        // we ignore the situation that `maxTxsInChunks` bigger than the number of txHash
        // cause that RollupAggregator will straighten it up
        if (maxTxsInChunks - txHashes.length / 32 > 0) {
            txHashes = ArrayUtil.addAll(txHashes, new byte[32 * (maxTxsInChunks - txHashes.length / 32)]);
        }
        this.numBlocks = ByteUtil.intToByte(blocks.size());
        this.l2Transactions = l2TxStream.toByteArray();
        calcChunkHashWithTxHashes(txHashes);
    }

    private byte numBlocks;

    private List<BlockContext> blocks;

    private byte[] l2Transactions;

    public byte[] hash;

    public BigInteger getStartBlockNumber() {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new RuntimeException("chunk has no block");
        }
        return blocks.get(0).getBlockNumber();
    }

    /**
     * Get the biggest block number for this chunk.
     *
     * @return included block number
     */
    public BigInteger getEndBlockNumber() {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new RuntimeException("chunk has no block");
        }
        return blocks.get(blocks.size() - 1).getBlockNumber();
    }

    public int getNumBlocksVal() {
        return numBlocks;
    }

    @SneakyThrows
    public long getL2RawTransactionTotalLength() {
        if (ObjectUtil.isEmpty(this.l2Transactions)) {
            return 0L;
        }
        long chunkTotalTxLength = 0;
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.l2Transactions));
        while (inputStream.available() > 0) {
            int size = inputStream.readInt();
            Assert.equals(size, inputStream.skipBytes(size));
            chunkTotalTxLength += size;
        }
        return chunkTotalTxLength;
    }

    @SneakyThrows
    public List<SignedRawTransaction> getL2TransactionList() {
        if (ObjectUtil.isEmpty(this.l2Transactions)) {
            return ListUtil.empty();
        }
        List<SignedRawTransaction> transactions = new ArrayList<>();
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.l2Transactions));
        while (inputStream.available() > 0) {
            int size = inputStream.readInt();
            byte[] rawTx = new byte[size];
            Assert.equals(size, inputStream.read(rawTx));
            transactions.add((SignedRawTransaction) EthTxDecoder.decode(HexUtil.encodeHexStr(rawTx)));
        }
        return transactions;
    }

    @SneakyThrows
    public List<byte[]> getL2TransactionHashes() {
        if (ObjectUtil.isEmpty(this.l2Transactions)) {
            return new ArrayList<>();
        }
        List<byte[]> hashes = new ArrayList<>();
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.l2Transactions));
        while (inputStream.available() > 0) {
            int size = inputStream.readInt();
            byte[] rawTx = new byte[size];
            Assert.equals(size, inputStream.read(rawTx));
            hashes.add(Hash.sha3(rawTx));
        }
        return hashes;
    }

    public void validate() {
        // continuous block numbers
        for (int i = 0; i < this.blocks.size() - 1; i++) {
            if (!this.blocks.get(i + 1).getBlockNumber().subtract(this.blocks.get(i).getBlockNumber()).equals(BigInteger.ONE)) {
                throw new InvalidChunkException("discontinuous blocks in chunk");
            }
        }
    }

    @SneakyThrows
    public byte[] serialize() {
        Assert.equals((int) numBlocks, blocks.size(), "num of blocks not equal with the list");
        blocks = blocks.stream().sorted(Comparator.comparing(BlockContext::getBlockNumber)).collect(Collectors.toList());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        stream.writeByte(numBlocks);
        for (BlockContext block : blocks) {
            stream.write(block.serialize());
        }
        if (ObjectUtil.isNotEmpty(l2Transactions)) {
            stream.write(l2Transactions);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("numBlocks", numBlocks);

        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(this.blocks);
        jsonObject.put("blockContexts", jsonArray);

        jsonObject.put("l2Transactions", HexUtil.encodeHexStr(ObjectUtil.defaultIfNull(l2Transactions, "".getBytes())));
        jsonObject.put("hash", HexUtil.encodeHexStr(getHash()));
        return jsonObject.toString();
    }

    private void dumpTxToChunk(Transaction tx, BlockHashStream blockHashStream, DataOutputStream l2TxStreamToWrite) {
        switch (tx.getType()) {
            case TRANSACTION_TYPE_LEGACY:
                try {
                    blockHashStream.writeL2TxHash(tx.getLegacyTx().getTxHash().getValue().toByteArray());
                    SignedRawTransaction signedRawTransaction = Utils.convertFromLegacyProtoTx(tx.getLegacyTx());
                    byte[] rawTx = TransactionEncoder.encode(signedRawTransaction, signedRawTransaction.getSignatureData());
                    l2TxStreamToWrite.writeInt(rawTx.length);
                    l2TxStreamToWrite.write(rawTx);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case TRANSACTION_TYPE_EIP_1559:
                try {
                    blockHashStream.writeL2TxHash(tx.getEip1559Tx().getTxHash().getValue().toByteArray());
                    SignedRawTransaction signedRawTransaction = Utils.convertFromEip1559ProtoTx(tx.getEip1559Tx());
                    byte[] rawTx = TransactionEncoder.encode(signedRawTransaction, signedRawTransaction.getSignatureData());
                    l2TxStreamToWrite.writeInt(rawTx.length);
                    l2TxStreamToWrite.write(rawTx);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case TRANSACTION_TYPE_EIP_2930:
                try {
                    blockHashStream.writeL2TxHash(tx.getEip2930Tx().getTxHash().getValue().toByteArray());
                    SignedRawTransaction signedRawTransaction = Utils.convertFromEip2930ProtoTx(tx.getEip2930Tx());
                    byte[] rawTx = TransactionEncoder.encode(signedRawTransaction, signedRawTransaction.getSignatureData());
                    l2TxStreamToWrite.writeInt(rawTx.length);
                    l2TxStreamToWrite.write(rawTx);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case TRANSACTION_TYPE_L1_MESSAGE:
                blockHashStream.writeL1MsgHash(tx.getL1MsgTx().getTxHash().getValue().toByteArray());
                break;
            default:
                throw new RuntimeException(StrUtil.format("tx type {} not support", tx.getType()));
        }
    }

    @SneakyThrows
    private void calcChunkHashWithTxHashes(byte[] txHashes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream streamToWrite = new DataOutputStream(stream);
        this.blocks.forEach(
                x -> {
                    try {
                        streamToWrite.write(x.serialize());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        if (ObjectUtil.isNotEmpty(txHashes)) {
            streamToWrite.write(txHashes);
        }
        this.hash = Hash.sha3(stream.toByteArray());
    }

    private static class BlockHashStream implements Closeable {

        @Getter
        private final BigInteger blockHeight;

        private final ByteArrayOutputStream l2TxHashStream;
        private final DataOutputStream l2TxHashStreamToWrite;
        private final ByteArrayOutputStream l1MsgTxHashStream;
        private final DataOutputStream l1MsgTxHashStreamToWrite;

        public BlockHashStream(BigInteger blockHeight) {
            this.blockHeight = blockHeight;
            l2TxHashStream = new ByteArrayOutputStream();
            l2TxHashStreamToWrite = new DataOutputStream(l2TxHashStream);
            l1MsgTxHashStream = new ByteArrayOutputStream();
            l1MsgTxHashStreamToWrite = new DataOutputStream(l1MsgTxHashStream);
        }

        @SneakyThrows
        public void writeL1MsgHash(byte[] hash) {
            l1MsgTxHashStreamToWrite.write(hash);
        }

        @SneakyThrows
        public void writeL2TxHash(byte[] hash) {
            l2TxHashStreamToWrite.write(hash);
        }

        @SneakyThrows
        public byte[] toByteArray() {
            l1MsgTxHashStreamToWrite.write(l2TxHashStream.toByteArray());
            if (l1MsgTxHashStream.size() % 32 != 0) {
                throw new RuntimeException(StrUtil.format("txHashStream size {} not multiple of 32", l1MsgTxHashStream.size()));
            }
            return l1MsgTxHashStream.toByteArray();
        }

        @Override
        @SneakyThrows
        public void close() {
            this.l2TxHashStreamToWrite.close();
            this.l1MsgTxHashStreamToWrite.close();
            this.l2TxHashStream.close();
            this.l1MsgTxHashStream.close();
        }
    }
}
