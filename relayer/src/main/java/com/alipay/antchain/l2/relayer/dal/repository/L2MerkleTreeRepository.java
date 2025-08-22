package com.alipay.antchain.l2.relayer.dal.repository;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.merkle.AppendMerkleTree;
import com.alipay.antchain.l2.relayer.dal.entities.L2MerkleTreeEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.L2MerkleTreeMapper;
import com.alipay.antchain.l2.relayer.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.Duration;

@Component
public class L2MerkleTreeRepository implements IL2MerkleTreeRepository{

    @Resource
    private L2MerkleTreeMapper l2MerkleTreeMapper;

    @Resource
    private RedissonClient redisson;

    @Value("${l2-relayer.tasks.cache.l2-merkle-tree-ttl:7200000}")
    private long l2MerkleTreeTTL;

    @Override
    public void saveMerkleTree(AppendMerkleTree merkleTree, BigInteger batchIndex) {
        L2MerkleTreeEntity merkleTreeEntity = new L2MerkleTreeEntity();
        merkleTreeEntity.setNextMsgNonce(merkleTree.getNextMessageNonce());
        merkleTreeEntity.setBranches(merkleTree.serializeBranch());
        merkleTreeEntity.setBatchIndex(batchIndex);
        l2MerkleTreeMapper.insert(merkleTreeEntity);

        cacheL2MerkleTree(merkleTreeEntity);
    }

    @Override
    public AppendMerkleTree getMerkleTree(BigInteger batchIndex) {
        L2MerkleTreeEntity entity = getL2MerkleTreeFromCache(batchIndex);
        if (ObjectUtil.isNotNull(entity)) {
            return ConvertUtil.convertFromL2MerkleTreeEntity(entity);
        }
        entity = l2MerkleTreeMapper.selectOne(
                new LambdaQueryWrapper<L2MerkleTreeEntity>().eq(L2MerkleTreeEntity::getBatchIndex, batchIndex)
        );
        if (ObjectUtil.isNull(entity)) {
            return AppendMerkleTree.EMPTY_TREE;
        }
        return ConvertUtil.convertFromL2MerkleTreeEntity(entity);
    }

    private void cacheL2MerkleTree(L2MerkleTreeEntity entity) {
        redisson.getBucket(getL2MerkleTreeCacheKey(entity.getBatchIndex()), StringCodec.INSTANCE)
                .setIfAbsent(JSON.toJSONString(entity), Duration.ofMillis(l2MerkleTreeTTL));
    }

    private L2MerkleTreeEntity getL2MerkleTreeFromCache(BigInteger batchIndex) {
        RBucket<String> bucket = redisson.getBucket(getL2MerkleTreeCacheKey(batchIndex), StringCodec.INSTANCE);
        String raw = bucket.get();
        if (ObjectUtil.isEmpty(raw)) {
            return null;
        }

        return JSON.parseObject(raw, L2MerkleTreeEntity.class);
    }

    private String getL2MerkleTreeCacheKey(BigInteger batchIndex) {
        return StrUtil.format("L2_MERKLE_TREE-{}", batchIndex);
    }
}
