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
import com.nimbusds.jwt.JWTClaimNames;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.starter.cloud.gateway.utils.TenantUtils;
import dev.macula.boot.result.Result;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code KongApiGlobalFilter} 验证 HMAC 请求并构造下游 JWT 所需的可信身份。
 *
 * @author rain
 * @since 2023/8/24 18:49
 */
@RequiredArgsConstructor
@Slf4j
public class KongApiGlobalFilter implements GlobalFilter, Ordered {
    private final RedisTemplate<String, Object> sysRedisTemplate;

    @Value("${spring.application.name:}")
    private String applicationName;

    /**
     * 验证 HMAC 签名及应用准入，发布 principal，并恢复验签所读取的请求体。
     *
     * @param exchange 当前请求
     * @param chain 下游过滤链
     * @return 验证成功后继续处理，验证失败时返回拒绝响应
     */
    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.defer(() -> {
            // 每次订阅都重新验证，失败时不能复用旧的 principal 或验签结果。
            exchange.getAttributes().remove(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
            if (KongApiUtils.resolveHmacToken(exchange) == null) {
                return chain.filter(exchange);
            }
            return Mono.fromCallable(() -> validate(exchange))
                // 验签读取 System Redis，不能阻塞响应式事件线程。
                .subscribeOn(Schedulers.boundedElastic())
                // 只兜底验证异常，不捕获后续过滤链的业务异常，也不输出底层异常详情。
                .onErrorReturn(Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN))
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        return ResponseUtils.writeResult(exchange.getResponse(), HttpStatus.FORBIDDEN, result);
                    }
                    exchange.getAttributes().put(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE, result.getData());
                    return chain.filter(restoreRequestBody(exchange));
                });
        });
    }

    /**
     * 读取应用缓存，校验归属租户、URL 许可及签名，再构造可信 principal。
     * 返回的 principal 由调用方决定是否发布；token 清理入口也必须复用此完整准入流程。
     * 本方法读取阻塞 Redis，响应式调用方须在 boundedElastic 上执行，并处理验证异常。
     *
     * @param exchange 当前 HMAC 请求，请求体须提前缓存
     * @return 可信 principal 或应用准入/验签失败结果
     */
    public Result<OAuth2AuthenticatedPrincipal> validate(ServerWebExchange exchange) {
        exchange.getAttributes().remove(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);

        String token = KongApiUtils.resolveHmacToken(exchange);
        String username = StrUtil.subBetween(token, GatewayConstants.HMAC_USERNAME_PREFIX, "\",");
        if (StrUtil.isBlank(username)) {
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "username缺少，验签失败");
        }

        // 检查应用是否存在
        Map<String, String> apps = sysRedisTemplate.<String, String>opsForHash()
            .entries(CacheConstants.SECURITY_SYSTEM_APPS + username);

        String secretKey = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_SECRIT_KEY);

        if (StrUtil.isBlank(secretKey)) {
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "秘钥未配置, 验签失败");
        }

        // 检查归属租户以及是否允许URL
        Result<Long> tenant = TenantUtils.checkTenantAndUrls(exchange, sysRedisTemplate, applicationName, apps);
        if (!tenant.isSuccess()) {
            return Result.failed(tenant.getCode(), tenant.getMsg(), tenant.getCause());
        }

        // 检查token的签名
        Result<String> signature = KongApiUtils.checkSign(exchange, secretKey);
        if (!signature.isSuccess()) {
            return Result.failed(signature.getCode(), signature.getMsg(), signature.getCause());
        }

        // 只有所有准入与验签检查都通过后，才允许将应用信息标记为可信身份。
        return Result.success(createPrincipal(exchange, username, tenant.getData()));
    }

    /**
     * 恢复参与验签的 POST、PUT、PATCH 请求体；其余方法沿用原 exchange。
     *
     * @param exchange 已通过验签的请求
     * @return 下游可继续读取请求体的 exchange
     */
    private ServerWebExchange restoreRequestBody(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            byte[] body = RequestUtils.getBody(exchange);
            // 验签将未缓存的请求体视为空，此处保持相同语义，避免空请求体恢复时出现空指针。
            ServerHttpRequest request = RequestUtils.rewriteRequestBody(exchange, body == null ? new byte[0] : body);
            return exchange.mutate().request(request).build();
        }
        return exchange;
    }

    /**
     * 使用已通过准入和验签的应用信息构造 principal，不重新从 Header 推断身份。
     *
     * @param exchange 已通过验签的请求
     * @param username 验签使用的应用编码
     * @param tenantId 准入检查解析出的认证业务租户
     * @return 保留既有 invoker、tokenId 语义的可信应用身份
     */
    private OAuth2AuthenticatedPrincipal createPrincipal(ServerWebExchange exchange, String username, Long tenantId) {
        // 保留既有上游调用名覆盖 JWT 主体的约定；不改变验签应用及其租户准入结果。
        String invoker = exchange.getRequest().getHeaders().getFirst(GatewayConstants.REMOTE_INVOKE_NAME);
        if (StrUtil.isNotBlank(invoker)) {
            username = invoker;
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(JWTClaimNames.SUBJECT, username);
        if (tenantId != null) {
            attributes.put(GlobalConstants.TENANT_ID_NAME, tenantId);
        }
        String tokenId = exchange.getRequest().getQueryParams().getFirst(GlobalConstants.TOKEN_ID_NAME);
        if (StrUtil.isNotBlank(tokenId)) {
            attributes.put(JWTClaimNames.JWT_ID, tokenId);
        }
        return new DefaultOAuth2AuthenticatedPrincipal(username, attributes, AuthorityUtils.NO_AUTHORITIES);
    }

    /**
     * 在统一 JWT 转换之前完成 HMAC 验证和身份构造。
     *
     * @return HMAC 认证阶段的过滤顺序
     */
    @Override
    public int getOrder() {
        return 200;
    }
}
