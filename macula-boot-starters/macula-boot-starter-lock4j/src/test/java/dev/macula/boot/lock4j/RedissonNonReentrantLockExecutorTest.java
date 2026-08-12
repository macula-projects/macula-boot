/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * {@link RedissonNonReentrantLockExecutor} 不可重入锁执行器测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class RedissonNonReentrantLockExecutorTest {

    private RedissonClient redissonClient;
    private RLock lock;
    private RedissonNonReentrantLockExecutor executor;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redissonClient.getLock("order:1")).thenReturn(lock);
        executor = new RedissonNonReentrantLockExecutor(redissonClient);
    }

    @Test
    void rejectsReentrantAcquisition() {
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThat(executor.acquire("order:1", "ignored", 1_000, 100)).isNull();
    }

    @Test
    void returnsLockAfterSuccessfulAcquisition() throws Exception {
        when(lock.isHeldByCurrentThread()).thenReturn(false);
        when(lock.tryLock(100, 1_000, TimeUnit.MILLISECONDS)).thenReturn(true);

        assertThat(executor.acquire("order:1", "ignored", 1_000, 100)).isSameAs(lock);
    }

    @Test
    void returnsNullWhenAcquisitionIsInterruptedAndRestoresInterruptFlag() throws Exception {
        when(lock.isHeldByCurrentThread()).thenReturn(false);
        when(lock.tryLock(100, 1_000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());

        try {
            assertThat(executor.acquire("order:1", "ignored", 1_000, 100)).isNull();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void releasesLockOwnedByCurrentThread() throws Exception {
        @SuppressWarnings("unchecked") RFuture<Void> future = mock(RFuture.class);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(lock.unlockAsync()).thenReturn(future);
        when(future.get()).thenReturn(null);

        assertThat(executor.releaseLock("order:1", "ignored", lock)).isTrue();
        verify(lock).unlockAsync();
    }

    @Test
    void doesNotReleaseLockOwnedByAnotherThread() {
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        assertThat(executor.releaseLock("order:1", "ignored", lock)).isFalse();
    }
}
