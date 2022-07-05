package org.macula.boot.starter.cache.test.service;

import org.macula.boot.starter.cache.test.vo.User;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <b>UserService</b> 用户服务测试类
 * </p>
 *
 * @author Rain
 * @since 2022-01-30
 */

@Component
public class UserServiceImpl implements UserService {

    public User getUser(String userId) {
        return new User(userId, "Rain", "password");
    }

    public User getUser2(String userId) {
        return new User(userId, "Rain2", "password2");
    }

    public User getUser3() {
        return new User("3333x", "Rain3", "password3");
    }
}
