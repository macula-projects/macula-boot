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

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将 Micrometer 当前 Span 的 Trace ID 添加到网关响应头。
 *
 * @author rain
 * @since 2024/1/26 17:30
 */
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "x-traceId";

    @Nullable
    private final Tracer tracer;

    private final boolean responseHeaderEnabled;

    public TraceIdGlobalFilter(@Nullable Tracer tracer, boolean responseHeaderEnabled) {
        this.tracer = tracer;
        this.responseHeaderEnabled = responseHeaderEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!responseHeaderEnabled || tracer == null) {
            return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        return chain.filter(exchange).doOnSuccess(signal -> addCurrentTraceId(headers));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void addCurrentTraceId(HttpHeaders headers) {
        Span span = tracer.currentSpan();
        if (span != null && span.context() != null) {
            String traceId = span.context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                headers.set(TRACE_ID_HEADER, traceId);
            }
        }
    }
}
