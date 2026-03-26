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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.l2.relayer.commons.enums.ChainTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.TransactionTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.Chunk;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

@Getter
@ShellCommandGroup(value = "Commands about core functions")
@ShellComponent
@Slf4j
public class CoreCommands extends BaseCommands {

    @Value("${grpc.client.admin.address:static://localhost:7088}")
    private String adminAddress;

    @GrpcClient("admin")
    private AdminServiceGrpc.AdminServiceBlockingStub adminServiceBlockingStub;

    @Override
    public boolean needAdminServer() {
        return true;
    }

    @ShellMethod(value = "Initialize the relayer with the start anchor batch")
    Object initAnchorBatch(
            @ShellOption(help = "Index of anchor batch", defaultValue = "") String anchorBatchIndex,
            @ShellOption(help = "Raw anchor batch header in hex format", defaultValue = "") String rawAnchorBatchHeaderHex,
            @ShellOption(help = "Next L2 message nonce", defaultValue = "") String nextL2MsgNonce,
            @ShellOption(help = "L2 merkle tree branches in hex format", defaultValue = "") String l2MerkleTreeBranchesHex
    ) {
        try {
            if (StrUtil.isNotEmpty(anchorBatchIndex)) {
                Response resp = adminServiceBlockingStub.initAnchorBatch(
                        InitAnchorBatchReq.newBuilder()
                                .setAnchorBatchIndex(new BigInteger(anchorBatchIndex).longValue())
                                .build()
                );
                if (resp.getCode() != 0) {
                    return "failed to init: " + resp.getErrorMsg();
                }
            } else if (StrUtil.isNotEmpty(rawAnchorBatchHeaderHex)) {
                BatchHeader batchHeader = BatchHeader.deserializeFrom(HexUtil.decodeHex(rawAnchorBatchHeaderHex));
                AppendMerkleTree merkleTree = l2MerkleTreeBranchesHex.isEmpty() ? null : new AppendMerkleTree(new BigInteger(nextL2MsgNonce), l2MerkleTreeBranchesHex);
                Response resp = adminServiceBlockingStub.initAnchorBatch(
                        InitAnchorBatchReq.newBuilder()
                                .setBatchHeaderInfo(
                                        BatchHeaderInfo.newBuilder()
                                                .setVersion(batchHeader.getVersion().getValueAsUint8())
                                                .setBatchIndex(batchHeader.getBatchIndex().longValue())
                                                .setDataHash(HexUtil.encodeHexStr(batchHeader.getDataHash()))
                                                .setParentBatchHash(HexUtil.encodeHexStr(batchHeader.getParentBatchHash()))
                                                .setL1MsgRollingHash(Numeric.toHexString(batchHeader.getL1MsgRollingHash()))
                                                .build()
                                ).setAnchorMerkleTree(
                                        L2MerkleTree.newBuilder()
                                                .setNextMsgNonce(ObjectUtil.isNull(merkleTree) ? NumberUtil.parseLong(nextL2MsgNonce, 0L) : merkleTree.getNextMessageNonce().longValue())
                                                .addAllBranches(
                                                        ObjectUtil.isNull(merkleTree) ? new ArrayList<>() :
                                                                Arrays.stream(merkleTree.getBranches())
                                                                        .map(Bytes32::getValue).map(ByteString::copyFrom)
                                                                        .collect(Collectors.toList())
                                                ).build()
                                ).build()
                );
                if (resp.getCode() != 0) {
                    return "failed to init: " + resp.getErrorMsg();
                }
            }

            return "success";
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "Set the start height for L1 polling")
    Object setL1StartHeight(
            @ShellOption(help = "The start height for L1 polling", defaultValue = "1") String startHeight
    ) {
        try {
            if (!NumberUtil.isInteger(startHeight)) {
                return "not a integer: " + startHeight;
            }
            Response resp = adminServiceBlockingStub.setL1StartHeight(
                    SetL1StartHeightReq.newBuilder()
                            .setStartHeight(startHeight)
                            .build()
            );
            if (resp.getCode() != 0) {
                return "failed to init: " + resp.getErrorMsg();
            }
            return "success";
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "Get the batch by the specific index")
    Object getBatch(
            @ShellOption(help = "Index of batch") String batchIndex,
            @ShellOption(help = "File to save the batch json", valueProvider = FileValueProvider.class, defaultValue = "") String filePath
    ) {
        Response response = adminServiceBlockingStub.getRawBatch(GetRawBatchReq.newBuilder().setBatchIndex(new BigInteger(batchIndex).toString()).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }

        BatchHeader batchHeader = BatchHeader.deserializeFrom(response.getGetRawBatchResp().getBatchHeader().toByteArray());
        List<Chunk> chunks = response.getGetRawBatchResp().getChunksList().stream()
                .map(x -> batchHeader.getVersion().getChunkCodec().deserialize(x.getRawChunk().toByteArray())).toList();
        var daInfo = response.getGetRawBatchResp().getDaInfo();

        JSONObject res = new JSONObject();
        res.put("batchHeader", JSON.parseObject(batchHeader.toJson()));
        res.put("chunks", chunks.stream().map(Chunk::toJson).map(JSON::parseObject).collect(Collectors.toList()));
        var daInfoObj = new JSONObject();
        daInfoObj.put("compressed", daInfo.getCompressed());
        daInfoObj.put("compressionRatio", daInfo.getCompressionRatio());
        daInfoObj.put("txCount", daInfo.getTxCount());
        var blobInfoObj = new JSONObject();
        blobInfoObj.put("blobSize", daInfo.getBlobInfo().getBlobSize());
        blobInfoObj.put("validBlobBytesSize", daInfo.getBlobInfo().getValidBlobBytesSize());
        daInfoObj.put("blobInfo", blobInfoObj);
        res.put("daInfo", daInfoObj);

        return saveToFileAndReturn(filePath, res);
    }

    @ShellMethod(value = "Get the proof by the specific message nonce")
    Object getL2MsgProof(
            @ShellOption(help = "Nonce of L2 message") String messageNonce,
            @ShellOption(help = "File to save the batch json", valueProvider = FileValueProvider.class, defaultValue = "") String filePath
    ) {
        Response response = adminServiceBlockingStub.getL2MsgProof(GetL2MsgProofReq.newBuilder().setMessageNonce(new BigInteger(messageNonce).toString()).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }

        JSONObject res = new JSONObject();
        res.put("batchIndex", response.getGetL2MsgProofResp().getBatchIndex());
        byte[] proofs = response.getGetL2MsgProofResp().getProof().toByteArray();
        res.put("proof", HexUtil.encodeHexStr(proofs));
        res.put("messageNonce", messageNonce);

        return saveToFileAndReturn(filePath, res);
    }

    @ShellMethod(value = "Retry the failed batch txs")
    Object retryBatchTx(
            @ShellOption(help = "Type of tx", valueProvider = EnumValueProvider.class) TransactionTypeEnum type,
            @ShellOption(help = "Index of from batch") long fromBatchIndex,
            @ShellOption(help = "Index of to batch") long toBatchIndex
    ) {
        var response = adminServiceBlockingStub.retryBatchTx(
                RetryBatchTxReq.newBuilder().setType(type.name())
                        .setFromBatchIndex(fromBatchIndex)
                        .setToBatchIndex(toBatchIndex)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "success and relayer will retry the txs";
    }

    @ShellMethod(value = "Query tx info about the batch")
    Object queryBatchTxInfo(
            @ShellOption(help = "Type of tx", valueProvider = EnumValueProvider.class) TransactionTypeEnum type,
            @ShellOption(help = "Index of batch") long batchIndex
    ) {
        var response = adminServiceBlockingStub.queryBatchTxInfo(
                QueryBatchTxInfoReq.newBuilder().setType(type.name()).setBatchIndex(batchIndex).build()
        );
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        var txInfo = response.getQueryBatchTxInfoResp().getTxInfo();
        return JSON.toJSONString(
                TxInfo.builder()
                        .type(txInfo.getType())
                        .originalTx(txInfo.getOriginalTx())
                        .latestTx(txInfo.getLatestTx())
                        .latestSendDate(txInfo.getLatestSendDate())
                        .state(txInfo.getState())
                        .retryCount(txInfo.getRetryCount())
                        .revertReason(txInfo.getRevertReason())
                        .build()
        );
    }

    @ShellMethod(value = "Query DA info about the batch")
    Object queryBatchDaInfo(
            @ShellOption(help = "Index of batch") long batchIndex
    ) {
        var response = adminServiceBlockingStub.queryBatchDaInfo(QueryBatchDaInfoReq.newBuilder().setBatchIndex(batchIndex).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        var daInfo = response.getQueryBatchDaInfoResp().getDaInfo();
        return JSON.toJSONString(DaInfo.builder()
                .daVersion(daInfo.getDaVersion())
                .compressed(daInfo.getCompressed())
                .compressionRatio(daInfo.getCompressionRatio())
                .txCount(daInfo.getTxCount())
                .blobSize(daInfo.getBlobInfo().getBlobSize())
                .validBlobBytesSize(daInfo.getBlobInfo().getValidBlobBytesSize())
                .build()
        );
    }

    @ShellMethod(value = "Speedup specified rollup transaction with gas price setting")
    Object speedupRollupTx(
            @ShellOption(help = "Chain type of rollup transaction", valueProvider = EnumValueProvider.class, defaultValue = "LAYER_ONE") ChainTypeEnum chainType,
            @ShellOption(help = "Type of rollup transaction", valueProvider = EnumValueProvider.class) TransactionTypeEnum txType,
            @ShellOption(help = "Index of batch") long batchIndex,
            @ShellOption(help = "Max fee per gas", defaultValue = "0") long maxFeePerGas,
            @ShellOption(help = "Max priority fee per gas", defaultValue = "0") long maxPriorityFeePerGas,
            @ShellOption(help = "Max fee per blob gas", defaultValue = "0") long maxFeePerBlobGas
    ) {
        var response = adminServiceBlockingStub.speedupTx(
                SpeedupTxReq.newBuilder()
                        .setChainType(chainType.name())
                        .setType(txType.name())
                        .setBatchIndex(batchIndex)
                        .setMaxFeePerGas(maxFeePerGas)
                        .setMaxPriorityFeePerGas(maxPriorityFeePerGas)
                        .setMaxFeePerBlobGas(maxFeePerBlobGas)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "successful to speedup rollup transaction";
    }

    @ShellMethod(value = "Query relayer addresses")
    Object queryRelayerAddress() {
        var response = adminServiceBlockingStub.queryRelayerAddress(Empty.getDefaultInstance());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        var jsonObj = new JSONObject();
        jsonObj.put("l1-blob", response.getQueryRelayerAddressResp().getL1BlobAddress());
        jsonObj.put("l1-legacy", response.getQueryRelayerAddressResp().getL1LegacyAddress());
        jsonObj.put("l2", response.getQueryRelayerAddressResp().getL2Address());
        return JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat);
    }

    @ShellMethod("Waste eth account nonce")
    Object wasteEthAccountNonce(
            @ShellOption(help = "Chain type of rollup transaction", valueProvider = EnumValueProvider.class, defaultValue = "L1") ChainType chainType,
            @ShellOption(help = "Address of eth account") String address,
            @ShellOption(help = "Nonce of eth account") long nonce
    ) {
        // Display confirmation prompt using Java 17 text block
        System.out.printf("""
        
        ========== CONFIRMATION REQUIRED ==========
        Chain Type: %s
        Account Address: %s
        Nonce Value: %d
        ===========================================
        
        Do you want to proceed with this operation? (yes/no): \
        """, chainType, address, nonce);

        // Read user confirmation from standard input
        var scanner = new Scanner(System.in);
        var confirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equalsIgnoreCase(confirm) && !"y".equalsIgnoreCase(confirm)) {
            return "Operation cancelled";
        }

        var response = adminServiceBlockingStub.wasteEthAccountNonce(
                WasteEthAccountNonceReq.newBuilder()
                        .setChainType(chainType)
                        .setAddress(address)
                        .setNonce(nonce)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "successful to waste eth account nonce with txhash: " + response.getWasteEthAccountNonceResp().getTxHash();
    }

    @ShellMethod("Commit batch manually")
    Object commitBatchManually(
            @ShellOption(help = "Index of batch") long batchIndex
    ) {
        // Display confirmation prompt using Java 17 text block
        System.out.printf("""
        
        ========== CONFIRMATION REQUIRED ==========
        Batch Index: %d
        ===========================================
        
        Commit batch manually will send tx to Ethereum, please be aware of what you doing now.
        Do you want to proceed with this operation? (yes/no): \
        """, batchIndex);

        // Read user confirmation from standard input
        var scanner = new Scanner(System.in);
        var confirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equalsIgnoreCase(confirm) && !"y".equalsIgnoreCase(confirm)) {
            return "Operation cancelled";
        }

        var response = adminServiceBlockingStub.commitBatchManually(CommitBatchManuallyReq.newBuilder().setBatchIndex(batchIndex).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "success with tx: " + response.getCommitBatchManuallyResp().getTxHash();
    }

    @ShellMethod("Commit proof manually")
    Object commitProofManually(
            @ShellOption(help = "Index of batch") long batchIndex,
            @ShellOption(help = "Type of proof", valueProvider = EnumValueProvider.class) ProofType proofType
    ) {
        // Display confirmation prompt using Java 17 text block
        System.out.printf("""
        
        ========== CONFIRMATION REQUIRED ==========
        Batch Index: %d
        Proof Type: %s
        ===========================================
        
        Commit proof manually will send tx to Ethereum, please be aware of what you doing now.
        Do you want to proceed with this operation? (yes/no): \
        """, batchIndex, proofType);
        // Read user confirmation from standard input
        var scanner = new Scanner(System.in);
        var confirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equalsIgnoreCase(confirm) && !"y".equalsIgnoreCase(confirm)) {
            return "Operation cancelled";
        }

        var response = adminServiceBlockingStub.commitProofManually(CommitProofManuallyReq.newBuilder()
                .setProofType(proofType).setBatchIndex(batchIndex).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "success with tx: " + response.getCommitProofManuallyResp().getTxHash();
    }

    @ShellMethod("Query the relayer account current nonce")
    Object queryRelayerAccountNonce(
            @ShellOption(help = "Chain type, default is L1", valueProvider = EnumValueProvider.class, defaultValue = "L1") ChainType chainType,
            @ShellOption(help = "Account type, e.g. BLOB", valueProvider = EnumValueProvider.class) AccType accType
    ) {
        var response = adminServiceBlockingStub.queryCurrNonce(QueryCurrNonceReq.newBuilder()
                .setChainType(chainType).setAccType(accType).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "nonce is " + response.getQueryCurrNonceResp().getNonce();
    }

    @ShellMethod("Update the relayer nonce, only supports local cached nonce for now")
    Object updateRelayerAccountNonceManually(
            @ShellOption(help = "Chain type, default is L1", valueProvider = EnumValueProvider.class, defaultValue = "L1") ChainType chainType,
            @ShellOption(help = "Account type, e.g. BLOB", valueProvider = EnumValueProvider.class) AccType accType,
            @ShellOption(help = "New nonce") long nonce
    ) {
        var response = adminServiceBlockingStub.updateNonceManually(UpdateNonceManuallyReq.newBuilder()
                .setChainType(chainType).setAccType(accType).setNonce(nonce).build());
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "successful to update nonce";
    }

    @ShellMethod(value = "Refetch batch proofs")
    Object refetchProof(
            @ShellOption(help = "Type of proof", valueProvider = EnumValueProvider.class, defaultValue = "TEE_PROOF") ProveTypeEnum proofType,
            @ShellOption(help = "Index of from batch") String fromBatchIndex,
            @ShellOption(help = "Index of to batch, included") String toBatchIndex
    ) {
        var response = adminServiceBlockingStub.refetchProof(
                RefetchProofReq.newBuilder()
                        .setProofType(proofType.name())
                        .setFromBatchIndex(fromBatchIndex)
                        .setToBatchIndex(toBatchIndex)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed: " + response.getErrorMsg();
        }
        return "successful to refetch batch proofs";
    }

    @ShellMethod(value = "Rollback to a specific subchain height. This is a dangerous operation that will delete data!")
    Object rollbackToSubchainHeight(
            @ShellOption(help = "Target batch index to rollback to (batches >= this index will be deleted)") long targetBatchIndex,
            @ShellOption(help = "Target block height to rollback to (BLOCK_PROCESSED will be set to this value - 1)") long targetBlockHeight,
            @ShellOption(help = "L1 message nonce threshold (L1 messages with nonce > this value will be reset)") long l1MsgNonceThreshold
    ) {
        System.out.printf("""
        
        ==================== IMPORTANT PREREQUISITE ====================
        Before running this rollback command, you MUST ensure that:
        
        *** ALL OTHER RELAYER NODES (except the one this CLI is connected to) ***
        *** HAVE BEEN COMPLETELY STOPPED!                                     ***
        
        This is required to prevent data inconsistency during rollback.
        The current CLI is connected to: %s
        ================================================================
        
        Have you stopped all other Relayer nodes? (yes/no): \
        """, adminAddress);

        var scanner = new Scanner(System.in);
        var prerequisiteConfirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equalsIgnoreCase(prerequisiteConfirm) && !"y".equalsIgnoreCase(prerequisiteConfirm)) {
            return "Operation cancelled - please stop all other Relayer nodes first";
        }

        System.out.printf("""
        
        ========== DANGER: SUBCHAIN HEIGHT ROLLBACK CONFIRMATION ==========
        Target Batch Index: %d (batches >= this will be deleted)
        Target Block Height: %d (BLOCK_PROCESSED will be set to %d)
        L1 Message Nonce Threshold: %d (L1 messages with nonce > this will be reset)
        ====================================================================
        
        The system will automatically find the chunk containing block %d.
        
        WARNING: This operation will:
        1. Delete all batches with batch_index >= %d
        2. Delete chunks where batch_index > %d OR (batch_index = %d AND chunk_index >= found_chunk_index)
        3. Delete batch prove requests
        4. Delete reliable transactions for rollup
        5. Delete L2 Merkle trees
        6. Reset L1 messages to MSG_READY state
        7. Delete/Reset L2 messages based on block height
        8. Delete batch oracle requests
        
        This operation CANNOT be undone!
        
        Do you want to proceed with this operation? (yes/no): \
        """, targetBatchIndex, targetBlockHeight, targetBlockHeight - 1, l1MsgNonceThreshold,
                targetBlockHeight, targetBatchIndex, targetBatchIndex, targetBatchIndex);

        var confirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equalsIgnoreCase(confirm) && !"y".equalsIgnoreCase(confirm)) {
            return "Operation cancelled";
        }

        System.out.print("Please type 'ROLLBACK' to confirm: ");
        var secondConfirm = scanner.nextLine().trim();
        if (!"ROLLBACK".equals(secondConfirm)) {
            return "Operation cancelled - confirmation text did not match";
        }

        var response = adminServiceBlockingStub.rollbackToSubchainHeight(
                RollbackToSubchainHeightReq.newBuilder()
                        .setTargetBatchIndex(targetBatchIndex)
                        .setTargetBlockHeight(targetBlockHeight)
                        .setL1MsgNonceThreshold(l1MsgNonceThreshold)
                        .build()
        );
        if (response.getCode() != 0) {
            return "Rollback failed: " + response.getErrorMsg();
        }
        var rollbackResp = response.getRollbackToSubchainHeightResp();
        return "Subchain height rollback completed: " + rollbackResp.getSummary();
    }

    private Object saveToFileAndReturn(@ShellOption(help = "File to save the batch json", valueProvider = FileValueProvider.class, defaultValue = "") String filePath, JSONObject res) {
        if (StrUtil.isNotEmpty(filePath)) {
            try {
                Files.write(Paths.get(filePath), res.toString(SerializerFeature.PrettyFormat).getBytes());
            } catch (IOException e) {
                log.error("failed to write file: {}", filePath, e);
                return "failed to write file: " + filePath;
            }
            return "success. ";
        }

        return res.toString(SerializerFeature.PrettyFormat);
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class TxInfo {
        private String type;
        private String originalTx;
        private String latestTx;
        private String latestSendDate;
        private String state;
        private int retryCount;
        private String revertReason;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class DaInfo {
        private int daVersion;
        private boolean compressed;
        private double compressionRatio;
        private long txCount;
        private int blobSize;
        private int validBlobBytesSize;
    }
}
