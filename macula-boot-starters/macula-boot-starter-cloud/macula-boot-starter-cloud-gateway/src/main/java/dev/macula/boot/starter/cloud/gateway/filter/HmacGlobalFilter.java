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

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.HmacUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestBodyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HmacGlobalFilter} 校验HMAC签名
 *
 * @author rain
 * @since 2023/8/24 18:49
 */
@RequiredArgsConstructor
@Slf4j
public class HmacGlobalFilter implements GlobalFilter, Ordered {
    private final RedisTemplate<String, Object> sysRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        String method = request.getMethodValue();
        String path = request.getURI().getPath();
        String restfulPath = method + ":" + path;

        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GatewayConstants.HMAC_AUTH_PREFIX)) {
            Result<String> result = HmacUtils.checkSign(exchange, sysRedisTemplate, restfulPath);
            if (!result.isSuccess()) {
                return ResponseUtils.writeErrorInfo(exchange.getResponse(), ApiResultCode.AKSK_ACCESS_FORBIDDEN,
                    result);
            }

            if ("POST".equals(method) || "PUT".equals(method)) {
                // 由于读过RequestBody，需要重新设置exchange
                byte[] bodyBytes = RequestBodyUtils.getBody(exchange);
                DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
                Flux<DataBuffer> bodyFlux = Flux.just(dataBufferFactory.wrap(bodyBytes));

                // 构建新的请求头
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(request.getHeaders());
                // 由于修改了传递参数，需要重新设置CONTENT_LENGTH，长度是字节长度，不是字符串长度
                int length = bodyBytes.length;
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.setContentLength(length);

                ServerHttpRequest newRequest = request.mutate().uri(request.getURI()).build();
                newRequest = new ServerHttpRequestDecorator(newRequest) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return bodyFlux;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return headers;
                    }
                };

                exchange = exchange.mutate().request(newRequest).response(exchange.getResponse()).build();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
