package org.macula.boot.starter.cache.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.macula.boot.starter.cache.test.service.UserService;
import org.macula.boot.starter.cache.test.vo.User;
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
        User cacheUser = (User) value.get();
        Assertions.assertEquals(user, cacheUser);
        System.out.println(cacheUser.getId() + ":" + cacheUser.getName());

        User user2 = userService.getUser2("2222x");

        User user3 = userService.getUser3();

    }
}
