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

package com.alipay.antchain.l2.relayer.engine.checker;

import org.redisson.Redisson;
import org.redisson.RedissonLock;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

public class SingleThreadRedissonLock extends RedissonLock {

    public SingleThreadRedissonLock(RedissonClient redisson, String lockName) {
        super(((Redisson) redisson).getCommandExecutor(), lockName);
    }

    @Override
    public RFuture<Boolean> tryLockAsync() {
        return tryLockAsync(-1);
    }

    @Override
    public void unlock() {
        try {
            get(unlockAsync(-1));
        } catch (RedisException e) {
            if (e.getCause() instanceof IllegalMonitorStateException) {
                throw (IllegalMonitorStateException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return super.isHeldByThread(-1);
    }
}
