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
    /** 专用资源服务器Key通过后复用原缓存及派生JWT清理，重复删除仍然成功。 */
    @Test
    @SuppressWarnings("unchecked")
    void callbackKeyCleansExistingCachesIdempotently() throws Exception {
        var properties = new GatewayProperties();
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(tokens.opsForValue()).thenReturn(values);
        var principal = new org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal(
            "fixture", java.util.Map.of("sub", "fixture", "iat", java.time.Instant.EPOCH), java.util.List.of());
        when(values.get(dev.macula.boot.constants.CacheConstants.GATEWAY_TOKEN_CACHE_KEY + "test-token"))
            .thenReturn(principal, null);
        var credentials = callbackCredentials(properties.getRmOpaqueTokenEndpoint());
        var filter = new RmOpaqueTokenEndpointFilter(properties, tokens, credentials);
        ReflectionTestUtils.setField(filter, "resourceClientId", "resource");
        for (int attempt = 0; attempt < 2; attempt++) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint())
                .header("Authorization", "Bearer sk-" + "a".repeat(43)).body("{\"token\":\"test-token\"}"));
            filter.filter(exchange, ignored -> Mono.error(new AssertionError("Callback must finish here"))).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        verify(tokens).delete(AddJwtGlobalFilter.buildKey(principal));
        verify(tokens).delete(dev.macula.boot.constants.CacheConstants.GATEWAY_TOKEN_CACHE_KEY + "test-token");
    }

    /** 错误/跨资源服务器/Query/重复凭据、Redis故障、HMAC和无凭据请求都不能清理。 */
    @Test
    @SuppressWarnings("unchecked")
    void callbackFailuresNeverReadOrDeleteTokenCache() throws Exception {
        for (int scenario = 0; scenario < 9; scenario++) {
            var properties = new GatewayProperties();
            RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
            var credentials = callbackCredentials(properties.getRmOpaqueTokenEndpoint());
            var filter = new RmOpaqueTokenEndpointFilter(properties, tokens, credentials);
            ReflectionTestUtils.setField(filter, "resourceClientId", scenario == 1 ? "other-resource" : "resource");
            var request = MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint()
                + (scenario == 2 ? "?access_token=sk-" + "a".repeat(43) : ""));
            if (scenario != 2 && scenario != 7 && scenario != 6) {
                request.header("Authorization", "Bearer sk-" + (scenario == 0 ? "b" : "a").repeat(43));
            }
            if (scenario == 6) { request.header("Authorization", "hmac username=\"app\", signature=\"fixture\""); }
            if (scenario == 8) { ReflectionTestUtils.setField(filter, "resourceClientId", ""); }
            if (scenario == 3) { request.header("Authorization", "Bearer sk-" + "a".repeat(43)); }
            if (scenario == 4) {
                when(credentials.<String, String>opsForHash().entries(anyString()))
                    .thenThrow(new IllegalStateException("synthetic Redis failure"));
            }
            if (scenario == 5) {
                when(credentials.<String, String>opsForHash().entries(anyString())).thenReturn(java.util.Map.of());
            }
            var exchange = MockServerWebExchange.from(request.body("{\"token\":\"test-token\"}"));
            filter.filter(exchange, ignored -> Mono.empty()).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(tokens);
        }
    }

    /** 认证通过仍须校验正文；解析异常和运行日志均不能泄漏token与API Key。 */
    @Test
    @SuppressWarnings("unchecked")
    void validatesPayloadWithoutLoggingCredentials() throws Exception {
        Logger logger = (Logger)LoggerFactory.getLogger(RmOpaqueTokenEndpointFilter.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            var properties = new GatewayProperties();
            RedisTemplate<String, Object> tokens = mock(RedisTemplate.class);
            var filter = new RmOpaqueTokenEndpointFilter(properties, tokens,
                callbackCredentials(properties.getRmOpaqueTokenEndpoint()));
            ReflectionTestUtils.setField(filter, "resourceClientId", "resource");
            for (String body : new String[] {"", "{}", "{\"token\":\"sensitive-json-fragment\""}) {
                var exchange = MockServerWebExchange.from(MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint())
                    .header("Authorization", "Bearer sk-" + "a".repeat(43)).body(body));
                filter.filter(exchange, ignored -> Mono.empty()).block();
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
            verifyNoInteractions(tokens);
            assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("sensitive-json-fragment", "sk-" + "a".repeat(43));
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            appender.stop();
        }
    }

    /** 非清理端点正常进入后续链路，不读取凭据或token缓存。 */
    @Test
    @SuppressWarnings("unchecked")
    void passesOtherEndpointsWithoutRedisAccess() {
        RedisTemplate<String, Object> tokens = mock(RedisTemplate.class), credentials = mock(RedisTemplate.class);
        var filter = new RmOpaqueTokenEndpointFilter(new GatewayProperties(), tokens, credentials);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/business"));
        var called = new java.util.concurrent.atomic.AtomicBoolean();
        filter.filter(exchange, ignored -> Mono.fromRunnable(() -> called.set(true))).block();
        assertThat(called).isTrue();
        verifyNoInteractions(tokens, credentials);
    }

    /** 构造只绑定单个资源服务器和准确路径的专用摘要。 */
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> callbackCredentials(String path) throws Exception {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        org.springframework.data.redis.core.HashOperations<String, String, String> hashes = mock(
            org.springframework.data.redis.core.HashOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
            .digest(("sk-" + "a".repeat(43)).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(hashes.entries(anyString())).thenReturn(java.util.Map.of());
        when(hashes.entries(dev.macula.boot.constants.CacheConstants.IAM_CALLBACK_CREDENTIAL_PREFIX + "resource"))
            .thenReturn(java.util.Map.of(dev.macula.boot.constants.CacheConstants.IAM_CALLBACK_KEY_DIGEST, digest,
                dev.macula.boot.constants.CacheConstants.IAM_CALLBACK_PATH, path));
        return redis;
    }

}
