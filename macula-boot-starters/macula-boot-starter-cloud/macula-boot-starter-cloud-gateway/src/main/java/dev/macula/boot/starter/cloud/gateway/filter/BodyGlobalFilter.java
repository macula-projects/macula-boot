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

import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * <b>BodyGlobalFilter</b> 请求缓存拦截器
 * </p>
 *
 * @author rain
 * @since 2023/2/20 11:50
 */
@Slf4j
public class BodyGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        HttpMethod method = exchange.getRequest().getMethod();
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            // 判断是否有加密参数，有则进行加解密操作，无则跳过
            // 判断是否是HMAC请求，不是也不缓存
            if (RequestUtils.isCryptoOrSignRequest(exchange) || KongApiUtils.isKongApiRequest(exchange)) {

                log.debug("GlobalCacheRequestBodyFilter start...");

                // 将 request body 中的内容 copy 一份，记录到 exchange 的一个自定义属性中
                Object cachedRequestBodyObject =
                    exchange.getAttributeOrDefault(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, null);
                // 如果已经缓存过，略过
                if (cachedRequestBodyObject != null) {
                    return chain.filter(exchange);
                }
                // 如果没有缓存过，获取字节数组存入 exchange 的自定义属性中
                return DataBufferUtils.join(exchange.getRequest().getBody()).map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        if (log.isDebugEnabled()) {
                            String requestBody = new String(bytes);
                            log.debug("请求缓存的body为：{}", requestBody);
                        }
                        return bytes;
                    }).defaultIfEmpty(new byte[0]).doOnNext(
                        bytes -> exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, bytes))
                    .then(chain.filter(exchange));
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
