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
package dev.macula.boot.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 租户与灰度版本 Micrometer 上下文访问器测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class ContextThreadLocalAccessorTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clearCurrentTenantId();
        GrayVersionContextHolder.clear();
    }

    @Test
    void shouldDiscoverAccessorsThroughServiceLoader() {
        ContextRegistry registry = new ContextRegistry().loadThreadLocalAccessors();

        assertThat(registry.getThreadLocalAccessors())
            .extracting(accessor -> accessor.key().toString())
            .contains(TenantContextThreadLocalAccessor.KEY, GrayVersionContextThreadLocalAccessor.KEY);
    }

    @Test
    void shouldCaptureRestoreAndClearContextOnReusedThread() throws Exception {
        ContextRegistry registry = new ContextRegistry()
            .registerThreadLocalAccessor(new TenantContextThreadLocalAccessor())
            .registerThreadLocalAccessor(new GrayVersionContextThreadLocalAccessor());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThat(executor.submit(this::currentContext).get()).isEqualTo("null:null");
            TenantContextHolder.setCurrentTenantId(1001L);
            GrayVersionContextHolder.setGrayVersion("gray-v2");
            ContextSnapshot snapshot = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .build()
                .captureAll();

            assertThat(executor.submit(snapshot.wrap(this::currentContext)).get())
                .isEqualTo("1001:gray-v2");
            assertThat(executor.submit(this::currentContext).get()).isEqualTo("null:null");
        } finally {
            executor.shutdownNow();
        }
    }

    private String currentContext() {
        return TenantContextHolder.getCurrentTenantId() + ":" + GrayVersionContextHolder.getGrayVersion();
    }
}
