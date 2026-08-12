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
import dev.macula.boot.starter.web.test.MaculaWebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * 验证 Web Starter 可通过 Spring Boot 自动配置导入机制装配。
 *
 * @author Rain
 * @since 2026/8/12
 */
@SpringBootTest(classes = MaculaWebApplication.class)
class WebAutoConfigurationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void discoversWebStarterAutoConfigurationFromApplicationClasspath() {
        assertThat(applicationContext.getBeansOfType(WebAutoConfiguration.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(MaculaWebMvcConfigurer.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(ControllerExceptionAdvice.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(ControllerResponseAdvice.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(TenantFilter.class)).hasSize(1);
    }
}
