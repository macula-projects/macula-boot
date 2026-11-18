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

package dev.macula.example.gateway.config;

import dev.macula.boot.starter.redis.config.RedissonConfigBuilder;
import dev.macula.boot.starter.redis.config.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@code RedisConfiguration} Redis配置
 *
 * @author rain
 * @since 2023/4/21 11:50
 */
@Configuration
public class RedisConfiguration {
    /**
     * 绑定网关自身 Redis 配置，用于 Token 缓存。
     * @return 网关 Redis 属性
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.data.redis")
    public DataRedisProperties redisProperties() {
        return new DataRedisProperties();
    }

    /**
     * 绑定 System Redis 配置，用于读取已发布的认证授权缓存。
     * @return System Redis 属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis.system")
    public DataRedisProperties sysRedisProperties() {
        return new DataRedisProperties();
    }

    /**
     * 创建网关 Redis 客户端，并在容器关闭时释放连接。
     * @param ctx 应用上下文
     * @param redisProperties 网关 Redis 属性
     * @return 网关 Redis 客户端
     * @throws Exception 配置读取或客户端创建失败
     */
    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(ApplicationContext ctx,
        @Qualifier("redisProperties") DataRedisProperties redisProperties) throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, redisProperties, new RedissonProperties());
        return Redisson.create(config);
    }

    /**
     * 使用明确的 System 属性创建独立客户端，避免被主属性 Bean 替代。
     * @param ctx 应用上下文
     * @param sysRedisProperties System Redis 属性
     * @return System Redis 客户端
     * @throws Exception 配置读取或客户端创建失败
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient sysRedissonClient(ApplicationContext ctx,
        @Qualifier("sysRedisProperties") DataRedisProperties sysRedisProperties)
        throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, sysRedisProperties, new RedissonProperties());
        return Redisson.create(config);
    }

    /**
     * 提供读取 System 缓存的指定名称模板，保持字符串键与 Hash 键序列化约定。
     * @param sysRedissonClient System Redis 客户端
     * @return System 缓存访问模板
     */
    @Bean(name = "sysRedisTemplate")
    public RedisTemplate<String, Object> sysRedisTemplate(
        @Qualifier("sysRedissonClient") RedissonClient sysRedissonClient) {
        //数据泛型类型
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        //设置连接工厂（Jedis或Lettuce）
        template.setConnectionFactory(new RedissonConnectionFactory(sysRedissonClient));

        //设置key的序列化方式---String
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        //初始化RedisTemplate的参数设置
        template.afterPropertiesSet();
        return template;
    }
}
