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

package dev.macula.boot.starter.web.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code JacksonAutoConfiguration} Jackson的配置
 *
 * @author rain
 * @since 2022/6/29 21:09
 */
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonConfiguration {

    private final JacksonProperties properties;

    public JacksonConfiguration(JacksonProperties properties) {
        this.properties = properties;
    }


    @Bean
    @ConditionalOnMissingBean
    public JsonMapperBuilderCustomizer customizer() {
        return builder -> {
            // 序列化时，对象为 null，是否抛异常
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            // 反序列化时，json 中包含 pojo 不存在属性时，是否抛异常
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            SimpleModule javaTimeFormats = new SimpleModule("maculaJavaTimeFormats");

            // 设置LocalDate的日期格式
            if (StrUtil.isNotEmpty(properties.getLocalDateFormat())) {
                javaTimeFormats.addSerializer(LocalDate.class,
                        new LocalDateSerializer(DateTimeFormatter.ofPattern(properties.getLocalDateFormat())));
                javaTimeFormats.addDeserializer(LocalDate.class,
                        new LocalDateDeserializer(DateTimeFormatter.ofPattern(properties.getLocalDateFormat())));
            }

            // 设置LocalTime的时间格式
            if (StrUtil.isNotEmpty(properties.getLocalTimeFormat())) {
                javaTimeFormats.addSerializer(LocalTime.class,
                        new LocalTimeSerializer(DateTimeFormatter.ofPattern(properties.getLocalTimeFormat())));
                javaTimeFormats.addDeserializer(LocalTime.class,
                        new LocalTimeDeserializer(DateTimeFormatter.ofPattern(properties.getLocalTimeFormat())));
            }

            // 设置LocalDateTime的日期时间格式
            if (StrUtil.isNotEmpty(properties.getLocalDateTimeFormat())) {
                javaTimeFormats.addSerializer(LocalDateTime.class,
                        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(properties.getLocalDateTimeFormat())));
                javaTimeFormats.addDeserializer(LocalDateTime.class,
                        new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(properties.getLocalDateTimeFormat())));
            }
            builder.addModule(javaTimeFormats);
        };
    }
}
