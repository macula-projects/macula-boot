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
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.filter.AddJwtGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.KongApiGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 资源服务器配置
 *
 * @author huan.fu 2021/8/24 - 上午10:08
 * @since 5.0.21
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
    private boolean defaultUrlRequireCheck = false;

    @NotNull
    private final OAuth2ResourceServerProperties properties;

    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthorizationManager<AuthorizationContext> authorizationManager) {
        //@formatter:off
        // 添加默认忽略的路径
        ignoreUrls.addAll(SecurityConstants.DEFAULT_IGNORE_URLS);
        http.oauth2ResourceServer()
            .bearerTokenConverter(serverBearerTokenAuthenticationConverter())
            .opaqueToken()
            .introspector(opaqueTokenIntrospector())
            .and()
            .accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint())
            .and()
            .authorizeExchange()
            .pathMatchers(Convert.toStrArray(ignoreUrls)).permitAll()
            .anyExchange()
            .access(authorizationManager)
            .and()
            .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint(authenticationEntryPoint())
            .and()
            .cors()
            .and()
            .csrf().disable();
        return http.build();
        //@formatter:on
    }

    @Bean
    public ServerAuthenticationConverter serverBearerTokenAuthenticationConverter() {
        ServerBearerTokenAuthenticationConverter converter = new ServerBearerTokenAuthenticationConverter();
        converter.setAllowUriQueryParameter(true);
        return converter;
    }

    /** 缓存身份需检查 exp，TTL 使用当前时刻到 exp 的剩余时间。 */
    @Bean
    public ReactiveOpaqueTokenIntrospector opaqueTokenIntrospector() {
        OAuth2ResourceServerProperties.Opaquetoken opaqueToken = properties.getOpaquetoken();
        ReactiveOpaqueTokenIntrospector delegate = new NimbusReactiveOpaqueTokenIntrospector(
            opaqueToken.getIntrospectionUri(), opaqueToken.getClientId(), opaqueToken.getClientSecret());
        return new CachingReactiveOpaqueTokenIntrospector(delegate, redisTemplate, java.time.Clock.systemUTC());
    }

    @Bean
    public ResourceServerAuthorizationManager authorizationManager(RedisTemplate<String, Object> sysRedisTemplate) {
        return new ResourceServerAuthorizationManager(sysRedisTemplate, onlyAuthUrls, defaultUrlRequireCheck);
    }

    /**
     * 自定义未授权响应
     */
    @Bean
    ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> Mono.defer(() -> Mono.just(exchange.getResponse())).flatMap(
                response -> ResponseUtils.writeResult(response, HttpStatus.UNAUTHORIZED,
                        Result.failed(ApiResultCode.ACCESS_UNAUTHORIZED)));
    }

    /**
     * token无效或者已过期自定义响应
     */
    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, e) -> Mono.defer(() -> Mono.just(exchange.getResponse())).flatMap(
                response -> ResponseUtils.writeResult(response, HttpStatus.UNAUTHORIZED,
                        Result.failed(ApiResultCode.TOKEN_INVALID_OR_EXPIRED)));
    }

    @Bean
    @Conditional(CorsCondition.class)
    public CorsConfigurationSource corsConfigurationSource(GlobalCorsProperties corsProperties) {
        if (corsProperties != null) {
            Map<String, CorsConfiguration> corsConfigurations = corsProperties.getCorsConfigurations();
            if (!corsConfigurations.isEmpty()) {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.setCorsConfigurations(corsConfigurations);
                return source;
            }
        }
        return null;
    }

    @Bean
    AddJwtGlobalFilter addJwtGlobalFilter(JwtEncoder jwtEncoder, JwtClaimsCustomizer jwtClaimsCustomizer,
                                          RedisTemplate<String, Object> redisTemplate) {
        return new AddJwtGlobalFilter(jwtEncoder, jwtClaimsCustomizer, redisTemplate);
    }

    @Bean
    KongApiGlobalFilter kongApiGlobalFilter(RedisTemplate<String, Object> sysRedisTemplate) {
        return new KongApiGlobalFilter(sysRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(JwtClaimsCustomizer.class)
    JwtClaimsCustomizer jwtClaimsCustomizer() {
        return builder -> {
        };
    }

    /** CORS 配置存在时才启用对应装配。
     * @since 5.0.21
     */
    static class CorsCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
            String prefix = "spring.cloud.gateway.globalcors";

            for (PropertySource<?> propertySource : environment.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                    for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                        if (propertyName.startsWith(prefix)) {
                            return ConditionOutcome.match("CORS configuration exists for path: " + propertyName);
                        }
                    }
                }
            }

            return ConditionOutcome.noMatch("CORS configuration does not exist");
        }
    }
}
