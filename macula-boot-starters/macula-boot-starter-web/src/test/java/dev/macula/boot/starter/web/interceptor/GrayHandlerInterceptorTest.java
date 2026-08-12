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
package dev.macula.boot.starter.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * {@link GrayHandlerInterceptor} 灰度上下文拦截测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class GrayHandlerInterceptorTest {

    private final GrayHandlerInterceptor interceptor = new GrayHandlerInterceptor();

    @AfterEach
    void clearContext() {
        GrayVersionContextHolder.clear();
    }

    @Test
    void storesGrayVersionForRequestAndClearsItAfterCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GlobalConstants.GRAY_VERSION_TAG, "v2");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(GrayVersionContextHolder.getGrayVersion()).isEqualTo("v2");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertThat(GrayVersionContextHolder.getGrayVersion()).isNull();
    }

    @Test
    void ignoresBlankGrayVersion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GlobalConstants.GRAY_VERSION_TAG, "");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(GrayVersionContextHolder.getGrayVersion()).isNull();
    }
}
