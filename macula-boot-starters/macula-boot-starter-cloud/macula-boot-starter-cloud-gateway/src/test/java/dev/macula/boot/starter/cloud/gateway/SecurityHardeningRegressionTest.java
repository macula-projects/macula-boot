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


package dev.macula.boot.starter.cloud.gateway;

import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.config.JwtConfiguration;
import dev.macula.boot.starter.cloud.gateway.filter.AddJwtGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.BodyGlobalFilter;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;


import dev.macula.boot.starter.cloud.gateway.security.CachingReactiveOpaqueTokenIntrospector;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 5.x JWT 装配、过期身份、缓存键类型和原请求体控制流回归。
 * @author Rain
 * @since 5.0.21
 */
class SecurityHardeningRegressionTest {
    /** 构造固定到期时间，避免测试依赖实际时钟。 */
    private OAuth2AuthenticatedPrincipal principal(Instant expiry) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "user");
        attributes.put("exp", expiry);
        return new DefaultOAuth2AuthenticatedPrincipal("user", attributes, Collections.emptyList());
    }

    /** 旧缓存即使仍存在，也不能接受 exp 已到期的身份。 */
    @Test
    @SuppressWarnings("unchecked")
    void rejectsExpiredCachedAndIntrospectedIdentity() {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(principal(now));
        ReactiveOpaqueTokenIntrospector delegate = mock(ReactiveOpaqueTokenIntrospector.class);
        when(delegate.introspect("test")).thenReturn(Mono.just(principal(now)));
        CachingReactiveOpaqueTokenIntrospector cache = new CachingReactiveOpaqueTokenIntrospector(delegate, redis, Clock.fixed(now, ZoneOffset.UTC));
        assertThatThrownBy(() -> cache.introspect("test").block()).isInstanceOf(BadOpaqueTokenException.class);
        verify(redis).delete(anyString());
        verify(delegate).introspect("test");
        verify(values, never()).set(anyString(), any(), any(Duration.class));
    }

    /** Redis TTL 是剩余时间，不能使用完整 exp-iat 重置寿命。 */
    @Test
    @SuppressWarnings("unchecked")
    void cachesOnlyRemainingLifetime() {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ReactiveOpaqueTokenIntrospector delegate = token -> Mono.just(principal(now.plusMillis(1500)));
        new CachingReactiveOpaqueTokenIntrospector(delegate, redis, Clock.fixed(now, ZoneOffset.UTC)).introspect("test").block();
        verify(values).set(anyString(), any(), eq(Duration.ofMillis(1500)));
    }

    /** 相同文本的 jti 和主体名称不能命中相同缓存。 */
    @Test
    void separatesIdAndName() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", 12L);
        OAuth2AuthenticatedPrincipal byName = new DefaultOAuth2AuthenticatedPrincipal("appB", attributes, Collections.emptyList());
        attributes = new HashMap<>(attributes);
        attributes.put("jti", "appB");
        OAuth2AuthenticatedPrincipal byId = new DefaultOAuth2AuthenticatedPrincipal("appA", attributes, Collections.emptyList());
        assertThat(AddJwtGlobalFilter.buildKey(byName)).contains(":u:a12:appB");
        assertThat(AddJwtGlobalFilter.buildKey(byId)).contains(":id:a12:appB").isNotEqualTo(AddJwtGlobalFilter.buildKey(byName));
    }

    /** 自定义编码器可以继续依赖框架默认 source。 */
    @Test
    void customEncoderCanDependOnFrameworkSource(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        java.nio.file.Path store = directory.resolve("test.jks");
        Process generator = new ProcessBuilder(java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "keytool").toString(),
            "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=IsolatedTest",
            "-validity", "1", "-storetype", "JKS", "-keystore", store.toString(),
            "-storepass", "test-only-store-password", "-keypass", "test-only-store-password", "-noprompt")
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        assertThat(generator.waitFor()).isZero();
        new ApplicationContextRunner().withUserConfiguration(CustomEncoder.class, JwtConfiguration.class)
            .withPropertyValues("macula.gateway.jwt.key-store=" + store.toUri(), "macula.gateway.jwt.key-alias=test",
                "macula.gateway.jwt.key-store-password=test-only-store-password").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(JWKSource.class).hasSingleBean(JwtEncoder.class);
            Jwt jwt = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
                JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(),
                JwtClaimsSet.builder().subject("user").build()));
            assertThat(jwt.getTokenValue()).isNotEmpty();
        });
    }

    /** 无参入口接受超过旧 1 MiB 的请求，仍保持构造与订阅时机。 */
    @Test
    void defaultBodyLimitIsTenMiBAndFlowIsPreserved() {
        assertThat(new GatewayProperties().getMaxCachedBodySize().toBytes()).isEqualTo(10 * 1024 * 1024);
        byte[] body = new byte[1024 * 1024 + 1];
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/test")
            .header("Authorization", "hmac username=\"app\", algorithm=\"hmac-sha256\"").body(reactor.core.publisher.Flux.just(new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(body))));
        AtomicInteger assembled = new AtomicInteger();
        AtomicInteger subscribed = new AtomicInteger();
        Mono<Void> result = new BodyGlobalFilter().filter(exchange, current -> {
            assembled.incrementAndGet();
            assertThat((Object)current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isNull();
            return Mono.fromRunnable(() -> {
                assertThat((byte[])current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).hasSize(body.length);
                subscribed.incrementAndGet();
            });
        });
        assertThat(assembled.get()).isEqualTo(1);
        assertThat(subscribed.get()).isZero();
        result.block();
        assertThat(subscribed.get()).isEqualTo(1);
    }

    /** 超限只在缓存聚合处返回标准 413，不能订阅下游。 */
    @Test
    void rejectsOversizedBodyBeforeSubscription() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/test").header("Authorization", "hmac username=\"app\", algorithm=\"hmac-sha256\"").body("xx"));
        AtomicInteger subscribed = new AtomicInteger();
        assertThatThrownBy(() -> new BodyGlobalFilter(DataSize.ofBytes(1)).filter(exchange,
            current -> Mono.fromRunnable(subscribed::incrementAndGet)).block())
            .isInstanceOf(ResponseStatusException.class);
        assertThat(subscribed.get()).isZero();
    }

    /**
     * 用户提供编码器，但继续使用框架密钥来源。
     * @since 5.0.21
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomEncoder {
        /** 不声明独立 source，验证默认 source 仍可用。 */
        @Bean
        JwtEncoder encoder(JWKSource<SecurityContext> source) {
            return new NimbusJwtEncoder(source);
        }
    }
}
