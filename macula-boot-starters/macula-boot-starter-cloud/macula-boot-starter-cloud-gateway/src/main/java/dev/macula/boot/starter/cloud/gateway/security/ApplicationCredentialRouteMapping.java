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

import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 将应用凭据分流限定在真正由 Spring Cloud Gateway 执行的路由。
 *
 * @author Rain
 * @since 6.1.0
 */
final class ApplicationCredentialRouteMapping implements HandlerMapping, PriorityOrdered {

    private final ObjectProvider<HandlerMapping> mappings;

    /** 延迟读取映射，避免 Security 配置初始化时提前创建路由和 MVC 处理器。 */
    ApplicationCredentialRouteMapping(ObjectProvider<HandlerMapping> mappings) {
        this.mappings = mappings;
    }

    /**
     * 标记安全链中等待路由过滤器认证的请求。标记由服务端写入，客户端不可指定。
     *
     * @param exchange 当前请求上下文
     */
    static void requireRoute(ServerWebExchange exchange) {
        exchange.getAttributes().put(GatewayConstants.APPLICATION_ROUTE_REQUIRED_ATTRIBUTE, true);
    }

    /**
     * 沿用 WebFlux 的映射优先级查找首个处理器，只允许交给实际运行应用认证的 Gateway 路由。
     * 将已匹配的处理器交给 DispatcherHandler 执行，避免再次消费请求体路由谓词。
     *
     * @param exchange 当前请求上下文
     * @return 已匹配的 Gateway 处理器，或非分流请求的空信号
     */
    @Override
    public Mono<Object> getHandler(ServerWebExchange exchange) {
        if (!Boolean.TRUE.equals(exchange.getAttribute(GatewayConstants.APPLICATION_ROUTE_REQUIRED_ATTRIBUTE))) {
            return Mono.empty();
        }
        return Flux.fromStream(() -> mappings.orderedStream().filter(mapping -> mapping != this))
                .concatMap(mapping -> mapping.getHandler(exchange)
                        .flatMap(handler -> mapping instanceof RoutePredicateHandlerMapping
                                && handler instanceof FilteringWebHandler ? Mono.just(handler) : denied()), 1)
                .next()
                .switchIfEmpty(Mono.defer(ApplicationCredentialRouteMapping::denied));
    }

    /** 在常规 WebFlux 映射之前拦截安全链标记的请求。 */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /** 缺少应用认证过滤链时拒绝，避免保护端点误接收未经验证的应用凭据。 */
    private static Mono<Object> denied() {
        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
