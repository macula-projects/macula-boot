/*
 * Copyright (c) 2022 Macula
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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.starter.cloud.gateway.utils.GatewayConstant;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * AddJwtFilter 给请求头添加JWT认证头，后续的微服务透过资源服务器验证JWT
 * </p>
 * 将网关认证信息转为微服务能够识别的安全信息，在 ResourceServerManager#check 鉴权之后执行
 *
 * @author Rain
 * @since 2022-02-20
 */
@Slf4j
@RequiredArgsConstructor
public class AddJwtFilter implements GlobalFilter, Ordered {

    private final String jwtSecret;

    private final RedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getHeaders().containsKey(GlobalConstants.AUTHORIZATION_KEY)) {
            // OAuth2的Token才转为JWT
            String token = exchange.getRequest().getHeaders().getFirst(GlobalConstants.AUTHORIZATION_KEY);
            if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GlobalConstants.TOKEN_PREFIX)) {
                return ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .cast(BearerTokenAuthentication.class)
                        .map(BearerTokenAuthentication::getPrincipal)
                        .filter(OAuth2AuthenticatedPrincipal.class::isInstance)
                        .cast(OAuth2AuthenticatedPrincipal.class)
                        .switchIfEmpty(Mono.error(new BadCredentialsException("Bad Credentials")))
                        .flatMap(principal -> {
                            ServerHttpRequest request = exchange.getRequest();
                            request = request.mutate()
                                    .header(GlobalConstants.AUTHORIZATION_KEY, GlobalConstants.TOKEN_PREFIX + generateJwtToken(principal))
                                    .build();

                            ServerWebExchange newExchange = exchange.mutate().request(request).build();
                            return chain.filter(newExchange);
                        });
            }

            // AK/SK转JWT
            if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, GatewayConstant.HMAC_AUTH_PREFIX)) {
                ServerHttpRequest request = exchange.getRequest();

                String username = StrUtil.subBetween(token, "hmac username=\"", "\",");
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(JWTClaimNames.SUBJECT, username);
                OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(username, attributes, AuthorityUtils.NO_AUTHORITIES);
                request = request.mutate().header(GlobalConstants.AUTHORIZATION_KEY, GlobalConstants.TOKEN_PREFIX + generateJwtToken(principal)).build();
                ServerWebExchange newExchange = exchange.mutate().request(request).build();

                return chain.filter(newExchange);
            }
        }
        return chain.filter(exchange);
    }

    @SneakyThrows
    /**
     * 根据OAuth2的内容生成JWT，采用HS256
     */
    private String generateJwtToken(OAuth2AuthenticatedPrincipal principal) {
        // 先看缓存，有则直接返回JWT
        String jwtStr = (String) redisTemplate.opsForValue().get(GatewayConstant.JWT_CACHE_KEY + principal.getName());
        if (StrUtil.isNotBlank(jwtStr)) {
            return jwtStr;
        }

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString());

        // copy oauth2服务器返回的attribute
        principal.getAttributes().forEach(builder::claim);

        // 处理时间
        builder.expirationTime(DateUtil.offsetMonth(new Date(), 12));
        builder.issueTime(new Date());

        // 如果缺少deptId、dataScope、nickname，设置默认值
        if (!principal.getAttributes().containsKey(GlobalConstants.JWT_NICKNAME_KEY)) {
            builder.claim(GlobalConstants.JWT_NICKNAME_KEY, principal.getName());
        }
        if (!principal.getAttributes().containsKey(GlobalConstants.JWT_DEPTID_KEY)) {
            builder.claim(GlobalConstants.JWT_DEPTID_KEY, GlobalConstants.ROOT_NODE_ID);
        }
        if (!principal.getAttributes().containsKey(GlobalConstants.JWT_DATASCOPE_KEY)) {
            builder.claim(GlobalConstants.JWT_DATASCOPE_KEY, 0);
        }

        JWTClaimsSet claimsSet = builder.build();

        JWSObject jwt = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claimsSet.toPayload());

        MACSigner signer = new MACSigner(jwtSecret);
        jwt.sign(signer);

        jwtStr = jwt.serialize();

        // 缓存JWT一天
        redisTemplate.opsForValue().set(GatewayConstant.JWT_CACHE_KEY + principal.getName(), jwtStr, 1, TimeUnit.DAYS);

        return jwtStr;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}