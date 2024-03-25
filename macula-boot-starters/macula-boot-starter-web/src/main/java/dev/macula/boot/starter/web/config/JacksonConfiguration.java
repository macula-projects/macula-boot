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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.macula.boot.starter.web.json.BigNumberSerializer;
import dev.macula.boot.starter.web.json.MaculaBeanSerializerModifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * {@code JacksonAutoConfiguration} Jackson的配置
 *
 * @author rain
 * @since 2022/6/29 21:09
 */
public class JacksonConfiguration {

    @Value("${macula.jackson.long-to-string:true}")
    private boolean longToString;

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 序列化时，对象为 null，是否抛异常
            builder.failOnEmptyBeans(false);
            // 反序列化时，json 中包含 pojo 不存在属性时，是否抛异常
            builder.failOnUnknownProperties(false);

            builder.modulesToInstall(new JavaTimeModule());

            if (longToString) {
                builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
                builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
                builder.serializerByType(Long.class, ToStringSerializer.instance);
                builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            } else {
                builder.serializerByType(BigDecimal.class, BigNumberSerializer.instance);
                builder.serializerByType(BigInteger.class, BigNumberSerializer.instance);
                builder.serializerByType(Long.class, BigNumberSerializer.instance);
                builder.serializerByType(Long.TYPE, BigNumberSerializer.instance);
            }
        };
    }
}
