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

import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.constants.GlobalConstants;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import dev.macula.boot.constants.CacheConstants;
import reactor.core.scheduler.Schedulers;

/**
 * 普通 HMAC 入口必须在调用下游之前执行网关归属校验。
 *
 * @author Rain
 * @since 6.1.0
 */
class KongApiGlobalFilterTest {
    @Test
    void rejectsOtherTenantBeforeInvokingChain() {
        var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
        AtomicBoolean invoked = new AtomicBoolean();
        exchange.getAttributes().put(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE, "previous");
        KongApiGlobalFilter filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("7", "12"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        filter.filter(exchange, ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(invoked).isFalse();
        assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
    }

    @Test
    void permitsMatchingTenantAndRejectsUnknownGateway() {
        AtomicBoolean invoked = new AtomicBoolean();
        KongApiGlobalFilter filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        filter.filter(KongApiUtilsTest.request("GET", "/orders", "app", ""), ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(invoked).isTrue();
        invoked.set(false);
        ReflectionTestUtils.setField(filter, "applicationName", "unknown-gateway");
        var unknown = KongApiUtilsTest.request("GET", "/orders", "app", "");
        filter.filter(unknown, ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(invoked).isFalse();
        assertThat(unknown.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    /** 验证 HMAC 过滤器发布的身份可通过统一工具读取，且保留原有 JWT claims。 */
    @Test
    void createsPrincipalWithVerifiedTenantInvokerAndTokenId() {
        var signed = KongApiUtilsTest.request("GET", "/orders?" + GlobalConstants.TOKEN_ID_NAME + "=fixed-id", "app", "");
        var exchange = signed.mutate().request(signed.getRequest().mutate()
            .header(GatewayConstants.REMOTE_INVOKE_NAME, "upstream").build()).build();
        var filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        AtomicBoolean invoked = new AtomicBoolean();
        filter.filter(exchange, forwarded -> {
            OAuth2AuthenticatedPrincipal principal = KongApiUtils.verifiedPrincipal(forwarded);
            assertThat(principal.getName()).isEqualTo("upstream");
            assertThat(principal.getAttributes()).containsEntry("sub", "upstream")
                .containsEntry("tenantId", 7L).containsEntry("jti", "fixed-id")
                .doesNotContainKeys("authType", "authTenantId");
            assertThat(principal.getAuthorities()).isEmpty();
            invoked.set(true);
            return Mono.empty();
        }).block();
        assertThat(invoked).isTrue();
    }

    @Test
    void preservesVerifiedPostBodyWhenPublishingPrincipal() {
        String body = "{\"message\":\"test\"}";
        var exchange = KongApiUtilsTest.request("POST", "/orders", "app", body);
        exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY,
            body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        AtomicBoolean invoked = new AtomicBoolean();
        filter.filter(exchange, forwarded -> {
            assertThat(forwarded.getAttributes()).containsKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
            return DataBufferUtils.join(forwarded.getRequest().getBody()).doOnNext(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(body);
                invoked.set(true);
            }).then();
        }).block();
        assertThat(invoked).isTrue();
    }

    /** 验证 Redis 查询离开事件线程，且下游异常不被认证失败兜底吞掉。 */
    @Test
    void offloadsRedisAndPropagatesDownstreamFailure() {
        var redis = KongApiUtilsTest.redis("12", "7");
        when(redis.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS)).thenAnswer(ignored -> {
            assertThat(Schedulers.isInNonBlockingThread()).isFalse();
            return java.util.Map.of("macula-samples", "12");
        });
        var filter = new KongApiGlobalFilter(redis);
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        var failure = new IllegalStateException("downstream failure");
        assertThatThrownBy(() -> filter.filter(KongApiUtilsTest.request("GET", "/orders", "app", ""),
            ignored -> Mono.error(failure)).subscribeOn(Schedulers.parallel()).block()).isSameAs(failure);
    }

    /** 验证网关租户缓存查询异常时拒绝请求并清除旧身份，不输出底层异常内容。 */
    @Test
    void rejectsCacheFailureWithoutInvokingDownstream() {
        var redis = KongApiUtilsTest.redis("12", "7");
        when(redis.<String, String>opsForHash().entries(anyString()))
            .thenThrow(new IllegalStateException("sensitive-cache-detail"));
        var filter = new KongApiGlobalFilter(redis);
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
        exchange.getAttributes().put(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE, "previous");
        AtomicBoolean invoked = new AtomicBoolean();
        filter.filter(exchange, ignored -> Mono.fromRunnable(() -> invoked.set(true))).block();
        assertThat(invoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
        assertThat(exchange.getResponse().getBodyAsString().block()).doesNotContain("sensitive-cache-detail");
    }

    /** 验证空 POST 请求通过验签后恢复为空请求体，不因缺少缓存数组而失败。 */
    @Test
    void restoresEmptyPostBodyWithoutCachedBytes() {
        var filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        AtomicBoolean invoked = new AtomicBoolean();
        filter.filter(KongApiUtilsTest.request("POST", "/orders", "app", ""), forwarded -> {
            assertThat(forwarded.getRequest().getHeaders().getContentLength()).isZero();
            assertThat(KongApiUtils.verifiedPrincipal(forwarded).getName()).isEqualTo("app");
            invoked.set(true);
            return Mono.empty();
        }).block();
        assertThat(invoked).isTrue();
    }

    /** 归属、认证租户或密钥非法时，完整验证不能发布身份或调用下游。 */
    @Test
    void rejectsInvalidTenantAndSecretBeforePublishingIdentity() {
        for (String[] tenants : new String[][] {{null, "7"}, {"invalid", "7"}, {"12", "invalid"},
            {"12", " "}, {"12", "9223372036854775808"}, {"12", "7"}}) {
            var applications = KongApiUtilsTest.redis(tenants[0], tenants[1]);
            if ("7".equals(tenants[1]) && "12".equals(tenants[0])) {
                when(applications.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_APPS + "app"))
                    .thenReturn(java.util.Map.of("secretKey", "wrong", "tenantId", "12"));
            }
            var filter = new KongApiGlobalFilter(applications);
            ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
            var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
                exchange.getAttributes().put(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE, "previous");
            AtomicBoolean invoked = new AtomicBoolean();
            filter.filter(exchange, ignored -> Mono.fromRunnable(() -> invoked.set(true))).block();
            assertThat(invoked).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
        }
    }

    /** URL 许可属于应用授权，即使纯验签成功也不能越过拒绝规则。 */
    @Test
    void enforcesUrlPermissionOutsideSignatureUtility() {
        var applications = KongApiUtilsTest.redis("12", "7");
        when(applications.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_APPS + "app"))
            .thenReturn(java.util.Map.of("secretKey", "test-secret", "tenantId", "12", "permitUrls", "POST:/other"));
        var filter = new KongApiGlobalFilter(applications);
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(exchange, "test-secret").isSuccess()).isTrue();
        AtomicBoolean invoked = new AtomicBoolean();
        filter.filter(exchange, ignored -> Mono.fromRunnable(() -> invoked.set(true))).block();
        assertThat(invoked).isFalse();
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("不允许访问该接口");
        assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
    }

    /** 业务租户来自准入结果，不读写调用线程的租户上下文；旧无认证租户配置继续回退归属。 */
    @Test
    void resolvesBusinessTenantWithoutThreadLocalSideEffects() {
        dev.macula.boot.context.TenantContextHolder.setCurrentTenantId(99L);
        try {
            for (String authTenant : new String[] {"7", ""}) {
                var filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", authTenant));
                ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
                filter.filter(KongApiUtilsTest.request("GET", "/orders", "app", ""), forwarded -> {
                    assertThat(KongApiUtils.verifiedPrincipal(forwarded).<Long>getAttribute("tenantId"))
                        .isEqualTo(authTenant.isEmpty() ? 12L : 7L);
                    return Mono.empty();
                }).block();
                assertThat(dev.macula.boot.context.TenantContextHolder.getCurrentTenantId()).isEqualTo(99L);
            }
        } finally {
            dev.macula.boot.context.TenantContextHolder.clearCurrentTenantId();
        }
    }

    /** 有效签名仍须通过集中 IP 准入，拒绝时清除旧身份且不能转发。 */
    @Test
    void enforcesPeerIpAfterValidHmacSignature() {
        var applications = KongApiUtilsTest.redis("12", "7");
        when(applications.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_APPS + "app"))
            .thenReturn(java.util.Map.of("secretKey", "test-secret", "tenantId", "12",
                CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS, "POST:/other\nGET:/orders",
                CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_IPS, "192.0.2.0/24"));
        var filter = new KongApiGlobalFilter(applications);
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        for (String ip : new String[] {"192.0.2.7", "203.0.113.1"}) {
            var signed = KongApiUtilsTest.request("GET", "/orders", "app", "");
            var exchange = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/orders")
                    .headers(signed.getRequest().getHeaders())
                    .header("X-Forwarded-For", "192.0.2.7")
                    .remoteAddress(new java.net.InetSocketAddress(ip, 1234)));
            exchange.getAttributes().put(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE, "stale");
            AtomicBoolean invoked = new AtomicBoolean();
            filter.filter(exchange, forwarded -> Mono.fromRunnable(() -> invoked.set(true))).block();
            assertThat(invoked.get()).isEqualTo(ip.equals("192.0.2.7"));
            if (!invoked.get()) {
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
            }
        }
    }

}
