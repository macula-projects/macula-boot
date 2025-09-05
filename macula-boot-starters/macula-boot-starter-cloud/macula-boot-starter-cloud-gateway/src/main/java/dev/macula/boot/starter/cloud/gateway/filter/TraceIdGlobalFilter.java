/*
 * Copyright (c) 2024 Macula
 *   macula.dev, China
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

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.apache.skywalking.apm.toolkit.webflux.WebFluxSkyWalkingOperators;
import org.springframework.beans.BeansException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@code TraceIdFilter} 获取线程中的traceId并添加到响应头中
 *
 * @author rain
 * @since 2024/1/26 17:30
 */
@Slf4j
public class TraceIdGlobalFilter implements GlobalFilter, Ordered, ApplicationContextAware {
    private static final String TRACE_TYPE_SLEUTH = "SLEUTH";
    private static final String TRACE_TYPE_SKYWALKING = "SKYWALKING";
    private ApplicationContext applicationContext;

    private Object tracing;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        String tracingType = this.tracingType();
        
        if (TRACE_TYPE_SLEUTH.equals(tracingType)) {
            if (tracing == null) {
                tracing = applicationContext.getBean(Tracing.class);
            }

            return chain.filter(exchange).doOnSuccess(signal -> {
                brave.propagation.TraceContext traceContext = ((Tracing)tracing).currentTraceContext().get();
                if (traceContext != null) {
                    final String traceId = traceContext.traceIdString();
                    headers.add("x-traceId", traceId);
                }
            });

        } else if (TRACE_TYPE_SKYWALKING.equals(tracingType)) {
            final String traceId = WebFluxSkyWalkingOperators.continueTracing(exchange, TraceContext::traceId);
            headers.add("x-traceId", traceId);
        }
        
        // 对于SkyWalking和没有追踪系统的情况，都直接执行filter
        return chain.filter(exchange);
    }

    private String tracingType() {
        if (isClassExists("brave.Tracing")) {
            return TRACE_TYPE_SLEUTH;
        }
        if (isClassExists("org.apache.skywalking.apm.toolkit.trace.TraceContext")) {
            return TRACE_TYPE_SKYWALKING;
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean isClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
