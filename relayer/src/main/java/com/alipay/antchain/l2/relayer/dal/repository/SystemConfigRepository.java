package com.alipay.antchain.l2.relayer.dal.repository;

import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerErrorCodeEnum;
import com.alipay.antchain.l2.relayer.commons.exceptions.L2RelayerException;
import com.alipay.antchain.l2.relayer.dal.entities.SystemConfigEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Component;

@Component
public class SystemConfigRepository implements ISystemConfigRepository {

    private static final String ANCHOR_BATCH_SET_FLAG = "ANCHOR_BATCH_SET_OR_NOT";

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource(name = "systemConfigCache")
    private Cache<String, String> systemConfigCache;

    @Override
    public String getSystemConfig(String key) {
        try {
            if (systemConfigCache.containsKey(key)) {
                return systemConfigCache.get(key, false);
            }

            SystemConfigEntity entity = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfigEntity>()
                            .select(ListUtil.of(SystemConfigEntity::getConfValue))
                            .eq(SystemConfigEntity::getConfKey, key)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            systemConfigCache.put(key, entity.getConfValue());
            return entity.getConfValue();
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to get system config for {}", key),
                    e
            );
        }
    }

    @Override
    public boolean hasSystemConfig(String key) {
        try {
            return systemConfigCache.containsKey(key) || systemConfigMapper.exists(
                    new LambdaQueryWrapper<SystemConfigEntity>()
                            .eq(SystemConfigEntity::getConfKey, key)
            );
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to judge that has config about {} or not", key),
                    e
            );
        }
    }

    @Override
    public void setSystemConfig(Map<String, String> configs) {
        try {
            configs.forEach((key, value) -> {
                systemConfigMapper.insert(
                        SystemConfigEntity.builder()
                                .confKey(key)
                                .confValue(value)
                                .build()
                );
                systemConfigCache.put(key, value);
            });
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to set system config map {}", JSON.toJSONString(configs)),
                    e
            );
        }
    }

    @Override
    public void setSystemConfig(String key, String value) {
        try {
            if (hasSystemConfig(key)) {
                systemConfigMapper.update(
                        SystemConfigEntity.builder()
                                .confValue(value)
                                .build(),
                        new LambdaUpdateWrapper<SystemConfigEntity>()
                                .eq(SystemConfigEntity::getConfKey, key)
                );
            } else {
                systemConfigMapper.insert(
                        SystemConfigEntity.builder()
                                .confKey(key)
                                .confValue(value)
                                .build()
                );
            }
            systemConfigCache.put(key, value);
        } catch (Exception e) {
            throw new L2RelayerException(
                    L2RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to set system config {} : {}", key, value),
                    e
            );
        }
    }

    @Override
    public boolean isAnchorBatchSet() {
        if (!hasSystemConfig(ANCHOR_BATCH_SET_FLAG)) {
            return false;
        }
        return Boolean.parseBoolean(getSystemConfig(ANCHOR_BATCH_SET_FLAG));
    }

    @Override
    public void markAnchorBatchHasBeenSet() {
        setSystemConfig(ANCHOR_BATCH_SET_FLAG, "true");
    }
}
