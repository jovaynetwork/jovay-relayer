package com.alipay.antchain.l2.relayer.engine.checker;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultDistributedTaskChecker")
public class DefaultDistributedTaskChecker extends LocalDistributedTaskChecker {

    @Resource
    private RedissonClient redisson;

    @Override
    public void addLocalFuture(BizTaskTypeEnum taskType, CompletableFuture<Void> future) {
        super.addLocalFuture(taskType, future);
        future.whenComplete((unused, throwable) -> {
            if (ObjectUtil.isNotNull(throwable)) {
                log.error("failed to process task : {}", taskType, throwable);
            }
            try {
                RLock lock = new SingleThreadRedissonLock(redisson, getLockKey(taskType));
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("local task {} finished, unlock it", taskType);
                } else {
                    // Could be the lock used to locked with this node has been deleted somehow.
                    // For example, over the TTL and watchdog not work as usual.
                    log.error("local task {} is not locked by current node but this node run the task somehow 😨!", taskType);
                }
            } catch (Throwable t) {
                log.error("failed to unlock task : {}", taskType, t);
            }
        });
    }

    @Override
    public boolean checkIfContinue(BizTaskTypeEnum taskType) {
        if (!super.checkIfContinue(taskType)) {
            return false;
        }
        RLock lock = new SingleThreadRedissonLock(redisson, getLockKey(taskType));
        return lock.tryLock();
    }

    private String getLockKey(BizTaskTypeEnum taskType) {
        return String.format("relayer:task:%s", taskType.name());
    }
}
