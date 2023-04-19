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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import dev.macula.boot.starter.cloud.gateway.filter.CryptoUrlsEndpointFilter;
import dev.macula.boot.starter.cloud.gateway.filter.GlobalCacheRequestBodyFilter;
import dev.macula.boot.starter.cloud.gateway.filter.ProcessCryptoReqResFilter;
import dev.macula.boot.starter.cloud.gateway.security.ResourceServerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyStore;

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
    public GlobalCacheRequestBodyFilter globalCacheRequestBodyFilter() {
        return new GlobalCacheRequestBodyFilter();
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public ProcessCryptoReqResFilter processCryptoReqResFilter(CryptoService cryptoService,
        GatewayProperties properties) {
        return new ProcessCryptoReqResFilter(cryptoService, properties);
    }

    @Bean
    @ConditionalOnBean(CryptoService.class)
    public CryptoUrlsEndpointFilter createCryptoUrlsEndpointFilter(CryptoService cryptoService,
        GatewayProperties properties) {
        return new CryptoUrlsEndpointFilter(cryptoService, properties);
    }
}
