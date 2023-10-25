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

import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * {@code CryptoUrlsEndpointFilter} 提供加密URL清单给前端
 *
 * @author rain
 * @since 2023/3/23 17:33
 */
@RequiredArgsConstructor
public class ProtectUrlsEndpointFilter implements WebFilter, Ordered {
    private final CryptoService cryptoService;
    private final GatewayProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        if (path.endsWith(GatewayConstants.PROTECT_URLS_ENDPOINT) || path.endsWith(
            GatewayConstants.PROTECT_KEY_ENDPOINT)) {
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String json = "";
            if (path.endsWith(GatewayConstants.PROTECT_URLS_ENDPOINT)) {
                json = JSONUtil.toJsonStr(Result.success(properties.getProtectUrls()));
            }
            if (path.endsWith(GatewayConstants.PROTECT_KEY_ENDPOINT)) {
                json = JSONUtil.toJsonStr(Result.success(cryptoService.getSm2PublicKey()));
            }
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            response.getHeaders().setContentLength(bytes.length);

            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
