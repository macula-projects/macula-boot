package org.macula.boot.starter.redis.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>
 * <b>RedisApplication</b> 启动类
 * </p>
 *
 * @author Rain
 * @since 2022-01-29
 */

@SpringBootApplication
public class RedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }
}