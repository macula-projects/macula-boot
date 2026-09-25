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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code ContextHolderTest} 租户与灰度版本上下文单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class ContextHolderTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clearCurrentTenantId();
        GrayVersionContextHolder.clear();
        GrayVersionMetaHolder.clear();
    }

    @Test
    void shouldStoreAndClearCurrentTenant() {
        TenantContextHolder.setCurrentTenantId(1001L);

        assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(1001L);

        TenantContextHolder.clearCurrentTenantId();
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    }

    @Test
    void shouldStoreAndClearRequestGrayVersion() {
        GrayVersionContextHolder.setGrayVersion("gray-v2");

        assertThat(GrayVersionContextHolder.getGrayVersion()).isEqualTo("gray-v2");

        GrayVersionContextHolder.clear();
        assertThat(GrayVersionContextHolder.getGrayVersion()).isNull();
    }

    @Test
    void shouldStoreAndClearInstanceGrayVersion() {
        GrayVersionMetaHolder.setGrayVersion("gray-meta-v1");

        assertThat(GrayVersionMetaHolder.getGrayVersion()).isEqualTo("gray-meta-v1");

        GrayVersionMetaHolder.clear();
        assertThat(GrayVersionMetaHolder.getGrayVersion()).isNull();
    }

}
