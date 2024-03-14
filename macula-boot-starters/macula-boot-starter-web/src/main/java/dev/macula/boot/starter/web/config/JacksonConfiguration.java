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
import dev.macula.boot.starter.web.json.MaculaBeanSerializerModifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * {@code JacksonAutoConfiguration} Jackson的配置
 *
 * @author rain
 * @since 2022/6/29 21:09
 */
public class JacksonConfiguration {

    @Value("${macula.json.nullToEmpty:false}")
    private boolean nullToEmpty;

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.serializerByType(Long.class, ToStringSerializer.instance);

            builder.modulesToInstall(new JavaTimeModule());

            if (nullToEmpty) {
                builder.postConfigurer(objectMapper -> {
                    // null 处理
                    objectMapper.setSerializerFactory(
                        objectMapper.getSerializerFactory().withSerializerModifier(new MaculaBeanSerializerModifier()));
                    objectMapper.getSerializerProvider().setNullValueSerializer(
                        MaculaBeanSerializerModifier.NullJsonSerializers.STRING_JSON_SERIALIZER);
                });
            }
        };
    }
}
