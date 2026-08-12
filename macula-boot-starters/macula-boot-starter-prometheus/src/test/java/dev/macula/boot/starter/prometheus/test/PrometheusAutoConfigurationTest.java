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
package dev.macula.boot.starter.prometheus.test;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.prometheus.config.PrometheusAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@code PrometheusAutoConfigurationTest} Prometheus自动配置单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class PrometheusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PrometheusAutoConfiguration.class))
        .withPropertyValues("spring.application.name=macula-prometheus-test");

    @Test
    @SuppressWarnings("unchecked")
    void addsApplicationNameAsCommonMetricTag() {
        contextRunner.run(context -> {
            MeterRegistryCustomizer<MeterRegistry> customizer = context.getBean(MeterRegistryCustomizer.class);
            SimpleMeterRegistry registry = new SimpleMeterRegistry();

            customizer.customize(registry);
            registry.counter("test-counter", "tag1", "value1");

            assertThat(registry.get("test-counter").counter().getId().getTag("application"))
                .isEqualTo("macula-prometheus-test");
        });
    }
}
