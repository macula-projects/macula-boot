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

import dev.macula.boot.starter.observability.logging.OpenTelemetryLogbackAppenderInstaller;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 统一可观测性 Macula 增量自动配置。
 *
 * @author Rain
 * @since 6.1.0
 */
@AutoConfiguration(after = OpenTelemetrySdkAutoConfiguration.class)
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "macula.observability", name = "enabled", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean("maculaApplicationMetricsCustomizer")
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingBean(name = "maculaApplicationMetricsCustomizer")
    public MeterRegistryCustomizer<MeterRegistry> maculaApplicationMetricsCustomizer(
        @Value("${spring.application.name:application}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnClass({OpenTelemetryAppender.class, ch.qos.logback.classic.Logger.class})
    @ConditionalOnBean(OpenTelemetry.class)
    @ConditionalOnMissingBean(OpenTelemetryLogbackAppenderInstaller.class)
    @ConditionalOnProperty(prefix = "management.logging.export", name = "enabled", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "management.logging.export.otlp", name = "enabled", matchIfMissing = true)
    public OpenTelemetryLogbackAppenderInstaller openTelemetryLogbackAppenderInstaller(
        OpenTelemetry openTelemetry, ObservabilityProperties properties) {
        return new OpenTelemetryLogbackAppenderInstaller(openTelemetry,
            properties.getLogging().getCaptureMdcAttributes());
    }
}
