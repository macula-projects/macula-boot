/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyStore;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 网关内部 JWT 的可配置签名来源，保留旧默认材料与自定义 Bean 扩展。
 *
 * @author felord.cn
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class JwtConfiguration {

    private final GatewayProperties properties;

    /** 保留手工构造入口，沿用旧默认材料。 */
    public JwtConfiguration() {
        this(new GatewayProperties());
    }

    /**
     * 注入网关配置；默认 source 的创建仅由是否已有 JWKSource 决定。
     * @param properties 网关配置
     */
    @Autowired
    public JwtConfiguration(GatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * 加载可配置的 RSA 私钥；显式配置出错时拒绝启动，不回退默认材料。
     * @return JWK 签名来源
     */
    @Bean
    @ConditionalOnMissingBean(JWKSource.class)
    public JWKSource<SecurityContext> jwkSource() {
        GatewayProperties.Jwt jwt = properties.getJwt();
        String location = jwt.getKeyStore();
        if (location == null || !(location.startsWith("classpath:") || location.startsWith("file:"))) {
            throw new IllegalStateException("JWT KeyStore 仅支持 file 或 classpath 资源");
        }
        if (jwt.getKeyStorePassword() == null || jwt.getKeyAlias() == null || jwt.getKeyAlias().trim().isEmpty()) {
            throw new IllegalStateException("JWT KeyStore 配置不完整");
        }
        char[] storePassword = jwt.getKeyStorePassword().toCharArray();
        char[] keyPassword = jwt.getKeyPassword() == null ? storePassword.clone() : jwt.getKeyPassword().toCharArray();
        try (InputStream input = new DefaultResourceLoader().getResource(location).getInputStream()) {
            KeyStore store = KeyStore.getInstance(jwt.getKeyStoreType());
            store.load(input, storePassword);
            RSAKey key = RSAKey.load(store, jwt.getKeyAlias(), keyPassword);
            if (key == null || !key.isPrivate()) {
                throw new IllegalStateException("JWT KeyStore 必须包含 RSA 私钥");
            }
            if ("classpath:jwk/jose.jks".equals(location)) {
                log.warn("网关正在使用公开内置 JWT 签名材料；生产环境请配置独立的 macula.gateway.jwt 密钥");
            }
            JWKSet keys = new JWKSet(key);
            return (selector, context) -> selector.select(keys);
        } catch (Exception exception) {
            // 不附带底层异常：Provider 或资源异常可能携带密码、路径中的凭据等配置值。
            throw new IllegalStateException("JWT KeyStore 加载失败，请检查资源、类型、密码和 RSA 私钥别名");
        } finally {
            Arrays.fill(storePassword, '\0');
            Arrays.fill(keyPassword, '\0');
        }
    }

    /**
     * JWT编码器，这个是网关自己生成的JWT给下游的微服务，与IAM无关
     *
     * @param jwkSource 已加载的签名来源
     * @return 可覆盖的内部 JWT 编码器
     */
    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}
