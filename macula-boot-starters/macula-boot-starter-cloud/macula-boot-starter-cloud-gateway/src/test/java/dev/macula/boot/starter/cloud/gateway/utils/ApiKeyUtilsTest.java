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

package dev.macula.boot.starter.cloud.gateway.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * API Key 输入来源、格式及可信身份边界测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class ApiKeyUtilsTest {
    @Test
    void resolvesHeaderAndQueryAcrossMethods() {
        assertThat(ApiKeyUtils.resolveApiKey(MockServerWebExchange.from(MockServerHttpRequest.get("/")
            .header("Authorization", "bEaReR sk-test")))).isEqualTo("sk-test");
        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.PATCH, HttpMethod.DELETE}) {
            assertThat(ApiKeyUtils.resolveApiKey(MockServerWebExchange.from(
                MockServerHttpRequest.method(method, "/?access_token=sk-test")))).isEqualTo("sk-test");
        }
    }

    @Test
    void rejectsAmbiguousAndMalformedCredentials() {
        for (var builder : new MockServerHttpRequest.BaseBuilder<?>[] {
            MockServerHttpRequest.get("/?access_token=sk-test&access_token=other"),
            MockServerHttpRequest.post("/?access_token=sk-test").header("Authorization", "Bearer other"),
            MockServerHttpRequest.get("/").header("Authorization", "Bearer other", "Bearer sk-test"),
            MockServerHttpRequest.get("/").header("Authorization", "Bearer sk-"),
            MockServerHttpRequest.get("/").header("Authorization", "Bearer sk-test value"),
            MockServerHttpRequest.get("/?access_token=sk-test&access_token="),
            MockServerHttpRequest.get("/").header("Authorization", "Bearer sk-test=bad"),
            MockServerHttpRequest.get("/").header("Authorization", "Bearer  sk-test")}) {
            assertThatThrownBy(() -> ApiKeyUtils.resolveApiKey(MockServerWebExchange.from(builder)))
                .isInstanceOf(OAuth2AuthenticationException.class).hasMessage("Invalid API Key credentials");
        }
    }

    @Test
    void leavesOtherSchemesAndAnonymousRequestsAlone() {
        for (String token : new String[] {"Bearer oauth-token", "hmac username=\"app\", signature=\"test\""}) {
            assertThat(ApiKeyUtils.resolveApiKey(MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Authorization", token)))).isNull();
        }
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        assertThat(ApiKeyUtils.resolveApiKey(exchange)).isNull();
        assertThatThrownBy(() -> ApiKeyUtils.verifiedPrincipal(exchange)).isInstanceOf(BadCredentialsException.class);
    }
}
