package com.alipay.antchain.l2.relayer.dal.mapper;

import java.util.List;

import com.alipay.antchain.l2.relayer.dal.entities.InterBlockchainMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface InterBlockchainMessageMapper extends BaseMapper<InterBlockchainMessageEntity> {

    boolean saveBatchCustom(List<InterBlockchainMessageEntity> list);
}
