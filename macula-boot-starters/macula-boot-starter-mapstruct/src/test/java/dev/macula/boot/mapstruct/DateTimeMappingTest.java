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
import dev.macula.boot.mapstruct.fixture.DateTimeEntity;
import dev.macula.boot.mapstruct.fixture.DateTimeForm;
import dev.macula.boot.mapstruct.fixture.DateTimeMapper;
import io.github.linpeilie.Converter;
import io.github.linpeilie.mapstruct.SpringConverterFactory;
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

/**
 * 实际生成并运行普通 MapStruct 与 Plus Mapper，验证共享转换器的注入和双向调用。
 * 测试模型不配置 uses 或模块配置，依赖 Parent 从主编译产物加载共享配置。
 *
 * @author Rain
 * @since 6.1.0
 */
class DateTimeMappingTest {

    /** 两类 Mapper 均通过 Spring 使用外部配置，不能退回内置 UTC 转换。 */
    @Test
    void generatedMappersUseConfiguredTimezoneBothWays() {
        assertMappings(Map.of("macula.time.storage-zone", "Asia/Shanghai"),
            LocalDateTime.of(2026, 11, 5, 8, 50, 30));
    }

    /** 未配置时，两类 Mapper 使用同一 JVM 默认时区且可以往返。 */
    @Test
    void generatedMappersUseSystemTimezoneWhenMissing() {
        assertMappings(Map.of(), LocalDateTime.ofInstant(
            Instant.parse("2026-11-05T00:50:30Z"), ZoneId.systemDefault()));
    }

    /** 只注册目标 Mapper 的轻量上下文，分别通过直接注入与 Plus Converter 验证。 */
    private void assertMappings(Map<String, Object> properties, LocalDateTime expected) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.registerBean(PropertySourcesPlaceholderConfigurer.class);
            context.register(MapStructAutoConfiguration.class);
            context.scan("dev.macula.boot.mapstruct.fixture");
            context.refresh();
            DateTimeForm form = new DateTimeForm();
            form.setCreateTime(Date.from(Instant.parse("2026-11-05T00:50:30Z")));
            DateTimeMapper mapper = context.getBean(DateTimeMapper.class);
            assertEquals(expected, mapper.toEntity(form).getCreateTime());
            assertEquals(form.getCreateTime(), mapper.toForm(mapper.toEntity(form)).getCreateTime());
            Converter converter = new Converter(new SpringConverterFactory(context));
            DateTimeEntity entity = converter.convert(form, DateTimeEntity.class);
            assertEquals(expected, entity.getCreateTime());
            assertEquals(form.getCreateTime(), converter.convert(entity, DateTimeForm.class).getCreateTime());
            form.setCreateTime(null);
            assertNull(mapper.toEntity(form).getCreateTime());
            assertNull(converter.convert(form, DateTimeEntity.class).getCreateTime());
            entity.setCreateTime(null);
            assertNull(mapper.toForm(entity).getCreateTime());
            assertNull(converter.convert(entity, DateTimeForm.class).getCreateTime());
        }
    }
}
