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

package dev.macula.boot.starter.cloud.gateway.utils;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.net.URI;

/**
 * {@code RequestBodyUtils} 读取ServerWebExchange的Body并缓存
 *
 * @author rain
 * @since 2023/2/20 14:09
 */
public class RequestUtils {

    public static boolean isCryptoOrSignRequest(ServerWebExchange exchange) {
        String sm4Key = exchange.getRequest().getHeaders().getFirst(GatewayConstants.SM4_KEY);
        String signature = exchange.getRequest().getHeaders().getFirst(GatewayConstants.SIGNATURE_NAME);
        String symAlg = exchange.getRequest().getHeaders().getFirst((GatewayConstants.SYM_ALG));
        return StrUtil.isNotBlank(sm4Key) && (StrUtil.isNotBlank(signature) || StrUtil.isNotBlank(symAlg));
    }

    public static byte[] getBody(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, null);
    }

    public static ServerHttpRequest rewriteRequestBody(ServerWebExchange exchange, byte[] body) {
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        Flux<DataBuffer> bodyFlux = Flux.just(dataBufferFactory.wrap(body));

        // 构建新的请求头
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        // 由于修改了传递参数，需要重新设置CONTENT_LENGTH，长度是字节长度，不是字符串长度
        int length = body.length;
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.setContentLength(length);

        ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(exchange.getRequest().getURI()).build();
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
        return newRequest;
    }

    public static String getOriginPath(ServerWebExchange exchange) {
        // GlobalFilter里面不带URL不带路由前缀，需要获取原始的请求
        URI uri = exchange.getRequest().getURI();
        PathContainer pathContainer =
            exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR);

        return pathContainer != null ? pathContainer.value() : uri.getPath();
    }
}
