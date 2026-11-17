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
package dev.macula.boot.starter.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * {@code GatewayPropertiesTest} 网关配置属性单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class GatewayPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues("macula.gateway.crypto-switch=false", "macula.gateway.sign-switch=true", "macula.gateway.force-crypto=true", "macula.gateway.trace-id-response-header-enabled=false", "macula.gateway.protect-urls.crypto[0]=/api/crypto/**", "macula.gateway.protect-urls.crypto[1]=/api/encrypt/**", "macula.gateway.protect-urls.sign[0]=/api/sign/**", "macula.gateway.gray.enabled=true");

    /** 默认值与已有部署及请求体限制约定一致。 */
    @Test
    void hasDocumentedDefaults() {
        GatewayProperties properties = new GatewayProperties();

        assertThat(properties.isCryptoSwitch()).isTrue();
        assertThat(properties.isSignSwitch()).isTrue();
        assertThat(properties.isForceCrypto()).isFalse();
        assertThat(properties.isForceSign()).isTrue();
        assertThat(properties.isTraceIdResponseHeaderEnabled()).isTrue();
        assertThat(properties.getProtectUrls().getCrypto()).isEmpty();
        assertThat(properties.getProtectUrls().getSign()).isEmpty();
        assertThat(properties.getGray().isEnabled()).isFalse();
        assertThat(properties.getRmOpaqueTokenEndpoint()).isEqualTo("/gateway/rm/opaqueToken");
    }

    /** 嵌套属性与路径清单保持可绑定。 */
    @Test
    void bindsNestedAndListProperties() {
        contextRunner.run(context -> {
            GatewayProperties properties = context.getBean(GatewayProperties.class);

            assertThat(properties.isCryptoSwitch()).isFalse();
            assertThat(properties.isSignSwitch()).isTrue();
            assertThat(properties.isForceCrypto()).isTrue();
            assertThat(properties.isTraceIdResponseHeaderEnabled()).isFalse();
            assertThat(properties.getProtectUrls().getCrypto()).containsExactly("/api/crypto/**", "/api/encrypt/**");
            assertThat(properties.getProtectUrls().getSign()).containsExactly("/api/sign/**");
            assertThat(properties.getGray().isEnabled()).isTrue();
        });
    }

    /** 密码属性不进入自动生成的配置字符串，大小边界在绑定时失败。 */
    @Test
    void protectsSecretsAndValidatesBodyLimit() {
        contextRunner.withPropertyValues("macula.gateway.jwt.key-store-password=private-store-secret",
            "macula.gateway.jwt.key-password=private-key-secret", "macula.gateway.max-cached-body-size=2MB").run(context -> {
                var properties = context.getBean(GatewayProperties.class);
                assertThat(properties.getMaxCachedBodySize().toBytes()).isEqualTo(2 * 1024 * 1024);
                assertThat(properties.toString()).doesNotContain("private-store-secret", "private-key-secret");
                assertThat(properties.getJwt().toString()).doesNotContain("private-store-secret", "private-key-secret");
            });
        for (String size : new String[] {"0B", "-1B", "3GB"}) {
            contextRunner.withPropertyValues("macula.gateway.max-cached-body-size=" + size)
                .run(context -> assertThat(context).hasFailed());
        }
        assertThat(new GatewayProperties().getMaxCachedBodySize().toBytes()).isEqualTo(10 * 1024 * 1024);
    }

    /**
     * 测试配置，用于注册网关配置属性。
     *
     * @since 2026/8/12
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewayProperties.class)
    static class PropertiesConfiguration {
    }
}
