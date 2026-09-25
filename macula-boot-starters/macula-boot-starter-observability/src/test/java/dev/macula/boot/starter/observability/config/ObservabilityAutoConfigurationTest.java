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
package dev.macula.boot.starter.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.observability.logging.OpenTelemetryLogbackAppenderInstaller;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

/**
 * {@link ObservabilityAutoConfiguration} 条件装配测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void shouldProvideApplicationMetricTagAndSafeDefaults() {
        contextRunner.withPropertyValues("spring.application.name=orders").run(context -> {
            ObservabilityProperties properties = context.getBean(ObservabilityProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getLogging().getCaptureMdcAttributes()).isEmpty();

            SimpleMeterRegistry registry = new SimpleMeterRegistry();
            context.getBean("maculaApplicationMetricsCustomizer", MeterRegistryCustomizer.class)
                .customize(registry);
            assertThat(registry.counter("requests").getId().getTag("application")).isEqualTo("orders");
        });
    }

    @Test
    void shouldBackOffFromUserMetricCustomizer() {
        MeterRegistryCustomizer<MeterRegistry> customizer = registry -> registry.config()
            .commonTags("application", "custom");
        contextRunner.withBean("maculaApplicationMetricsCustomizer", MeterRegistryCustomizer.class,
                () -> customizer)
            .run(context -> assertThat(context.getBean("maculaApplicationMetricsCustomizer"))
                .isSameAs(customizer));
    }

    @Test
    void shouldDisableMaculaAutoConfiguration() {
        contextRunner.withPropertyValues("macula.observability.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ObservabilityProperties.class);
                assertThat(context).doesNotHaveBean("maculaApplicationMetricsCustomizer");
                assertThat(context).doesNotHaveBean(OpenTelemetryLogbackAppenderInstaller.class);
            });
    }

    @Test
    void shouldInstallLogbackAppenderOnlyWhenLoggingExportIsEnabled() {
        contextRunner.withBean(OpenTelemetry.class, OpenTelemetry::noop)
            .run(context -> assertThat(context)
                .hasSingleBean(OpenTelemetryLogbackAppenderInstaller.class));

        contextRunner.withBean(OpenTelemetry.class, OpenTelemetry::noop)
            .withPropertyValues("management.logging.export.otlp.enabled=false")
            .run(context -> assertThat(context)
                .doesNotHaveBean(OpenTelemetryLogbackAppenderInstaller.class));

        contextRunner.withBean(OpenTelemetry.class, OpenTelemetry::noop)
            .withPropertyValues("management.logging.export.enabled=false")
            .run(context -> assertThat(context)
                .doesNotHaveBean(OpenTelemetryLogbackAppenderInstaller.class));
    }

    @Test
    void shouldInstallAppenderAfterBootCreatesOpenTelemetry() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                ObservabilityAutoConfiguration.class, OpenTelemetrySdkAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OpenTelemetry.class);
                assertThat(context).hasSingleBean(OpenTelemetryLogbackAppenderInstaller.class);
            });
    }

    @Test
    void shouldBindNestedYamlResourceAttributesToOpenTelemetryKeys() throws Exception {
        String yaml = """
            management:
              opentelemetry:
                resource-attributes:
                  service:
                    name: orders
                    namespace: macula
                  deployment:
                    environment:
                      name: test
            """;
        PropertySource<?> source = new YamlPropertySourceLoader().load("nested-resource-attributes",
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8))).get(0);

        OpenTelemetryProperties properties = new Binder(ConfigurationPropertySources.from(source))
            .bind("management.opentelemetry", Bindable.of(OpenTelemetryProperties.class))
            .get();

        assertThat(properties.getResourceAttributes())
            .containsEntry("service.name", "orders")
            .containsEntry("service.namespace", "macula")
            .containsEntry("deployment.environment.name", "test");
    }
}
