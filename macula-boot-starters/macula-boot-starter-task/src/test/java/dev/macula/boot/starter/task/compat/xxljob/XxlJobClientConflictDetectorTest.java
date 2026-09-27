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

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests detection of an accidentally reintroduced official XXL client.
 *
 * @author Rain
 * @since 6.1.0
 */
class XxlJobClientConflictDetectorTest {

    @Test
    void acceptsClasspathWithoutOfficialExecutor() {
        ClassLoader classLoader = new ClassLoader(null) {
        };

        assertThatCode(() -> new XxlJobClientConflictDetector(classLoader).afterPropertiesSet())
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsClasspathContainingOfficialExecutor() throws MalformedURLException {
        URL resource = new URL("file:/xxl-job-core.jar!/com/xxl/job/core/executor/XxlJobExecutor.class");
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return XxlJobClientConflictDetector.XXL_EXECUTOR_RESOURCE.equals(name) ? resource : null;
            }
        };

        assertThatThrownBy(() -> new XxlJobClientConflictDetector(classLoader).afterPropertiesSet())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("remove com.xuxueli:xxl-job-core");
    }
}
