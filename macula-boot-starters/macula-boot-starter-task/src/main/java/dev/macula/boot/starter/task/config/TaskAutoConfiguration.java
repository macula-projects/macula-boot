/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.task.config;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import com.aizuda.snailjob.client.starter.SnailJobClientJobCoreAutoConfiguration;
import com.aizuda.snailjob.client.starter.SnailJobClientRetryCoreAutoConfiguration;
import dev.macula.boot.starter.task.compat.xxljob.XxlJobClientConflictDetector;
import dev.macula.boot.starter.task.compat.xxljob.XxlJobSnailJobConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@code TaskAutoConfiguration} 定时任务自动配置
 *
 * @author rain
 * @since 2023/7/4 19:28
 */
@AutoConfiguration(before = {SnailJobClientJobCoreAutoConfiguration.class,
    SnailJobClientRetryCoreAutoConfiguration.class})
@ConditionalOnProperty(prefix = "macula.task", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TaskProperties.class)
@EnableSnailJob
@Import(XxlJobSnailJobConfiguration.class)
public class TaskAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    XxlJobClientConflictDetector xxlJobClientConflictDetector() {
        return new XxlJobClientConflictDetector();
    }
}
