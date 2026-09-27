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

package dev.macula.boot.starter.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>TaskProperties</b> 任务模块的配置
 * </p>
 *
 * @author Rain
 * @since 2024/4/11
 */
@Data
@ConfigurationProperties(prefix = "macula.task")
public class TaskProperties {

    /**
     * Whether the task starter is enabled.
     */
    private boolean enabled = true;

    /**
     * XXL-compatible handler adapter settings.
     */
    private XxlJobAdapter xxlJobAdapter = new XxlJobAdapter();

    /**
     * Controls the XXL-compatible handler adapter.
     *
     * @since 6.1.0
     */
    @Data
    public static class XxlJobAdapter {

        /**
         * Whether XXL-compatible handlers are registered with SnailJob.
         */
        private boolean enabled = true;
    }
}
