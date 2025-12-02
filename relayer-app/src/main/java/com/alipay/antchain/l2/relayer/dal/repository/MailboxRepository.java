package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;
import com.alipay.antchain.l2.relayer.dal.entities.InterBlockchainMessageEntity;
import com.alipay.antchain.l2.relayer.dal.mapper.InterBlockchainMessageMapper;
import com.alipay.antchain.l2.relayer.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailboxRepository implements IMailboxRepository {

    @Resource
    private InterBlockchainMessageMapper interBlockchainMessageMapper;

    @Resource
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public List<InterBlockchainMessageDO> peekReadyMessages(InterBlockchainMessageTypeEnum type, int batchSize) {
        List<InterBlockchainMessageEntity> entities = interBlockchainMessageMapper.selectList(
                new LambdaQueryWrapper<InterBlockchainMessageEntity>()
                        .eq(InterBlockchainMessageEntity::getType, type)
                        .eq(InterBlockchainMessageEntity::getState, InterBlockchainMessageStateEnum.MSG_READY)
                        .last("limit " + batchSize)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }

        return entities.stream().map(ConvertUtil::convertFromInterBlockchainMessageEntity)
                .sorted(Comparator.comparing(InterBlockchainMessageDO::getNonce))
                .collect(Collectors.toList());
    }

    @Override
    public void saveMessages(List<InterBlockchainMessageDO> messageDOList) {
        if (ObjectUtil.isEmpty(messageDOList)) {
            return;
        }
        MybatisBatch<InterBlockchainMessageEntity> mybatisBatch = new MybatisBatch<>(
                sqlSessionFactory,
                messageDOList.stream().map(ConvertUtil::convertFromInterBlockchainMessageDO).collect(Collectors.toList())
        );
        MybatisBatch.Method<InterBlockchainMessageEntity> method = new MybatisBatch.Method<>(InterBlockchainMessageMapper.class);
        mybatisBatch.execute(method.insert());
    }

    @Override
    public void updateMessageState(InterBlockchainMessageTypeEnum type, long nonce, InterBlockchainMessageStateEnum state) {
        interBlockchainMessageMapper.update(
                null,
                new LambdaUpdateWrapper<InterBlockchainMessageEntity>()
                        .set(InterBlockchainMessageEntity::getState, state)
                        .eq(InterBlockchainMessageEntity::getType, type)
                        .eq(InterBlockchainMessageEntity::getNonce, nonce)
        );
    }

    @Override
    public InterBlockchainMessageDO getMessage(InterBlockchainMessageTypeEnum type, long nonce) {
        InterBlockchainMessageEntity entity = interBlockchainMessageMapper.selectOne(
                new LambdaQueryWrapper<InterBlockchainMessageEntity>()
                        .eq(InterBlockchainMessageEntity::getType, type)
                        .eq(InterBlockchainMessageEntity::getNonce, nonce)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return ConvertUtil.convertFromInterBlockchainMessageEntity(entity);
    }

    @Override
    public void saveL2MsgProofs(Map<BigInteger, byte[]> proofs) {
        if (ObjectUtil.isEmpty(proofs)) {
            return;
        }
        MybatisBatch<InterBlockchainMessageEntity> mybatisBatch = new MybatisBatch<>(
                sqlSessionFactory,
                proofs.entrySet().stream().map(
                        x -> {
                            InterBlockchainMessageEntity entity = new InterBlockchainMessageEntity();
                            entity.setType(InterBlockchainMessageTypeEnum.L2_MSG);
                            entity.setNonce(x.getKey().longValue());
                            entity.setProof(x.getValue());
                            entity.setState(InterBlockchainMessageStateEnum.MSG_PROVED);
                            return entity;
                        }
                ).collect(Collectors.toList())
        );
        MybatisBatch.Method<InterBlockchainMessageEntity> method = new MybatisBatch.Method<>(InterBlockchainMessageMapper.class);
        mybatisBatch.execute(method.update(
                interBlockchainMessageEntity -> new LambdaUpdateWrapper<InterBlockchainMessageEntity>()
                        .set(InterBlockchainMessageEntity::getProof, interBlockchainMessageEntity.getProof())
                        .set(InterBlockchainMessageEntity::getState, interBlockchainMessageEntity.getState())
                        .eq(InterBlockchainMessageEntity::getType, interBlockchainMessageEntity.getType())
                        .eq(InterBlockchainMessageEntity::getNonce, interBlockchainMessageEntity.getNonce())
        ));
    }

    @Override
    public L2MsgProofData getL2MsgProof(BigInteger msgNonce) {
        InterBlockchainMessageEntity entity = interBlockchainMessageMapper.selectOne(
                new LambdaQueryWrapper<InterBlockchainMessageEntity>()
                        .eq(InterBlockchainMessageEntity::getType, InterBlockchainMessageTypeEnum.L2_MSG)
                        .eq(InterBlockchainMessageEntity::getNonce, msgNonce)
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }

        return ConvertUtil.convertFromL2MsgProofEntity(entity);
    }

    @Override
    public List<byte[]> getMsgHashes(InterBlockchainMessageTypeEnum type, BigInteger batchIndex) {
        List<InterBlockchainMessageEntity> entities = interBlockchainMessageMapper.selectList(
                new LambdaQueryWrapper<InterBlockchainMessageEntity>()
                        .select(ListUtil.toList(InterBlockchainMessageEntity::getMsgHash, InterBlockchainMessageEntity::getNonce))
                        .eq(InterBlockchainMessageEntity::getType, type)
                        .eq(InterBlockchainMessageEntity::getBatchIndex, batchIndex)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream()
                .sorted(Comparator.comparing(InterBlockchainMessageEntity::getNonce))
                .map(x -> HexUtil.decodeHex(x.getMsgHash()))
                .collect(Collectors.toList());
    }
}
