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

import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.dal.entities.InterBlockchainMessageEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.InterBlockchainMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

@Component
public class MsgRepositoryImpl implements IMsgRepository {

    @Resource
    private InterBlockchainMessageMapper interBlockchainMessageMapper;

    @Override
    public L2MsgProofData getL2MsgProof(long nonce) {
        InterBlockchainMessageEntity entity = interBlockchainMessageMapper.selectOne(
                new LambdaQueryWrapper<InterBlockchainMessageEntity>()
                        .eq(InterBlockchainMessageEntity::getType, InterBlockchainMessageTypeEnum.L2_MSG)
                        .eq(InterBlockchainMessageEntity::getNonce, nonce)
        );
        if (entity == null) {
            return null;
        }
        return L2MsgProofData.builder()
                .batchIndex(entity.getBatchIndex())
                .msgNonce(BigInteger.valueOf(entity.getNonce()))
                .merkleProof(entity.getProof())
                .build();
    }
}
