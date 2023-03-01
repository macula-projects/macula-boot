/*
 * Copyright (c) 2022 Macula
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

import dev.macula.boot.starter.redis.test.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * <p>
 * <b>RedissonTest</b> Redis客户端测试类
 * </p>
 *
 * @author Rain
 * @since 2022-01-29
 */

@SpringBootTest
public class RedissonTest {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Test
    public void testRedisTemplate() {
        redisTemplate.opsForValue().set("key", "value");
        Assertions.assertEquals("value", redisTemplate.opsForValue().get("key"));
        System.out.println(redisTemplate.opsForValue().get("key"));
    }

    @Test
    public void testRedissonClient() {
        redissonClient.getBucket("key").set("value");
        Assertions.assertEquals("value", redissonClient.getBucket("key").get());
        System.out.println(redissonClient.getBucket("key").get());
    }

    @Test
    public void testRedisVo() {
        User user = new User("123", "rain.wang", "123456");
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.opsForValue().set("user:redis", user);
        Assertions.assertEquals(user, redisTemplate.opsForValue().get("user:redis"));
        System.out.println(((User)redisTemplate.opsForValue().get("user:redis")).getName());

        redissonClient.getBucket("user:redisson").set(user);
        Assertions.assertEquals(user, redissonClient.getBucket("user:redisson").get());
        System.out.println(((User)redissonClient.getBucket("user:redisson").get()).getName());
    }
}
