package org.macula.boot.starter.redis.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.macula.boot.starter.redis.config.RedissonConfigBuilder;
import org.macula.boot.starter.redis.config.RedissonProperties;
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
import org.springframework.context.annotation.Primary;
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
public class RedissonTwoRedis {
    @Autowired
    @Qualifier("redissonClientOne")
    private RedissonClient redissonClientOne;

    @Autowired
    @Qualifier("redissonClientTwo")
    private RedissonClient redissonClientTwo;

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

    @Test
    public void testRedissonClientTwo() {
        redissonClientTwo.getBucket("key").set("value2");
        Assertions.assertEquals("value2", redissonClientTwo.getBucket("key").get());
        System.out.println(redissonClientTwo.getBucket("key").get());
    }

    @TestConfiguration
    public static class RedisConfig {
        @Bean(name = "redissonPropertiesOne")
        @ConfigurationProperties(prefix = "spring.redis.redisson.one")
        public RedissonProperties redissonPropertiesOne() {
            return new RedissonProperties();
        }

        @Primary
        @Bean(name = "redissonClientOne", destroyMethod = "shutdown")
        public RedissonClient redissonClientOne(ApplicationContext ctx, @Qualifier("redissonPropertiesOne") RedissonProperties redissonProperties) throws Exception {
            Config config = RedissonConfigBuilder.create().build(ctx, null, redissonProperties);
            return Redisson.create(config);
        }

        @Bean(name = "redissonPropertiesTwo")
        @ConfigurationProperties(prefix = "spring.redis.redisson.tow")
        public RedissonProperties redissonProperties() {
            return new RedissonProperties();
        }

        @Bean(name = "redissonClientTwo", destroyMethod = "shutdown")
        public RedissonClient redissonClientTwo(ApplicationContext ctx, @Qualifier("redissonPropertiesTwo") RedissonProperties redissonProperties) throws Exception {
            Config config = RedissonConfigBuilder.create().build(ctx, null, redissonProperties);
            return Redisson.create(config);
        }
    }
}