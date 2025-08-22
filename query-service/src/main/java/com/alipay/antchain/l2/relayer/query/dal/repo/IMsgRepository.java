package com.alipay.antchain.l2.relayer.query.dal.repo;

import com.alipay.antchain.l2.relayer.commons.l2basic.L2MsgProofData;

public interface IMsgRepository {

    L2MsgProofData getL2MsgProof(long nonce);
}
