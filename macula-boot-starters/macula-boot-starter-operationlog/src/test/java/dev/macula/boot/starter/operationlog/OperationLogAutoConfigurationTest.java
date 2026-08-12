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
package dev.macula.boot.starter.operationlog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link OperationLogAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class OperationLogAutoConfigurationTest {

    @Test
    void createsAspectAndListenerForWebApplications() {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(OperationLogAutoConfiguration.class))
            .withPropertyValues("spring.application.name=orders")
            .run(context -> {
                assertThat(context).hasSingleBean(OperationLogAspect.class);
                assertThat(context).hasSingleBean(OperationLogListener.class);
            });
    }

    @Test
    void doesNotActivateForNonWebApplications() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(OperationLogAutoConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(OperationLogAspect.class);
                assertThat(context).doesNotHaveBean(OperationLogListener.class);
            });
    }
}
