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

import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * token 清理入口的租户拒绝必须先于读取或删除 token 缓存。
 *
 * @author Rain
 * @since 6.1.0
 */
class RmOpaqueTokenEndpointFilterTest {
    @Test
    @SuppressWarnings("unchecked")
    void rejectsMismatchedTenantWithoutTokenSideEffects() {
        GatewayProperties properties = new GatewayProperties();
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app", "{\"token\":\"test-token\"}");
        RmOpaqueTokenEndpointFilter filter = new RmOpaqueTokenEndpointFilter(properties, tokens,
            hmac(KongApiUtilsTest.redis("7", "12"), "macula-samples-gateway"));
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(tokens);
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownGatewayCannotCleanTokens() {
        GatewayProperties properties = new GatewayProperties();
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app", "{\"token\":\"test-token\"}");
        RmOpaqueTokenEndpointFilter filter = new RmOpaqueTokenEndpointFilter(properties, tokens,
            hmac(KongApiUtilsTest.redis("12", "7"), "unknown-gateway"));
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(tokens);
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchingTenantCanReachTokenLookup() {
        GatewayProperties properties = new GatewayProperties();
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(tokens.opsForValue()).thenReturn(values);
        var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app", "{\"token\":\"test-token\"}");
        RmOpaqueTokenEndpointFilter filter = new RmOpaqueTokenEndpointFilter(properties, tokens,
            hmac(KongApiUtilsTest.redis("12", "7"), "macula-samples-gateway"));
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(values).get(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void explicitlyDisabledHmacRetainsLegacyBehavior() {
        GatewayProperties properties = new GatewayProperties();
        properties.setForceHmacRmOpaqueTokenEndpoint(false);
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(tokens.opsForValue()).thenReturn(values);
        var applications = KongApiUtilsTest.redis("7", null);
        var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app", "{\"token\":\"test-token\"}");
        new RmOpaqueTokenEndpointFilter(properties, tokens, hmac(applications, "macula-samples-gateway"))
            .filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(applications);
    }
    /** token 清理仍须校验应用 URL 许可和签名，不能因改用纯验签工具而绕过策略。 */
    @Test
    @SuppressWarnings("unchecked")
    void deniedUrlAndInvalidSignatureCannotCleanTokens() {
        for (boolean deniedUrl : new boolean[] {true, false}) {
            var properties = new GatewayProperties();
            RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
            var applications = KongApiUtilsTest.redis("12", "7");
            when(applications.<String, String>opsForHash().entries(
                dev.macula.boot.constants.CacheConstants.SECURITY_SYSTEM_APPS + "app"))
                .thenReturn(java.util.Map.of("secretKey", deniedUrl ? "test-secret" : "wrong",
                    "tenantId", "12", "permitUrls", deniedUrl ? "GET:/other" : "POST:/**"));
            var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app",
                "{\"token\":\"test-token\"}");
            new RmOpaqueTokenEndpointFilter(properties, tokens, hmac(applications, "macula-samples-gateway"))
                .filter(exchange, ignored -> Mono.empty()).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(tokens);
        }
    }

    /** DEBUG 日志不得包含成功请求 token、失败 Header/Cookie 或 JSON 解析异常中的正文片段。 */
    @Test
    @SuppressWarnings("unchecked")
    void debugLogsNeverContainRawCredentialsOrPayload() {
        Logger logger = (Logger)LoggerFactory.getLogger(RmOpaqueTokenEndpointFilter.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            var properties = new GatewayProperties();
            RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
            ValueOperations<String, Object> values = mock(ValueOperations.class);
            when(tokens.opsForValue()).thenReturn(values);
            var filter = new RmOpaqueTokenEndpointFilter(properties, tokens,
                hmac(KongApiUtilsTest.redis("12", "7"), "macula-samples-gateway"));
            var valid = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app",
                "{\"token\":\"sensitive-opaque-token\"}");
            filter.filter(valid, ignored -> Mono.empty()).block();
            assertThat(valid.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            var invalid = MockServerWebExchange.from(MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint())
                .header("Authorization", "Bearer sk-sensitive-key").header("Cookie", "SESSION=sensitive-cookie"));
            filter.filter(invalid, ignored -> Mono.empty()).block();
            assertThat(invalid.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            properties.setForceHmacRmOpaqueTokenEndpoint(false);
            var malformed = MockServerWebExchange.from(MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint())
                .body("{\"token\":\"sensitive-json-fragment\""));
            filter.filter(malformed, ignored -> Mono.empty()).block();
            assertThat(malformed.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("sensitive-opaque-token", "sk-sensitive-key",
                    "sensitive-cookie", "sensitive-json-fragment");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            appender.stop();
        }
    }

    /** 创建与生产相同准入配置的 HMAC 验证器，避免清理测试绕过过滤器策略。 */
    private KongApiGlobalFilter hmac(RedisTemplate<String, Object> applications, String applicationName) {
        var filter = new KongApiGlobalFilter(applications);
        ReflectionTestUtils.setField(filter, "applicationName", applicationName);
        return filter;
    }

    /** 强制 HMAC 的清理入口不能绕过 IP，拒绝时不读写 token 缓存。 */
    @Test
    @SuppressWarnings("unchecked")
    void deniedIpCannotReadOrRemoveTokenCache() {
        var properties = new GatewayProperties();
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        var applications = KongApiUtilsTest.redis("12", "7");
        when(applications.<String, String>opsForHash().entries(
            dev.macula.boot.constants.CacheConstants.SECURITY_SYSTEM_APPS + "app"))
            .thenReturn(java.util.Map.of("secretKey", "test-secret", "tenantId", "12",
                dev.macula.boot.constants.CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_IPS, "192.0.2.0/24"));
        var exchange = KongApiUtilsTest.request("POST", properties.getRmOpaqueTokenEndpoint(), "app",
            "{\"token\":\"test-token\"}");
        new RmOpaqueTokenEndpointFilter(properties, tokens, hmac(applications, "macula-samples-gateway"))
            .filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(tokens);
    }

}
