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
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.TenantContextHolder;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * JWT 租户来自可信 HMAC 身份，缓存沿用原 buildKey 规则。
 *
 * @author Rain
 * @since 6.1.0
 */
class AddJwtGlobalFilterTest {
    @Test
    @SuppressWarnings("unchecked")
    void anonymousRequestRunsDownstreamOnce() {
        AddJwtGlobalFilter filter = new AddJwtGlobalFilter(mock(JwtEncoder.class), ignored -> { },
            mock(RedisTemplate.class));
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        var principal = new DefaultOAuth2AuthenticatedPrincipal("user", Map.of("sub", "user"),
            AuthorityUtils.NO_AUTHORITIES);
        var authentication = new org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication(
            principal, new org.springframework.security.oauth2.core.OAuth2AccessToken(
                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER, "test", null, null),
            AuthorityUtils.NO_AUTHORITIES);
        var exchange = org.springframework.mock.web.server.MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/public"));
        filter.filter(exchange, forwarded -> Mono.fromRunnable(calls::incrementAndGet))
            .contextWrite(org.springframework.security.core.context.ReactiveSecurityContextHolder
                .withAuthentication(authentication)).block();
        assertThat(calls).hasValue(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void apiKeyUsesVerifiedClaimsAndCleansQueryOnce() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenAnswer(invocation -> {
            org.springframework.security.oauth2.jwt.JwtEncoderParameters parameters = invocation.getArgument(0);
            assertThat(parameters.getClaims().getClaims()).containsEntry("tenantId", 12L)
                .containsEntry("authType", "apikey").containsEntry("applicationId", "42");
            return Jwt.withTokenValue("signed-key-jwt").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).subject("app").build();
        });
        var filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        ReflectionTestUtils.setField(filter, "issuerUri", "https://test.invalid");
        var exchange = org.springframework.mock.web.server.MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/orders?access_token=sk-test&keep=yes"));
        exchange.getAttributes().put(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE,
            new DefaultOAuth2AuthenticatedPrincipal("app", Map.of("sub", "app", "tenantId", 12L,
                "authType", "apikey", "applicationId", "42"), AuthorityUtils.NO_AUTHORITIES));
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        filter.filter(exchange, forwarded -> {
            assertThat(forwarded.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer signed-key-jwt");
            assertThat(forwarded.getRequest().getQueryParams()).doesNotContainKey("access_token").containsKey("keep");
            return Mono.fromRunnable(calls::incrementAndGet);
        }).block();
        assertThat(calls).hasValue(1);
        org.mockito.Mockito.verify(values).get(CacheConstants.GATEWAY_JWT_CACHE_KEY + "apikey:tn12:app");
    }

    @Test
    @SuppressWarnings("unchecked")
    void apiKeyWithoutVerifiedIdentityCannotGenerateJwt() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        var filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        var exchange = org.springframework.mock.web.server.MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer sk-test"));
        assertThatThrownBy(() -> filter.filter(exchange, ignored -> Mono.empty()).block())
            .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(encoder, redis);
    }

