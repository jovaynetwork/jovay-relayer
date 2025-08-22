package com.alipay.antchain.l2.relayer.engine.checker;

import java.util.concurrent.CompletableFuture;

import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;

public interface IDistributedTaskChecker {

    void addLocalFuture(BizTaskTypeEnum taskType, CompletableFuture<Void> future);

    boolean checkIfContinue(BizTaskTypeEnum taskType);
}
