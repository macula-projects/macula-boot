/*
 * Copyright (c) 2023 Macula
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

package dev.macula.boot.starter.redis.test;

import dev.macula.boot.starter.redis.config.RedissonConfigBuilder;
import dev.macula.boot.starter.redis.config.RedissonProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>
 * <b>RedissonTwoRedis</b> 两个Redis的配置测试
 * </p>
 *
 * @author Rain
 * @since 2022-01-29
 */
@SpringBootTest
public class RedissonOneRedis {
    @Autowired
    private RedissonClient redissonClientOne;

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testRedisTemplate() {
        redisTemplate.opsForValue().set("key", "value");
        Assertions.assertEquals("value", redisTemplate.opsForValue().get("key"));
        System.out.println(redisTemplate.opsForValue().get("key"));
    }

    @Test
    public void testRedissonClientOne() {
        redissonClientOne.getBucket("key").set("value1");
        Assertions.assertEquals("value1", redissonClientOne.getBucket("key").get());
        System.out.println(redissonClientOne.getBucket("key").get());
    }

    @TestConfiguration
    public static class RedisConfig {
        @Bean(name = "redissonPropertiesOne")
        @ConfigurationProperties(prefix = "spring.redis.redisson.one")
        public RedissonProperties redissonPropertiesOne() {
            return new RedissonProperties();
        }

        @Bean(name = "redissonClientOne", destroyMethod = "shutdown")
        public RedissonClient redissonClientOne(ApplicationContext ctx,
            @Qualifier("redissonPropertiesOne") RedissonProperties redissonProperties) throws Exception {
            Config config = RedissonConfigBuilder.create().build(ctx, null, redissonProperties);
            return Redisson.create(config);
        }
    }
}