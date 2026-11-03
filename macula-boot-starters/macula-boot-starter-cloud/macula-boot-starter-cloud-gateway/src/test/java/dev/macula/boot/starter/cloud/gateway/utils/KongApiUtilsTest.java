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

import cn.hutool.core.date.DatePattern;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.TenantContextHolder;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 通过真实 HMAC 签名验证应用归属、认证租户和失败时的身份隔离。
 *
 * @author Rain
 * @since 6.1.0
 */
public class KongApiUtilsTest {
    @AfterEach
    void clearTenant() {
        TenantContextHolder.clearCurrentTenantId();
    }

    @Test
    void acceptsMatchingOwnerAndUsesSeparateAuthTenant() {
        var exchange = request("GET", "/orders", "app", "");
        TenantContextHolder.setCurrentTenantId(99L);
        assertThat(KongApiUtils.checkSign(exchange, redis("12", "7"), "GET:/orders", "macula-samples-gateway").isSuccess()).isTrue();
        KongApiUtils.HmacIdentity identity = exchange.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        assertThat(identity.applicationCode()).isEqualTo("app");
        assertThat(identity.tenantId()).isEqualTo(7L);
        assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(99L);
    }

    @Test
    void rejectsWrongOrMissingOwnerWithoutPublishingIdentity() {
        for (String owner : new String[] {"7", null, "invalid"}) {
            var exchange = request("GET", "/orders", "app", "");
            exchange.getAttributes().put(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE,
                new KongApiUtils.HmacIdentity("previous", 12L));
            assertThat(KongApiUtils.checkSign(exchange, redis(owner, "7"), "GET:/orders", "macula-samples-gateway").isSuccess()).isFalse();
            assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        }
    }

