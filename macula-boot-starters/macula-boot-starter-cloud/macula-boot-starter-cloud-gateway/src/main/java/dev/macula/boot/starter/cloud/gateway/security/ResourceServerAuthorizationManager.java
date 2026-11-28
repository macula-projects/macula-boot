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

package dev.macula.boot.starter.cloud.gateway.security;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.cloud.gateway.utils.ApiKeyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网关自定义鉴权管理器
 *
 * @author <a href="mailto:xianrui0365@163.com">haoxr</a>
 * @since 5.0.0
 */
@RequiredArgsConstructor
public class ResourceServerAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final RedisTemplate<String, Object> sysRedisTemplate;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    // URL 规则以接收请求的网关应用名作为命名空间。
    @Value("${spring.application.name}")
    private String appName;

    /**
     * 校验普通 OAuth 请求的 URL 权限，将 API Key 和 HMAC 请求分流到后续认证 GlobalFilter。
     * 应用凭据在此放行仅表示允许继续验证，不代表已经认证成功。
     *
     * @param mono Spring Security 提供的认证身份
     * @param authorizationContext 当前请求的授权上下文
     * @return URL 授权或应用凭据分流结果
     */
    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> mono,
                                                AuthorizationContext authorizationContext) {
        ServerHttpRequest request = authorizationContext.getExchange().getRequest();
        // 预检请求放行
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return Mono.just(new AuthorizationDecision(true));
        }
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        String restfulPath = appName + ":" + method + ":" + path;

        // 如果token以"bearer "为前缀，到此方法里说明TOKEN有效即已认证
        String token = RequestUtils.getHeaderOrQueryToken(authorizationContext.getExchange());

        // API Key 只在此分流，由后续 GlobalFilter 校验凭据和租户准入。
        if (ApiKeyUtils.resolveApiKey(authorizationContext.getExchange()) != null) {
            ApplicationCredentialRouteMapping.requireRoute(authorizationContext.getExchange());
            return Mono.just(new AuthorizationDecision(true));
        }

        // Bear Token，需要验证权限（如果系统间访问需要验证权限要用oauth生成token，grant_type=client_credentials
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, SecurityConstants.TOKEN_PREFIX)) {
            // 鉴权
            return checkPerm(mono, restfulPath);
        }

        // Hmac Token，属于系统访问的API
        if (KongApiUtils.resolveHmacToken(authorizationContext.getExchange()) != null) {
            // 只允许进入由 KongApiGlobalFilter 校验签名的 Gateway 路由。
            ApplicationCredentialRouteMapping.requireRoute(authorizationContext.getExchange());
            return Mono.just(new AuthorizationDecision(true));
        }

        // 没有 Token不放行
        return Mono.just(new AuthorizationDecision(false));
    }

    /**
     * RedisTemplate 调用延迟至订阅，在有界线程池执行，避免阻塞 WebFlux 线程。
     *
     * @return 当前 URL 权限映射，未初始化时返回空映射
     */
    private Mono<Map<String, Object>> loadUrlRules() {
        return Mono.fromCallable(() -> sysRedisTemplate.<String, Object>opsForHash()
            .entries(CacheConstants.SECURITY_URL_PERM_ROLES_KEY)).subscribeOn(Schedulers.boundedElastic())
            .defaultIfEmpty(Map.of());
    }

    /**
     * 从现有规则映射中汇总匹配 URL 的角色。
     *
     * @param mono Spring Security 核验后的身份
     * @param restfulPath 带应用名和方法的规则匹配路径
     * @return Bearer URL 授权决定
     */
    private Mono<AuthorizationResult> checkPerm(Mono<Authentication> mono, String restfulPath) {
        return loadUrlRules().flatMap(rules -> {
            List<String> authorizedRoles = new ArrayList<>();
            for (Map.Entry<String, Object> rule : rules.entrySet()) {
                if (pathMatcher.match(rule.getKey(), restfulPath)) {
                    authorizedRoles.addAll(Convert.toList(String.class, rule.getValue()));
                }
            }
            return checkRoles(mono, authorizedRoles);
        });
    }

    /**
     * 只使用已认证主体的角色，保留 ROOT 和反向角色的现有规则。
     *
     * @param mono Spring Security 核验后的身份
     * @param authorizedRoles 当前 URL 的角色集合
     * @return 角色判定结果
     */
    private Mono<AuthorizationResult> checkRoles(Mono<Authentication> mono, List<String> authorizedRoles) {
        // 判断正向角色是否包含，反向角色是否包含，取其and结果
        // 注意：Authentication中的authorities的ROLE的CODE以ROLE_开头，如果是反向角色会是ROLE_!xxx这种形式
        return mono.filter(Authentication::isAuthenticated).flatMapIterable(Authentication::getAuthorities)
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.substring(SecurityConstants.AUTHORITIES_PREFIX.length())) // 去掉ROLE_前缀
            .collectList().map(authorities -> {
                // 如果是超级管理员直接放行
                if (authorities.contains(SecurityConstants.ROOT_ROLE_CODE)) {
                    return true;
                }

                boolean containsRoles =
                    authorities.stream().filter(role -> !role.startsWith(SecurityConstants.NEGATED_ROLE_PREFIX))
                        .anyMatch(authorizedRoles::contains);

                boolean containsNegatedRoles =
                    authorities.stream().filter(role -> role.startsWith(SecurityConstants.NEGATED_ROLE_PREFIX))
                        .anyMatch(role -> authorizedRoles.contains(
                            role.substring(SecurityConstants.NEGATED_ROLE_PREFIX.length())));

                return containsRoles && !containsNegatedRoles;
            }).map(authorized -> (AuthorizationResult)new AuthorizationDecision(authorized))
            .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
