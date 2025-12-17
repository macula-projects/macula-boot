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

package dev.macula.boot.starter.security.config;

import cn.hutool.core.convert.Convert;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.starter.security.utils.ResponseUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ConditionalOnIssuerLocationJwtDecoder;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ConditionalOnPublicKeyJwtDecoder;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * <b>ResourceServerConfig</b> 资源服务器的安全配置
 * 本配置适应JWT和普通Token，根据Token决定采用哪种Provider
 * </p>
 *
 * @author Rain
 * @since 2020-03-19
 */

@RequiredArgsConstructor
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@Configuration
public class ResourceServerConfiguration implements ApplicationContextAware {

    private final OAuth2ResourceServerProperties properties;

    private final SecurityProperties securityProperties;

    private ApplicationContext applicationContext;

    /**
     * 配置安全过滤器链
     *
     * @param http HTTP安全配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        // 1. OAuth2 资源服务器配置（JWT 认证）
        http.oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(applicationContext.getBean(JwtDecoder.class)) // 获取自定义 JwtDecoder
                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // 自定义 JWT 认证转换器
            )
            .accessDeniedHandler(accessDeniedHandler()) // 拒绝访问处理器
            .authenticationEntryPoint(authenticationEntryPoint()) // 认证入口点
        )
        // 2. 会话管理（无状态）
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        // 3. CSRF 禁用（前后端分离场景）
        .csrf(AbstractHttpConfigurer::disable)
        // 4. 响应头配置（允许同源 iframe）
        .headers(headers -> headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
        )
        // 5. 请求授权规则
        .authorizeHttpRequests(authorize -> authorize
            // 放行忽略的 URL
            .requestMatchers(Convert.toStrArray(securityProperties.getIgnoreUrls())).permitAll()
            // 其他请求需要认证
            .anyRequest().authenticated()
        )
        // 6. 异常处理（7.0 中可直接配置，无需重复 and()）
        .exceptionHandling(exceptions -> exceptions
            .accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint())
        );

        return http.build();
        // @formatter:on
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> ResponseUtils.writeErrorInfo(response,
            ApiResultCode.TOKEN_ACCESS_FORBIDDEN);
    }

    /**
     * token 无效或者已过期自定义响应
     */
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, accessDeniedException) -> ResponseUtils.writeErrorInfo(response,
            ApiResultCode.TOKEN_INVALID_OR_EXPIRED);
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        // 从 JWT 转换为 Authentication
        return new Converter<>() {
            private final JwtGrantedAuthoritiesConverter jwtScopeAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
            private final JwtGrantedAuthoritiesConverter jwtAuthoritiesAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

            @Override
            public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
                Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
                return new JwtAuthenticationToken(jwt, authorities);
            }

            // 从 JWT的authorities属性中读取权限
            private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
                // 先把 SCOPE变成authorities
                Collection<GrantedAuthority> authorities = jwtScopeAuthoritiesConverter.convert(jwt);
                // 再把authorities属性转换过来，相加
                jwtAuthoritiesAuthoritiesConverter.setAuthoritiesClaimName(SecurityConstants.AUTHORITIES_KEY);
                jwtAuthoritiesAuthoritiesConverter.setAuthorityPrefix(SecurityConstants.AUTHORITIES_PREFIX);
                authorities.addAll(jwtAuthoritiesAuthoritiesConverter.convert(jwt));
                return authorities;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    JwtDecoder jwtDecoderByJwkKeySetUri() {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        NimbusJwtDecoder nimbusJwtDecoder =
            NimbusJwtDecoder.withJwkSetUri(jwt.getJwkSetUri()).cache(new ConcurrentMapCache("macula-jwk-set-cache"))
                .jwsAlgorithms(this::jwsAlgorithms).build();
        String issuerUri = jwt.getIssuerUri();
        if (issuerUri != null) {
            nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        }
        return nimbusJwtDecoder;
    }

    @Bean
    @ConditionalOnPublicKeyJwtDecoder
    JwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        RSAPublicKey publicKey = (RSAPublicKey)KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(getKeySpec(jwt.readPublicKey())));
        return NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(SignatureAlgorithm.from(jwt.getJwsAlgorithms().get(0))).build();
    }

    private byte[] getKeySpec(String keyValue) {
        keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        return Base64.getMimeDecoder().decode(keyValue);
    }

    @Bean
    @ConditionalOnIssuerLocationJwtDecoder
    SupplierJwtDecoder jwtDecoderByIssuerUri() {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        return new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(jwt.getIssuerUri()));
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @Conditional(SecretCondition.class)
    JwtDecoder jwtDecoderBySecret() {
        // 根据给定的字节数组使用 AES加密算法构造一个密钥
        byte[] secrets = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secrets, 0, secrets.length, "HMACSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
        for (String algorithm : this.properties.getJwt().getJwsAlgorithms()) {
            signatureAlgorithms.add(SignatureAlgorithm.from(algorithm));
        }
    }
}
