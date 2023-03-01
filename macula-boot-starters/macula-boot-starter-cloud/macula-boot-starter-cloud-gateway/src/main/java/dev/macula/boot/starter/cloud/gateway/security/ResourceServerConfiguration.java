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

import cn.hutool.core.convert.Convert;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源服务器配置
 *
 * @author huan.fu 2021/8/24 - 上午10:08
 */

@ConfigurationProperties(prefix = "macula.gateway.security")
@Configuration
@RequiredArgsConstructor
public class ResourceServerConfiguration {
    @Setter
    private List<String> ignoreUrls = new ArrayList<>();

    @Setter
    private List<String> onlyAuthUrls = Collections.emptyList();

    @Setter
    String jwtSecret = "macula_secret$terces_alucam$123456";

    @NotNull
    private final OAuth2ResourceServerProperties properties;

    @NotNull
    private final RedisTemplate redisTemplate;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // 添加默认忽略的路径
        ignoreUrls.addAll(SecurityConstants.DEFAULT_IGNORE_URLS);

        http.oauth2ResourceServer().opaqueToken().introspector(opaqueTokenIntrospector()).and()
            .accessDeniedHandler(accessDeniedHandler()).authenticationEntryPoint(authenticationEntryPoint())

            .and().authorizeExchange().pathMatchers(Convert.toStrArray(ignoreUrls)).permitAll().anyExchange()
            .access(authorizationManager()).and().exceptionHandling().accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint()).and().csrf().disable();

        return http.build();
    }

    @Bean
    public ReactiveOpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new ReactiveOpaqueTokenIntrospector() {
            final OAuth2ResourceServerProperties.Opaquetoken opaqueToken = properties.getOpaquetoken();
            final ReactiveOpaqueTokenIntrospector delegate =
                new NimbusReactiveOpaqueTokenIntrospector(opaqueToken.getIntrospectionUri(), opaqueToken.getClientId(),
                    opaqueToken.getClientSecret());

            @Override
            public Mono<OAuth2AuthenticatedPrincipal> introspect(String token) {
                return this.delegate.introspect(token).map(
                    principal -> new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), principal.getAttributes(),
                        extractAuthorities(principal)));
            }

            // 自定义获取用户的authorities
            private Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
                List<GrantedAuthority> result = new ArrayList<>(principal.getAuthorities());

                List<String> authorities = principal.getAttribute(SecurityConstants.AUTHORITIES_KEY);
                if (authorities != null) {
                    result.addAll(authorities.stream()
                        .map(role -> new SimpleGrantedAuthority(SecurityConstants.AUTHORITIES_PREFIX + role))
                        .collect(Collectors.toList()));
                }
                return result;
            }
        };
    }

    @Bean
    public ResourceServerAuthorizationManager authorizationManager() {
        return new ResourceServerAuthorizationManager(redisTemplate, onlyAuthUrls);
    }

    /**
     * 自定义未授权响应
     */
    @Bean
    ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> Mono.defer(() -> Mono.just(exchange.getResponse()))
            .flatMap(response -> ResponseUtils.writeErrorInfo(response, ApiResultCode.ACCESS_UNAUTHORIZED));
    }

    /**
     * token无效或者已过期自定义响应
     */
    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, e) -> Mono.defer(() -> Mono.just(exchange.getResponse()))
            .flatMap(response -> ResponseUtils.writeErrorInfo(response, ApiResultCode.TOKEN_INVALID_OR_EXPIRED));
    }

    @Bean
    AddJwtFilter addJwtFilter() {
        return new AddJwtFilter(jwtSecret, redisTemplate);
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedMethod("*");
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
