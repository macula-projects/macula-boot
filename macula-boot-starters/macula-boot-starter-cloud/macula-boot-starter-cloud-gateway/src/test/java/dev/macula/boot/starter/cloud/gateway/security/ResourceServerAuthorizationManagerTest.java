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
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * API Key 仅分流，普通 Bearer 仍受 URL 角色限制。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ResourceServerAuthorizationManagerTest {
    @Test
    void apiKeyIsDeferredButAnonymousIsDenied() {
        var manager = new ResourceServerAuthorizationManager(mock(RedisTemplate.class), List.of(), true);
        assertThat(manager.authorize(Mono.empty(), context("Bearer sk-test")).block().isGranted()).isTrue();
        assertThat(manager.authorize(Mono.empty(), new AuthorizationContext(MockServerWebExchange.from(
            MockServerHttpRequest.get("/orders")))).block().isGranted()).isFalse();
    }

    @Test
    void ordinaryBearerStillRequiresAuthorizedRole() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, String, Object> hashes = mock(HashOperations.class);
        when(redis.<String, Object>opsForHash()).thenReturn(hashes);
        when(hashes.entries(CacheConstants.SECURITY_URL_PERM_ROLES_KEY))
            .thenReturn(Map.of("sample:GET:/orders", List.of("ADMIN")));
        var manager = new ResourceServerAuthorizationManager(redis, List.of(), true);
        ReflectionTestUtils.setField(manager, "appName", "sample");
        assertThat(manager.authorize(Mono.just(new TestingAuthenticationToken("user", "test", "ROLE_USER")),
            context("Bearer oauth")).block().isGranted()).isFalse();
        assertThat(manager.authorize(Mono.just(new TestingAuthenticationToken("admin", "test", "ROLE_ADMIN")),
            context("Bearer oauth")).block().isGranted()).isTrue();
    }

    private AuthorizationContext context(String token) {
        return new AuthorizationContext(MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
            .header("Authorization", token)));
    }
}
