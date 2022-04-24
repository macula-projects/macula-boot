package org.macula.boot.starter.gateway.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.macula.boot.starter.commons.constants.SecurityConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

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

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .cast(BearerTokenAuthentication.class)
            .map(BearerTokenAuthentication::getPrincipal)
            .filter(OAuth2AuthenticatedPrincipal.class::isInstance)
            .cast(OAuth2AuthenticatedPrincipal.class)
            .flatMap(principal -> {
                ServerHttpRequest request = exchange.getRequest();
                request = request.mutate()
                    .header(SecurityConstants.AUTHORIZATION_KEY, SecurityConstants.TOKEN_PREFIX + generateJwtToken(principal))
                    .build();

                ServerWebExchange newExchange = exchange.mutate().request(request).build();
                return chain.filter(newExchange);
            });
    }

    @SneakyThrows
    /**
     * 根据OAuth2的内容生成JWT，采用HS256
     */
    private String generateJwtToken(OAuth2AuthenticatedPrincipal principal) {

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString());
        principal.getAttributes().forEach(builder::claim);
        JWTClaimsSet claimsSet = builder.build();

        JWSObject jwt = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claimsSet.toPayload());

        MACSigner signer = new MACSigner(jwtSecret);
        jwt.sign(signer);

        return jwt.serialize();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}