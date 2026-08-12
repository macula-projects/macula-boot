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

import com.baomidou.lock.executor.LockExecutor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link Lock4JAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class Lock4JAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(Lock4JAutoConfiguration.class)
        .withBean(RedissonClient.class, () -> mock(RedissonClient.class));

    @Test
    void createsNonReentrantExecutorWhenRedissonClientIsAvailable() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LockExecutor.class);
            assertThat(context.getBean(LockExecutor.class)).isInstanceOf(RedissonNonReentrantLockExecutor.class);
        });
    }
}
