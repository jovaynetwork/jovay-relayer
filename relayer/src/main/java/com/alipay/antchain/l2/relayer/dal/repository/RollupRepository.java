package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.*;
import com.alipay.antchain.l2.relayer.commons.exceptions.BlockPollingException;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.models.*;
import com.alipay.antchain.l2.relayer.core.tracer.TraceServiceClient;
import com.alipay.antchain.l2.relayer.dal.entities.*;
import com.alipay.antchain.l2.relayer.dal.mapper.*;
import com.alipay.antchain.l2.relayer.utils.ConvertUtil;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.utils.Numeric;

@Component
@Slf4j
public class RollupRepository implements IRollupRepository {

    private static final String L2BLOCK_TRACE_CACHE_KEY = "L2_BLOCK_TRACE@{}";

    private static final String L2CHUNK_CACHE_KEY = "L2_CHUNK@{}-{}";

    private static final String L2BATCH_ETH_BLOBS_CACHE_KEY = "L2_BATCH_BLOBS@{}";

    @Value("${l2-relayer.tasks.cache.l2-block-trace-ttl:1024000}")
    private int l2BlockTraceCacheTTL;

    @Value("${l2-relayer.tasks.cache.l2-chunk-ttl:2048000}")
    private int l2ChunkCacheTTL;

    @Value("${l2-relayer.tasks.cache.l2-batch-eth-blobs-ttl:300000}")
    private int l2BatchEthBlobsCacheTTL;

    @Value("${l2-relayer.tasks.block-polling.l2.default.start-processed-block-num-val:0}")
    private String startProcessedBlockNumberVal;

    @Resource
    private RedissonClient redisson;

    @Resource
    private ChunksMapper chunksMapper;

    @Resource
    private BatchesMapper batchesMapper;

    @Resource
    private RollupNumberRecordMapper rollupNumberRecordMapper;

    @Resource
    private BatchProveRequestMapper batchProveRequestMapper;

    @Resource
    private ReliableTransactionMapper reliableTransactionMapper;

    @Resource
    private TraceServiceClient traceServiceClient;

    @Override
    public void cacheL2BlockTrace(@NonNull BasicBlockTrace blockTrace) {
        log.info("cache l2 block trace: {}", blockTrace.getHeader().getNumber());
        redisson.getBucket(getL2blockTraceCacheKey(BigInteger.valueOf(blockTrace.getHeader().getNumber())), ByteArrayCodec.INSTANCE)
                .setIfAbsent(blockTrace.toByteArray(), Duration.ofMillis(l2BlockTraceCacheTTL));
    }

    @Override
    @SneakyThrows
    public BasicBlockTrace getL2BlockTraceFromCache(BigInteger blockNumber) {
        RBucket<byte[]> bucket = redisson.getBucket(getL2blockTraceCacheKey(blockNumber), ByteArrayCodec.INSTANCE);
        byte[] raw = bucket.get();
        if (ObjectUtil.isEmpty(raw)) {
            return null;
        }
        log.debug("get l2 block trace from cache: {}", blockNumber);
        return BasicBlockTrace.parseFrom(raw);
    }

    @Override
    public List<BasicBlockTrace> getL2BlockTraceRange(BigInteger start, BigInteger end) {
        log.info("try to get block traces from {} to {} included", start, end);
        List<BasicBlockTrace> blockTraces = new ArrayList<>();
        for (BigInteger h = start; h.compareTo(end) <= 0; h = h.add(BigInteger.ONE)) {
            log.debug("try to get block trace for height {}", h);
            blockTraces.add(getL2BlockTrace(h));
        }
        blockTraces.sort(Comparator.comparingLong(o -> o.getHeader().getNumber()));
        return blockTraces;
    }

