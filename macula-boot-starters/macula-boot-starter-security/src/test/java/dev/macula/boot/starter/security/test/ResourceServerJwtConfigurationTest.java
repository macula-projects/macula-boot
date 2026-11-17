/*
 * Copyright (c) 2026 Macula
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


package dev.macula.boot.starter.security.test;

import cn.hutool.extra.spring.SpringUtil;
import dev.macula.boot.starter.security.config.ResourceServerConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证资源服务器只在 HMAC 模式要求显式 Secret，其他验证来源不受影响。
 *
 * @author Rain
 * @since 6.1.0
 */
class ResourceServerJwtConfigurationTest {
    @TempDir
    Path directory;
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(Infrastructure.class, ResourceServerConfiguration.class);

    /** 缺失、空白和不足 32 字节的 Secret 必须启动失败。 */
    @Test
    void requiresExplicitStrongSecret() {
        for (String secret : new String[] {"", "  ", "short"}) {
            runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.secret=" + secret)
                .run(context -> assertThat(context).hasFailed());
        }
        for (String secret : new String[] {"test-secret-at-least-thirty-two-bytes-long", "中文中文中文中文中文中文"}) {
            runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.secret=" + secret)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class));
        }
    }

    /** JWK、issuer 和公钥路径可以在没有 HMAC Secret 时正常装配。 */
    @Test
    void acceptsIndependentVerificationSources() throws Exception {
        for (String property : new String[] {"jwk-set-uri=http://127.0.0.1:1/jwks", "issuer-uri=http://127.0.0.1:1"}) {
            runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt." + property)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class));
        }
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String encoded = Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded());
        Path publicKey = directory.resolve("public.pem");
        Files.writeString(publicKey, "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----");
        runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.public-key-location=" + publicKey.toUri())
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class));
    }

    /** 自定义验签器优先，即使其他来源或 Secret 无效也不触发默认装配。 */
    @Test
    void backsOffForCustomDecoder() {
        JwtDecoder custom = mock(JwtDecoder.class);
        runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.secret=short",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:1/jwks")
            .withBean("customDecoder", JwtDecoder.class, () -> custom).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class);
                assertThat(context.getBean(JwtDecoder.class)).isSameAs(custom);
            });
    }

    /** 提供 Servlet 安全基础设施，完全不连接外部身份源。
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
    static class Infrastructure {
        /** 注册 Spring 工具的当前上下文。 */
        @Bean
        static SpringUtil springUtil() {
            return new SpringUtil();
        }

        /** 提供空路由映射供 ignore-urls 收集。 */
        @Bean
        RequestMappingHandlerMapping requestMappingHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
    }
}
