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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import java.net.URL;

/**
 * Rejects the official XXL client when the minimal compatibility API is active.
 *
 * @author Rain
 * @since 6.1.0
 */
public final class XxlJobClientConflictDetector implements InitializingBean {

    static final String XXL_EXECUTOR_RESOURCE = "com/xxl/job/core/executor/XxlJobExecutor.class";

    private final ClassLoader classLoader;

    public XxlJobClientConflictDetector() {
        this(ClassUtils.getDefaultClassLoader());
    }

    XxlJobClientConflictDetector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void afterPropertiesSet() {
        URL officialExecutor = classLoader == null ? null : classLoader.getResource(XXL_EXECUTOR_RESOURCE);
        if (officialExecutor != null) {
            throw new IllegalStateException("Official xxl-job-core conflicts with macula-boot-starter-task; "
                + "remove com.xuxueli:xxl-job-core and use the built-in XXL-to-SnailJob adapter");
        }
    }
}
