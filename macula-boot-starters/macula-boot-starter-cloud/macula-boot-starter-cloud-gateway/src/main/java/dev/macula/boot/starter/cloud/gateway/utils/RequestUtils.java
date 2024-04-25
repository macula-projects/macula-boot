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
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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

    public static String getHeaderOrQueryToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        if (StrUtil.isBlank(token)) {
            token = exchange.getRequest().getQueryParams().getFirst(SecurityConstants.QUERY_AUTHORIZATION_KEY);
            token = StrUtil.isBlank(token) ? token : SecurityConstants.TOKEN_PREFIX + token;
        }
        return token;
    }

    public static URI removeParam(URI uri, String... names) {
        try {
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                // If query is empty, no parameters to remove
                return uri;
            }

            // Split query string into key-value pairs
            String[] queryParams = query.split("&");
            List<String> updatedParams = new ArrayList<>();

            // Iterate over key-value pairs and add those that are not to be removed
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                String paramName = keyValue[0];
                boolean removeParam = false;

                // Check if current parameter should be removed
                for (String name : names) {
                    if (paramName.equals(name)) {
                        removeParam = true;
                        break;
                    }
                }

                // If parameter should not be removed, add it to updated parameters list
                if (!removeParam) {
                    updatedParams.add(param);
                }
            }

            // Construct new query string without removed parameters
            String newQuery = null;
            if (!updatedParams.isEmpty()) {
                newQuery = String.join("&", updatedParams);
            }
            // Reconstruct URI with updated query string
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) {
        String url = "https://www.lyf.com/user/info?access_token=xxx&id=12333&enc=test";
        URI uri = UriComponentsBuilder.fromHttpUrl(url).build(true).toUri();
        URI newUri = RequestUtils.removeParam(uri, "access_token", "id");
        System.out.println(newUri.toString());
    }
}
