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
package dev.macula.boot.starter.cache.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.macula.boot.starter.cache.TwoLevelCacheManager;
import dev.macula.boot.starter.cache.TwoLevelCacheProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.SubscriptionListener;

/**
 * {@link TwoLevelCacheAutoConfiguration} 的条件装配测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class TwoLevelCacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TwoLevelCacheAutoConfiguration.class));

    @Test
    void backsOffWhenRedisCacheTypeIsNotSelected() {
        contextRunner.withPropertyValues("spring.cache.type=none").run(context -> {
            assertThat(context).doesNotHaveBean(TwoLevelCacheManager.class);
            assertThat(context).doesNotHaveBean(TwoLevelCacheProperties.class);
        });
    }

    @Test
    void doesNotRequireResilience4jOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader("io.github.resilience4j"))
            .withPropertyValues("spring.cache.type=redis")
            .withBean(RedisConnectionFactory.class, this::redisConnectionFactory)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(TwoLevelCacheManager.class);
            });
    }

    @Test
    void createsRedisTemplateAndCacheManagerForRedisCacheType() {
        contextRunner.withPropertyValues("spring.cache.type=redis")
            .withBean(RedisConnectionFactory.class, this::redisConnectionFactory)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(TwoLevelCacheProperties.class);
                assertThat(context).hasSingleBean(TwoLevelCacheManager.class);
                assertThat(context).hasBean(TwoLevelCacheAutoConfiguration.CACHE_REDIS_TEMPLATE_NAME);
            });
    }

    private RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.isSubscribed()).thenReturn(false);
        doAnswer(invocation -> {
            MessageListener listener = invocation.getArgument(0);
            if (listener instanceof SubscriptionListener subscriptionListener) {
                int channelCount = invocation.getArguments().length - 1;
                for (int index = 1; index < invocation.getArguments().length; index++) {
                    subscriptionListener.onChannelSubscribed(invocation.getArgument(index), channelCount);
                }
            }
            return null;
        }).when(connection).subscribe(any(MessageListener.class), any(byte[][].class));
        return connectionFactory;
    }
}
