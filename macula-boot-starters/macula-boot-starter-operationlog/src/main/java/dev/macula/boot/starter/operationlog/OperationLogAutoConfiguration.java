/*
 * Copyright (c) 2023 Macula
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

package dev.macula.boot.starter.operationlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 操作日志自动配置类
 *
 * @author Gordian
 * @since 2025-11-19
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableAsync
@ConditionalOnWebApplication
public class OperationLogAutoConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public OperationLogListener operationLogListener() {
        return new OperationLogListener();
    }

    @Bean
    public OperationLogAspect operationLogAspect(ApplicationEventPublisher publisher) {
        String appName = environment.getProperty("spring.application.name", "unknown-app");
        return new OperationLogAspect(publisher, appName);
    }

}
