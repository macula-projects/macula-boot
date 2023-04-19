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

package dev.macula.boot.starter.redis.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/**
 * {@code RedisSerializerUtil} 获取JSON序列化工具
 *
 * @author rain
 * @since 2023/4/18 18:23
 */
public class RedisSerializerUtil {
    public static Jackson2JsonRedisSerializer<Object> getJsonRedisSerializer() {
        //创建JSON序列化工具
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
            new Jackson2JsonRedisSerializer<>(Object.class);
        //创建序列化规则objectMapper 工具将会遵循objectMapper中定义的规则
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        //设置序列化可见度(PropertyAccessor表示序列化的范围 Visibility用于设置访问权限（访问修饰符）)
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //启动"自行在JSON中添加类型信息" 并进行相关设置（若无需反序列化可不设置）
        /*
            类型信息在JSON中的形式（默认为WRAPPER_ARRAY）
            WRAPPER_ARRAY
            WRAPPER_Object
            PROPERTY
            若不需要反序列化 即不需要类的类型信息 则可使用EXISTING_PROPERTY
            下文中会详细说明不同参数对类型信息在JSON中的形式的影响
         */
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),  //多态类型验证程序（必须参数）
            ObjectMapper.DefaultTyping.NON_FINAL,        //允许序列化的类型（此处表示类不可被final修饰 但String, Boolean, Integer, Double除外）
            JsonTypeInfo.As.WRAPPER_ARRAY);
        //将objectMapper传入工具
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);//JSON序列化与反序列化将会遵循objectMapper中定义的规则
        return jackson2JsonRedisSerializer;
    }
}
