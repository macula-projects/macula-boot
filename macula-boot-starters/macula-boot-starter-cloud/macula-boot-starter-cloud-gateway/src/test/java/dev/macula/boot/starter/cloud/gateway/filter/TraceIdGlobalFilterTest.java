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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link TraceIdGlobalFilter} Micrometer Trace 响应头测试。
 *
 * @author rain
 * @since 6.1.0
 */
class TraceIdGlobalFilterTest {

    @Test
    void shouldAddCurrentMicrometerTraceId() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("0123456789abcdef0123456789abcdef");
        MockServerWebExchange exchange = exchange();

        new TraceIdGlobalFilter(tracer, true).filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("x-traceId"))
            .isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    void shouldContinueWithoutTracerOrCurrentSpan() {
        MockServerWebExchange withoutTracer = exchange();
        new TraceIdGlobalFilter(null, true).filter(withoutTracer, ignored -> Mono.empty()).block();
        assertThat(withoutTracer.getResponse().getHeaders().getFirst("x-traceId")).isNull();

        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        MockServerWebExchange withoutSpan = exchange();
        new TraceIdGlobalFilter(tracer, true).filter(withoutSpan, ignored -> Mono.empty()).block();
        assertThat(withoutSpan.getResponse().getHeaders().getFirst("x-traceId")).isNull();
    }

    @Test
    void shouldHonorDisabledResponseHeader() {
        Tracer tracer = mock(Tracer.class);
        MockServerWebExchange exchange = exchange();

        new TraceIdGlobalFilter(tracer, false).filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("x-traceId")).isNull();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/health").build());
    }
}
