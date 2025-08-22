package com.alipay.antchain.l2.relayer.commons.exceptions;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ProveTypeEnum;
import com.alipay.antchain.l2.status.L2ErrorCode;

public class ProofNotReadyException extends CallRemoteServiceFailedException {

    public ProofNotReadyException(ProveTypeEnum proveType, BigInteger batchIndex) {
        super(L2ErrorCode.L2_PROVER_ERROR_TASK_RUNNING, "task is running", StrUtil.format("{} proof of batch {} not ready", proveType.name(), batchIndex.toString()));
    }
}
