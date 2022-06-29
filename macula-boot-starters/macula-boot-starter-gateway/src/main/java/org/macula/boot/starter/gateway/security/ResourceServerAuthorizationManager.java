package org.macula.boot.starter.gateway.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.macula.boot.starter.commons.constants.GlobalConstants;
import org.macula.boot.starter.commons.constants.SecurityConstants;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 网关自定义鉴权管理器
 *
 * @author <a href="mailto:xianrui0365@163.com">haoxr</a>
 */
@RequiredArgsConstructor
@Slf4j
public class ResourceServerAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final RedisTemplate redisTemplate;

    private final List<String> onlyAuthUrls;

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

        // RESTFul接口权限设计 @link https://www.cnblogs.com/haoxianrui/p/14961707.html
        String restfulPath = method + ":" + path;

        // 如果token以"bearer "为前缀，到此方法里说明TOKEN有效即已认证
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, SecurityConstants.TOKEN_PREFIX) ) {
            // 这里要可配置哪些路径仅认证，无需鉴权
            if (onlyAuthUrls.stream().anyMatch(s -> pathMatcher.match(s, path))) {
                // 不是需要鉴权的URL，直接放行
                return Mono.just(new AuthorizationDecision(true));
            }
        } else {
            return Mono.just(new AuthorizationDecision(false));
        }

        /*
         * 鉴权开始，TODO 需要在管理端同步URL权限-角色集合
         *
         * 缓存取 [URL权限-角色集合] 规则数据
         * urlPermRolesRules = [{'key':'GET:/api/v1/users/*','value':['ADMIN','TEST']},...]
         */
        Map<String, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);

        // 根据请求路径获取有访问权限的角色列表
        // 拥有访问权限的角色
        List<String> authorizedRoles = new ArrayList<>();
        // 是否需要鉴权，默认未设置拦截规则不需鉴权
        boolean requireCheck = false;

        for (Map.Entry<String, Object> permRoles : urlPermRolesRules.entrySet()) {
            String perm = permRoles.getKey();
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

        // 判断Token中携带的用户角色是否有权限访问
        Mono<AuthorizationDecision> authorizationDecisionMono = mono
            .filter(Authentication::isAuthenticated)
            .flatMapIterable(Authentication::getAuthorities)
            .map(GrantedAuthority::getAuthority)
            .any(authority -> {
                String roleCode = authority.substring(SecurityConstants.AUTHORITIES_PREFIX.length()); // 用户的角色
                if (GlobalConstants.ROOT_ROLE_CODE.equals(roleCode)) {
                    return true; // 如果是超级管理员则放行
                }
                boolean hasAuthorized = CollectionUtil.isNotEmpty(authorizedRoles) && authorizedRoles.contains(roleCode);
                return hasAuthorized;
            })
            .map(AuthorizationDecision::new)
            .defaultIfEmpty(new AuthorizationDecision(false));
        return authorizationDecisionMono;
    }
}