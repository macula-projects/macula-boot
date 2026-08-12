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
package dev.macula.boot.starter.websocket.feign;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import feign.RequestTemplate;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link WebSocketFeignRequestInterceptor} 请求头转发测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class WebSocketFeignRequestInterceptorTest {

    private final WebSocketFeignRequestInterceptor interceptor = new WebSocketFeignRequestInterceptor();

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        GrayVersionContextHolder.clear();
    }

    @Test
    void relaysJwtTraceIdAndGrayVersionOutsideServletRequest() {
        Jwt jwt = new Jwt("encoded-token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), Map
            .of("sub", "alice"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        GrayVersionContextHolder.setGrayVersion("gray-v2");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get(SecurityConstants.AUTHORIZATION_KEY))
            .containsExactly(SecurityConstants.TOKEN_PREFIX + "encoded-token");
        assertThat(template.headers().get(GlobalConstants.FEIGN_REQ_ID)).singleElement().asString().isNotBlank();
        assertThat(template.headers().get(GlobalConstants.GRAY_VERSION_TAG)).containsExactly("gray-v2");
    }

    @Test
    void leavesRegularServletRequestsToThePrimaryFeignInterceptor() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).isEmpty();
    }
}
