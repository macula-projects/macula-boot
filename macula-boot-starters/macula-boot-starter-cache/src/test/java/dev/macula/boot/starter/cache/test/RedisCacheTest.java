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

package dev.macula.boot.starter.cache.test;

import dev.macula.boot.starter.cache.test.service.UserService;
import dev.macula.boot.starter.cache.test.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * <p>
 * <b>RedisCacheTest</b> REDIS缓存测试
 * </p>
 *
 * @author Rain
 * @since 2022-01-30
 */
@SpringBootTest
public class RedisCacheTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testCacheable() {
        User user = userService.getUser("1111x");
        Cache cache = cacheManager.getCache("user-service");
        Assertions.assertNotNull(cache);
        Cache.ValueWrapper value = cache.get("getUser:1111x");
        Assertions.assertNotNull(value);
        User cacheUser = (User)value.get();
        Assertions.assertEquals(user, cacheUser);
        System.out.println(cacheUser.getId() + ":" + cacheUser.getName());

        User user2 = userService.getUser2("2222x");

        User user3 = userService.getUser3();

    }
}
