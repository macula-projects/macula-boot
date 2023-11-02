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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

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
    public BodyGlobalFilter bodyGlobalFilter() {
        return new BodyGlobalFilter();
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public CryptoGlobalFilter cryptoGlobalFilter(CryptoService cryptoService, GatewayProperties properties) {
        return new CryptoGlobalFilter(cryptoService, properties);
    }

    @Bean
    public SignCheckGlobalFilter tamperProofGlobalFilter(CryptoService cryptoService, GatewayProperties properties,
        RedisTemplate<String, Object> redisTemplate) {
        return new SignCheckGlobalFilter(cryptoService, properties, redisTemplate);
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public ProtectUrlsEndpointFilter cryptoUrlsEndpointFilter(CryptoService cryptoService,
        GatewayProperties properties) {
        return new ProtectUrlsEndpointFilter(cryptoService, properties);
    }

    @ConditionalOnBean(JWKSource.class)
    @Bean
    public NimbusJwkSetEndpointFilter nimbusJwkSetEndpointFilter(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwkSetEndpointFilter(jwkSource);
    }

    @Bean
    public GrayscalePublishFilter grayscalePublishFilter(GatewayProperties gatewayProperties) {
        return new GrayscalePublishFilter(gatewayProperties);
    }

    @Bean
    public RmOpaqueTokenEndpointFilter rmOpaqueTokenEndpointFilter(GatewayProperties gatewayProperties,
        RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, Object> sysRedisTemplate){
        return new RmOpaqueTokenEndpointFilter(gatewayProperties, redisTemplate, sysRedisTemplate);
    }
}
