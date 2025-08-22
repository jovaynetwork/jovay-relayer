package com.alipay.antchain.l2.relayer.utils;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.Batch;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.dal.entities.*;
import lombok.NonNull;
import org.web3j.utils.Numeric;

public class ConvertUtil {

    public static String listToString(List<String> list) {
        return list.stream().reduce((s1, s2) -> StrUtil.join("^", s1, s2)).orElse("");
    }

    public static List<String> stringToList(String str) {
        return StrUtil.split(str, "^");
    }

    public static BizDistributedTask convertFromBizDTTaskEntity(BizTaskEntity entity) {
        BizDistributedTask bizDistributedTask = new BizDistributedTask();
        bizDistributedTask.setNodeId(entity.getNodeId());
        bizDistributedTask.setTaskType(entity.getTaskType());
        bizDistributedTask.setStartTime(entity.getStartTimestamp().getTime());
        return bizDistributedTask;
    }

    public static BizTaskEntity convertFromBizDistributedTask(BizDistributedTask task) {
        BizTaskEntity entity = new BizTaskEntity();
        entity.setTaskType(task.getTaskType());
        entity.setNodeId(task.getNodeId());
        entity.setStartTimestamp(new Date(task.getStartTime()));
        return entity;
    }

    public static ActiveNode convertFromDTActiveNodeEntityActiveNode(ActiveNodeEntity entity) {
        ActiveNode node = new ActiveNode();
        node.setNodeIp(entity.getNodeIp());
        node.setNodeId(entity.getNodeId());
        node.setLastActiveTime(entity.getLastActiveTime().getTime());
        node.setStatus(entity.getStatus());
        return node;
    }

    public static ChunksEntity convertFromChunkWrapper(final ChunkWrapper chunkWrapper) {
        ChunksEntity entity = new ChunksEntity();
        entity.setBatchIndex(chunkWrapper.getBatchIndex().toString());
        entity.setChunkIndex(chunkWrapper.getChunkIndex());
        entity.setZkCycleSum(chunkWrapper.getZkCycleSum());
        entity.setStartNumber(chunkWrapper.getChunk().getStartBlockNumber().toString());
        entity.setEndNumber(chunkWrapper.getChunk().getEndBlockNumber().toString());
        entity.setNumBlocks(chunkWrapper.getChunk().getNumBlocksVal());
        entity.setRawChunk(chunkWrapper.getChunk().serialize());
        entity.setChunkHash(HexUtil.encodeHexStr(chunkWrapper.getChunk().getHash()));
        return entity;
    }

    public static ChunkWrapper convertFromChunksEntity(ChunksEntity entity) {
        return new ChunkWrapper(
                new BigInteger(entity.getBatchIndex()),
                entity.getChunkIndex(),
                HexUtil.decodeHex(entity.getChunkHash()),
                entity.getZkCycleSum(),
                entity.getRawChunk()
        );
    }

    public static BatchesEntity convertFromBatch(BatchWrapper batchWrapper) {
        BatchesEntity entity = new BatchesEntity();
        entity.setVersion((int) batchWrapper.getBatchHeader().getVersion());
        entity.setBatchHeaderHash(HexUtil.encodeHexStr(batchWrapper.getBatchHeader().getHash()));
        entity.setBatchIndex(batchWrapper.getBatchHeader().getBatchIndex().toString());
        entity.setL1MsgRollingHash(HexUtil.encodeHexStr(batchWrapper.getBatchHeader().getL1MsgRollingHash()));
        entity.setL1MessagePopped(batchWrapper.getL1MessagePopped());
        entity.setTotalL1MessagePopped(batchWrapper.getTotalL1MessagePopped());
        entity.setDataHash(HexUtil.encodeHexStr(batchWrapper.getBatchHeader().getDataHash()));
        entity.setParentBatchHash(HexUtil.encodeHexStr(batchWrapper.getBatchHeader().getParentBatchHash()));
        entity.setStartNumber(batchWrapper.getBatch().getStartBlockNumber().toString());
        entity.setEndNumber(batchWrapper.getBatch().getEndBlockNumber().toString());
        entity.setChunkNum(batchWrapper.getBatch().getChunks().size());
        entity.setPostStateRoot(HexUtil.encodeHexStr(batchWrapper.getPostStateRoot()));
        entity.setL2MsgRoot(HexUtil.encodeHexStr(batchWrapper.getL2MsgRoot()));
        return entity;
    }

    public static BatchesEntity convertFromBatchHeader(
            BatchHeader batchHeader,
            long totalL1MessagePopped,
            long l1MessagePopped,
            BigInteger startBlockNum,
            BigInteger endBlockNum
    ) {
        BatchesEntity entity = new BatchesEntity();
        entity.setVersion((int) batchHeader.getVersion());
        entity.setBatchHeaderHash(HexUtil.encodeHexStr(batchHeader.getHash()));
        entity.setBatchIndex(batchHeader.getBatchIndex().toString());
        entity.setL1MsgRollingHash(HexUtil.encodeHexStr(batchHeader.getL1MsgRollingHash()));
        entity.setL1MessagePopped(l1MessagePopped);
        entity.setTotalL1MessagePopped(totalL1MessagePopped);
        entity.setDataHash(HexUtil.encodeHexStr(batchHeader.getDataHash()));
        entity.setParentBatchHash(HexUtil.encodeHexStr(batchHeader.getParentBatchHash()));
        entity.setStartNumber(startBlockNum.toString());
        entity.setEndNumber(endBlockNum.toString());
        entity.setPostStateRoot(HexUtil.encodeHexStr(new byte[32]));
        entity.setL2MsgRoot(HexUtil.encodeHexStr(new byte[32]));
        return entity;
    }

