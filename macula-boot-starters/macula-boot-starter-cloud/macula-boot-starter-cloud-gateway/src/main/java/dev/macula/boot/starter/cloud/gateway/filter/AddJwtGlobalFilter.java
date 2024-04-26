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
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.context.TenantContextHolder;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.security.JwtClaimsCustomizer;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * AddJwtGlobalFilter 给请求头添加JWT认证头，后续的微服务透过资源服务器验证JWT
 * </p>
 * 将网关认证信息转为微服务能够识别的安全信息，在 ResourceServerManager#check 鉴权之后执行
 *
 * @author Rain
 * @since 2022-02-20
 */
@Slf4j
@RequiredArgsConstructor
public class AddJwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtEncoder jwtEncoder;

    private final JwtClaimsCustomizer jwtClaimsCustomizer;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://127.0.0.1:9010}")
    private String issuerUri;

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String token = RequestUtils.getHeaderOrQueryToken(exchange);

        // OAuth2的Token才转为JWT
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, SecurityConstants.TOKEN_PREFIX)) {
            return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                    .cast(BearerTokenAuthentication.class).map(BearerTokenAuthentication::getPrincipal)
                    .filter(OAuth2AuthenticatedPrincipal.class::isInstance).cast(OAuth2AuthenticatedPrincipal.class)
                    .switchIfEmpty(Mono.error(new BadCredentialsException("Bad Credentials"))).flatMap(principal -> {
                        ServerHttpRequest request = exchange.getRequest();

                        // 去掉query String access_token
                        if (request.getQueryParams().containsKey(SecurityConstants.QUERY_AUTHORIZATION_KEY)) {
                            request = request.mutate()
                                    .uri(
                                            UriComponentsBuilder.fromUri(request.getURI())
                                                    .replaceQueryParam(SecurityConstants.QUERY_AUTHORIZATION_KEY, Collections.EMPTY_LIST)
                                                    .build(true)
                                                    .toUri()
                                    ).build();
                        }

                        // 添加JWT到header
                        request = request.mutate()
                                .header(SecurityConstants.AUTHORIZATION_KEY,
                                        SecurityConstants.TOKEN_PREFIX + generateJwtToken(principal)
                                ).build();

                        // remove access_token in query parameters
                        ServerWebExchange newExchange = exchange.mutate().request(request).build();

                        return chain.filter(newExchange);
                    });
        }

        // AK/SK转JWT
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GatewayConstants.HMAC_AUTH_PREFIX)) {
            ServerHttpRequest request = exchange.getRequest();

            String username = StrUtil.subBetween(token, "hmac username=\"", "\",");

            // 如果有invoke头，表示上游访问名称
            String invokeUsername =
                    exchange.getRequest().getHeaders().getFirst(GatewayConstants.REMOTE_INVOKE_NAME);
            if (StrUtil.isNotBlank(invokeUsername)) {
                username = invokeUsername;
            }

            Long tenantId = TenantContextHolder.getCurrentTenantId();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(JWTClaimNames.SUBJECT, username);

            // JWT携带租户ID
            if (Objects.nonNull(tenantId)) {
                attributes.put(GlobalConstants.TENANT_ID_NAME, tenantId);
            }
            String tokenId = request.getQueryParams().getFirst(GlobalConstants.TOKEN_ID_NAME);
            if (StrUtil.isNotBlank(tokenId)) {
                attributes.put(JWTClaimNames.JWT_ID, tokenId);
            }

            OAuth2AuthenticatedPrincipal principal =
                    new DefaultOAuth2AuthenticatedPrincipal(username, attributes, AuthorityUtils.NO_AUTHORITIES);
            request = request.mutate().header(SecurityConstants.AUTHORIZATION_KEY,
                    SecurityConstants.TOKEN_PREFIX + generateJwtToken(principal)).build();
            ServerWebExchange newExchange = exchange.mutate().request(request).build();

            return chain.filter(newExchange);
        }

        return chain.filter(exchange);
    }

    /**
     * 根据OAuth2的内容生成JWT，采用HS256
     *
     * @param principal 登录凭据
     * @return JWT
     */
    @SneakyThrows
    private String generateJwtToken(OAuth2AuthenticatedPrincipal principal) {
        // 先看缓存，有则直接返回JWT（需要认证服务器每次登录返回不同的JTI）
        String jwtStr;
        jwtStr = (String) redisTemplate.opsForValue().get(buildKey(principal));
        if (StrUtil.isNotBlank(jwtStr)) {
            return jwtStr;
        }

        JwtClaimsSet.Builder jwtClaimBuilder = JwtClaimsSet.builder();
        // copy oauth2服务器返回的attribute
        principal.getAttributes().forEach(jwtClaimBuilder::claim);

        Instant issuedAt = Instant.now();
        // 处理时间
        jwtClaimBuilder.expiresAt(issuedAt.plus(30, ChronoUnit.DAYS));
        jwtClaimBuilder.issuedAt(issuedAt);

        // 如果缺少jti、deptId、dataScope、nickname，设置默认值
        if (!principal.getAttributes().containsKey(JWTClaimNames.JWT_ID)) {
            jwtClaimBuilder.id(UUID.randomUUID().toString());
        }
        if (!principal.getAttributes().containsKey(SecurityConstants.JWT_NICKNAME_KEY)) {
            jwtClaimBuilder.claim(SecurityConstants.JWT_NICKNAME_KEY, principal.getName());
        }
        if (!principal.getAttributes().containsKey(SecurityConstants.JWT_DEPTID_KEY)) {
            jwtClaimBuilder.claim(SecurityConstants.JWT_DEPTID_KEY, SecurityConstants.ROOT_NODE_ID);
        }
        if (!principal.getAttributes().containsKey(SecurityConstants.JWT_DATASCOPE_KEY)) {
            jwtClaimBuilder.claim(SecurityConstants.JWT_DATASCOPE_KEY, 0);
        }
        // 如果principal没有issue，需要设置jwt的issue
        if (!principal.getAttributes().containsKey(JWTClaimNames.ISSUER)) {
            jwtClaimBuilder.claim(JWTClaimNames.ISSUER, issuerUri);
        }
        // 外部定制claims
        jwtClaimsCustomizer.customize(jwtClaimBuilder);

        JwsAlgorithm jwsAlgorithm = SignatureAlgorithm.RS256;
        JwsHeader.Builder jwsHeaderBuilder = JwsHeader.with(jwsAlgorithm);

        JwtClaimsSet claims = jwtClaimBuilder.build();
        JwsHeader jwsHeader = jwsHeaderBuilder.build();

        Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));
        jwtStr = jwt.getTokenValue();

        long between = ChronoUnit.MINUTES.between(claims.getIssuedAt(), claims.getExpiresAt());
        // 有效期内缓存JWT
        redisTemplate.opsForValue().set(buildKey(principal), jwtStr, between, TimeUnit.MINUTES);

        return jwtStr;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    public static String buildKey(OAuth2AuthenticatedPrincipal principal) {
        // 生成缓存JWT的KEY
        // 当有租户ID时，加上a{tenantId}作为KEY，以防止应用的租户会修改
        String app = "";
        if (principal.getAttributes().containsKey(GlobalConstants.TENANT_ID_NAME)) {
            app = "a" + principal.getAttributes().get(GlobalConstants.TENANT_ID_NAME) + ":";
        }

        // 当有JWT_ID的时候，以JWT_ID作为KEY缓存
        if (principal.getAttributes().containsKey(JWTClaimNames.JWT_ID)) {
            return CacheConstants.GATEWAY_JWT_CACHE_KEY + app + principal.getAttribute(JWTClaimNames.JWT_ID);
        }

        return CacheConstants.GATEWAY_JWT_CACHE_KEY + app + principal.getName();
    }
}