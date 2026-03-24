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

package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.l2.relayer.cli.core.beacon.BeaconApiClient;
import com.alipay.antchain.l2.relayer.commons.abi.Rollup;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlobsDaData;
import com.alipay.antchain.l2.relayer.commons.l2basic.BlockContext;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.*;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

@Getter
@ShellCommandGroup(value = "Commands about rollup DA")
@ShellComponent
@Slf4j
public class DaCommands {

    /**
     * TODO: 没有将L1MsgTx的原文放到block数据中
     *
     * @param elRpcUrl
     * @param clRpcUrl
     * @param batchCommitTxHash
     * @param slot
     * @param outputDir
     * @return {@link Object }
     */
    @ShellMethod(value = "Fetch batch payload from DA without L1Msg")
    @SneakyThrows
    public Object fetchBatchPayloadFromDa(
            @ShellOption(help = "EL node rpc url") String elRpcUrl,
            @ShellOption(help = "CL node rpc url") String clRpcUrl,
            @ShellOption(help = "Txhash committed raw batch with blobs") String batchCommitTxHash,
            @ShellOption(help = "Beacon block slot where blobs are") String slot,
            @ShellOption(help = "The versioned hashed for blobs upload by Jovay", defaultValue = "") List<String> versionedHashes,
            @ShellOption(help = "The directory to save the batch data", defaultValue = "./", valueProvider = FileValueProvider.class) String outputDir
    ) {
        try (var web3j = Web3j.build(new HttpService(elRpcUrl))) {
            var receiptResp = web3j.ethGetTransactionReceipt(batchCommitTxHash).send();
            if (ObjectUtil.isNull(receiptResp)) {
                return "receipt resp is null";
            }
            if (receiptResp.hasError()) {
                return "receipt resp has error: " + receiptResp.getError().getMessage();
            }
            var events = Rollup.getCommitBatchEvents(receiptResp.getTransactionReceipt().get());
            if (ObjectUtil.isEmpty(events)) {
                return "no commit batch events found";
            }

            var batchIndex = events.get(0).batchIndex;
            var batchHash = Numeric.toHexString(events.get(0).batchHash);

            var beaconApiClient = new BeaconApiClient(clRpcUrl);
            var ethBlobs = beaconApiClient.queryBlobs(new BigInteger(slot), versionedHashes);
            if (ObjectUtil.isNull(ethBlobs)) {
                return "no blobs found";
            }

            var blobsDaData = BlobsDaData.buildFrom(new EthBlobs(ethBlobs.blobs()));
            var chunksPayload = (ChunksPayload) blobsDaData.getBatchPayload();
            chunksPayload.validate();
            if (ObjectUtil.isEmpty(chunksPayload.chunks())) {
                return "no chunks inside DA payload";
            }

            var outputDirPath = Paths.get(outputDir);
            if (!Files.exists(outputDirPath)) {
                Files.createDirectories(outputDirPath);
            }
            if (!Files.isDirectory(outputDirPath)) {
                throw new RuntimeException("output path is not a directory: " + outputDirPath);
            }
            var batchOutputPath = Paths.get(outputDir, "batch_" + batchIndex);
            if (!Files.exists(batchOutputPath)) {
                Files.createDirectories(batchOutputPath);
            }
            if (!Files.isDirectory(batchOutputPath)) {
                throw new RuntimeException("batch output path is not a directory: " + batchOutputPath);
            }

            dumpBatchMeta(batchOutputPath, chunksPayload, blobsDaData, batchIndex, batchHash);
            dumpBatchData(chunksPayload, batchOutputPath);

            return "success, plz check output dir: " + batchOutputPath;
        } catch (Exception e) {
            return "failed: " + e.getMessage();
        }
    }

    private void dumpBatchData(ChunksPayload chunksPayload, Path batchOutputPath) throws IOException {
        for (int i = 0; i < chunksPayload.chunks().size(); i++) {
            var chunk = chunksPayload.chunks().get(i);
            var chunkOutputPath = Paths.get(batchOutputPath.toUri().getPath(), "chunk_" + i);
            if (!Files.exists(chunkOutputPath)) {
                Files.createDirectories(chunkOutputPath);
            }
            if (!Files.isDirectory(chunkOutputPath)) {
                throw new RuntimeException("chunk output path is not a directory: " + chunkOutputPath);
            }

            var txList = chunk.getL2TransactionList();
            var offset = 0;
            for (int j = 0; j < chunk.getBlocks().size(); j++) {
                var block = chunk.getBlocks().get(j);
                var rawBlock = new JSONObject();
                var nonL1MsgTxCount = block.getNumTransactions() - block.getNumL1Messages();
                var rawTxList = txList.subList(offset, offset + nonL1MsgTxCount).stream().map(
                        tx -> Numeric.toHexString(TransactionEncoder.encode(tx, tx.getSignatureData()))
                ).toList();
                blockToJson(block, rawBlock);
                rawBlock.put("tx_list", rawTxList);

                Files.write(Paths.get(chunkOutputPath.toUri().getPath(), "block_" + block.getBlockNumber().toString() + ".json"),
                        JSON.toJSONBytes(rawBlock, SerializerFeature.PrettyFormat));

                offset += nonL1MsgTxCount;
            }
        }
    }

    private void dumpBatchMeta(Path batchOutputPath, ChunksPayload chunksPayload, BlobsDaData blobsDaData, BigInteger batchIndex, String batchHash) throws IOException {
        var chunkInfo = new JSONArray();
        for (int i = 0; i < chunksPayload.chunks().size(); i++) {
            var chunk = chunksPayload.chunks().get(i);
            var allTxHashes = chunk.getL2TransactionHashes().stream().map(Numeric::toHexString).toList();

            var blockArr = new JSONArray();
            var offset = 0;
            for (int j = 0; j < chunk.getBlocks().size(); j++) {
                var block = chunk.getBlocks().get(j);
                var blockMeta = new JSONObject();
                var nonL1MsgTxCount = block.getNumTransactions() - block.getNumL1Messages();
                var currTxHashes = allTxHashes.subList(offset, offset + nonL1MsgTxCount);
                blockToJson(block, blockMeta);
                blockMeta.put("tx_hashes", currTxHashes);
                blockArr.add(blockMeta);
                offset += nonL1MsgTxCount;
            }

            var chunkMeta = new JSONObject();
            chunkMeta.put("index", i);
            chunkMeta.put("start_block_number", chunk.getStartBlockNumber());
            chunkMeta.put("end_block_number", chunk.getEndBlockNumber());
            chunkMeta.put("blocks", blockArr);

            chunkInfo.add(chunkMeta);
        }

        var output = new JSONObject();
        output.put("batch_version", (int) blobsDaData.getBatchVersion().getValue());
        output.put("batch_index", batchIndex);
        output.put("batch_hash", batchHash);
        output.put("da_version", (int) blobsDaData.getDaVersion().toByte());
        output.put("chunk_info", chunkInfo);

        Files.write(Paths.get(batchOutputPath.toUri().getPath(), "batch_meta.json"), JSON.toJSONBytes(output, SerializerFeature.PrettyFormat));
    }

    private void blockToJson(BlockContext block, JSONObject blockMeta) {
        blockMeta.put("spec_version", block.getSpecVersion());
        blockMeta.put("block_number", block.getBlockNumber());
        blockMeta.put("timestamp", block.getTimestamp());
        blockMeta.put("base_fee", block.getBaseFee());
        blockMeta.put("gas_limit", block.getGasLimit());
        blockMeta.put("num_transactions", block.getNumTransactions());
        blockMeta.put("num_l1_messages", block.getNumL1Messages());
    }
}
