package org.macula.boot.starter.cache.test.service;

import org.macula.boot.starter.cache.test.vo.User;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

/**
 * <p>
 * <b>UserService</b>
 * </p>
 *
 * @author Rain
 * @since 2022-01-30
 */
@CacheConfig(cacheNames = "user-service")
public interface UserService {

    @Cacheable(key = "#root.methodName + ':' + #userId")
    User getUser(String userId);

    @Cacheable
    User getUser2(String userId);

    @Cacheable
    User getUser3();

}
