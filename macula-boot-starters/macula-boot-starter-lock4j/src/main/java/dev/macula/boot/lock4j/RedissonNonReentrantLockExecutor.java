/*
 * Copyright (c) 2024 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.lock4j;

import com.baomidou.lock.executor.AbstractLockExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 不可重入锁
 *
 * @author Gordian
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonNonReentrantLockExecutor extends AbstractLockExecutor<RLock> {

    private final RedissonClient redissonClient;

    @Override
    public boolean renewal() {
        return true;
    }

    @Override
    public RLock acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        try {
            final RLock lockInstance = redissonClient.getLock(lockKey);
            if (lockInstance.isHeldByCurrentThread()) {
                return null; // 不可重入
            }
            final boolean locked = lockInstance.tryLock(acquireTimeout, expire, TimeUnit.MILLISECONDS);
            return obtainLockInstance(locked, lockInstance);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public boolean releaseLock(String key, String value, RLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            try {
                lockInstance.unlockAsync().get();
                return true;
            } catch (ExecutionException | InterruptedException e) {
                return false;
            }
        }
        return false;
    }

}
