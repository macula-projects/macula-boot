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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.hutool.core.convert.Convert;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.filter.AddJwtGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.ApiKeyGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.KongApiGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.utils.ApiKeyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * 资源服务器配置
 *
 * @author huan.fu 2021/8/24 - 上午10:08
 * @since 5.0.0
 */

@ConfigurationProperties(prefix = "macula.gateway.security")
@Configuration
@RequiredArgsConstructor
public class ResourceServerConfiguration {

    @Setter
    private List<String> ignoreUrls = new ArrayList<>();

    @NotNull
    private final OAuth2ResourceServerProperties properties;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 配置资源服务器安全链，在认证前使用全局 CORS 规则处理跨域请求。
     *
     * @param http 响应式安全配置
     * @param authorizationManager URL 授权管理器
     * @return 网关安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ReactiveAuthorizationManager<AuthorizationContext> authorizationManager) {
        //@formatter:off
        // 添加默认忽略的路径
        ignoreUrls.addAll(SecurityConstants.DEFAULT_IGNORE_URLS);
        http.oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenConverter(serverBearerTokenAuthenticationConverter())
            .opaqueToken(opaque -> opaque.introspector(opaqueTokenIntrospector()))
        )
        .exceptionHandling(exceptions -> exceptions
            .accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint())
        )
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(Convert.toStrArray(ignoreUrls)).permitAll()
            .anyExchange().access(authorizationManager)
        )
        // 在认证前复用全局 CORS 配置，使允许来源也能读取认证失败响应。
        .cors(Customizer.withDefaults())
        // 凭据由客户端显式传递，不从 Cookie 提取；引入 Cookie/Session 认证时须重新评估。
        .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
        //@formatter:on
    }

    /**
     * 将 API Key 留给 Gateway 认证过滤器，普通 Bearer 继续使用标准 OAuth token 提取规则。
     *
     * @return 同时执行 API Key 输入边界检查的认证转换器
     */
    @Bean
    public ServerAuthenticationConverter serverBearerTokenAuthenticationConverter() {
        ServerBearerTokenAuthenticationConverter converter = new ServerBearerTokenAuthenticationConverter();
        converter.setAllowUriQueryParameter(true);
        return exchange -> Mono.defer(() -> ApiKeyUtils.resolveApiKey(exchange) != null
                ? Mono.empty() : converter.convert(exchange));
    }

    /**
     * 装配按剩余有效期缓存的内省器，命中缓存仍执行过期检查。
     *
     * @return 带共享 Token 缓存的内省器
     */
    @Bean
    public ReactiveOpaqueTokenIntrospector opaqueTokenIntrospector() {
        OAuth2ResourceServerProperties.Opaquetoken opaqueToken = properties.getOpaquetoken();

        assert opaqueToken.getClientId() != null;
        assert opaqueToken.getClientSecret() != null;

        ReactiveOpaqueTokenIntrospector delegate = SpringReactiveOpaqueTokenIntrospector
                .withIntrospectionUri(opaqueToken.getIntrospectionUri())
                .clientId(opaqueToken.getClientId())
                .clientSecret(opaqueToken.getClientSecret())
                .build();

        return new CachingReactiveOpaqueTokenIntrospector(delegate, redisTemplate, java.time.Clock.systemUTC());
    }

    /**
     * 为进入网关安全链的请求创建 URL 授权管理器。
     *
     * @param sysRedisTemplate 发布 URL 角色映射的 System Redis 连接
     * @return 共用同一套 URL 规则的管理器
     */
    @Bean
    public ResourceServerAuthorizationManager authorizationManager(RedisTemplate<String, Object> sysRedisTemplate) {
        return new ResourceServerAuthorizationManager(sysRedisTemplate);
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
     * token 无效或者已过期自定义响应
     */
    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, e) -> Mono.defer(() -> Mono.just(exchange.getResponse())).flatMap(
                response -> ResponseUtils.writeResult(response, HttpStatus.UNAUTHORIZED,
                        Result.failed(ApiResultCode.TOKEN_INVALID_OR_EXPIRED)));
    }

    /**
     * 将 Gateway 全局规则提供给 Security 和提前执行的保护配置端点。
     *
     * @param corsProperties Gateway 全局跨域规则
     * @return 配置源；映射为空时返回 null，不增加默认开放规则
     */
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

    /**
     * 装配可覆盖的 API Key 过滤器；关闭功能时仍由过滤器明确拒绝 API Key 请求。
     *
     * @param sysRedisTemplate System 的密钥及租户缓存
     * @return API Key 认证过滤器
     */
    @Bean
    @ConditionalOnMissingBean(ApiKeyGlobalFilter.class)
    public ApiKeyGlobalFilter apiKeyGlobalFilter(RedisTemplate<String, Object> sysRedisTemplate) {
        return new ApiKeyGlobalFilter(sysRedisTemplate);
    }

    /**
     * 装配负责 HMAC 验签、应用准入及 principal 构造的过滤器。
     *
     * @param sysRedisTemplate System 的应用及租户缓存
     * @return HMAC 认证过滤器
     */
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

    /**
     * 判断网关是否声明全局 CORS 配置的条件。
     *
     * @since 5.0.0
     */
    static class CorsCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
            String prefix = "spring.cloud.gateway.server.webflux.globalcors";

            for (PropertySource<?> propertySource : environment.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
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
