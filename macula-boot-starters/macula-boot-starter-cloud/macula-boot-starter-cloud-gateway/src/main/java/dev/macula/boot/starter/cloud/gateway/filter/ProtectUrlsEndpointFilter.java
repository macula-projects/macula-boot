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
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsProcessor;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 提供加密 URL 清单和公钥，并在返回配置前校验跨域请求。
 *
 * @author rain
 * @since 2023/3/23 17:33
 */
@RequiredArgsConstructor
public class ProtectUrlsEndpointFilter implements WebFilter, Ordered {
    private final CryptoService cryptoService;
    private final GatewayProperties properties;
    private final CorsConfigurationSource corsConfigurationSource;
    private CorsProcessor corsProcessor = new DefaultCorsProcessor();

    /**
     * 处理保护配置端点；跨域拒绝和预检在读取配置、生成正文前结束。
     *
     * @param exchange 当前请求与响应
     * @param chain 后续过滤器链
     * @return 响应完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        if (path.endsWith(GatewayConstants.PROTECT_URLS_ENDPOINT) || path.endsWith(
            GatewayConstants.PROTECT_KEY_ENDPOINT)) {
            // 本过滤器早于 Security 执行，不能依赖安全链替这些端点完成 CORS 校验。
            if (corsConfigurationSource != null && !corsProcessor.process(
                corsConfigurationSource.getCorsConfiguration(exchange), exchange)) {
                return response.setComplete();
            }
            if (CorsUtils.isPreFlightRequest(request)) {
                return response.setComplete();
            }
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

    /**
     * 保持保护配置端点在安全链及路由处理前执行的顺序。
     *
     * @return 端点过滤顺序
     */
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 300;
    }
}
