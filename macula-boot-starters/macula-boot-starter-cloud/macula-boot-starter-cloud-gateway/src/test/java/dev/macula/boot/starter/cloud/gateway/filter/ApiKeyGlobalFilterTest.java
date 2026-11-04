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

package dev.macula.boot.starter.cloud.gateway.filter;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.ApiKeyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * API Key 密钥验证、租户准入及失败关闭测试。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ApiKeyGlobalFilterTest {
    private RedisTemplate<String, Object> redis;
    private HashOperations<String, Object, Object> hashes;
    private ApiKeyGlobalFilter filter;
    private Map<Object, Object> identity;

    @BeforeEach
    void setUp() {
        redis = mock(RedisTemplate.class);
        hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        identity = new HashMap<>(Map.of("applicationCode", "app", "applicationId", "42", "tenantId", "12"));
        when(hashes.entries(anyString())).thenReturn(identity);
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_TENANTS)).thenReturn(Map.of("sample", "12"));
        filter = new ApiKeyGlobalFilter(redis);
        ReflectionTestUtils.setField(filter, "applicationName", "sample-gateway");
    }

    @Test
    void matchingTenantPublishesIdentityAndPreservesHeader() {
        var exchange = request();
        AtomicInteger calls = new AtomicInteger();
        filter.filter(exchange, forwarded -> {
            assertThat(ApiKeyUtils.verifiedPrincipal(forwarded).<Long>getAttribute("tenantId")).isEqualTo(12L);
            assertThat(forwarded.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer sk-test");
            return Mono.fromRunnable(calls::incrementAndGet);
        }).block();
        assertThat(calls).hasValue(1);
    }

    @Test
    void invalidOwnerOrIdentityCannotReachDownstream() {
        for (Object tenant : new Object[] {"7", "", "invalid", "-1", "-2", "9223372036854775808", 12L}) {
            identity.put("tenantId", tenant);
            assertRejected(request());
        }
        identity.remove("tenantId");
        assertRejected(request());
        identity.put("tenantId", "12");
        for (String field : new String[] {"applicationCode", "applicationId"}) {
            Object value = identity.remove(field);
            assertRejected(request());
            identity.put(field, value);
        }
    }

    @Test
    void missingUnknownAndFailingCachesAreRejectedWithoutLeakingSecrets() {
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_TENANTS)).thenReturn(Map.of());
        assertRejected(request());
        when(hashes.entries(anyString())).thenReturn(Map.of());
        assertRejected(request());
        when(hashes.entries(anyString())).thenThrow(new IllegalStateException("secret sk-test"));
        assertRejected(request());
    }

    @Test
    void disabledOrAmbiguousInputNeverReadsRedis() {
        ReflectionTestUtils.setField(filter, "enabled", false);
        assertRejected(request());
        verifyNoInteractions(hashes);
        ReflectionTestUtils.setField(filter, "enabled", true);
        assertRejected(MockServerWebExchange.from(MockServerHttpRequest.get("/?access_token=sk-test")
            .header("Authorization", "Bearer other")));
        verifyNoInteractions(hashes);
    }

    @Test
    void callerTenantHeaderCannotOverrideAdmissionAndOldIdentityIsCleared() {
        identity.put("tenantId", "7");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
            .header("Authorization", "Bearer sk-test").header("tenantId", "7"));
        exchange.getAttributes().put(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE, "previous");
        assertRejected(exchange);
    }

    /** 非 API Key 请求无论功能是否启用都应放行，且不得访问密钥缓存。 */
    @Test
    void nonApiKeyRequestPassesOnceWithoutRedis() {
        for (boolean enabled : new boolean[] {true, false}) {
            ReflectionTestUtils.setField(filter, "enabled", enabled);
            AtomicInteger calls = new AtomicInteger();
            filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/")),
                ignored -> Mono.fromRunnable(calls::incrementAndGet)).block();
            assertThat(calls).hasValue(1);
        }
        verifyNoInteractions(hashes);
    }

    /** API Key 应用许可按 HTTP 方法及路径匹配，不允许的接口不得到达下游。 */
    @Test
    void enforcesApplicationUrlPermission() {
        identity.put(CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS, "POST:/allowed");
        assertRejected(request());
        identity.put(CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS, "GET:/");
        AtomicInteger calls = new AtomicInteger();
        filter.filter(request(), ignored -> Mono.fromRunnable(calls::incrementAndGet)).block();
        assertThat(calls).hasValue(1);
    }

    /** API Key 同样以归属租户准入，以独立认证租户构造 principal。 */
    @Test
    void usesAuthenticationTenantAfterOwnerAdmission() {
        identity.put(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID, "7");
        AtomicInteger calls = new AtomicInteger();
        filter.filter(request(), forwarded -> {
            assertThat(ApiKeyUtils.verifiedPrincipal(forwarded).<Long>getAttribute("tenantId")).isEqualTo(7L);
            return Mono.fromRunnable(calls::incrementAndGet);
        }).block();
        assertThat(calls).hasValue(1);
        identity.put(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID, "invalid");
        assertRejected(request());
    }

    /** 验证成功后的下游异常原样传播，不能被验证阶段的 Result 兜底处理。 */
    @Test
    void propagatesDownstreamFailureAfterSuccessfulValidation() {
        var failure = new IllegalStateException("downstream failure");
        assertThatThrownBy(() -> filter.filter(request(), ignored -> Mono.error(failure)).block())
            .isSameAs(failure);
    }

    private MockServerWebExchange request() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/").header("Authorization", "Bearer sk-test"));
    }

    private void assertRejected(MockServerWebExchange exchange) {
        AtomicInteger calls = new AtomicInteger();
        filter.filter(exchange, ignored -> Mono.fromRunnable(calls::incrementAndGet)).block();
        assertThat(calls).hasValue(0);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE);
        assertThat(exchange.getResponse().getBodyAsString().block()).doesNotContain("sk-test", "secret");
    }
}