    /** 缺失、空白或不可信的 OAuth 认证上下文必须返回认证错误，不能产生空指针或继续转发。 */
    @Test
    @SuppressWarnings("unchecked")
    void rejectsMissingOrInvalidOAuthAuthenticationWithoutNullPointer() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        var filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        var unauthenticated = mock(BearerTokenAuthentication.class);
        var wrongPrincipal = mock(BearerTokenAuthentication.class);
        when(wrongPrincipal.isAuthenticated()).thenReturn(true);
        when(wrongPrincipal.getPrincipal()).thenReturn("unexpected");
        var emptyPrincipal = mock(BearerTokenAuthentication.class);
        when(emptyPrincipal.isAuthenticated()).thenReturn(true);
        List<Mono<SecurityContext>> contexts = List.of(
            Mono.empty(), Mono.just(new SecurityContextImpl()),
            Mono.just(new SecurityContextImpl(new TestingAuthenticationToken("user", "test", "ROLE_USER"))),
            Mono.just(new SecurityContextImpl(unauthenticated)),
            Mono.just(new SecurityContextImpl(wrongPrincipal)),
            Mono.just(new SecurityContextImpl(emptyPrincipal)));
        for (Mono<SecurityContext> context : contexts) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
                .header("Authorization", "Bearer oauth"));
            java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
            assertThatThrownBy(() -> filter.filter(exchange, ignored -> Mono.fromRunnable(calls::incrementAndGet))
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(context)).block())
                .isInstanceOf(BadCredentialsException.class).hasMessage("Missing authenticated OAuth principal");
            assertThat(calls).hasValue(0);
        }
        verifyNoInteractions(encoder, redis);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clearCurrentTenantId();
    }

    @Test
    @SuppressWarnings("unchecked")
    void signsVerifiedTenantAndUsesOriginalTenantCacheKeys() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        Set<String> keys = new HashSet<>();
        when(values.get(anyString())).thenAnswer(invocation -> {
            keys.add(invocation.getArgument(0));
            return null;
        });
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenAnswer(invocation -> {
            org.springframework.security.oauth2.jwt.JwtEncoderParameters parameters = invocation.getArgument(0);
            assertThat(parameters.getClaims().getClaims().get(GlobalConstants.TENANT_ID_NAME)).isIn(7L, 8L);
            assertThat(parameters.getClaims().getClaims()).doesNotContainKey("authTenantId");
            return Jwt.withTokenValue("signed-test-jwt").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).subject("app").build();
        });
        AddJwtGlobalFilter filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        ReflectionTestUtils.setField(filter, "issuerUri", "https://test.invalid");
        TenantContextHolder.setCurrentTenantId(99L);
        for (String app : new String[] {"first", "second"}) {
            for (long tenant : new long[] {7L, 8L}) {
                var exchange = KongApiUtilsTest.request("GET", "/orders", app, "");
                var hmac = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", Long.toString(tenant)));
                ReflectionTestUtils.setField(hmac, "applicationName", "macula-samples-gateway");
                hmac.filter(exchange, verified -> filter.filter(verified, forwarded -> {
                    assertThat(forwarded.getRequest().getHeaders().getFirst("Authorization"))
                        .isEqualTo("Bearer signed-test-jwt");
                    return Mono.empty();
                })).block();
            }
        }
        assertThat(keys).containsExactlyInAnyOrder(
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "tn7:first",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "tn8:first",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "tn7:second",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "tn8:second");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refusesHmacWithoutSuccessfulVerification() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        AddJwtGlobalFilter filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
        assertThatThrownBy(() -> filter.filter(exchange, ignored -> Mono.empty()).block())
            .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(encoder, redis);
    }

    /** 共用租户与 jti 规则，仅为 API Key 增加类型段，并保持 OAuth/HMAC 原键。 */
    @Test
    void preservesNonHmacCacheKeyContract() {
        var principal = new DefaultOAuth2AuthenticatedPrincipal("user",
            Map.of("tenantId", 12L, "jti", "token-id"), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(principal))
            .isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "tn12:token-id");
        var apiKey = new DefaultOAuth2AuthenticatedPrincipal("app",
            Map.of("authType", "apikey"), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(apiKey)).isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "apikey:app");
        var tenantApiKey = new DefaultOAuth2AuthenticatedPrincipal("app",
            Map.of("authType", "apikey", "tenantId", 12L), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(tenantApiKey))
            .isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "apikey:tn12:app");
        var apiKeyWithJti = new DefaultOAuth2AuthenticatedPrincipal("app",
            Map.of("authType", "apikey", "tenantId", 12L, "jti", "token-id"), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(apiKeyWithJti))
            .isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "apikey:tn12:token-id");
    }
}
