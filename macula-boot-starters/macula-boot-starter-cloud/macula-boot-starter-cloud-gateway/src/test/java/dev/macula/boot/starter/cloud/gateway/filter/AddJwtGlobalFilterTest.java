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
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
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
import java.util.HashSet;
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
                exchange.getAttributes().put(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE,
                    new KongApiUtils.HmacIdentity(app, tenant));
                filter.filter(exchange, forwarded -> {
                    assertThat(forwarded.getRequest().getHeaders().getFirst("Authorization"))
                        .isEqualTo("Bearer signed-test-jwt");
                    return Mono.empty();
                }).block();
            }
        }
        assertThat(keys).containsExactlyInAnyOrder(
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "a7:first",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "a8:first",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "a7:second",
            CacheConstants.GATEWAY_JWT_CACHE_KEY + "a8:second");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refusesHmacWithoutSuccessfulVerification() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        AddJwtGlobalFilter filter = new AddJwtGlobalFilter(encoder, ignored -> { }, redis);
        assertThatThrownBy(() -> filter.filter(KongApiUtilsTest.request("GET", "/orders", "app", ""),
            ignored -> Mono.empty()).block()).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(encoder, redis);
    }

    @Test
    void preservesNonHmacCacheKeyContract() {
        var principal = new DefaultOAuth2AuthenticatedPrincipal("user",
            Map.of("tenantId", 12L, "jti", "token-id"), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(principal))
            .isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "a12:token-id");
        var apiKey = new DefaultOAuth2AuthenticatedPrincipal("app",
            Map.of("authType", "apikey"), AuthorityUtils.NO_AUTHORITIES);
        assertThat(AddJwtGlobalFilter.buildKey(apiKey)).isEqualTo(CacheConstants.GATEWAY_JWT_CACHE_KEY + "app");
    }
}
