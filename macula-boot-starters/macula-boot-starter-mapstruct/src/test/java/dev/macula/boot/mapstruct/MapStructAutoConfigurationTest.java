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
import io.github.linpeilie.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证转换器自动发现、属性绑定、条件装配及用户覆盖，无需业务 Import。
 *
 * @author Rain
 * @since 6.1.0
 */
class MapStructAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MapStructAutoConfiguration.class));

    /** 默认启用并使用 JVM 默认时区。 */
    @Test
    void defaultBeanUsesSystemTimezone() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DateTimeConverter.class);
            Date value = Date.from(Instant.parse("2026-11-05T00:50:30Z"));
            assertThat(context.getBean(DateTimeConverter.class).toLocalDateTime(value))
                .isEqualTo(LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()));
        });
    }

    /** 外部配置覆盖系统默认值。 */
    @Test
    void externalTimezoneIsBound() {
        runner.withPropertyValues("macula.time.storage-zone=Asia/Shanghai").run(context -> {
            Date value = Date.from(Instant.parse("2026-11-05T00:50:30Z"));
            assertThat(context.getBean(DateTimeConverter.class).toLocalDateTime(value))
                .isEqualTo(LocalDateTime.of(2026, 11, 5, 8, 50, 30));
        });
    }

    /** 用户 Bean 优先，即便外部时区非法也不创建默认转换器。 */
    @Test
    void userBeanOverridesDefault() {
        DateTimeConverter custom = new DateTimeConverter("UTC");
        runner.withBean(DateTimeConverter.class, () -> custom)
            .withPropertyValues("macula.time.storage-zone=not-a-timezone")
            .run(context -> assertThat(context.getBean(DateTimeConverter.class)).isSameAs(custom));
    }

    /** 显式禁用后不注册默认转换器。 */
    @Test
    void disabledDoesNotCreateBean() {
        runner.withPropertyValues("macula.mapstruct.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(DateTimeConverter.class));
    }

    /** Plus 不在 classpath 时不装配。 */
    @Test
    void missingPlusDoesNotCreateBean() {
        runner.withClassLoader(new FilteredClassLoader(Converter.class))
            .run(context -> assertThat(context).doesNotHaveBean(DateTimeConverter.class));
    }

    /** 非法时区使默认 Bean 初始化失败。 */
    @Test
    void invalidTimezoneFailsStartup() {
        runner.withPropertyValues("macula.time.storage-zone=not-a-timezone")
            .run(context -> assertThat(context).hasFailed());
    }

    /** 仅启用 Boot 自动配置即可从 imports 发现，无组件扫描或手工 Import。 */
    @Test
    void bootDiscoversConfigurationFromImports() {
        new ApplicationContextRunner().withUserConfiguration(DiscoveryConfiguration.class)
            .withPropertyValues("macula.time.storage-zone=Asia/Shanghai")
            .run(context -> {
                assertThat(context).hasSingleBean(DateTimeConverter.class);
                Converter converter = context.getBean(Converter.class);
                DateTimeForm form = new DateTimeForm();
                form.setCreateTime(Date.from(Instant.parse("2026-11-05T00:50:30Z")));
                DateTimeEntity entity = converter.convert(form, DateTimeEntity.class);
                assertThat(entity.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 11, 5, 8, 50, 30));
                assertThat(converter.convert(entity, DateTimeForm.class).getCreateTime()).isEqualTo(form.getCreateTime());
            });
    }

    /** 模拟无组件扫描的业务应用入口。
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class DiscoveryConfiguration {
    }
}
