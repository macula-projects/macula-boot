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

package dev.macula.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * 验证示例两套 Redis 属性隔离与指定名称模板装配，不连接真实 Redis。
 *
 * @author Rain
 * @since 6.1.0
 */
class RedisConfigurationTest {
    /** 使用不同地址和数据库，确保 System 客户端不会误用主属性 Bean。 */
    @Test
    void bindsIndependentRedisPropertiesAndCreatesSystemTemplate() {
        List<Config> configs = new ArrayList<>();
        try (var redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class))).thenAnswer(invocation -> {
                configs.add(invocation.getArgument(0));
                return mock(RedissonClient.class);
            });
            new ApplicationContextRunner()
                .withUserConfiguration(BindingInfrastructure.class, RedisConfiguration.class)
                .withPropertyValues("spring.data.redis.host=gateway.example", "spring.data.redis.port=6381",
                    "spring.data.redis.database=1", "spring.data.redis.system.host=system.example",
                    "spring.data.redis.system.port=6382", "spring.data.redis.system.database=2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("redisProperties", DataRedisProperties.class).getPort()).isEqualTo(6381);
                    assertThat(context.getBean("sysRedisProperties", DataRedisProperties.class).getPort()).isEqualTo(6382);
                    assertThat(context.getBean(DataRedisProperties.class))
                        .isSameAs(context.getBean("redisProperties"));
                    assertThat(context.getBean("sysRedisTemplate", RedisTemplate.class).getConnectionFactory())
                        .isInstanceOf(RedissonConnectionFactory.class);
                    assertThat(configs).hasSize(2);
                    assertThat(configs).extracting(config -> config.useSingleServer().getAddress())
                        .containsExactlyInAnyOrder("redis://gateway.example:6381", "redis://system.example:6382");
                    assertThat(configs).extracting(config -> config.useSingleServer().getDatabase())
                        .containsExactlyInAnyOrder(1, 2);
                    assertThat(context.getBean("redissonClient"))
                        .isNotSameAs(context.getBean("sysRedissonClient"));
                });
        }
    }

    /**
     * 注册真实的属性绑定处理器，避免仅测试手动赋值。
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties
    static class BindingInfrastructure {
    }
}
