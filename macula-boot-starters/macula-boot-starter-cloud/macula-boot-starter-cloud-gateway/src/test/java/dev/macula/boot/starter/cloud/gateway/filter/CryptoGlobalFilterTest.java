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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证加解密仍可执行，同时 INFO/DEBUG 不输出密钥和业务明文。
 *
 * @author Rain
 * @since 6.1.0
 */
class CryptoGlobalFilterTest {
    /** 使用内存日志检查真实的请求解密和响应加密。 */
    @Test
    void encryptsWithoutLoggingSecrets() {
        CryptoService service = mock(CryptoService.class);
        String key = "private-sm4-key";
        String clearRequest = "{\"account\":\"private-request-value\"}";
        when(service.decryptSm4Key("wrapped-key")).thenReturn(key);
        when(service.decrypt("request-cipher", key)).thenReturn(clearRequest);
        when(service.encrypt("private-response-value", key)).thenReturn("response-cipher");
        when(service.decrypt("query-cipher", key)).thenReturn("account=private-query-value");
        Logger logger = (Logger)LoggerFactory.getLogger(CryptoGlobalFilter.class);
        Level original = logger.getLevel();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        logger.setLevel(Level.DEBUG);
        try {
            var filter = new CryptoGlobalFilter(service, new GatewayProperties());
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/demo")
                .contentType(MediaType.APPLICATION_JSON).header("sm4-key", "wrapped-key").header("sym-alg", "SM4").build());
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR, PathContainer.parsePath("/demo"));
            exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY,
                "{\"data\":\"request-cipher\"}".getBytes(StandardCharsets.UTF_8));
            filter.filter(exchange, current -> DataBufferUtils.join(current.getRequest().getBody()).flatMap(buffer -> {
                try {
                    assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo(clearRequest);
                } finally {
                    DataBufferUtils.release(buffer);
                }
                return current.getResponse().writeWith(Flux.just(current.getResponse().bufferFactory()
                    .wrap("{\"data\":\"private-response-value\"}".getBytes(StandardCharsets.UTF_8))));
            })).block();
            assertThat(exchange.getResponse().getBodyAsString().block()).contains("response-cipher").doesNotContain("private-response-value");
            var get = MockServerWebExchange.from(MockServerHttpRequest.get("/demo?data=query-cipher")
                .header("sm4-key", "wrapped-key").header("sym-alg", "SM4").build());
            get.getAttributes().put(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR, PathContainer.parsePath("/demo"));
            filter.filter(get, current -> {
                assertThat(current.getRequest().getURI().getRawQuery()).contains("private-query-value");
                return reactor.core.publisher.Mono.empty();
            }).block();
            assertThat(events.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                .doesNotContain(key, "private-request-value", "private-response-value", "private-query-value"));
        } finally {
            logger.detachAppender(events);
            logger.setLevel(original);
            events.stop();
        }
    }
}
