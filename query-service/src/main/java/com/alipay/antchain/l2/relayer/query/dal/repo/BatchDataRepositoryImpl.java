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
