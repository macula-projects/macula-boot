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

package dev.macula.boot.starter.task.compat.xxljob;

import com.aizuda.snailjob.client.job.core.Scanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the XXL-to-SnailJob compatibility bridge.
 *
 * @author Rain
 * @since 6.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Scanner.class)
@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "macula.task.xxl-job-adapter", name = "enabled", havingValue = "true",
    matchIfMissing = true)
public class XxlJobSnailJobConfiguration {

    @Bean
    @ConditionalOnMissingBean(XxlJobSnailJobScanner.class)
    XxlJobSnailJobScanner xxlJobSnailJobScanner(ConfigurableApplicationContext applicationContext) {
        return new XxlJobSnailJobScanner(applicationContext);
    }
}
