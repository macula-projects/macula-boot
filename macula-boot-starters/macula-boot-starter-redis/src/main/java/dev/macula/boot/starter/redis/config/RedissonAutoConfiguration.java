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

package dev.macula.boot.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * <b>RedissonAutoConfiguration</b> Redisson自动配置类
 * </p>
 *
 * @author Rain
 * @since 2022-01-28
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@EnableConfigurationProperties({RedissonProperties.class, RedisProperties.class})
public class RedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public RedissonReactiveClient redissonReactive(RedissonClient redisson) {
        return redisson.reactive();
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public RedissonRxClient redissonRxClient(RedissonClient redisson) {
        return redisson.rxJava();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(
            ApplicationContext ctx,
            RedisProperties redisProperties,
            RedissonProperties redissonProperties,
            List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) throws IOException {

        Config config = RedissonConfigBuilder.create().build(ctx, redisProperties, redissonProperties);
        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        //数据泛型类型
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        //设置连接工厂（Jedis或Lettuce）
        template.setConnectionFactory(redisConnectionFactory);

        /*
        自定义JSON序列化设置
         */
        //创建JSON序列化工具
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        //创建序列化规则objectMapper 工具将会遵循objectMapper中定义的规则
        ObjectMapper objectMapper = new ObjectMapper();
        //设置序列化可见度(PropertyAccessor表示序列化的范围 Visibility用于设置访问权限（访问修饰符）)
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //启动"自行在JSON中添加类型信息" 并进行相关设置（若无需反序列化可不设置）
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(), //多态类型验证程序（必须参数）
                ObjectMapper.DefaultTyping.NON_FINAL,       //允许序列化的类型（此处表示类不可被final修饰 但String, Boolean, Integer, Double除外）
                JsonTypeInfo.As.WRAPPER_ARRAY);             /*
                                                                类型信息在JSON中的形式（默认为WRAPPER_ARRAY）
                                                                WRAPPER_ARRAY
                                                                WRAPPER_Object
                                                                PROPERTY
                                                                若不需要反序列化 即不需要类的类型信息 则可使用EXISTING_PROPERTY
                                                                下文中会详细说明不同参数对类型信息在JSON中的形式的影响
                                                            */
        //将objectMapper传入工具
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);//JSON序列化与反序列化将会遵循objectMapper中定义的规则

        //设置key的序列化方式---String
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        //设置value的序列化方式---JSON(上方自定义的JSON序列化设置)
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        //初始化RedisTemplate的参数设置
        template.afterPropertiesSet();

        return template;
    }
}
