package org.macula.boot.starter.redis.test;

import org.macula.boot.starter.redis.config.RedissonConfigBuilder;
import org.macula.boot.starter.redis.config.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

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