    @Override
    public BasicBlockTrace getL2BlockTrace(BigInteger height) {
        BasicBlockTrace blockTrace = getL2BlockTraceFromCache(height);
        if (ObjectUtil.isNotNull(blockTrace)) {
            return blockTrace;
        }
        blockTrace = traceServiceClient.getBasicTrace(height);
        if (ObjectUtil.isNull(blockTrace)) {
            throw new BlockPollingException("get null block trace for height {} !", height);
        }
        if (blockTrace.getHeader().getNumber() != height.longValue()) {
            throw new BlockPollingException("block trace height not match, height: {}, blockTrace: {}", height, blockTrace.getHeader().getNumber());
        }
        cacheL2BlockTrace(blockTrace);
        return blockTrace;
    }

    @Override
    public void updateRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type, BigInteger number) {
        log.debug("update rollup number record: chainType: {}, numberType: {}, number: {}", chainType, type, number);
        RollupNumberRecordEntity entity = new RollupNumberRecordEntity();
        entity.setChainType(chainType);
        entity.setRecordType(type);
        entity.setNumber(number.toString());
        if (
                rollupNumberRecordMapper.exists(
                        new LambdaQueryWrapper<RollupNumberRecordEntity>()
                                .eq(RollupNumberRecordEntity::getChainType, chainType)
                                .eq(RollupNumberRecordEntity::getRecordType, type)
                )
        ) {
            if (
                    rollupNumberRecordMapper.update(
                            entity,
                            new LambdaUpdateWrapper<RollupNumberRecordEntity>()
                                    .eq(RollupNumberRecordEntity::getChainType, chainType)
                                    .eq(RollupNumberRecordEntity::getRecordType, type)
                    ) != 1
            ) {
                throw new RuntimeException(String.format("update to DB failed, chainType: %s, numberType: %s, number: %s", chainType, type, number));
            }
            return;
        }
        rollupNumberRecordMapper.insert(entity);
    }

    @Override
    public BigInteger getRollupNumberRecord(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type) {
        RollupNumberRecordEntity entity = rollupNumberRecordMapper.selectOne(
                new LambdaQueryWrapper<RollupNumberRecordEntity>()
                        .eq(RollupNumberRecordEntity::getChainType, chainType)
                        .eq(RollupNumberRecordEntity::getRecordType, type)
        );
        if (ObjectUtil.isNull(entity)) {
            return getDefaultRecordNumber(chainType, type);
        }
        return entity.getNumberValue();
    }

    @Override
    public RollupNumberInfo getRollupNumberInfo(ChainTypeEnum chainType, RollupNumberRecordTypeEnum type) {
        RollupNumberRecordEntity entity = rollupNumberRecordMapper.selectOne(
                new LambdaQueryWrapper<RollupNumberRecordEntity>()
                        .eq(RollupNumberRecordEntity::getChainType, chainType)
                        .eq(RollupNumberRecordEntity::getRecordType, type)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return new RollupNumberInfo(entity.getNumberValue(), entity.getGmtModified());
    }

    @Override
    public void saveChunk(ChunkWrapper chunkWrapper) {
        ChunksEntity entity = ConvertUtil.convertFromChunkWrapper(chunkWrapper);
        if (chunksMapper.exists(
                new LambdaQueryWrapper<ChunksEntity>()
                        .eq(ChunksEntity::getBatchIndex, entity.getBatchIndex())
                        .eq(ChunksEntity::getChunkIndex, entity.getChunkIndex())
        )) {
            if (chunksMapper.update(
                    entity,
                    new LambdaUpdateWrapper<ChunksEntity>()
                            .eq(ChunksEntity::getBatchIndex, entity.getBatchIndex())
                            .eq(ChunksEntity::getChunkIndex, entity.getChunkIndex())
            ) != 1) {
                throw new RuntimeException(String.format("update to DB failed, batchIndex: %s, chunkIndex: %s", chunkWrapper.getBatchIndex(), chunkWrapper.getChunkIndex()));
            }
        } else {
            chunksMapper.insert(entity);
        }

        cacheL2Chunk(chunkWrapper);
    }

    private void cacheL2Chunk(ChunkWrapper chunkWrapper) {
        log.info("cache l2 chunk: {}-{}", chunkWrapper.getBatchIndex(), chunkWrapper.getChunkIndex());
        redisson.getBucket(getL2ChunkCacheKey(chunkWrapper.getBatchIndex(), chunkWrapper.getChunkIndex()), StringCodec.INSTANCE)
                .set(chunkWrapper.toJson(), Duration.ofMillis(l2ChunkCacheTTL));
    }

    private ChunkWrapper getL2ChunkFromCache(BigInteger batchIndex, long chunkIndex) {
        RBucket<String> bucket = redisson.getBucket(getL2ChunkCacheKey(batchIndex, chunkIndex), StringCodec.INSTANCE);
        String raw = bucket.get();
        if (StrUtil.isEmpty(raw)) {
            return null;
        }
        log.debug("get l2 chunk from cache: {}-{}", batchIndex, chunkIndex);
        return ChunkWrapper.decodeFromJson(raw);
    }

    @Override
    public ChunkWrapper getChunk(BigInteger batchIndex, long chunkIndex) {
        ChunkWrapper chunkWrapper = getL2ChunkFromCache(batchIndex, chunkIndex);
        if (ObjectUtil.isNotNull(chunkWrapper)) {
            return chunkWrapper;
        }

        ChunksEntity entity = chunksMapper.selectOne(
                new LambdaQueryWrapper<ChunksEntity>()
                        .eq(ChunksEntity::getBatchIndex, batchIndex.toString())
                        .eq(ChunksEntity::getChunkIndex, chunkIndex)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return ConvertUtil.convertFromChunksEntity(entity);
    }

    @Override
    public List<ChunkWrapper> getChunks(BigInteger batchIndex) {
        List<ChunksEntity> entities = chunksMapper.selectList(
                new LambdaQueryWrapper<ChunksEntity>()
                        .eq(ChunksEntity::getBatchIndex, batchIndex.toString())
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(
                x -> new ChunkWrapper(
                        new BigInteger(x.getBatchIndex()),
                        x.getChunkIndex(),
                        HexUtil.decodeHex(x.getChunkHash()),
                        x.getZkCycleSum(),
                        x.getRawChunk()
                )
        ).sorted(Comparator.comparingLong(ChunkWrapper::getChunkIndex)).collect(Collectors.toList());
    }

    @Override
    public void saveBatch(BatchWrapper batchWrapper) {
        BatchesEntity entity = ConvertUtil.convertFromBatch(batchWrapper);
        if (batchesMapper.exists(
                new LambdaQueryWrapper<BatchesEntity>()
                        .eq(BatchesEntity::getBatchIndex, entity.getBatchIndex())
        )) {
            batchesMapper.update(
                    entity,
                    new LambdaQueryWrapper<BatchesEntity>()
                            .eq(BatchesEntity::getBatchIndex, entity.getBatchIndex())
            );
            if (ObjectUtil.isNotNull(batchWrapper.getBatch().getEthBlobs())) {
                cacheBatchBlobs(batchWrapper.getBatchIndex(), batchWrapper.getBatch().getEthBlobs());
            }
            return;
        }
        batchesMapper.insert(entity);
        if (ObjectUtil.isNotNull(batchWrapper.getBatch().getEthBlobs())) {
            cacheBatchBlobs(batchWrapper.getBatchIndex(), batchWrapper.getBatch().getEthBlobs());
        }
    }

    @Override
    public void savePartialBatchHeader(BatchHeader batchHeader, long totalL1MessagePopped, long l1MessagePopped, BigInteger startBlockNum, BigInteger endBlockNum) {
        BatchesEntity entity = ConvertUtil.convertFromBatchHeader(batchHeader, totalL1MessagePopped, l1MessagePopped, startBlockNum, endBlockNum);
        batchesMapper.insert(entity);
    }

    @Override
    public boolean hasBatch(BigInteger batchIndex) {
        return batchesMapper.exists(
                new LambdaQueryWrapper<BatchesEntity>()
                        .eq(BatchesEntity::getBatchIndex, batchIndex)
        );
    }

    @Override
    public BatchWrapper getBatch(BigInteger batchIndex) {
       return getBatch(batchIndex, true);
    }

    @Override
    public BatchWrapper getBatch(BigInteger batchIndex, boolean withChunksOrNot) {
        BatchesEntity entity = batchesMapper.selectOne(
                new LambdaQueryWrapper<BatchesEntity>()
                        .eq(BatchesEntity::getBatchIndex, batchIndex.toString())
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        if (withChunksOrNot) {
            List<ChunkWrapper> chunks = getChunks(batchIndex);
            if (ObjectUtil.isEmpty(chunks)) {
                throw new RuntimeException("empty chunks for batch " + batchIndex);
            }
            return ConvertUtil.convertFromBatchEntityAndChunks(entity, chunks, getL2BatchBlobsFromCache(batchIndex));
        }
        return ConvertUtil.convertFromBatchEntityWithoutChunks(entity);
    }

    @Override
    public BatchHeader getBatchHeader(BigInteger batchIndex) {
        BatchesEntity entity = batchesMapper.selectOne(
                new LambdaQueryWrapper<BatchesEntity>()
                        .eq(BatchesEntity::getBatchIndex, batchIndex.toString())
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }

        return ConvertUtil.convertFromBatchEntity(entity);
    }

    @Override
    public void createBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType) {
        BatchProveRequestEntity entity = new BatchProveRequestEntity();
        entity.setBatchIndex(batchIndex);
        entity.setProveType(proveType);
        entity.setState(BatchProveRequestStateEnum.PENDING);
        batchProveRequestMapper.insert(entity);
    }

    @Override
    public List<BatchProveRequestDO> peekPendingBatchProveRequest(int batchSize, ProveTypeEnum proveType) {
        List<BatchProveRequestEntity> entities = batchProveRequestMapper.selectList(
                new LambdaQueryWrapper<BatchProveRequestEntity>()
                        .eq(BatchProveRequestEntity::getState, BatchProveRequestStateEnum.PENDING)
                        .eq(ObjectUtil.isNotNull(proveType), BatchProveRequestEntity::getProveType, proveType)
                        .last("limit " + batchSize)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }

        return entities.stream().map(x -> BeanUtil.copyProperties(x, BatchProveRequestDO.class)).collect(Collectors.toList());
    }

    @Override
    public BatchProveRequestDO getBatchProveRequest(BigInteger batchIndex, ProveTypeEnum proveType) {
        BatchProveRequestEntity entity = batchProveRequestMapper.selectOne(
                new LambdaQueryWrapper<BatchProveRequestEntity>()
                        .eq(BatchProveRequestEntity::getBatchIndex, batchIndex)
                        .eq(BatchProveRequestEntity::getProveType, proveType)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return BeanUtil.copyProperties(entity, BatchProveRequestDO.class);
    }

    @Override
    public void updateBatchProveRequestState(BigInteger batchIndex, ProveTypeEnum proveType, BatchProveRequestStateEnum state) {
        BatchProveRequestEntity entity = new BatchProveRequestEntity();
        entity.setState(state);
        Assert.equals(1, batchProveRequestMapper.update(
                entity,
                new LambdaUpdateWrapper<BatchProveRequestEntity>()
                        .eq(BatchProveRequestEntity::getBatchIndex, batchIndex.toString())
                        .eq(BatchProveRequestEntity::getProveType, proveType)
        ));
    }

    @Override
    public void saveBatchProofAndUpdateReqState(BigInteger batchIndex, ProveTypeEnum proveType, byte[] rawProof) {
        BatchProveRequestEntity entity = new BatchProveRequestEntity();
        entity.setProof(rawProof);
        entity.setState(BatchProveRequestStateEnum.PROVE_READY);
        Assert.equals(1, batchProveRequestMapper.update(
                entity,
                new LambdaUpdateWrapper<BatchProveRequestEntity>()
                        .eq(BatchProveRequestEntity::getBatchIndex, batchIndex.toString())
                        .eq(BatchProveRequestEntity::getProveType, proveType)
        ));
    }

    @Override
    public void insertReliableTransaction(@NonNull ReliableTransactionDO reliableTransactionDO) {
        if (ObjectUtil.isNull(reliableTransactionDO.getState())) {
            reliableTransactionDO.setState(ReliableTransactionStateEnum.TX_PENDING);
        }
        Assert.equals(1, reliableTransactionMapper.insert(BeanUtil.copyProperties(reliableTransactionDO, ReliableTransactionEntity.class)));
    }

    @Override
    public void updateReliableTransaction(ReliableTransactionDO reliableTransactionDO) {
        ReliableTransactionEntity entity = BeanUtil.copyProperties(
                reliableTransactionDO, ReliableTransactionEntity.class,
                ReliableTransactionDO.Fields.batchIndex,
                ReliableTransactionDO.Fields.transactionType,
                ReliableTransactionDO.Fields.chainType,
                ReliableTransactionDO.Fields.originalTxHash
        );
        Assert.equals(1, reliableTransactionMapper.update(
                entity,
                new LambdaUpdateWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getChainType, reliableTransactionDO.getChainType())
                        .eq(ReliableTransactionEntity::getTransactionType, reliableTransactionDO.getTransactionType())
                        .eq(ReliableTransactionEntity::getBatchIndex, reliableTransactionDO.getBatchIndex().toString())
        ));
    }

    @Override
    public void updateReliableTransactionState(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType, ReliableTransactionStateEnum state) {
        ReliableTransactionEntity entity = new ReliableTransactionEntity();
        entity.setState(state);
        Assert.equals(1, reliableTransactionMapper.update(
                entity,
                new LambdaUpdateWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getChainType, chainType)
                        .eq(ReliableTransactionEntity::getTransactionType, transactionType)
                        .eq(ReliableTransactionEntity::getBatchIndex, batchIndex.toString())
        ));
    }

    @Override
    public void updateReliableTransactionState(String originalTxHash, ReliableTransactionStateEnum state) {
        ReliableTransactionEntity entity = new ReliableTransactionEntity();
        entity.setState(state);
        Assert.equals(1, reliableTransactionMapper.update(
                entity,
                new LambdaUpdateWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getOriginalTxHash, originalTxHash)
        ));
    }

    @Override
    public List<ReliableTransactionDO> getTxPendingReliableTransactions(int batchSize) {
        List<ReliableTransactionEntity> entities = reliableTransactionMapper.selectList(
                new LambdaQueryWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getState, ReliableTransactionStateEnum.TX_PENDING)
                        .last("limit " + batchSize)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(x -> BeanUtil.copyProperties(x, ReliableTransactionDO.class)).collect(Collectors.toList());
    }

    @Override
    public List<ReliableTransactionDO> getNotFinalizedReliableTransactions(ChainTypeEnum chainType, int batchSize) {
        var entities = reliableTransactionMapper.selectList(
                new LambdaQueryWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getChainType, chainType)
                        .and(
                                w -> w.eq(
                                                ReliableTransactionEntity::getState,
                                                ReliableTransactionStateEnum.TX_PENDING)
                                        .or(wrapper -> wrapper.eq(
                                                ReliableTransactionEntity::getState,
                                                ReliableTransactionStateEnum.TX_PACKAGED
                                        ))
                        ).last("limit " + batchSize)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(x -> BeanUtil.copyProperties(x, ReliableTransactionDO.class)).collect(Collectors.toList());
    }

    @Override
    public List<ReliableTransactionDO> getFailedReliableTransactions(int batchSize, int retryCountLimit) {
        var entities = reliableTransactionMapper.selectList(
                new LambdaQueryWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getState, ReliableTransactionStateEnum.TX_FAILED)
                        .lt(ReliableTransactionEntity::getRetryCount, retryCountLimit)
                        .last("limit " + batchSize)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(x -> BeanUtil.copyProperties(x, ReliableTransactionDO.class)).collect(Collectors.toList());
    }

    @Override
    public ReliableTransactionDO getReliableTransaction(@NonNull String originalTxHash) {
        originalTxHash = Numeric.prependHexPrefix(originalTxHash.toLowerCase());
        ReliableTransactionEntity entity = reliableTransactionMapper.selectOne(
                new LambdaQueryWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getOriginalTxHash, originalTxHash)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return BeanUtil.copyProperties(entity, ReliableTransactionDO.class);
    }

    @Override
    public ReliableTransactionDO getReliableTransaction(ChainTypeEnum chainType, BigInteger batchIndex, TransactionTypeEnum transactionType) {
        ReliableTransactionEntity entity = reliableTransactionMapper.selectOne(
                new LambdaQueryWrapper<ReliableTransactionEntity>()
                        .eq(ReliableTransactionEntity::getChainType, chainType)
                        .eq(ReliableTransactionEntity::getTransactionType, transactionType)
                        .eq(ReliableTransactionEntity::getBatchIndex, batchIndex.toString())
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return BeanUtil.copyProperties(entity, ReliableTransactionDO.class);
    }

    private BigInteger getDefaultRecordNumber(ChainTypeEnum chainType, RollupNumberRecordTypeEnum recordType) {
        if (chainType == ChainTypeEnum.LAYER_ONE) {
            return switch (recordType) {
                case BLOCK_PROCESSED -> null;
                default -> BigInteger.ZERO;
            };
        }
        return switch (recordType) {
            case BLOCK_PROCESSED -> new BigInteger(startProcessedBlockNumberVal);
            case NEXT_CHUNK, ZK_ROWS_ACCUMULATOR, NEXT_CHUNK_TX_COUNT, NEXT_CHUNK_CALL_DATA_COUNT, BATCH_COMMITTED ->
                    BigInteger.ZERO;
            case NEXT_BATCH, NEXT_MSG_PROVE_BATCH -> null;
        };
    }

    private void cacheBatchBlobs(BigInteger batchIndex, @NonNull EthBlobs ethBlobs) {
        redisson.getBucket(getL2BatchEthBlobsCacheKey(batchIndex), ByteArrayCodec.INSTANCE)
                .set(ethBlobs.toBytes(), Duration.ofMillis(l2BatchEthBlobsCacheTTL));
    }

    private EthBlobs getL2BatchBlobsFromCache(BigInteger batchIndex) {
        RBucket<byte[]> bucket = redisson.getBucket(getL2BatchEthBlobsCacheKey(batchIndex), ByteArrayCodec.INSTANCE);
        byte[] raw = bucket.get();
        if (ObjectUtil.isEmpty(raw)) {
            return null;
        }
        log.debug("get l2 batch blobs from cache: {}", batchIndex);
        return EthBlobs.fromBytes(raw);
    }

    private String getL2blockTraceCacheKey(BigInteger blockNumber) {
        return StrUtil.format(L2BLOCK_TRACE_CACHE_KEY, blockNumber.toString());
    }

    private String getL2ChunkCacheKey(BigInteger batchIndex, long chunkIndex) {
        return StrUtil.format(L2CHUNK_CACHE_KEY, batchIndex.toString(), Long.toString(chunkIndex));
    }

    private String getL2BatchEthBlobsCacheKey(BigInteger batchIndex) {
        return StrUtil.format(L2BATCH_ETH_BLOBS_CACHE_KEY, batchIndex.toString());
    }
}
