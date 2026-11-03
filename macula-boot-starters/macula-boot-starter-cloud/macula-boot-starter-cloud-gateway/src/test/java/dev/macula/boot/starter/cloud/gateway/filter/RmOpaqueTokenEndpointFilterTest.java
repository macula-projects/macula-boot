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
            KongApiUtilsTest.redis("7", "12"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
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
            KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "unknown-gateway");
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
            KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
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
        new RmOpaqueTokenEndpointFilter(properties, tokens, applications)
            .filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(applications);
    }
}