    @Test
    void supportsUnrestrictedGatewayAndOldCache() {
        var exchange = request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(exchange, redis("12", null), "GET:/orders").isSuccess()).isTrue();
        KongApiUtils.HmacIdentity identity = exchange.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        assertThat(identity.tenantId()).isEqualTo(12L);
        var withoutTenant = request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(withoutTenant, redis(null, null), "GET:/orders").isSuccess()).isTrue();
        KongApiUtils.HmacIdentity emptyIdentity = withoutTenant.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        assertThat(emptyIdentity.tenantId()).isNull();
    }

    @Test
    void invalidAuthTenantAndBadSignatureNeverPublishIdentity() {
        for (String invalidTenant : new String[] {"invalid", " ", "9223372036854775808"}) {
            var invalid = request("GET", "/orders", "app", "");
            assertThat(KongApiUtils.checkSign(invalid, redis("12", invalidTenant), "GET:/orders", "macula-samples-gateway").isSuccess()).isFalse();
            assertThat(invalid.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        }
        var badSignature = request("GET", "/orders", "app", "");
        var wrongKey = redis("12", "7");
        when(wrongKey.<String, String>opsForHash().entries(anyString())).thenReturn(Map.of("secretKey", "wrong"));
        assertThat(KongApiUtils.checkSign(badSignature, wrongKey, "GET:/orders").isSuccess()).isFalse();
        assertThat(badSignature.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
    }

    @Test
    void deniedPathAndExpiredRequestDoNotPublishIdentity() {
        var denied = request("GET", "/orders", "app", "");
        var cache = redis("12", "7");
        when(cache.<String, String>opsForHash().entries(anyString())).thenReturn(
            Map.of("secretKey", "test-secret", "tenantId", "12", "permitUrls", "POST:/other"));
        assertThat(KongApiUtils.checkSign(denied, cache, "GET:/orders", "macula-samples-gateway").isSuccess()).isFalse();
        assertThat(denied.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        var expired = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
            .header("Authorization", "hmac username=\"app\", signature=\"invalid\"")
            .header("Date", "Mon, 01 Jan 2024 00:00:00 GMT"));
        assertThat(KongApiUtils.checkSign(expired, redis("12", "7"), "GET:/orders", "macula-samples-gateway").isSuccess()).isFalse();
    }

    @Test
    void interleavedRequestsAndLegacySignatureDoNotChangeThreadContext() {
        var first = request("GET", "/orders", "first", "");
        var second = request("GET", "/orders", "second", "");
        KongApiUtils.checkSign(first, redis("12", "7"), "GET:/orders", "macula-samples-gateway");
        KongApiUtils.checkSign(second, redis("12", "8"), "GET:/orders", "macula-samples-gateway");
        KongApiUtils.HmacIdentity one = first.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        KongApiUtils.HmacIdentity two = second.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        assertThat(one.tenantId()).isEqualTo(7L);
        assertThat(two.tenantId()).isEqualTo(8L);
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
        assertThat(KongApiUtils.checkSign(first, redis("12", null), "GET:/orders").isSuccess()).isTrue();
        KongApiUtils.HmacIdentity legacy = first.getAttribute(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        assertThat(legacy.tenantId()).isEqualTo(12L);
        assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
        TenantContextHolder.setCurrentTenantId(99L);
        assertThat(KongApiUtils.checkSign(first, redis("12", null), "GET:/orders").isSuccess()).isTrue();
        assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(99L);
    }

    @Test
    void resolvesGatewayTenantUsingLongestCaseInsensitiveMatchingCode() {
        var cache = redis("12", null);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("macula", "1", "macula-samples", "12"));
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(12L);
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "macula-cloud-gateway")).isEqualTo(1L);
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "other-gateway")).isEqualTo(-1L);
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "")).isEqualTo(-1L);
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, null)).isEqualTo(-1L);
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "MACULA-SAMPLES-gateway")).isEqualTo(12L);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("MACULA", "1", "MaCuLa-SaMpLeS", "12"));
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "macula-samples-GATEWAY")).isEqualTo(12L);
    }

    @Test
    void missingAndInvalidTenantCacheResolveToUnknownWithoutBroaderFallback() {
        var cache = redis("12", null);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of());
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(-1L);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("macula", "1", "macula-samples", "invalid", "", "12"));
        assertThat(KongApiUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(-1L);
        var exchange = request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(exchange, redis("-1", "12"), "GET:/orders", "unknown-gateway").isSuccess()).isFalse();
    }

    /** 创建使用测试密钥签名的请求。 */
    public static MockServerWebExchange request(String method, String path, String app, String body) {
        try {
            String date = DatePattern.HTTP_DATETIME_FORMAT.format(new Date());
            String payload = "date: " + date + "\n" + method + " " + path + " HTTP/1.1";
            if (!"GET".equals(method) && !"DELETE".equals(method)) {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
                payload += "\ndigest: SHA-256=" + java.util.Base64.getEncoder().encodeToString(hash);
            }
            String signature = new HMac(HmacAlgorithm.HmacSHA256,
                "test-secret".getBytes(StandardCharsets.UTF_8)).digestBase64(payload, false);
            return MockServerWebExchange.from(MockServerHttpRequest.method(org.springframework.http.HttpMethod.valueOf(method), path)
                .header("Date", date).header("Authorization", "hmac username=\"" + app
                    + "\", algorithm=\"hmac-sha256\", signature=\"" + signature + "\"").body(body));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** 创建不依赖外部 Redis 的应用配置缓存。 */
    @SuppressWarnings("unchecked")
    public static RedisTemplate<String, Object> redis(String owner, String authTenant) {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, String, String> hashes = mock(HashOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        Map<String, String> values = new HashMap<>();
        values.put(CacheConstants.SECURITY_SYSTEM_APPS_SECRIT_KEY, "test-secret");
        if (owner != null) {
            values.put(GlobalConstants.TENANT_ID_NAME, owner);
        }
        if (authTenant != null) {
            values.put(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID, authTenant);
        }
        when(hashes.entries(anyString())).thenReturn(values);
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("macula-samples", "12", "macula-cloud", "1"));
        return redis;
    }
}
