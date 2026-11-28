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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证入口路径、应用归属以及缺失权限快照的匹配行为。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ApplicationPermissionAuthorizerTest {

    /** 使用改写前路径匹配，并在权限撤销或缓存不可确认时拒绝。 */
    @Test
    void matchesOriginalPathAndRequiresApplicationGrant() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> cache = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(cache);
        String key = "macula:cloud:system:app_perms:12:42";
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders/42"));
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, URI.create("https://gateway.invalid/sales/orders/42"));
        Map<String, String> credential = Map.of("tenantId", "12", "applicationId", "42", "authTenantId", "7");
        when(cache.get(key)).thenReturn(List.of("GET:/sales/orders/*"));
        assertThat(ApplicationPermissionAuthorizer.isAllowed(redis, exchange, credential)).isTrue();
        assertThat(ApplicationPermissionAuthorizer.isAllowed(redis, exchange,
                Map.of("tenantId", "7", "applicationId", "42"))).isFalse();
        when(cache.get(key)).thenReturn(List.of("GET:/orders/*"), List.of("GET:/sales/orders/*", 42), List.of(), null);
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThat(ApplicationPermissionAuthorizer.isAllowed(redis, exchange, credential)).isFalse();
        }
    }
}
