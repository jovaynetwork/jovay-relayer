package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
            @ShellOption(help = "Raw anchor batch header in hex format", defaultValue = "") String nextL2MsgNonce,
            @ShellOption(help = "Raw anchor batch header in hex format", defaultValue = "") String l2MerkleTreeBranchesHex
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
                                                .setVersion(batchHeader.getVersion())
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
                .map(x -> {
                    Chunk chunk = Chunk.deserializeFrom(x.getRawChunk().toByteArray());
                    chunk.setHash(HexUtil.decodeHex(x.getHash()));
                    return chunk;
                }).toList();

        JSONObject res = new JSONObject();
        res.put("batchHeader", JSON.parseObject(batchHeader.toJson()));
        res.put("chunks", chunks.stream().map(Chunk::toJson).map(JSON::parseObject).collect(Collectors.toList()));

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
}
