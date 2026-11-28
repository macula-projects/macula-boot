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

import java.util.List;
import java.util.Map;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.starter.cloud.gateway.filter.ApiKeyGlobalFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证安全链分流最终受 WebFlux 实际处理器选择约束。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ApplicationCredentialRouteMappingTest {

    /** 本地端点优先选择时拒绝应用凭据，实际 Gateway 路由仍进入应用过滤链。 */
    @Test
    void protectsLocalHandlersAndDelegatesActualRoutes() {
        var gatewayMapping = mock(RoutePredicateHandlerMapping.class);
        var gatewayHandler = mock(FilteringWebHandler.class);
        when(gatewayMapping.getOrder()).thenReturn(1);
        when(gatewayMapping.getHandler(any())).thenReturn(Mono.just(gatewayHandler));
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, String, String> hashes = mock(HashOperations.class);
        ValueOperations<String, Object> rules = mock(ValueOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        when(redis.opsForValue()).thenReturn(rules);
        when(hashes.entries(CacheConstants.APIKEY_CACHE_PREFIX + "sk-valid"))
                .thenReturn(Map.of("applicationCode", "fixture", "applicationId", "42", "tenantId", "12"));
        when(hashes.entries(CacheConstants.SECURITY_SYSTEM_TENANTS)).thenReturn(Map.of("fixture", "12"));
        when(rules.get(CacheConstants.APPLICATION_PERMISSIONS_CACHE_PREFIX + "12:42"))
                .thenReturn(List.of("GET:/routed"));
        var apiKeyFilter = new ApiKeyGlobalFilter(redis);
        ReflectionTestUtils.setField(apiKeyFilter, "applicationName", "fixture-gateway");

        try (var context = new GenericApplicationContext()) {
            var dispatcher = new DispatcherHandler();
            context.getBeanFactory().registerSingleton("webHandler", dispatcher);
            when(gatewayHandler.handle(any())).thenAnswer(invocation -> {
                ServerWebExchange exchange = invocation.getArgument(0);
                var rewritten = exchange.mutate().request(exchange.getRequest().mutate().path("/local").build()).build();
                return apiKeyFilter.filter(rewritten, dispatcher::handle);
            });
            context.registerBean("applicationCredentialRouteMapping", ApplicationCredentialRouteMapping.class,
                    () -> new ApplicationCredentialRouteMapping(context.getBeanProvider(HandlerMapping.class)));
            context.registerBean(FixtureLocalMapping.class);
            context.getBeanFactory().registerSingleton("routeMapping", gatewayMapping);
            context.registerBean(SimpleHandlerAdapter.class);
            context.registerBean(WebFluxResponseStatusExceptionHandler.class);
            context.registerBean("captureEntrance", WebFilter.class, () ->
                    new ResourceServerConfiguration(new OAuth2ResourceServerProperties(), redis)
                            .applicationEntrancePathFilter());
            context.registerBean("securityFixture", WebFilter.class, () -> (exchange, chain) -> {
                if (exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null) {
                    var manager = new ResourceServerAuthorizationManager(redis);
                    return manager.authorize(Mono.empty(),
                            new AuthorizationContext(exchange))
                            .flatMap(decision -> {
                                Assertions.assertTrue(decision.isGranted());
                                return chain.filter(exchange);
                            });
                }
                return chain.filter(exchange);
            });
            context.refresh();
            dispatcher.setApplicationContext(context);
            var client = WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/local").header(HttpHeaders.AUTHORIZATION, "Bearer sk-test")
                    .exchange().expectStatus().isForbidden();
            client.get().uri("/local").header(HttpHeaders.AUTHORIZATION, "hmac username=fixture")
                    .exchange().expectStatus().isForbidden();
            client.get().uri("/local").exchange().expectStatus().isNoContent();
            client.get().uri("/routed").header(HttpHeaders.AUTHORIZATION, "Bearer sk-test")
                    .exchange().expectStatus().isForbidden();
            client.get().uri("/routed").header(HttpHeaders.AUTHORIZATION, "Bearer sk-valid")
                    .exchange().expectStatus().isNoContent();
        }
        verify(gatewayMapping, times(2)).getHandler(any());
        verify(gatewayHandler, times(2)).handle(any());
    }

    /**
     * 测试本地处理器的优先级高于网关路由。
     *
     * @since 6.1.0
     */
    static final class FixtureLocalMapping implements HandlerMapping, Ordered {
        /** 仅本地端点由此映射处理。 */
        @Override
        public Mono<Object> getHandler(ServerWebExchange exchange) {
            if (!"/local".equals(exchange.getRequest().getPath().value())) {
                return Mono.empty();
            }
            return Mono.just((WebHandler) local -> {
                local.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return local.getResponse().setComplete();
            });
        }

        /** 与默认 WebFlux 控制器先于 Gateway 路由的顺序一致。 */
        @Override
        public int getOrder() {
            return 0;
        }
    }
}
