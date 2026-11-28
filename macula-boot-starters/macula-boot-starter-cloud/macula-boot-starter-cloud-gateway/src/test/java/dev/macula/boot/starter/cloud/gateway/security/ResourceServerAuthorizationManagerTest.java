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
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 校验应用凭据和 Bearer 的 URL 授权边界，以及不可用权限快照的处理。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ResourceServerAuthorizationManagerTest {
    private HashOperations<String, String, Object> hashes;
    private ResourceServerAuthorizationManager manager;

    /** 初始化缓存替身，使用正向角色规则和一个空角色条目。 */
    @BeforeEach
    void setUp() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        hashes = mock(HashOperations.class);
        when(redis.<String, Object>opsForHash()).thenReturn(hashes);
        when(hashes.entries(CacheConstants.SECURITY_URL_PERM_ROLES_KEY))
            .thenReturn(Map.of("sample:GET:/orders/*", List.of("ADMIN"), "sample:GET:/restricted", List.of()));
        manager = new ResourceServerAuthorizationManager(redis);
        ReflectionTestUtils.setField(manager, "appName", "sample");
    }

    /** 应用凭据直接进入后续验证，角色缓存与这条链路无关。 */
    @Test
    void applicationCredentialsBypassRoleLookup() {
        for (String token : List.of("Bearer sk-test", GatewayConstants.HMAC_AUTH_PREFIX + " fixture")) {
            for (String path : List.of("/open-orders", "/orders/42", "/restricted")) {
                assertThat(manager.authorize(Mono.empty(), context(path, token)).block().isGranted()).isTrue();
            }
        }
        verifyNoInteractions(hashes);
    }

    /** 已认证的 Bearer 按现有正、反向角色和 ROOT 规则处理。 */
    @Test
    void ordinaryBearerRequiresAuthorizedRole() {
        assertThat(manager.authorize(identity("ROLE_USER"), context("/orders/42", "Bearer oauth")).block().isGranted())
            .isFalse();
        assertThat(manager.authorize(identity("ROLE_ADMIN"), context("/orders/42", "Bearer oauth")).block().isGranted())
            .isTrue();
        assertThat(manager.authorize(identity("ROLE_ADMIN", "ROLE_!ADMIN"), context("/orders/42", "Bearer oauth"))
            .block().isGranted()).isFalse();
        assertThat(manager.authorize(identity("ROLE_ADMIN"), context("/open-orders", "Bearer oauth")).block().isGranted())
            .isFalse();
        assertThat(manager.authorize(identity("ROLE_ROOT"), context("/open-orders", "Bearer oauth")).block().isGranted())
            .isTrue();
    }

    /** 未提供凭据时拒绝，预检仍沿用既有放行且无缓存读取。 */
    @Test
    void anonymousIsDeniedAndPreflightSkipsRoleLookup() {
        assertThat(manager.authorize(Mono.empty(), new AuthorizationContext(MockServerWebExchange.from(
            MockServerHttpRequest.get("/orders")))).block().isGranted()).isFalse();
        assertThat(manager.authorize(Mono.empty(), new AuthorizationContext(MockServerWebExchange.from(
            MockServerHttpRequest.options("/orders")))).block().isGranted()).isTrue();
        verifyNoInteractions(hashes);
    }

    /** 构造带固定角色的已认证主体。 */
    private Mono<Authentication> identity(String... roles) {
        return Mono.just(new TestingAuthenticationToken("fixture", "test", roles));
    }

    /** 构造包含指定凭据和路径的授权上下文。 */
    private AuthorizationContext context(String path, String token) {
        return new AuthorizationContext(MockServerWebExchange.from(MockServerHttpRequest.get(path)
            .header("Authorization", token)));
    }
}
