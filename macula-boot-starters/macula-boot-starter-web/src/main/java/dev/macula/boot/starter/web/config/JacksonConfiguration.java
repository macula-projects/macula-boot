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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 序列化时，对象为 null，是否抛异常
            builder.failOnEmptyBeans(false);
            // 反序列化时，json 中包含 pojo 不存在属性时，是否抛异常
            builder.failOnUnknownProperties(false);
            // 安装JSR310日期的序列化和反序列化
            builder.modulesToInstall(new JavaTimeModule());

            // 设置LocalDate的日期格式
            if (StrUtil.isNotEmpty(properties.getLocalDateFormat())) {
                builder.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(properties.getLocalDateFormat())))
                        .deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(properties.getLocalDateFormat())));
            }

            // 设置LocalTime的时间格式
            if (StrUtil.isNotEmpty(properties.getLocalTimeFormat())) {
                builder.serializers(new LocalTimeSerializer(DateTimeFormatter.ofPattern(properties.getLocalTimeFormat())))
                        .deserializers(new LocalTimeDeserializer(DateTimeFormatter.ofPattern(properties.getLocalTimeFormat())));
            }

            // 设置LocalDateTime的日期时间格式
            if (StrUtil.isNotEmpty(properties.getLocalDateTimeFormat())) {
                builder.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(properties.getLocalDateTimeFormat())))
                        .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(properties.getLocalDateTimeFormat())));
            }
        };
    }
}
