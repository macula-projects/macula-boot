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
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
 * {@code RmOpaqueTokenEndpointFilter} 提供 opaque token 清理及 IAM 资源服务器专用 API Key 准入
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

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id:}")
    private String resourceClientId;

    /**
     * 匹配 token 清理端点，仅通过资源服务器专用 API Key 认证后操作缓存。
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
            return Mono.fromCallable(() -> validCallbackKey(exchange))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(false)
                .flatMap(valid -> valid ? handlerRmOpaqueToken(exchange, response)
                    : ResponseUtils.writeResult(response, HttpStatus.FORBIDDEN,
                        Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED)));

        }
        return chain.filter(exchange);
    }

    /**
     * 只信任 System 发布到共享 Redis 的专用摘要，绑定本网关内省身份和登记路径。
     * 普通业务 API Key、Query、重复 Header 及缓存故障均不能获得清理权限。
     *
     * @param exchange 当前清理请求
     * @return 是否通过专用回调认证
     */
    private boolean validCallbackKey(ServerWebExchange exchange) {
        List<String> headers = exchange.getRequest().getHeaders().get("Authorization");
        if (headers == null || headers.size() != 1 || StringUtils.isBlank(resourceClientId)
            || !exchange.getRequest().getQueryParams().isEmpty()
            || !exchange.getRequest().getPath().value().equals(properties.getRmOpaqueTokenEndpoint())) {
            return false;
        }
        String header = headers.get(0);
        if (!header.matches("Bearer sk-[A-Za-z0-9_-]{43}")) {
            return false;
        }
        Map<String, String> credential = sysRedisTemplate.<String, String>opsForHash()
            .entries(CacheConstants.IAM_CALLBACK_CREDENTIAL_PREFIX + resourceClientId);
        String expected = credential.get(CacheConstants.IAM_CALLBACK_KEY_DIGEST);
        if (expected == null || !exchange.getRequest().getPath().value()
            .equals(credential.get(CacheConstants.IAM_CALLBACK_PATH))) {
            return false;
        }
        try {
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(header.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    /**
     * 读取已认证请求体，供 token 参数解析使用。
     *
     * @param exchange 当前清理请求
     * @param response 响应
     * @return 完整准入及缓存清理结果
     */
    private Mono<Void> handlerRmOpaqueToken(ServerWebExchange exchange, ServerHttpResponse response) {
        return DataBufferUtils.join(exchange.getRequest().getBody(), 65536).map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            }).defaultIfEmpty(new byte[0])
            .flatMap(bytes -> rmOpaqueToken(bytes, response));
    }

    /**
     * 校验单 token 参数后在工作线程清理现有 token 与派生 JWT 缓存，非法输入不能产生清理副作用。
     *
     * @param body 已认证请求的原始正文
     * @param response 回执响应
     * @return 缓存清理结果
     */
    private Mono<Void> rmOpaqueToken(byte[] body, ServerHttpResponse response) {
        return Mono.just(body).flatMap(bytes -> {
            String requestBody = new String(bytes, StandardCharsets.UTF_8);
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
            return Mono.fromCallable(() -> {
                String opaqueTokenCacheKey = CacheConstants.GATEWAY_TOKEN_CACHE_KEY + rmOpaqueTokenForm.getToken();
                OAuth2AuthenticatedPrincipal cachedPrincipal =
                    (OAuth2AuthenticatedPrincipal) redisTemplate.opsForValue().get(opaqueTokenCacheKey);
                if (cachedPrincipal != null) {
                    redisTemplate.delete(AddJwtGlobalFilter.buildKey(cachedPrincipal));
                    redisTemplate.delete(opaqueTokenCacheKey);
                }
                return true;
            }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(ignored -> ResponseUtils.writeResult(response, HttpStatus.OK, Result.success()));
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
