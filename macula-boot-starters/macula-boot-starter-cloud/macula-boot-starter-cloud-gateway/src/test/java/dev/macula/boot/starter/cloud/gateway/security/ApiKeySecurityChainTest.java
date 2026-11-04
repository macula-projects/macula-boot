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

package dev.macula.boot.starter.cloud.gateway.security;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.starter.cloud.gateway.filter.AddJwtGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.ApiKeyGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.KongApiGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 真实 Security WebFilter 链和 Gateway 排序链配合模拟基础设施的回归测试。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ApiKeySecurityChainTest {
    private ReactiveOpaqueTokenIntrospector introspector;
    private WebTestClient client;
    private ApiKeyGlobalFilter apiKey;
    private AtomicInteger downstream;
    private RedisTemplate<String, Object> redis;
    private HashOperations<String, Object, Object> hashes;

    @BeforeEach
    void setUp() {
        downstream = new AtomicInteger();
        redis = mock(RedisTemplate.class);
        hashes = mock(HashOperations.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForValue()).thenReturn(values);
        when(hashes.entries(anyString())).thenReturn(Map.of());
        when(hashes.entries(CacheConstants.APIKEY_CACHE_PREFIX + "sk-test")).thenReturn(
            Map.of("applicationCode", "app", "applicationId", "42", "tenantId", "12"));
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_TENANTS)).thenReturn(Map.of("sample", "12"));
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_APPS + "app")).thenReturn(
            Map.of("secretKey", "test-secret", "tenantId", "12", "permitUrls", "GET:/orders"));
        when(hashes.entries(CacheConstants.SECURITY_URL_PERM_ROLES_KEY))
            .thenReturn(Map.of("sample-gateway:GET:/orders", List.of("ADMIN")));
        introspector = mock(ReactiveOpaqueTokenIntrospector.class);
        when(introspector.introspect(anyString())).thenReturn(Mono.just(
            new DefaultOAuth2AuthenticatedPrincipal("user", Map.of("sub", "user", "jti", "oauth-id"),
                AuthorityUtils.createAuthorityList("ROLE_USER"))));
        var config = new TestResourceServerConfiguration(redis, introspector);
        config.setIgnoreUrls(new java.util.ArrayList<>(List.of("/public")));
        var manager = new ResourceServerAuthorizationManager(redis, List.of(), true);
        ReflectionTestUtils.setField(manager, "appName", "sample-gateway");
        var security = config.securityWebFilterChain(ServerHttpSecurity.http(), manager);
        apiKey = new ApiKeyGlobalFilter(redis);
        ReflectionTestUtils.setField(apiKey, "applicationName", "sample-gateway");
        var hmac = new KongApiGlobalFilter(redis);
        ReflectionTestUtils.setField(hmac, "applicationName", "sample-gateway");
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("signed-jwt").header("alg", "RS256")
            .subject("app").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());
        var jwt = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        ReflectionTestUtils.setField(jwt, "issuerUri", "https://test.invalid");
        var handler = new FilteringWebHandler(List.of(jwt, apiKey, hmac), false);
        var route = Route.async().id("test").uri("https://test.invalid").predicate(exchange -> true)
            .filter(new OrderedGatewayFilter((exchange, chain) -> {
                downstream.incrementAndGet();
                if (!exchange.getRequest().getPath().value().equals("/public")) {
                    assertThat(exchange.getRequest().getHeaders().getFirst("Authorization"))
                        .isEqualTo("Bearer signed-jwt");
                }
                assertThat(exchange.getRequest().getQueryParams()).doesNotContainKey("access_token");
                return exchange.getResponse().setComplete();
            }, 2000)).build();
        client = WebTestClient.bindToWebHandler(exchange -> {
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
            return handler.handle(exchange);
        }).webFilter(new WebFilterChainProxy(security)).build();
    }

    @Test
    void apiKeyHeaderAndQueryAcrossMethodsNeverCallIntrospection() {
        client.get().uri("/orders").header("Authorization", "Bearer sk-test").exchange().expectStatus().isOk();
        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.PATCH, HttpMethod.DELETE}) {
            client.method(method).uri("/orders?access_token=sk-test&keep=value")
                .exchange().expectStatus().isOk();
        }
        assertThat(downstream).hasValue(6);
        verifyNoInteractions(introspector);
    }

    @Test
    void ordinaryBearerStillCallsIntrospectionAndRequiresRole() {
        client.get().uri("/orders").header("Authorization", "Bearer oauth").exchange().expectStatus().isUnauthorized();
        assertThat(downstream).hasValue(0);
        verify(introspector).introspect("oauth");
        when(introspector.introspect("admin")).thenReturn(Mono.just(new DefaultOAuth2AuthenticatedPrincipal(
            "admin", Map.of("sub", "admin", "jti", "admin-id"), AuthorityUtils.createAuthorityList("ROLE_ADMIN"))));
        client.get().uri("/orders").header("Authorization", "Bearer admin").exchange().expectStatus().isOk();
        assertThat(downstream).hasValue(1);
    }

    @Test
    void invalidKeysAndDisabledFeatureAreRejectedEvenOnIgnoredRoutes() {
        client.get().uri("/public").header("Authorization", "Bearer sk-missing").exchange().expectStatus().isForbidden();
        ReflectionTestUtils.setField(apiKey, "enabled", false);
        client.get().uri("/public").header("Authorization", "Bearer sk-test").exchange().expectStatus().isForbidden();
        assertThat(downstream).hasValue(0);
        verifyNoInteractions(introspector);
    }

    @Test
    void duplicateAndConflictingCredentialsAreRejectedBeforeLookup() {
        client.get().uri("/orders").header("Authorization", "Bearer sk-test", "Bearer other")
            .exchange().expectStatus().isBadRequest();
        client.post().uri("/orders?access_token=sk-test").header("Authorization", "Bearer oauth")
            .exchange().expectStatus().isBadRequest();
        client.post().uri("/orders?access_token=sk-test&access_token=sk-other")
            .exchange().expectStatus().isBadRequest();
        assertThat(downstream).hasValue(0);
        verify(hashes, never()).entries(CacheConstants.APIKEY_CACHE_PREFIX + "sk-test");
        verifyNoInteractions(introspector);
    }

    @Test
    void validHmacStillVerifiesAndConvertsWithoutIntrospection() {
        var request = KongApiUtilsTest.request("GET", "/orders", "app", "").getRequest();
        client.get().uri("/orders").headers(headers -> headers.addAll(request.getHeaders()))
            .exchange().expectStatus().isOk();
        assertThat(downstream).hasValue(1);
        verifyNoInteractions(introspector);
    }

    /**
     * 仅替换远程 introspector，保留生产 Security 配置和 converter。
     *
     * @since 6.1.0
     */
    private static final class TestResourceServerConfiguration extends ResourceServerConfiguration {
        private final ReactiveOpaqueTokenIntrospector introspector;

        private TestResourceServerConfiguration(RedisTemplate<String, Object> redis,
            ReactiveOpaqueTokenIntrospector introspector) {
            super(new OAuth2ResourceServerProperties(), redis);
            this.introspector = introspector;
        }

        @Override
        public ReactiveOpaqueTokenIntrospector opaqueTokenIntrospector() {
            return introspector;
        }
    }
}
