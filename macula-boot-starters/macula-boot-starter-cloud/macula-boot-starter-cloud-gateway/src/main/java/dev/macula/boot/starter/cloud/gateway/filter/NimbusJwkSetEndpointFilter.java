/*
 * Copyright (c) 2023 Macula
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

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * {@code NimbusJwkSetEndpointFilter} Jwk Set，通过请求客户端可以生成JwtDecoder
 *
 * @author rain
 * @since 2023/5/19 10:29
 */
public class NimbusJwkSetEndpointFilter implements WebFilter, Ordered {
    /**
     * The default endpoint {@code URI} for JWK Set requests.
     */
    private static final String DEFAULT_JWK_SET_ENDPOINT_URI = "/oauth2/jwks";
    private final JWKSource<SecurityContext> jwkSource;
    private final JWKSelector jwkSelector;
    private final ServerWebExchangeMatcher requestMatcher;

    /**
     * Constructs a {@code NimbusJwkSetEndpointFilter} using the provided parameters.
     *
     * @param jwkSource the {@code com.nimbusds.jose.jwk.source.JWKSource}
     */
    public NimbusJwkSetEndpointFilter(JWKSource<SecurityContext> jwkSource) {
        this(jwkSource, DEFAULT_JWK_SET_ENDPOINT_URI);
    }

    /**
     * Constructs a {@code NimbusJwkSetEndpointFilter} using the provided parameters.
     *
     * @param jwkSource         the {@code com.nimbusds.jose.jwk.source.JWKSource}
     * @param jwkSetEndpointUri the endpoint {@code URI} for JWK Set requests
     */
    public NimbusJwkSetEndpointFilter(JWKSource<SecurityContext> jwkSource, String jwkSetEndpointUri) {
        Assert.notNull(jwkSource, "jwkSource cannot be null");
        Assert.hasText(jwkSetEndpointUri, "jwkSetEndpointUri cannot be empty");
        this.jwkSource = jwkSource;
        this.jwkSelector = new JWKSelector(new JWKMatcher.Builder().build());
        this.requestMatcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, jwkSetEndpointUri);
        ;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return requestMatcher.matches(exchange).flatMap(matchResult -> {
            if (matchResult.isMatch()) {
                ServerHttpResponse response = exchange.getResponse();
                JWKSet jwkSet;
                try {
                    jwkSet = new JWKSet(this.jwkSource.get(this.jwkSelector, null));
                } catch (Exception ex) {
                    return Mono.error(
                        new IllegalStateException("Failed to select the JWK(s) -> " + ex.getMessage(), ex));
                }

                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                byte[] bytes = jwkSet.toString().getBytes(StandardCharsets.UTF_8);

                response.getHeaders().setContentLength(bytes.length);

                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return response.writeWith(Mono.just(buffer));
            }
            return chain.filter(exchange);
        });
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 100;
    }
}
