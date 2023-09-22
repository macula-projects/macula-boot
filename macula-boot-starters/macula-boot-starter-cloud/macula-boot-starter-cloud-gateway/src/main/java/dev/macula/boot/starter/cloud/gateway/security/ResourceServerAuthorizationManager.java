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
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网关自定义鉴权管理器
 *
 * @author <a href="mailto:xianrui0365@163.com">haoxr</a>
 */
@RequiredArgsConstructor
@Slf4j
public class ResourceServerAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final RedisTemplate<String, Object> sysRedisTemplate;

    // 给前端个人的API（只认证Token，不鉴权)
    private final List<String> onlyAuthUrls;

    // 默认URL是否需要检查权限
    private final boolean defaultUrlRequireCheck;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> mono, AuthorizationContext authorizationContext) {
        ServerHttpRequest request = authorizationContext.getExchange().getRequest();
        // 预检请求放行
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return Mono.just(new AuthorizationDecision(true));
        }
        PathMatcher pathMatcher = new AntPathMatcher();
        String method = request.getMethodValue();
        String path = request.getURI().getPath();

        String restfulPath = method + ":" + path;

        // 如果token以"bearer "为前缀，到此方法里说明TOKEN有效即已认证
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);

        // Bear Token，需要验证权限（如果系统间访问需要验证权限要用oauth生成token，grant_type=client_credentials
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, SecurityConstants.TOKEN_PREFIX)) {
            // 这里要可配置哪些路径仅认证，无需鉴权(比如前端API)
            if (onlyAuthUrls.stream().anyMatch(s -> pathMatcher.match(s, path))) {
                // 不是需要鉴权的URL，直接放行
                return Mono.just(new AuthorizationDecision(true));
            }

            // 鉴权
            return checkPerm(mono, restfulPath);
        }

        // Hmac Token，属于系统访问的API
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GatewayConstants.HMAC_AUTH_PREFIX)) {
            // 放行，由后面的HmacGlobalFilter校验签名
            return Mono.just(new AuthorizationDecision(true));
        }

        // 没有 Token不放行
        return Mono.just(new AuthorizationDecision(false));
    }

    /**
     * 鉴权
     * <p>
     * 缓存取 [URL权限-角色集合] 规则数据 urlPermRolesRules = [{'key':'GET:/i18n-base/v1/users/*','value':['ADMIN','TEST']},...]
     */
    private Mono<AuthorizationDecision> checkPerm(Mono<Authentication> mono, String restfulPath) {

        Map<String, Object> urlPermRolesRules =
            sysRedisTemplate.<String, Object>opsForHash().entries(CacheConstants.SECURITY_URL_PERM_ROLES_KEY);

        // 根据请求路径获取有访问权限的角色列表
        // 拥有访问权限的角色
        List<String> authorizedRoles = new ArrayList<>();

        // 是否需要鉴权，默认未设置拦截规则不需鉴权
        boolean requireCheck = defaultUrlRequireCheck;

        PathMatcher pathMatcher = new AntPathMatcher();
        for (Map.Entry<String, Object> permRoles : urlPermRolesRules.entrySet()) {
            String perm = permRoles.getKey();
            perm = perm.replace(":", ":/**");
            if (pathMatcher.match(perm, restfulPath)) {
                List<String> roles = Convert.toList(String.class, permRoles.getValue());
                authorizedRoles.addAll(Convert.toList(String.class, roles));
                if (!requireCheck) {
                    requireCheck = true;
                }
            }
        }

        // 没有设置拦截规则放行
        if (!requireCheck) {
            return Mono.just(new AuthorizationDecision(true));
        }

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
            }).map(AuthorizationDecision::new).defaultIfEmpty(new AuthorizationDecision(false));
    }
}