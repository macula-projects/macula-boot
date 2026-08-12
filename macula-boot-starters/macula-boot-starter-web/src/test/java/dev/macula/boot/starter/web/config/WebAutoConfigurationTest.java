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
package dev.macula.boot.starter.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import dev.macula.boot.starter.web.filter.TenantFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link WebAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class WebAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, WebAutoConfiguration.class));

    @Test
    void createsDefaultWebInfrastructureInServletApplication() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(JacksonProperties.class);
            assertThat(context).hasSingleBean(MaculaWebMvcConfigurer.class);
            assertThat(context).hasSingleBean(ControllerExceptionAdvice.class);
            assertThat(context).hasSingleBean(ControllerResponseAdvice.class);
            assertThat(context).hasSingleBean(TenantFilter.class);
        });
    }

    @Test
    void allowsResponseAndExceptionAdviceToBeDisabled() {
        webContextRunner.withPropertyValues("macula.web.exception-advice=false", "macula.web.response-advice=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ControllerExceptionAdvice.class);
                assertThat(context).doesNotHaveBean(ControllerResponseAdvice.class);
                assertThat(context).hasSingleBean(TenantFilter.class);
            });
    }

    @Test
    void doesNotActivateInNonWebApplication() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations
            .of(JacksonAutoConfiguration.class, WebAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(WebAutoConfiguration.class));
    }
}
