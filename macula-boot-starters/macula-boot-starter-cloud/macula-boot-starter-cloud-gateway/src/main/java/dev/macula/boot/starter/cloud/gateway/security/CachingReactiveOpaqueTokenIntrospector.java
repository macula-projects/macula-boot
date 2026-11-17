/*
 * Copyright (c) 2026 Macula
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


package dev.macula.boot.starter.cloud.gateway.security;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.SecurityConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 按剩余有效期缓存内省身份，缓存命中不能绕过 Token 过期检查。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class CachingReactiveOpaqueTokenIntrospector implements ReactiveOpaqueTokenIntrospector {
    private static final Duration NO_EXPIRY_CACHE_TTL = Duration.ofMinutes(5);
    private final ReactiveOpaqueTokenIntrospector delegate;
    private final RedisTemplate<String, Object> redis;
    private final Clock clock;

    /**
     * 注入内省入口、共享缓存和时间源，便于精确验证过期边界。
     * @param delegate 权威内省实现
     * @param redis 网关共享 Token 缓存
     * @param clock 当前时间源
     */
    public CachingReactiveOpaqueTokenIntrospector(ReactiveOpaqueTokenIntrospector delegate,
        RedisTemplate<String, Object> redis, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate);
        this.redis = Objects.requireNonNull(redis);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * 缓存过期时重新内省；非法属性和内省失败均不能沿用旧身份。
     * @param token 待验证的 opaque Token
     * @return 当前有效的认证身份
     */
    @Override
    public Mono<OAuth2AuthenticatedPrincipal> introspect(String token) {
        // RedisTemplate 是阻塞 API，读写均离开响应式事件循环，且在订阅时才执行。
        return Mono.fromCallable(() -> cached(token)).subscribeOn(Schedulers.boundedElastic())
            .switchIfEmpty(Mono.defer(() -> delegate.introspect(token))
                .flatMap(principal -> Mono.fromCallable(() -> cache(token, principal))
                    .subscribeOn(Schedulers.boundedElastic())));
    }

    /**
     * 读取缓存并校验过期时间，过期身份删除后由权威来源重新确认。
     * @param token 待验证 Token
     * @return 有效缓存身份；缺失或已过期时返回 null
     * @throws BadOpaqueTokenException 缓存类型或 exp 非法
     */
    private OAuth2AuthenticatedPrincipal cached(String token) {
        Object value = redis.opsForValue().get(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token);
        if (value == null) {
            return null;
        }
        if (!(value instanceof OAuth2AuthenticatedPrincipal principal)) {
            throw new BadOpaqueTokenException("Invalid cached identity");
        }
        Instant exp = expiration(principal);
        if (exp != null && !exp.isAfter(clock.instant())) {
            redis.delete(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token);
            return null;
        }
        return principal;
    }

    /** 检查内省结果并按剩余 TTL 缓存，禁止因 iat 较早而延长有效期。 */
    private OAuth2AuthenticatedPrincipal cache(String token, OAuth2AuthenticatedPrincipal principal) {
        Instant exp = expiration(principal);
        Duration ttl = exp == null ? NO_EXPIRY_CACHE_TTL : Duration.between(clock.instant(), exp);
        if (ttl.isNegative() || ttl.isZero()) {
            throw new BadOpaqueTokenException("Expired token");
        }
        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        Object roles = principal.getAttributes().get(SecurityConstants.AUTHORITIES_KEY);
        if (roles instanceof List<?> list) {
            for (Object role : list) {
                if (!(role instanceof String name)) {
                    throw new BadOpaqueTokenException("Invalid token authorities");
                }
                authorities.add(new SimpleGrantedAuthority(SecurityConstants.AUTHORITIES_PREFIX + name));
            }
        } else if (roles != null) {
            throw new BadOpaqueTokenException("Invalid token authorities");
        }
        var normalized = new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), principal.getAttributes(), authorities);
        // Redis TTL 使用毫秒精度；不足一毫秒时仍校验身份，但不写入可能失效的缓存。
        if (ttl.toMillis() > 0) {
            redis.opsForValue().set(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token, normalized, ttl);
        }
        return normalized;
    }

    /** 仅接受内省协议转换后的 Instant；存在但非法的 exp 不得当作无过期时间处理。 */
    private Instant expiration(OAuth2AuthenticatedPrincipal principal) {
        if (!principal.getAttributes().containsKey(OAuth2TokenIntrospectionClaimNames.EXP)) {
            return null;
        }
        Object value = principal.getAttributes().get(OAuth2TokenIntrospectionClaimNames.EXP);
        if (!(value instanceof Instant exp)) {
            throw new BadOpaqueTokenException("Invalid token expiration");
        }
        return exp;
    }
}
