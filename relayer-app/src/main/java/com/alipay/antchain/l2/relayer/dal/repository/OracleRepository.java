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

package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.OracleRequestTypeEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTransactionStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.OracleTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.L1BlockFeeInfo;
import com.alipay.antchain.l2.relayer.commons.models.OracleRequestDO;
import com.alipay.antchain.l2.relayer.dal.entities.OracleRequestEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.OracleRequestMapper;
import com.alipay.antchain.l2.relayer.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@Component
@Slf4j
public class OracleRepository implements IOracleRepository {

    @Resource
    private OracleRequestMapper oracleRequestMapper;

    @Override
    public List<OracleRequestDO> peekRequests(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state, int size) {
        List<OracleRequestEntity> entities = oracleRequestMapper.selectList(
                new LambdaQueryWrapper<OracleRequestEntity>()
                        .eq(oracleType != null, OracleRequestEntity::getOracleType, oracleType)
                        .eq(requestType != null, OracleRequestEntity::getOracleTaskType, requestType)
                        .eq(OracleRequestEntity::getTxState, state)
                        .orderByDesc(true, OracleRequestEntity::getRequestIndex)
                        .last("limit " + size)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(x -> BeanUtil.copyProperties(x, OracleRequestDO.class)).collect(Collectors.toList());
    }

    @Override
    public BigInteger peekLatestRequestIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state) {
        OracleRequestEntity entity = oracleRequestMapper.selectOne(
                new QueryWrapper<OracleRequestEntity>()
                        .select("max(request_index) as request_index")
                        .eq("oracle_type", oracleType)
                        .eq("oracle_task_type", requestType)
                        .eq(ObjectUtil.isNotNull(state), "tx_state", state)
        );
        if(ObjectUtil.isEmpty(entity)) {
            return null;
        }
        return entity.getRequestIndex();
    }

    @Override
    public BigInteger peekLatestRequestIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType) {
        return peekLatestRequestIndex(oracleType, requestType, null);
    }

    @Override
    public OracleRequestDO peekLatestRequest(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state) {
        var entity = oracleRequestMapper.selectOne(
                new LambdaQueryWrapper<OracleRequestEntity>()
                        .eq(oracleType != null, OracleRequestEntity::getOracleType, oracleType)
                        .eq(requestType != null, OracleRequestEntity::getOracleTaskType, requestType)
                        .eq(OracleRequestEntity::getTxState, state)
                        .eqSql(OracleRequestEntity::getRequestIndex, "select max(request_index) from oracle_request")
        );
        if (ObjectUtil.isEmpty(entity)) {
            return null;
        }
        return BeanUtil.copyProperties(entity, OracleRequestDO.class);
    }

    @Override
    public OracleRequestDO peekRequestByTypeAndIndex(OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, String index) {
        OracleRequestEntity entity = oracleRequestMapper.selectOne(
                new LambdaQueryWrapper<OracleRequestEntity>()
                        .eq(OracleRequestEntity::getOracleType, oracleType)
                        .eq(OracleRequestEntity::getOracleTaskType, requestType)
                        .eq(OracleRequestEntity::getRequestIndex, index)
        );
        if(ObjectUtil.isNull(entity)) {
            return null;
        }
        return BeanUtil.copyProperties(entity, OracleRequestDO.class);
    }

    @Override
    public void saveBlockFeeInfo(L1BlockFeeInfo blockFeeInfo) {
        log.debug("save block header record, block height: {}", blockFeeInfo.getNumber());
        if (
                oracleRequestMapper.exists(
                        new LambdaQueryWrapper<OracleRequestEntity>()
                                .eq(OracleRequestEntity::getOracleType, OracleTypeEnum.L2_GAS_ORACLE)
                                .eq(OracleRequestEntity::getOracleTaskType, OracleRequestTypeEnum.L1_BLOCK_UPDATE)
                                .eq(OracleRequestEntity::getRequestIndex, blockFeeInfo.getNumber())
                )
        ) {
            throw new RuntimeException(String.format("save block header to DB failed, oracleType: %s, oracleRequestType: %s, blockHeight: %s", OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, blockFeeInfo.getNumber()));
        } else {
            oracleRequestMapper.insert(
                    ConvertUtil.convertFromL1BlockFeeInfo(blockFeeInfo, OracleTypeEnum.L2_GAS_ORACLE, OracleRequestTypeEnum.L1_BLOCK_UPDATE, OracleTransactionStateEnum.INIT)
            );
        }
    }

    @Override
    public void saveRollupTxReceipt(BigInteger batchIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestTypeEnum, TransactionReceipt txReceipt) {
        log.debug("create rollup gas oracle request, batchIndex: {}, oracleType: {}, oracleRequestType: {}, rollup txHash: {}", batchIndex, oracleType, requestTypeEnum, txReceipt.getTransactionHash());
        if (
                oracleRequestMapper.exists(
                        new LambdaQueryWrapper<OracleRequestEntity>()
                                .eq(OracleRequestEntity::getOracleType, oracleType)
                                .eq(OracleRequestEntity::getOracleTaskType, requestTypeEnum)
                                .eq(OracleRequestEntity::getRequestIndex, batchIndex)
                )
        ) {
            throw new RuntimeException(
                    String.format("insert rollup gas oracle request record to DB failed, batchIndex: %s, oracleType: %s, oracleRequestType: %s, txHash: %s", batchIndex.toString(), oracleType, requestTypeEnum, txReceipt.getTransactionHash())
            );
        } else {
            oracleRequestMapper.insert(
                    ConvertUtil.convertFromTxReceipt(txReceipt, batchIndex,  oracleType, requestTypeEnum, OracleTransactionStateEnum.INIT)
            );
        }
    }

    @Override
    public void updateRequestState(String requestIndex, OracleTypeEnum oracleType, OracleRequestTypeEnum requestType, OracleTransactionStateEnum state) {
        OracleRequestEntity entity = new OracleRequestEntity();
        entity.setTxState(state);
        Assert.equals(1, oracleRequestMapper.update(
                entity,
                new LambdaUpdateWrapper<OracleRequestEntity>()
                        .eq(OracleRequestEntity::getOracleType, oracleType)
                        .eq(OracleRequestEntity::getOracleTaskType, requestType)
                        .eq(OracleRequestEntity::getRequestIndex, requestIndex)
        ));
    }
}
