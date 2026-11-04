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
import reactor.core.scheduler.Schedulers;

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
    private final KongApiGlobalFilter kongApiGlobalFilter;

    /**
     * 匹配 token 清理端点，按配置执行 HMAC 完整准入后再操作缓存。
     *
     * @param exchange 当前请求
     * @param chain 非清理请求的后续过滤链
     * @return 清理处理结果或后续过滤链结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String restfulPath = method + ":" + path;
        if (path.endsWith(properties.getRmOpaqueTokenEndpoint()) && HttpMethod.POST == request.getMethod()) {
            if (log.isDebugEnabled()) {
                log.debug("删除opaqueToken 日志：method： {}, path： {}， restfulPath: {}", method, path, restfulPath);
            }
            if (properties.isForceHmacRmOpaqueTokenEndpoint() && !KongApiUtils.isKongApiRequest(exchange)) {
                if (log.isDebugEnabled()) {
                    log.debug("删除opaqueToken 拒绝：未携带 HMAC 凭据");
                }
                return ResponseUtils.writeResult(response, HttpStatus.FORBIDDEN,
                    Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED));
            } else {
                return handlerRmOpaqueToken(exchange, response);
            }

        }
        return chain.filter(exchange);
    }

    /**
     * 缓存清理请求体，供 HMAC 摘要校验和后续 token 参数解析共用。
     *
     * @param exchange 当前清理请求
     * @param response 响应
     * @return 完整准入及缓存清理结果
     */
    private Mono<Void> handlerRmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            }).defaultIfEmpty(new byte[0])
            .doOnNext(bytes -> exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, bytes))
            .flatMap(bytes -> validateAndRmOpaqueToken(exchange, response));
    }

    /**
     * 强制 HMAC 时先复用完整应用准入，再执行 token 缓存操作；不能仅调用纯验签工具。
     *
     * @param exchange 已缓存请求体的清理请求
     * @param response 响应
     * @return 验证失败响应，或缓存清理结果
     */
    private Mono<Void> validateAndRmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response) {
        if (properties.isForceHmacRmOpaqueTokenEndpoint()) {
            return Mono.fromCallable(() -> kongApiGlobalFilter.validate(exchange))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN))
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        return ResponseUtils.writeResult(response, HttpStatus.FORBIDDEN, result);
                    }
                    return rmOpaqueToken(exchange, response);
                });
        }
        return rmOpaqueToken(exchange, response);
    }

    private Mono<Void> rmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response) {
        return Mono.just(RequestUtils.getBody(exchange)).flatMap(bytes -> {
            String requestBody = new String(bytes);
            if (StringUtils.isBlank(requestBody)) {
                return Mono.empty();
            }
            try {
                return Mono.just(new JSONObject(requestBody).toBean(RmOpaqueTokenQuery.class));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    // 解析异常可能包含原始 JSON 片段，只保留异常类型作为失败类别。
                    log.debug("删除opaqueToken 请求体解析失败: {}", e.getClass().getSimpleName());
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

    /**
     * 不透明令牌查询参数。
     *
     * @since 5.0.0
     */
    @Data
    private static class RmOpaqueTokenQuery {
        private String dealerNo;
        private String token;
    }
}
