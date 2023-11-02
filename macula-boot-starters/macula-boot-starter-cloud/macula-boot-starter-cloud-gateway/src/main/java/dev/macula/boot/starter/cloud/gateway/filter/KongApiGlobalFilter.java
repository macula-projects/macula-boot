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
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@code HmacGlobalFilter} 校验HMAC签名
 *
 * @author rain
 * @since 2023/8/24 18:49
 */
@RequiredArgsConstructor
@Slf4j
public class KongApiGlobalFilter implements GlobalFilter, Ordered {
    private final RedisTemplate<String, Object> sysRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        String restfulPath = method + ":" + path;

        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GatewayConstants.HMAC_AUTH_PREFIX)) {
            Result<String> result = KongApiUtils.checkSign(exchange, sysRedisTemplate, restfulPath);
            if (!result.isSuccess()) {
                return ResponseUtils.writeErrorInfo(exchange.getResponse(), ApiResultCode.AKSK_ACCESS_FORBIDDEN,
                    result);
            }

            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                // 由于读过RequestBody，需要重新设置exchange
                byte[] bodyBytes = RequestUtils.getBody(exchange);
                ServerHttpRequest newRequest = RequestUtils.rewriteRequestBody(exchange, bodyBytes);
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
