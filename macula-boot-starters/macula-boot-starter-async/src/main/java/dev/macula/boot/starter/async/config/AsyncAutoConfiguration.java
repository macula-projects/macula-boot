/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.async.config;

import com.alibaba.ttl.TtlRunnable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code AsyncAutoConfiguration} transmit-thread-local配置
 *
 * @author Rain
 * @since 2024/5/24 19:24
 */
@AutoConfiguration
@EnableAsync
@EnableScheduling
public class AsyncAutoConfiguration {
    
    @Bean
    public TaskDecorator ttlTaskDecorator() {
        return TtlRunnable::get;
    }
}
