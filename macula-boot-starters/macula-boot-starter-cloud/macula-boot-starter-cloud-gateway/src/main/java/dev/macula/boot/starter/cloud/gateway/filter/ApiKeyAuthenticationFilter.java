package dev.macula.boot.starter.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter implements WebFilter, Ordered {

    private static final String BEARER_SK_PREFIX = "Bearer sk-";
    private static final String APIKEY_REDIS_PREFIX = "macula:cloud:system:application:apikey:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = RequestUtils.getHeaderOrQueryToken(exchange);

        if (StrUtil.isBlank(token) || !StrUtil.startWithIgnoreCase(token, BEARER_SK_PREFIX)) {
            return chain.filter(exchange);
        }

        String apikey = token.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        String redisKey = APIKEY_REDIS_PREFIX + apikey;

        return Mono.fromCallable(() -> redisTemplate.opsForHash().entries(redisKey))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entries -> {
                    if (entries == null || entries.isEmpty()) {
                        log.warn("API Key验证失败，key不存在: {}", apikey);
                        return ResponseUtils.writeResult(exchange.getResponse(),
                                Result.failed(ApiResultCode.TOKEN_INVALID_OR_EXPIRED));
                    }

                    String applicationCode = (String) entries.get("applicationCode");
                    String applicationId = (String) entries.get("applicationId");
                    String tenantId = (String) entries.get("tenantId");

                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("sub", applicationCode);
                    attributes.put("applicationId", applicationId);
                    attributes.put("nickname", applicationCode);
                    attributes.put("authType", "apikey");

                    if (StrUtil.isNotBlank(tenantId)) {
                        attributes.put(GlobalConstants.TENANT_ID_NAME, Long.parseLong(tenantId));
                    }

                    OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                            applicationCode, attributes, AuthorityUtils.NO_AUTHORITIES);

                    OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                            apikey, Instant.now(), Instant.now().plusSeconds(3600));

                    BearerTokenAuthentication authentication = new BearerTokenAuthentication(
                            principal, accessToken, AuthorityUtils.NO_AUTHORITIES);

                    ServerHttpRequest newRequest = exchange.getRequest().mutate()
                            .headers(headers -> headers.remove(SecurityConstants.AUTHORIZATION_KEY))
                            .build();

                    ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();

                    return chain.filter(newExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 400;
    }
}
