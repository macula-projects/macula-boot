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

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code RequestBodyUtils} is
 *
 * @author rain
 * @since 2023/2/20 14:09
 */
public class RequestBodyUtils {

    public static byte[] getBody(ServerWebExchange exchange) {
        // 如果没有缓存过，获取字节数组存入 exchange 的自定义属性中
        Object cachedRequestBodyObject =
            exchange.getAttributeOrDefault(GatewayConstant.CACHED_REQUEST_BODY_OBJECT_KEY, null);
        if (cachedRequestBodyObject == null) {
            DataBufferUtils.join(exchange.getRequest().getBody()).map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                }).defaultIfEmpty(new byte[0])
                .doOnNext(bytes -> exchange.getAttributes().put(GatewayConstant.CACHED_REQUEST_BODY_OBJECT_KEY, bytes));

        }
        return exchange.getAttributeOrDefault(GatewayConstant.CACHED_REQUEST_BODY_OBJECT_KEY, null);
    }
}