    public static BatchWrapper convertFromBatchEntityAndChunks(BatchesEntity entity, @NonNull List<ChunkWrapper> chunks, EthBlobs blobs) {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.setBatch(
                Batch.builder()
                        .batchHeader(
                                BatchHeader.builder()
                                        .version(ByteUtil.intToByte(entity.getVersion()))
                                        .batchIndex(new BigInteger(entity.getBatchIndex()))
                                        .l1MsgRollingHash(Numeric.hexStringToByteArray(entity.getL1MsgRollingHash()))
                                        .dataHash(HexUtil.decodeHex(entity.getDataHash()))
                                        .parentBatchHash(HexUtil.decodeHex(entity.getParentBatchHash()))
                                        .hash(HexUtil.decodeHex(entity.getBatchHeaderHash()))
                                        .build()
                        ).chunks(chunks.stream().map(ChunkWrapper::getChunk).collect(Collectors.toList()))
                        .blobs(blobs)
                        .build()
        );
        wrapper.setPostStateRoot(HexUtil.decodeHex(entity.getPostStateRoot()));
        wrapper.setL2MsgRoot(HexUtil.decodeHex(entity.getL2MsgRoot()));
        wrapper.setStartBlockNumber(new BigInteger(entity.getStartNumber()));
        wrapper.setEndBlockNumber(new BigInteger(entity.getEndNumber()));
        wrapper.setTotalL1MessagePopped(entity.getTotalL1MessagePopped());
        wrapper.setL1MessagePopped(entity.getL1MessagePopped());

        return wrapper;
    }

    public static BatchWrapper convertFromBatchEntityWithoutChunks(BatchesEntity entity) {
        BatchWrapper wrapper = new BatchWrapper();
        wrapper.setBatch(
                Batch.builder()
                        .batchHeader(
                                BatchHeader.builder()
                                        .version(ByteUtil.intToByte(entity.getVersion()))
                                        .batchIndex(new BigInteger(entity.getBatchIndex()))
                                        .l1MsgRollingHash(Numeric.hexStringToByteArray(entity.getL1MsgRollingHash()))
                                        .dataHash(HexUtil.decodeHex(entity.getDataHash()))
                                        .parentBatchHash(HexUtil.decodeHex(entity.getParentBatchHash()))
                                        .hash(HexUtil.decodeHex(entity.getBatchHeaderHash()))
                                        .build()
                        ).build()
        );
        wrapper.setPostStateRoot(HexUtil.decodeHex(entity.getPostStateRoot()));
        wrapper.setL2MsgRoot(HexUtil.decodeHex(entity.getL2MsgRoot()));
        wrapper.setStartBlockNumber(new BigInteger(entity.getStartNumber()));
        wrapper.setEndBlockNumber(new BigInteger(entity.getEndNumber()));
        wrapper.setTotalL1MessagePopped(entity.getTotalL1MessagePopped());
        wrapper.setL1MessagePopped(entity.getL1MessagePopped());

        return wrapper;
    }

    public static BatchHeader convertFromBatchEntity(BatchesEntity entity) {
        return BatchHeader.builder()
                .version(ByteUtil.intToByte(entity.getVersion()))
                .batchIndex(new BigInteger(entity.getBatchIndex()))
                .l1MsgRollingHash(Numeric.hexStringToByteArray(entity.getL1MsgRollingHash()))
                .dataHash(HexUtil.decodeHex(entity.getDataHash()))
                .parentBatchHash(HexUtil.decodeHex(entity.getParentBatchHash()))
                .hash(HexUtil.decodeHex(entity.getBatchHeaderHash()))
                .build();
    }

    public static InterBlockchainMessageDO convertFromInterBlockchainMessageEntity(InterBlockchainMessageEntity entity) {
        return InterBlockchainMessageDO.builder()
                .msgHash(HexUtil.decodeHex(entity.getMsgHash()))
                .nonce(BigInteger.valueOf(entity.getNonce()))
                .type(entity.getType())
                .sourceBlockHeight(new BigInteger(entity.getSourceBlockHeight()))
                .sourceTxHash(entity.getSourceTxHash())
                .sender(entity.getSender())
                .receiver(entity.getReceiver())
                .rawMessage(entity.getRawMessage())
                .state(entity.getState())
                .build();
    }

    public static InterBlockchainMessageEntity convertFromInterBlockchainMessageDO(@NonNull InterBlockchainMessageDO interBlockchainMessageDO) {
        InterBlockchainMessageEntity entity = new InterBlockchainMessageEntity();
        entity.setType(interBlockchainMessageDO.getType());
        entity.setBatchIndex(interBlockchainMessageDO.getBatchIndex());
        entity.setMsgHash(interBlockchainMessageDO.getMsgHashHex());
        entity.setNonce(interBlockchainMessageDO.getNonce().longValue());
        entity.setSender(interBlockchainMessageDO.getSender());
        entity.setReceiver(interBlockchainMessageDO.getReceiver());
        entity.setSourceBlockHeight(interBlockchainMessageDO.getSourceBlockHeight().toString());
        entity.setSourceTxHash(interBlockchainMessageDO.getSourceTxHash());
        entity.setRawMessage(interBlockchainMessageDO.getRawMessage());
        entity.setState(interBlockchainMessageDO.getState());
        return entity;
    }

    public static L2MsgProofData convertFromL2MsgProofEntity(InterBlockchainMessageEntity entity) {
        return L2MsgProofData.builder()
                .msgNonce(BigInteger.valueOf(entity.getNonce()))
                .batchIndex(entity.getBatchIndex())
                .merkleProof(entity.getProof())
                .build();
    }

    public static AppendMerkleTree convertFromL2MerkleTreeEntity(L2MerkleTreeEntity entity) {
        return new AppendMerkleTree(entity.getNextMsgNonce(), entity.getBranches());
    }
}
