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

package dev.macula.boot.starter.task.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Supplies the SnailJob enabled default before Boot evaluates its auto-configuration conditions.
 *
 * @author Rain
 * @since 6.1.0
 */
public final class TaskEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "maculaTaskDefaults";
    static final String OVERRIDE_PROPERTY_SOURCE_NAME = "maculaTaskOverrides";
    static final String TASK_ENABLED = "macula.task.enabled";
    static final String SNAIL_JOB_ENABLED = "snail-job.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(TASK_ENABLED, Boolean.class, true)) {
            environment.getPropertySources().addFirst(
                new MapPropertySource(OVERRIDE_PROPERTY_SOURCE_NAME, Map.of(SNAIL_JOB_ENABLED, Boolean.FALSE)));
            return;
        }
        if (environment.containsProperty(SNAIL_JOB_ENABLED)) {
            return;
        }
        environment.getPropertySources().addLast(
            new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(SNAIL_JOB_ENABLED, Boolean.TRUE)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
