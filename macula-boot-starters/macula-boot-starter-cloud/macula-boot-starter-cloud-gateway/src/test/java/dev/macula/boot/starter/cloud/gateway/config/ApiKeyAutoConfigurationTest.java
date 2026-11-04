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

import dev.macula.boot.starter.cloud.gateway.filter.ApiKeyGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * API Key GlobalFilter 默认、禁用拒绝和用户 Bean 覆盖装配测试。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class ApiKeyAutoConfigurationTest {
    private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
        .withUserConfiguration(GatewayAutoConfiguration.class)
        .withBean("sysRedisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class))
        .withBean(ServerHttpSecurity.class, ServerHttpSecurity::http)
        .withBean(OAuth2ResourceServerProperties.class, () -> {
            var properties = new OAuth2ResourceServerProperties();
            properties.getOpaquetoken().setIntrospectionUri("https://test.invalid/introspect");
            properties.getOpaquetoken().setClientId("test");
            properties.getOpaquetoken().setClientSecret("test");
            return properties;
        }).withPropertyValues("spring.application.name=sample-gateway");

    @Test
    void defaultIsGlobalFilterAndNotWebFilter() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ApiKeyGlobalFilter.class);
            assertThat(context.getBean(ApiKeyGlobalFilter.class)).isNotInstanceOf(WebFilter.class);
            var factory = context.getSourceApplicationContext().getBeanFactory();
            assertThat(factory.getBeanDefinition("apiKeyGlobalFilter").getFactoryBeanName())
                .isEqualTo(factory.getBeanDefinition("kongApiGlobalFilter").getFactoryBeanName());
        });
    }

    @Test
    void disabledFeatureRetainsARejectingGlobalFilter() {
        runner.withPropertyValues("macula.gateway.apikey.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ApiKeyGlobalFilter.class);
            assertThat(ReflectionTestUtils.getField(context.getBean(ApiKeyGlobalFilter.class), "enabled"))
                .isEqualTo(false);
        });
    }

    @Test
    void userFilterOverridesDefault() {
        var custom = new ApiKeyGlobalFilter(mock(RedisTemplate.class));
        runner.withBean("customApiKey", ApiKeyGlobalFilter.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ApiKeyGlobalFilter.class);
            assertThat(context.getBean(ApiKeyGlobalFilter.class)).isSameAs(custom);
        });
    }
}
