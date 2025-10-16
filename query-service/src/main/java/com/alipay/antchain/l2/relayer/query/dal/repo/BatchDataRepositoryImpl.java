package com.alipay.antchain.l2.relayer.query.dal.repo;

import java.math.BigInteger;
import jakarta.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.dal.entities.BatchesEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.BatchesMapper;
import com.alipay.antchain.l2.relayer.query.commons.BatchMeta;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

@Component
public class BatchDataRepositoryImpl implements IBatchDataRepository {

    @Resource
    private BatchesMapper batchesMapper;

    @Override
    public BatchMeta getBatchMeta(BigInteger batchIndex) {
        var entity = batchesMapper.selectOne(
                new LambdaQueryWrapper<BatchesEntity>()
                        .select(BatchesEntity::getStartNumber, BatchesEntity::getEndNumber)
                        .eq(BatchesEntity::getBatchIndex, batchIndex.toString())
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return BatchMeta.builder()
                .batchIndex(batchIndex)
                .startBlock(new BigInteger(entity.getStartNumber()))
                .endBlock(new BigInteger(entity.getEndNumber()))
                .build();
    }
}
