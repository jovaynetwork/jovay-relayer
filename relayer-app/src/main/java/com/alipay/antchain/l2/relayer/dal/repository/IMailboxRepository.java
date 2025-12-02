package com.alipay.antchain.l2.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.InterBlockchainMessageDO;

public interface IMailboxRepository {

    List<InterBlockchainMessageDO> peekReadyMessages(InterBlockchainMessageTypeEnum type, int batchSize);

    void saveMessages(List<InterBlockchainMessageDO> messageDOS);

    void updateMessageState(InterBlockchainMessageTypeEnum type, long nonce, InterBlockchainMessageStateEnum state);

    InterBlockchainMessageDO getMessage(InterBlockchainMessageTypeEnum type, long nonce);

    void saveL2MsgProofs(Map<BigInteger, byte[]> proofs);

    L2MsgProofData getL2MsgProof(BigInteger msgNonce);

    List<byte[]> getMsgHashes(InterBlockchainMessageTypeEnum type, BigInteger batchIndex);
}
