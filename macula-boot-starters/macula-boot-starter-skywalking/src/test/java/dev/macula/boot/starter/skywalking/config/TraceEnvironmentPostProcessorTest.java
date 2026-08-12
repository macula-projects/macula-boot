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
package dev.macula.boot.starter.skywalking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link TraceEnvironmentPostProcessor} 环境变量处理测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class TraceEnvironmentPostProcessorTest {

    @Test
    void suppliesTraceAwareLoggingPatternByDefault() {
        MockEnvironment environment = new MockEnvironment();

        new TraceEnvironmentPostProcessor().postProcessEnvironment(environment, mock(SpringApplication.class));

        assertThat(environment.getProperty("logging.pattern.level")).contains("%X{tid:-}");
    }

    @Test
    void preservesUserLoggingPattern() {
        MockEnvironment environment = new MockEnvironment().withProperty("logging.pattern.level", "custom-pattern");

        new TraceEnvironmentPostProcessor().postProcessEnvironment(environment, mock(SpringApplication.class));

        assertThat(environment.getProperty("logging.pattern.level")).isEqualTo("custom-pattern");
    }

    @Test
    void canDisableDefaultPattern() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.skywalking.default-logging-pattern-enabled", "false");

        new TraceEnvironmentPostProcessor().postProcessEnvironment(environment, mock(SpringApplication.class));

        assertThat(environment.getProperty("logging.pattern.level")).isNull();
    }
}
