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
package dev.macula.boot.starter.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * {@link TenantFilter} 租户上下文过滤测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void clearContext() {
        TenantContextHolder.clearCurrentTenantId();
    }

    @Test
    void resolvesTenantFromHeaderBeforeInvokingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GlobalConstants.TENANT_ID_NAME, "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(42L);
        verify(chain).doFilter(request, response);
    }

    @Test
    void fallsBackToRequestParameterAndClearsMissingTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(GlobalConstants.TENANT_ID_NAME, "7");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(7L);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    }
}
