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

/**
 * Represents a chunk in the Layer 2 rollup system.
 * <p>
 * A chunk is a collection of consecutive blocks that are grouped together for batch processing.
 * It contains block contexts and L2 transaction data that will be submitted to Layer 1 as part of a batch.
 * <p>
 * Key components:
 * <ul>
 *   <li>Block contexts: Metadata for each block in the chunk</li>
 *   <li>L2 transactions: Serialized transaction data from all blocks</li>
 *   <li>Number of blocks: Count of blocks included in this chunk</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chunk {

    /**
     * Deserializes a chunk from raw byte array.
     * <p>
     * This method reconstructs a chunk from its serialized byte representation,
     * supporting both batch v1 and v2 formats.
     *
     * @param raw        the raw byte array containing serialized chunk data
     * @param forBatchV2 true if deserializing for batch v2 format (uses 4-byte block count),
     *                   false for v1 format (uses 1-byte block count)
     * @return the deserialized chunk instance
     * @throws IllegalArgumentException if the raw data is invalid or too short
     */
    public static Chunk deserializeFrom(byte[] raw, boolean forBatchV2) {
        Assert.isTrue(raw.length >= 1, "raw chunk is empty");
        Chunk chunk = new Chunk();

        int offset = 0;
        if (forBatchV2) {
            chunk.setNumBlocks(BytesUtils.getUint32(raw, offset));
            offset += 4;
            Assert.isTrue(raw.length >= chunk.getNumBlocks() * BlockContext.BLOCK_CONTEXT_SIZE + 4,
                    "raw chunk is too short for block contexts");
        } else {
            chunk.setNumBlocks(BytesUtils.getUint8AsInteger(raw, offset++));
            Assert.isTrue(raw.length > chunk.getNumBlocks() * BlockContext.BLOCK_CONTEXT_SIZE,
                    "raw chunk is too short for block contexts");
        }

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

    /**
     * Creates a chunk from JSON string representation.
     *
     * @param rawJson the JSON string containing chunk data
     * @return the chunk instance created from JSON
     */
    public static Chunk fromJson(String rawJson) {
        var result = new Chunk();
        var jsonObj = JSON.parseObject(rawJson);
        result.setNumBlocks(jsonObj.getByte("numBlocks"));
        result.setBlocks(jsonObj.getJSONArray("blockContexts").toJavaList(BlockContext.class));
        result.setL2Transactions(HexUtil.decodeHex(jsonObj.getString("l2Transactions")));
        return result;
    }

    /**
     * Constructs a chunk from a list of block traces.
     * <p>
     * This constructor processes block traces, extracts transactions, and builds
     * the chunk structure including block contexts and serialized transaction data.
     * Blocks must be consecutive and will be sorted by block number.
     *
     * @param blocks the list of block traces to include in this chunk
     * @throws IllegalArgumentException if the blocks list is empty or blocks are not consecutive
     * @throws RuntimeException if transaction hash size is not a multiple of 32
     */
    @SneakyThrows
    public Chunk(@NonNull List<BasicBlockTrace> blocks) {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new IllegalArgumentException("empty blocks");
        }

        blocks.sort(Comparator.comparingLong(o -> o.getHeader().getNumber()));

        this.blocks = new ArrayList<>();
        ByteArrayOutputStream l2TxStream = new ByteArrayOutputStream();
        DataOutputStream l2TxStreamToWrite = new DataOutputStream(l2TxStream);
        List<BlockHashStream> streams = new ArrayList<>();
        for (BasicBlockTrace block : blocks) {
            if (!this.blocks.isEmpty()) {
                Assert.equals(
                        this.blocks.get(this.blocks.size() - 1).getBlockNumber().add(BigInteger.ONE),
                        BigInteger.valueOf(block.getHeader().getNumber()),
                        "block number not continuous"
                );
            }
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
        this.numBlocks = blocks.size();
        this.l2Transactions = l2TxStream.toByteArray();
    }

    /**
     * The number of blocks contained in this chunk.
     */
    private long numBlocks;

    /**
     * The list of block contexts containing metadata for each block.
     */
    private List<BlockContext> blocks;

    /**
     * The serialized L2 transaction data from all blocks in this chunk.
     */
    private byte[] l2Transactions;

    /**
     * Gets the starting block number of this chunk.
     *
     * @return the first block number in this chunk
     * @throws RuntimeException if the chunk has no blocks
     */
    public BigInteger getStartBlockNumber() {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new RuntimeException("chunk has no block");
        }
        return blocks.get(0).getBlockNumber();
    }

    /**
     * Gets the ending block number of this chunk.
     * <p>
     * This represents the highest block number contained within this chunk.
     *
     * @return the last block number in this chunk
     * @throws RuntimeException if the chunk has no blocks
     */
    public BigInteger getEndBlockNumber() {
        if (ObjectUtil.isEmpty(blocks)) {
            throw new RuntimeException("chunk has no block");
        }
        return blocks.get(blocks.size() - 1).getBlockNumber();
    }

    /**
     * Calculates the total length of all raw L2 transactions in this chunk.
     * <p>
     * This method reads through the serialized transaction data and sums up
     * the size of each individual transaction.
     *
     * @return the total byte size of all L2 transactions, or 0 if no transactions exist
     */
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

    /**
     * Retrieves the list of decoded L2 transactions from this chunk.
     * <p>
     * This method deserializes the raw transaction data and decodes each transaction
     * into a SignedRawTransaction object.
     *
     * @return the list of signed raw transactions, or an empty list if no transactions exist
     */
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

    /**
     * Retrieves the list of transaction hashes for all L2 transactions in this chunk.
     * <p>
     * This method computes the SHA3 hash for each raw transaction without fully decoding them.
     *
     * @return the list of transaction hashes as byte arrays, or an empty list if no transactions exist
     */
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

    /**
     * Validates the integrity of this chunk.
     * <p>
     * This method ensures that all blocks in the chunk have consecutive block numbers
     * with no gaps.
     *
     * @throws InvalidChunkException if blocks are not consecutive
     */
    public void validate() {
        // continuous block numbers
        for (int i = 0; i < this.blocks.size() - 1; i++) {
            if (!this.blocks.get(i + 1).getBlockNumber().subtract(this.blocks.get(i).getBlockNumber()).equals(BigInteger.ONE)) {
                throw new InvalidChunkException("discontinuous blocks in chunk");
            }
        }
    }

    /**
     * Serializes this chunk to a byte array.
     * <p>
     * The serialization format depends on the batch version:
     * <ul>
     *   <li>Batch v2: Uses 4-byte block count</li>
     *   <li>Batch v1: Uses 1-byte block count (max 255 blocks)</li>
     * </ul>
     * The serialized data includes block count, block contexts, and L2 transaction data.
     *
     * @param forBatchV2 true to serialize for batch v2 format, false for v1 format
     * @return the serialized byte array
     * @throws IllegalArgumentException if block count exceeds 255 for v1 format
     */
    @SneakyThrows
    public byte[] serialize(boolean forBatchV2) {
        Assert.equals((int) numBlocks, blocks.size(), "num of blocks not equal with the list");
        blocks = blocks.stream().sorted(Comparator.comparing(BlockContext::getBlockNumber)).collect(Collectors.toList());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        if (forBatchV2) {
            stream.writeInt((int) numBlocks);
        } else {
            Assert.isTrue(numBlocks <= 255, "num of blocks too large for uint8");
            stream.writeByte((byte) numBlocks);
        }
        for (BlockContext block : blocks) {
            stream.write(block.serialize());
        }
        if (ObjectUtil.isNotEmpty(l2Transactions)) {
            stream.write(l2Transactions);
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts this chunk to a JSON string representation.
     *
     * @return the JSON string containing chunk data
     */
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("numBlocks", numBlocks);

        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(this.blocks);
        jsonObject.put("blockContexts", jsonArray);

        jsonObject.put("l2Transactions", HexUtil.encodeHexStr(ObjectUtil.defaultIfNull(l2Transactions, "".getBytes())));
        return jsonObject.toString();
    }

    /**
     * Processes and writes a transaction to the chunk's data streams.
     * <p>
     * This method handles different transaction types (Legacy, EIP-1559, EIP-2930, L1 Message)
     * and writes their hashes and raw data to the appropriate streams.
     *
     * @param tx                 the transaction to process
     * @param blockHashStream    the stream for writing transaction hashes
     * @param l2TxStreamToWrite  the stream for writing raw transaction data
     * @throws RuntimeException if the transaction type is not supported or an I/O error occurs
     */
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

    /**
     * Internal helper class for managing transaction hash streams for a single block.
     * <p>
     * This class maintains separate streams for L1 message hashes and L2 transaction hashes,
     * and combines them in the correct order for chunk serialization.
     */
    private static class BlockHashStream implements Closeable {

        /**
         * The block height/number for this hash stream.
         */
        @Getter
        private final BigInteger blockHeight;

        /**
         * Stream for L2 transaction hashes.
         */
        private final ByteArrayOutputStream l2TxHashStream;
        private final DataOutputStream l2TxHashStreamToWrite;

        /**
         * Stream for L1 message transaction hashes.
         */
        private final ByteArrayOutputStream l1MsgTxHashStream;
        private final DataOutputStream l1MsgTxHashStreamToWrite;

        /**
         * Constructs a new BlockHashStream for the specified block height.
         *
         * @param blockHeight the block number for this hash stream
         */
        public BlockHashStream(BigInteger blockHeight) {
            this.blockHeight = blockHeight;
            l2TxHashStream = new ByteArrayOutputStream();
            l2TxHashStreamToWrite = new DataOutputStream(l2TxHashStream);
            l1MsgTxHashStream = new ByteArrayOutputStream();
            l1MsgTxHashStreamToWrite = new DataOutputStream(l1MsgTxHashStream);
        }

        /**
         * Writes an L1 message transaction hash to the stream.
         *
         * @param hash the L1 message transaction hash (32 bytes)
         */
        @SneakyThrows
        public void writeL1MsgHash(byte[] hash) {
            l1MsgTxHashStreamToWrite.write(hash);
        }

        /**
         * Writes an L2 transaction hash to the stream.
         *
         * @param hash the L2 transaction hash (32 bytes)
         */
        @SneakyThrows
        public void writeL2TxHash(byte[] hash) {
            l2TxHashStreamToWrite.write(hash);
        }

        /**
         * Combines and returns all transaction hashes as a byte array.
         * <p>
         * L1 message hashes are written first, followed by L2 transaction hashes.
         * The total size must be a multiple of 32 bytes.
         *
         * @return the combined byte array of all transaction hashes
         * @throws RuntimeException if the total hash size is not a multiple of 32
         */
        @SneakyThrows
        public byte[] toByteArray() {
            l1MsgTxHashStreamToWrite.write(l2TxHashStream.toByteArray());
            if (l1MsgTxHashStream.size() % 32 != 0) {
                throw new RuntimeException(StrUtil.format("txHashStream size {} not multiple of 32", l1MsgTxHashStream.size()));
            }
            return l1MsgTxHashStream.toByteArray();
        }

        /**
         * Closes all streams associated with this BlockHashStream.
         */
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
