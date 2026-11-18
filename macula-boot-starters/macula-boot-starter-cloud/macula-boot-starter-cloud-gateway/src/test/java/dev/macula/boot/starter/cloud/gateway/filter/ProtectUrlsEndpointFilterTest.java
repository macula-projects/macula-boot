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
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 保护配置端点的跨域拒绝与预检短路边界。
 *
 * @author Rain
 * @since 6.1.0
 */
class ProtectUrlsEndpointFilterTest {
    private static final String ORIGIN = "https://frontend.example";
    private final CryptoService crypto = mock(CryptoService.class);
    private final GatewayProperties properties = mock(GatewayProperties.class);
    private final WebFilterChain chain = mock(WebFilterChain.class);

    /** 验证两个端点拒绝跨域时不读取业务配置或返回正文。 */
    @ParameterizedTest
    @ValueSource(strings = {"/gateway/protect/key", "/gateway/protect/urls"})
    void rejectsOriginBeforeReadingConfiguration(String path) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://gateway.example" + path)
            .header(HttpHeaders.ORIGIN, "https://untrusted.example"));
        filter(true).filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).isEmpty();
        verifyNoInteractions(crypto, properties, chain);
    }

    /** 验证预检在配置允许、拒绝及无配置时均不读取业务配置。 */
    @ParameterizedTest
    @ValueSource(strings = {"/gateway/protect/key", "/gateway/protect/urls"})
    void preflightNeverProducesBusinessBody(String path) {
        for (boolean configured : new boolean[] {true, false}) {
            for (String method : List.of("GET", "DELETE")) {
                var exchange = MockServerWebExchange.from(MockServerHttpRequest.options("https://gateway.example" + path)
                    .header(HttpHeaders.ORIGIN, ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method));
                filter(configured).filter(exchange, chain).block();
                if (configured && method.equals("DELETE")) {
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                } else {
                    assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
                }
                if (configured && method.equals("GET")) {
                    assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isEqualTo(ORIGIN);
                }
                assertThat(exchange.getResponse().getBodyAsString().block()).isEmpty();
            }
        }
        verifyNoInteractions(crypto, properties, chain);
    }

    /** 同源、允许的跨域及无配置源的实际请求保持原返回格式。 */
    @ParameterizedTest
    @ValueSource(strings = {"/gateway/protect/key", "/gateway/protect/urls"})
    void actualRequestsRetainTheirPayload(String path) {
        when(crypto.getSm2PublicKey()).thenReturn("test-public-key");
        var urls = new GatewayProperties.ProtectUrl();
        urls.setCrypto(List.of("/orders"));
        when(properties.getProtectUrls()).thenReturn(urls);
        for (boolean configured : new boolean[] {true, false}) {
            for (boolean crossOrigin : new boolean[] {true, false}) {
                var request = MockServerHttpRequest.get("https://gateway.example" + path);
                if (crossOrigin) {
                    request.header(HttpHeaders.ORIGIN, ORIGIN);
                }
                var exchange = MockServerWebExchange.from(request);
                filter(configured).filter(exchange, chain).block();
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(exchange.getResponse().getBodyAsString().block())
                    .contains(path.endsWith("key") ? "test-public-key" : "/orders");
                if (configured && crossOrigin) {
                    assertThat(exchange.getResponse().getHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .containsExactly(ORIGIN);
                }
            }
        }
        verifyNoInteractions(chain);
    }

    /** 非保护配置路径继续执行后续过滤器。 */
    @Test
    void otherPathsContinueTheChain() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter(true).filter(exchange, chain).block();
        verify(chain).filter(exchange);
        verifyNoInteractions(crypto, properties);
    }

    /** 创建仅允许指定前端读取配置的过滤器。 */
    private ProtectUrlsEndpointFilter filter(boolean configured) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(ORIGIN));
        cors.setAllowedMethods(List.of(HttpMethod.GET.name()));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new ProtectUrlsEndpointFilter(crypto, properties, configured ? source : null);
    }
}
