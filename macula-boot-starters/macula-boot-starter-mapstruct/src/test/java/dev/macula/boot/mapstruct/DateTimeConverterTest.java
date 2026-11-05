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

package dev.macula.boot.mapstruct;

import dev.macula.boot.mapstruct.config.MapStructAutoConfiguration;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证可选时区配置的 Spring 注入和双向转换。
 *
 * @author Rain
 * @since 6.1.0
 */
class DateTimeConverterTest {

    /** 未提供配置时也能创建 Bean，并使用 JVM 默认时区。 */
    @Test
    void missingPropertyUsesSystemTimezone() {
        try (var context = context(Map.of())) {
            DateTimeConverter converter = context.getBean(DateTimeConverter.class);
            Date value = Date.from(Instant.parse("2026-11-05T00:50:30Z"));
            assertEquals(LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()),
                converter.toLocalDateTime(value));
            assertEquals(value, converter.toDate(converter.toLocalDateTime(value)));
            assertNull(converter.toDate(null));
            assertNull(converter.toLocalDateTime(null));
        }
    }

    /** 显式时区优先于系统默认值，空白配置则回退系统默认值。 */
    @Test
    void configuredTimezoneOverridesSystemDefaultAndBlankFallsBack() {
        Date value = Date.from(Instant.parse("2026-11-05T00:50:30Z"));
        try (var context = context(Map.of("macula.time.storage-zone", "Asia/Shanghai"))) {
            DateTimeConverter converter = context.getBean(DateTimeConverter.class);
            assertEquals(LocalDateTime.of(2026, 11, 5, 8, 50, 30), converter.toLocalDateTime(value));
            assertEquals(value, converter.toDate(LocalDateTime.of(2026, 11, 5, 8, 50, 30)));
        }
        DateTimeConverter blank = new DateTimeConverter(" ");
        assertEquals(LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()),
            blank.toLocalDateTime(value));
    }

    /** 非法时区应明确失败，不静默回退系统时区。 */
    @Test
    void invalidTimezoneFailsFast() {
        assertThrows(DateTimeException.class, () -> new DateTimeConverter("not-a-timezone"));
    }

    /** 使用严格占位符解析验证缺省值，不依赖完整应用或外部服务。 */
    private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.registerBean(PropertySourcesPlaceholderConfigurer.class);
        context.register(MapStructAutoConfiguration.class);
        context.refresh();
        return context;
    }
}
