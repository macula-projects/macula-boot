/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

package dev.macula.boot.mapstruct.config;

import dev.macula.boot.mapstruct.DateTimeConverter;
import io.github.linpeilie.Converter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 默认注册日期时间转换器，支持外部时区、用户 Bean 覆盖及显式禁用。
 * 编译期 Mapper 对转换器的引用由 Parent 和 GlobalMapperConfig 提供。
 *
 * @author Rain
 * @since 6.1.0
 */
@AutoConfiguration
@ConditionalOnClass(Converter.class)
@ConditionalOnProperty(prefix = "macula.mapstruct", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DateTimeProperties.class)
public class MapStructAutoConfiguration {

    /**
     * 在用户没有提供转换器时，按外部配置或 JVM 默认时区创建共享 Bean。
     *
     * @param properties 存储时区属性
     * @return Mapper 使用的日期时间转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public DateTimeConverter dateTimeConverter(DateTimeProperties properties) {
        return new DateTimeConverter(properties.getStorageZone());
    }
}
