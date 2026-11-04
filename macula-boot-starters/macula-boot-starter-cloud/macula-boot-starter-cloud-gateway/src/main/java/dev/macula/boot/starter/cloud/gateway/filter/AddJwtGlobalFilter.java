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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jwt.JWTClaimNames;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.security.JwtClaimsCustomizer;
import dev.macula.boot.starter.cloud.gateway.utils.ApiKeyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtils;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * 将 OAuth、API Key 和 HMAC 的已验证身份统一转换为下游可验证的 JWT。
 * <p>
 * OAuth 身份来自 Spring Security；应用身份由各自的认证 GlobalFilter 写入请求属性。
 * 本过滤器只负责 JWT 缓存、签发和请求改写，不解析凭据来构造认证身份。
 *
 * @author Rain
 * @since 2022-02-20
 */
@Slf4j
@RequiredArgsConstructor
public class AddJwtGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 保持既有内部 JWT 有效期，外部凭据仍逐请求验证。
     */
    private static final long JWT_VALIDITY_DAYS = 30;

    /**
     * JWT 缓存中标识业务租户段的前缀。
     */
    private static final String TENANT_CACHE_SEGMENT_PREFIX = "tn";

    private final JwtEncoder jwtEncoder;

    private final JwtClaimsCustomizer jwtClaimsCustomizer;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://127.0.0.1:9010}")
    private String issuerUri;

    /**
     * 为已认证请求替换 JWT 并清理 Query 凭据；未携带认证信息的请求沿用原 exchange。
     *
     * @param exchange 当前请求
     * @param chain 下游过滤链
     * @return 请求处理结果；识别到凭据但缺少可信身份时以认证错误终止
     */
    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.defer(() -> resolvePrincipal(exchange)
                .map(principal -> {
                    ServerHttpRequest request = exchange.getRequest();
                    request = request.mutate()
                            .uri(UriComponentsBuilder.fromUri(request.getURI())
                                    .replaceQueryParam(SecurityConstants.QUERY_AUTHORIZATION_KEY).build(true).toUri())
                            .header(SecurityConstants.AUTHORIZATION_KEY,
                                    SecurityConstants.TOKEN_PREFIX + generateJwtToken(principal)).build();
                    return exchange.mutate().request(request).build();
                })
                // 在 exchange 阶段处理空身份，避免对 Mono<Void> 兜底而重复执行下游。
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter));
    }

    /**
     * 按凭据类型读取对应认证阶段建立的身份，不把请求中的主体或租户字段当成认证结果。
     *
     * @param exchange 当前请求
     * @return 可信身份；无相关凭据时为空，存在凭据但身份缺失时失败
     */
    private Mono<OAuth2AuthenticatedPrincipal> resolvePrincipal(ServerWebExchange exchange) {
        String token = RequestUtils.getHeaderOrQueryToken(exchange);
        // API Key 也是 Bearer 格式，必须先分流，且只能使用前置过滤器的验证结果。
        if (ApiKeyUtils.resolveApiKey(exchange) != null) {
            return Mono.just(ApiKeyUtils.verifiedPrincipal(exchange));
        }

        // HMAC Token
        if (KongApiUtils.resolveHmacToken(exchange) != null) {
            return Mono.just(KongApiUtils.verifiedPrincipal(exchange));
        }

        // OAUTH2 Token
        if (StrUtil.startWithIgnoreCase(token, SecurityConstants.TOKEN_PREFIX)) {
            // 上下文存在时也可能尚未认证；将 null 转为空信号，统一进入认证失败分支。
            return ReactiveSecurityContextHolder.getContext()
                    .mapNotNull(SecurityContext::getAuthentication)
                    .ofType(BearerTokenAuthentication.class)
                    .filter(BearerTokenAuthentication::isAuthenticated)
                    .mapNotNull(BearerTokenAuthentication::getPrincipal)
                    .ofType(OAuth2AuthenticatedPrincipal.class)
                    .switchIfEmpty(Mono.error(new BadCredentialsException("Missing authenticated OAuth principal")));
        }
        return Mono.empty();
    }

    /**
     * 优先复用身份对应的缓存，否则补齐标准 claims、调用定制器并以 RS256 签发 JWT。
     *
     * @param principal 已验证身份及其可信 claims
     * @return 缓存中或本次签发的 JWT
     */
    @SneakyThrows
    private String generateJwtToken(OAuth2AuthenticatedPrincipal principal) {
        // OAuth 依赖每次登录使用不同 jti，应用身份则按各自的租户和主体规则复用缓存。
        String jwtStr;
        jwtStr = (String) redisTemplate.opsForValue().get(buildKey(principal));
        if (StrUtil.isNotBlank(jwtStr)) {
            return jwtStr;
        }

        JwtClaimsSet.Builder jwtClaimBuilder = JwtClaimsSet.builder();
        // 保留认证阶段生成的 claims，避免在 JWT 转换时重新解释应用身份。
        principal.getAttributes().forEach(jwtClaimBuilder::claim);

        Instant issuedAt = Instant.now();
        // 保留既有 30 天内部 JWT 有效期；外部凭据是否有效仍由前置认证阶段判断。
        jwtClaimBuilder.expiresAt(issuedAt.plus(JWT_VALIDITY_DAYS, ChronoUnit.DAYS));
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
        // 已有 issuer 保持原值，缺省时使用网关配置的签发者。
        if (!principal.getAttributes().containsKey(JWTClaimNames.ISSUER)) {
            jwtClaimBuilder.claim(JWTClaimNames.ISSUER, issuerUri);
        }
        // 在标准缺省值补齐后允许应用定制 claims。
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

    /**
     * 按认证来源、租户和身份生成 JWT 缓存键，避免 API Key 与同名 HMAC 应用串用 claims。
     *
     * @param principal 已验证身份
     * @return API Key 专用键，或沿用 OAuth/HMAC 的 jti 优先缓存键
     */
    public static String buildKey(OAuth2AuthenticatedPrincipal principal) {

        String prefix = CacheConstants.GATEWAY_JWT_CACHE_KEY;

        // APIKEY 加多一个前缀
        if (GatewayConstants.APIKEY_AUTH_TYPE.equals(principal.getAttribute(GatewayConstants.AUTH_TYPE_ATTRIBUTE))) {
            prefix += GatewayConstants.APIKEY_AUTH_TYPE + ":";
        }

        // 将业务租户作为缓存键的一段，避免应用更换租户后复用旧 JWT
        String tenate = "";
        if (principal.getAttributes().containsKey(GlobalConstants.TENANT_ID_NAME)) {
            tenate = TENANT_CACHE_SEGMENT_PREFIX + principal.getAttributes().get(GlobalConstants.TENANT_ID_NAME) + ":";
        }

        // 生成缓存JWT的KEY
        // 当有JWT_ID的时候，以JWT_ID作为KEY缓存
        if (principal.getAttributes().containsKey(JWTClaimNames.JWT_ID)) {
            return prefix + tenate + principal.getAttribute(JWTClaimNames.JWT_ID);
        }
        return prefix + tenate + principal.getName();
    }

    /**
     * 在 API Key 和 HMAC 完成凭据验证、发布 principal 后执行 JWT 转换。
     *
     * @return JWT 转换阶段的过滤顺序
     */
    @Override
    public int getOrder() {
        return 1000;
    }
}
