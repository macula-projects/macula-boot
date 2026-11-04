/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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

import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jwt.JWTClaimNames;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.utils.ApiKeyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import dev.macula.boot.starter.cloud.gateway.utils.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 在 Gateway 过滤链校验 API Key 和应用归属，保存请求级可信身份。
 *
 * @author rain
 * @since 5.0.0
 */
@RequiredArgsConstructor
public class ApiKeyGlobalFilter implements GlobalFilter, Ordered {
    private final RedisTemplate<String, Object> sysRedisTemplate;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Value("${macula.gateway.apikey.enabled:true}")
    private boolean enabled = true;

    /**
     * 校验 API Key 输入、功能开关及应用准入，成功后才向后续过滤器发布可信 principal。
     *
     * @param exchange 当前请求
     * @param chain 下游过滤链
     * @return 非 API Key 继续处理；API Key 验证失败时返回拒绝响应
     */
    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.defer(() -> {
            // 清除可能遗留的验证结果，失败请求不得复用之前的 principal。
            exchange.getAttributes().remove(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE);

            // GlobalFilter 会收到所有路由请求，必须先分流，避免拦截 OAuth、HMAC 和匿名路由。
            try {
                if (ApiKeyUtils.resolveApiKey(exchange) == null) {
                    return chain.filter(exchange);
                }
            } catch (OAuth2AuthenticationException exception) {
                return reject(exchange);
            }
            if (!enabled) {
                return reject(exchange);
            }
            return Mono.fromCallable(() -> validate(exchange))
                    // 密钥与租户映射查询均为阻塞 Redis 调用，移出响应式事件线程。
                    .subscribeOn(Schedulers.boundedElastic())
                    // 缓存等非预期异常统一拒绝；此兜底位于下游调用之前，不吞掉下游业务异常。
                    .onErrorReturn(Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED))
                    .flatMap(result -> {
                        if (!result.isSuccess()) {
                            return ResponseUtils.writeResult(exchange.getResponse(), HttpStatus.FORBIDDEN, result);
                        }
                        exchange.getAttributes().put(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE, result.getData());
                        return chain.filter(exchange);
                    });
        });
    }

    /**
     * 从 System 缓存读取应用身份，确认归属租户与本网关一致后构造 principal。
     *
     * @param exchange ServerWebExchange
     * @return 包含可信 principal 的成功结果，或身份字段/租户准入不满足时的拒绝结果
     */
    private Result<OAuth2AuthenticatedPrincipal> validate(ServerWebExchange exchange) {

        String apiKey = ApiKeyUtils.resolveApiKey(exchange);

        // 检查APIKEY
        Map<String, String> apps = sysRedisTemplate.<String, String>opsForHash()
                .entries(CacheConstants.APIKEY_CACHE_PREFIX + apiKey);

        String code = apps.get(CacheConstants.APIKEY_APPLICATION_CODE_ATTRIBUTE);
        String applicationId = apps.get(CacheConstants.APIKEY_APPLICATION_ID_ATTRIBUTE);
        if (StrUtil.isBlank(code) || StrUtil.isBlank(applicationId)) {
            return Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED);
        }

        // 检查归属租户以及是否允许URL
        Result<Long> tenant = TenantUtils.checkTenantAndUrls(exchange, sysRedisTemplate, applicationName, apps);
        if (!tenant.isSuccess()) {
            return Result.failed(tenant.getCode(), tenant.getMsg(), tenant.getCause());
        }

        return Result.success(createPrincipal(code, applicationId, tenant.getData()));
    }

    /**
     * 使用已通过字段检查和租户准入的应用信息构造 principal，供统一 JWT 转换使用。
     *
     * @param applicationCode 已校验的应用编码，作为主体及默认昵称
     * @param applicationId 已校验的应用 ID
     * @param tenantId 准入通过后解析的认证业务租户，可与应用归属租户不同
     * @return 携带 API Key 认证标识且不附加用户角色的可信应用身份
     */
    private OAuth2AuthenticatedPrincipal createPrincipal(String applicationCode, String applicationId, Long tenantId) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(JWTClaimNames.SUBJECT, applicationCode);
        attributes.put(SecurityConstants.JWT_NICKNAME_KEY, applicationCode);
        attributes.put(GlobalConstants.TENANT_ID_NAME, tenantId);

        attributes.put(CacheConstants.APIKEY_APPLICATION_ID_ATTRIBUTE, applicationId);
        attributes.put(GatewayConstants.AUTH_TYPE_ATTRIBUTE, GatewayConstants.APIKEY_AUTH_TYPE);

        return new DefaultOAuth2AuthenticatedPrincipal(applicationCode, attributes, AuthorityUtils.NO_AUTHORITIES);
    }

    /**
     * 返回统一拒绝响应，不暴露原始密钥、缓存内容或底层异常详情。
     *
     * @param exchange 当前请求
     * @return 拒绝响应的写入结果
     */
    private Mono<Void> reject(ServerWebExchange exchange) {
        return ResponseUtils.writeResult(exchange.getResponse(), HttpStatus.FORBIDDEN,
                Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED));
    }

    /**
     * 在 HMAC 过滤器及统一 JWT 转换之前处理 API Key。
     *
     * @return API Key 认证阶段的过滤顺序
     */
    @Override
    public int getOrder() {
        return 190;
    }
}
