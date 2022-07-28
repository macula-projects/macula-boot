/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
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

import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * {@code WebAutoConfiguration} is WEB的自动化配置
 *
 * @author rain
 * @since 2022/6/30 10:40
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
@Import({JacksonConfiguration.class})
public class WebAutoConfiguration {

    @Bean
    public MaculaWebMvcConfigurer maculaWebMvcConfigurer() {
        return new MaculaWebMvcConfigurer();
    }

    @Bean
    @ConditionalOnProperty(name = "macula.web.exception-advice", havingValue = "true", matchIfMissing = true)
    public ControllerExceptionAdvice controllerExceptionAdvice() {
        return new ControllerExceptionAdvice();
    }

    @Bean
    @ConditionalOnProperty(name = "macula.web.response-advice", havingValue = "true", matchIfMissing = true)
    public ControllerResponseAdvice controllerResponseAdvice() {
        return new ControllerResponseAdvice();
    }

}
