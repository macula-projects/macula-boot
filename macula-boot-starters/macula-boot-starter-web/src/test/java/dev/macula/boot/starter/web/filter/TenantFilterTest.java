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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /** 清理测试线程上下文。 */
    @AfterEach
    void clearContext() {
        TenantContextHolder.clearCurrentTenantId();
    }

    /** 请求链内部可读取 Header 租户，退出后清理。 */
    @Test
    void resolvesTenantFromHeaderBeforeInvokingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GlobalConstants.TENANT_ID_NAME, "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(42L);
        filter.doFilter(request, response, chain);
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    }

    /** 无 Header 时读取参数，无租户时清除旧值。 */
    @Test
    void fallsBackToRequestParameterAndClearsMissingTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(GlobalConstants.TENANT_ID_NAME, "7");

        filter.doFilter(request, new MockHttpServletResponse(),
            (req, res) -> assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(7L));
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    }
    /** 下游异常或非法租户值也不得泄漏线程上下文。 */
    @Test
    void clearsOnFailure() {
        TenantContextHolder.setCurrentTenantId(99L);
        MockHttpServletRequest invalid = new MockHttpServletRequest();
        invalid.addHeader(GlobalConstants.TENANT_ID_NAME, "bad");
        assertThatThrownBy(() -> filter.doFilter(invalid, new MockHttpServletResponse(), mock(FilterChain.class)))
            .isInstanceOf(NumberFormatException.class);
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GlobalConstants.TENANT_ID_NAME, "7");
        MockHttpServletRequest valid = request;
        assertThatThrownBy(() -> filter.doFilter(valid, new MockHttpServletResponse(), (req, res) -> {
            throw new IllegalStateException("downstream");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    }
}
