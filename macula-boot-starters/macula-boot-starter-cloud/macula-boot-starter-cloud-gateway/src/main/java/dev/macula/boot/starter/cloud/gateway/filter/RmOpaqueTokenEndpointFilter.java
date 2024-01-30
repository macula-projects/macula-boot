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

import cn.hutool.json.JSONObject;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static dev.macula.boot.result.ApiResultCode.VALIDATE_ERROR;

/**
 * {@code RmOpaqueTokenEndpointFilter} 提供移除opaqueToken给IPAAS
 *
 * @author haohaoqiu
 * @since 2023/11/1 16:33
 */
@Slf4j
@RequiredArgsConstructor
public class RmOpaqueTokenEndpointFilter implements WebFilter, Ordered {
    private final GatewayProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> sysRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String method = request.getMethodValue();
        String path = request.getURI().getPath();
        String restfulPath = method + ":" + path;
        if (path.endsWith(properties.getRmOpaqueTokenEndpoint()) && HttpMethod.POST == request.getMethod()) {
            if (log.isDebugEnabled()) {
                log.debug("删除opaqueToken 日志：method： {}, path： {}， restfulPath: {}", method, path, restfulPath);
            }
            if (properties.isForceHmacRmOpaqueTokenEndpoint() && !KongApiUtils.isKongApiRequest(exchange)) {
                if (log.isDebugEnabled()) {
                    log.debug("删除opaqueToken 未进行kong验证：header： {}", request.getHeaders().entrySet());
                }
                return ResponseUtils.writeResult(response, HttpStatus.FORBIDDEN,
                    Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED));
            } else {
                return handlerRmOpaqueToken(exchange, response, restfulPath);
            }

        }
        return chain.filter(exchange);
    }

    private Mono<Void> handlerRmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response,
        String restfulPath) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            }).defaultIfEmpty(new byte[0])
            .doOnNext(bytes -> exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, bytes))
            .flatMap(bytes -> rmOpaqueToken(exchange, response, restfulPath));
    }

    private Mono<Void> rmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response, String restfulPath) {
        if (properties.isForceHmacRmOpaqueTokenEndpoint()) {
            Result<String> result = KongApiUtils.checkSign(exchange, sysRedisTemplate, restfulPath);
            if (!result.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug("删除opaqueToken kong验证失败： header : {}",
                        exchange.getRequest().getHeaders().entrySet());
                }
                return ResponseUtils.writeResult(exchange.getResponse(), HttpStatus.FORBIDDEN, result);
            }
        }
        return rmOpaqueToken(exchange, response);
    }

    private Mono<Void> rmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response) {
        return Mono.just(RequestUtils.getBody(exchange)).flatMap(bytes -> {
            String requestBody = new String(bytes);
            if (log.isDebugEnabled()) {
                log.debug("删除opaqueToken requestBody: {}", requestBody);
            }
            if (StringUtils.isBlank(requestBody)) {
                return Mono.empty();
            }
            try {
                return Mono.just(new JSONObject(requestBody).toBean(RmOpaqueTokenQuery.class));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("删除opaqueToken 转换RmOpaqueTokenQuery失败: {}", e.getMessage(), e);
                }
                return Mono.empty();
            }
        }).defaultIfEmpty(new RmOpaqueTokenQuery()).flatMap(rmOpaqueTokenForm -> {
            if (StringUtils.isBlank(rmOpaqueTokenForm.getToken())) {
                return ResponseUtils.writeResult(response, HttpStatus.BAD_REQUEST, Result.failed(VALIDATE_ERROR));
            }
            String opaqueTokenCacheKey = CacheConstants.GATEWAY_TOKEN_CACHE_KEY + rmOpaqueTokenForm.getToken();
            OAuth2AuthenticatedPrincipal cachedPrincipal =
                (OAuth2AuthenticatedPrincipal)redisTemplate.opsForValue().get(opaqueTokenCacheKey);
            if (cachedPrincipal != null) {
                redisTemplate.delete(AddJwtGlobalFilter.buildKey(cachedPrincipal));
                redisTemplate.delete(opaqueTokenCacheKey);
            }
            return ResponseUtils.writeResult(response, HttpStatus.OK, Result.success());
        });
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 200;
    }

    @Data
    private static class RmOpaqueTokenQuery {
        private String dealerNo;
        private String token;
    }
}
