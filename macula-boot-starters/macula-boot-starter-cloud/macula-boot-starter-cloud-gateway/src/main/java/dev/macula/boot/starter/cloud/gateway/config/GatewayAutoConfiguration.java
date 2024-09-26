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

package dev.macula.boot.starter.cloud.gateway.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import dev.macula.boot.starter.cloud.gateway.filter.*;
import dev.macula.boot.starter.cloud.gateway.security.ResourceServerConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * <p>
 * <b>ResourceServerConfiguration</b> 作为资源服务的网关配置
 * </p>
 *
 * @author Rain
 * @since 2022-02-19
 */
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewayProperties.class)
@Import({ResourceServerConfiguration.class, JwtConfiguration.class})
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "macula.cloud.gray.enabled", havingValue = "true", matchIfMissing = true)
    public GrayscalePublishGlobalFilter grayscalePublishGlobalFilter(GatewayProperties gatewayProperties) {
        return new GrayscalePublishGlobalFilter(gatewayProperties);
    }

    @Bean
    public BodyGlobalFilter bodyGlobalFilter() {
        return new BodyGlobalFilter();
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public SignCheckGlobalFilter signCheckGlobalFilter(CryptoService cryptoService, GatewayProperties properties,
                                                       RedisTemplate<String, Object> redisTemplate) {
        return new SignCheckGlobalFilter(cryptoService, properties, redisTemplate);
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public CryptoGlobalFilter cryptoGlobalFilter(CryptoService cryptoService, GatewayProperties properties) {
        return new CryptoGlobalFilter(cryptoService, properties);
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public ProtectUrlsEndpointFilter protectUrlsEndpointFilter(CryptoService cryptoService,
                                                               GatewayProperties properties, @Nullable CorsConfigurationSource corsConfigurationSource) {
        return new ProtectUrlsEndpointFilter(cryptoService, properties, corsConfigurationSource);
    }

    @ConditionalOnBean(JWKSource.class)
    @Bean
    public NimbusJwkSetEndpointFilter nimbusJwkSetEndpointFilter(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwkSetEndpointFilter(jwkSource);
    }

    @Bean
    @ConditionalOnProperty(value = "macula.gateway.rm-opaque-token.enabled", havingValue = "true",
            matchIfMissing = true)
    public RmOpaqueTokenEndpointFilter rmOpaqueTokenEndpointFilter(GatewayProperties gatewayProperties,
                                                                   RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, Object> sysRedisTemplate) {
        return new RmOpaqueTokenEndpointFilter(gatewayProperties, redisTemplate, sysRedisTemplate);
    }

    @Bean
    public TraceIdGlobalFilter traceIdGlobalFilter() {
        return new TraceIdGlobalFilter();
    }
}
