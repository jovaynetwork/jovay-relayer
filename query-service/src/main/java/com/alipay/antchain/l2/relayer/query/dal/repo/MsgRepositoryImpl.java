package com.alipay.antchain.l2.relayer.query.dal.repo;

import java.math.BigInteger;

import javax.annotation.Resource;

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
