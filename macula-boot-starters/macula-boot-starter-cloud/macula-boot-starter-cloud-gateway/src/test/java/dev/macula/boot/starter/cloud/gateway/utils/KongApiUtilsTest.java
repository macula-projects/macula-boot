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
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 纯 HMAC 签名校验及其无身份发布副作用的回归测试，提供其他测试共用的签名请求夹具。
 *
 * @author Rain
 * @since 6.1.0
 */
public class KongApiUtilsTest {
    /** 清除验证无 ThreadLocal 副作用时设置的测试上下文。 */
    @AfterEach
    void clearTenant() {
        TenantContextHolder.clearCurrentTenantId();
    }

    /** 签名规则覆盖方法、Query 和请求体，验签不产生应用身份或改变租户上下文。 */
    @Test
    void verifiesRequestWithoutPublishingIdentity() {
        TenantContextHolder.setCurrentTenantId(99L);
        for (String method : new String[] {"GET", "DELETE", "POST", "PUT", "PATCH"}) {
            String body = "{\"message\":\"test\"}";
            var exchange = request(method, "/orders?keep=value", "app", body);
            exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY,
                body.getBytes(StandardCharsets.UTF_8));
            assertThat(KongApiUtils.checkSign(exchange, "test-secret").isSuccess()).isTrue();
            assertThat(exchange.getAttributes()).doesNotContainKey(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
            assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo(99L);
        }
    }

    /** 错误密钥、空密钥及路径、Query、Body 被修改时均须拒绝。 */
    @Test
    void rejectsWrongSecretAndModifiedRequest() {
        var request = request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(request, "wrong").isSuccess()).isFalse();
        assertThat(KongApiUtils.checkSign(request, "").isSuccess()).isFalse();
        var modified = request.mutate().request(request.getRequest().mutate()
            .uri(java.net.URI.create("/other?changed=yes")).build()).build();
        assertThat(KongApiUtils.checkSign(modified, "test-secret").isSuccess()).isFalse();
        var body = request("POST", "/orders", "app", "original");
        body.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY,
            "modified".getBytes(StandardCharsets.UTF_8));
        assertThat(KongApiUtils.checkSign(body, "test-secret").isSuccess()).isFalse();
    }

    /** 缺少签名、缺少日期、非法日期及过期请求不能通过验签。 */
    @Test
    void rejectsIncompleteAndExpiredSignatures() {
        var missing = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
            .header("Authorization", "hmac username=\"app\","));
        assertThat(KongApiUtils.checkSign(missing, "test-secret").isSuccess()).isFalse();
        for (String date : new String[] {"", "invalid", "Mon, 01 Jan 2024 00:00:00 GMT"}) {
            var invalid = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
                .header("Authorization", "hmac username=\"app\", signature=\"invalid\"").header("Date", date));
            assertThat(KongApiUtils.checkSign(invalid, "test-secret").isSuccess()).isFalse();
        }
    }

    /** 单独验签不构成完整认证，已有请求属性也不应被工具清理或覆盖。 */
    @Test
    void pureSignatureDoesNotChangePublishedAttributes() {
        var exchange = request("GET", "/orders", "app", "");
        assertThat(KongApiUtils.checkSign(exchange, "test-secret").isSuccess()).isTrue();
        assertThatThrownBy(() -> KongApiUtils.verifiedPrincipal(exchange)).isInstanceOf(BadCredentialsException.class);
        exchange.getAttributes().put("testAttribute", "unchanged");
        assertThat(KongApiUtils.checkSign(exchange, "test-secret").isSuccess()).isTrue();
        assertThat(exchange.<String>getAttribute("testAttribute")).isEqualTo("unchanged");
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
