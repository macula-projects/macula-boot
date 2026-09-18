/*
 * Copyright (c) 2023-2026 Macula
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
package dev.macula.boot.starter.cache.test;

import dev.macula.boot.starter.cache.test.service.UserService;
import dev.macula.boot.starter.cache.test.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

/**
 * Redis 两级缓存集成测试，验证显式 SpEL key、单参数默认 key 和无参数 key 的写入结果。
 *
 * @author Rain
 * @since 5.0.0
 */
@SpringBootTest
public class RedisCacheIT {

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 清理上次执行留下的本地和 Redis 缓存，保证测试可重复执行。
     */
    @BeforeEach
    void clearCache() {
        Cache cache = getUserCache();
        cache.clear();
    }

    /**
     * 验证 {@code @Cacheable} 在三种常见方法签名下都会写入预期的 key 和 value。
     */
    @Test
    void testCacheable() {
        Cache cache = getUserCache();

        User user = userService.getUser("1111x");
        Assertions.assertEquals(user, getCachedUser(cache, "getUser:1111x"));

        User user2 = userService.getUser2("2222x");
        Assertions.assertEquals(user2, getCachedUser(cache, "2222x"));

        User user3 = userService.getUser3();
        Assertions.assertEquals(user3, getCachedUser(cache, SimpleKey.EMPTY));
    }

    private Cache getUserCache() {
        Cache cache = cacheManager.getCache("user-service");
        Assertions.assertNotNull(cache, "user-service cache should be configured");
        return cache;
    }

    private User getCachedUser(Cache cache, Object key) {
        Cache.ValueWrapper value = cache.get(key);
        Assertions.assertNotNull(value, () -> "cache entry should exist for key: " + key);
        return Assertions.assertInstanceOf(User.class, value.get());
    }
}
