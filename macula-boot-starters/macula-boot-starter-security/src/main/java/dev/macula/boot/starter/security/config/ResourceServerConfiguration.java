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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

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
@ConfigurationProperties(prefix = "macula.security")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class ResourceServerConfiguration implements ApplicationContextAware {

    @Value("${spring.security.oauth2.resourceserver.jwt.secret:macula_secret$terces_alucam$123456}")
    String jwtSecret;

    @Setter
    List<String> ignoreUrls = new ArrayList<>();

    private final OAuth2ResourceServerProperties properties;

    private ApplicationContext applicationContext;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 添加默认忽略的路径
        // @formatter:off
        ignoreUrls.addAll(SecurityConstants.DEFAULT_IGNORE_URLS);

        http.oauth2ResourceServer()
            .jwt()
            .decoder(applicationContext.getBean(JwtDecoder.class))
            .jwtAuthenticationConverter(jwtAuthenticationConverter())
            .and()
            .accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint());

        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .authorizeRequests().antMatchers(Convert.toStrArray(ignoreUrls)).permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling().accessDeniedHandler(accessDeniedHandler())
            .authenticationEntryPoint(authenticationEntryPoint());
        return http.build();
        // @formatter:on
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> ResponseUtils.writeErrorInfo(response,
            ApiResultCode.TOKEN_ACCESS_FORBIDDEN);
    }

    /**
     * token无效或者已过期自定义响应
     */
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, accessDeniedException) -> ResponseUtils.writeErrorInfo(response,
            ApiResultCode.TOKEN_INVALID_OR_EXPIRED);
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        // 从JWT转换为Authentication
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            private final JwtGrantedAuthoritiesConverter jwtScopeAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
            private final JwtGrantedAuthoritiesConverter jwtAuthoritiesAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

            @Override
            public final AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
                return new JwtAuthenticationToken(jwt, authorities);
            }

            // 从JWT的authorities属性中读取权限
            private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
                // 先把SCOPE变成authorities
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
        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwt.getJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.from(jwt.getJwsAlgorithm())).build();
        String issuerUri = jwt.getIssuerUri();
        if (issuerUri != null) {
            nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        }
        return nimbusJwtDecoder;
    }

    @Bean
    @Conditional(KeyValueCondition.class)
    JwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        RSAPublicKey publicKey = (RSAPublicKey)KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(getKeySpec(jwt.readPublicKey())));
        return NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(SignatureAlgorithm.from(jwt.getJwsAlgorithm())).build();
    }

    private byte[] getKeySpec(String keyValue) {
        keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        return Base64.getMimeDecoder().decode(keyValue);
    }

    @Bean
    @Conditional(IssuerUriCondition.class)
    SupplierJwtDecoder jwtDecoderByIssuerUri() {
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        return new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(jwt.getIssuerUri()));
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @Conditional(SecretCondition.class)
    JwtDecoder jwtDecoderBySecret() throws UnsupportedEncodingException {
        // 根据给定的字节数组使用AES加密算法构造一个密钥
        byte[] secrets = jwtSecret.getBytes("UTF-8");
        SecretKey secretKey = new SecretKeySpec(secrets, 0, secrets.length, "HMACSHA256");
        JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        return jwtDecoder;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
