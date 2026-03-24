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

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.dal.entities.ActiveNodeEntity;
import com.alipay.antchain.l2.relayer.dal.entities.BizTaskEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.BizTaskMapper;
import com.alipay.antchain.l2.relayer.dal.mapper.ActiveNodeMapper;
import com.alipay.antchain.l2.relayer.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ScheduleRepository implements IScheduleRepository {

    private static final String SCHEDULE_LOCK_KEY = "L2_RELAYER_SCHEDULE_LOCK";

    @Resource
    private RedissonClient redisson;

    @Resource
    private ActiveNodeMapper activeNodeMapper;

    @Resource
    private BizTaskMapper bizTaskMapper;

    @Override
    public Lock getDispatchLock() {
        return redisson.getLock(SCHEDULE_LOCK_KEY);
    }

    @Override
    @Synchronized
    public void activate(String nodeId, String nodeIp) {
        try {
            if (
                    1 != activeNodeMapper.update(
                            ActiveNodeEntity.builder()
                                    .nodeId(nodeId)
                                    .nodeIp(nodeIp)
                                    .lastActiveTime(new Date())
                                    .status(ActiveNodeStatusEnum.ONLINE)
                                    .build(),
                            new LambdaUpdateWrapper<ActiveNodeEntity>()
                                    .eq(ActiveNodeEntity::getNodeId, nodeId)
                    )
            ) {
                activeNodeMapper.insert(
                        ActiveNodeEntity.builder()
                                .nodeId(nodeId)
                                .nodeIp(nodeIp)
                                .lastActiveTime(new Date())
                                .status(ActiveNodeStatusEnum.ONLINE)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format("failed to activate node ( id: {}, ip: {} )", nodeId, nodeIp),
                    e
            );
        }
    }

    @Override
    public List<ActiveNode> getAllActiveNodes() {
        try {
            return activeNodeMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromDTActiveNodeEntityActiveNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to get all active nodes",
                    e
            );
        }
    }

    @Override
    public ActiveNode getActiveNodeByNodeId(String nodeId) {
        try {
            ActiveNodeEntity entity = activeNodeMapper.selectOne(
                    new LambdaQueryWrapper<ActiveNodeEntity>().eq(ActiveNodeEntity::getNodeId, nodeId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromDTActiveNodeEntityActiveNode(entity);
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    e,
                    "failed to get active node by node id {}",
                    nodeId
            );
        }
    }

    @Override
    public void updateStatusOfActiveNodes(List<ActiveNode> nodes) {
        try {
            nodes.forEach(
                    x -> activeNodeMapper.update(
                            ActiveNodeEntity.builder()
                                    .nodeId(x.getNodeId())
                                    .status(x.getStatus())
                                    .build(),
                            new LambdaUpdateWrapper<ActiveNodeEntity>()
                                    .eq(ActiveNodeEntity::getNodeId, x.getNodeId())
                    )
            );
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to update status for active nodes",
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getAllBizDistributedTasks() {
        try {
            return bizTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get all biz distributed tasks",
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId) {
        try {
            return bizTaskMapper.selectList(
                            new LambdaQueryWrapper<BizTaskEntity>()
                                    .eq(BizTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get biz distributed tasks for node " + nodeId,
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = L2RelayerException.class)
    public void batchInsertBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    bizDistributedTask -> bizTaskMapper.insert(
                            ConvertUtil.convertFromBizDistributedTask(
                                    bizDistributedTask
                            )
                    )
            );
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getTaskType)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = L2RelayerException.class)
    public void batchUpdateBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> bizTaskMapper.update(
                            BizTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .startTimestamp(new Date(task.getStartTime()))
                                    .build(),
                            new LambdaUpdateWrapper<BizTaskEntity>()
                                    .eq(BizTaskEntity::getTaskType, task.getTaskType())
                    )
            );
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getTaskType)
                    ),
                    e
            );
        }
    }
}
