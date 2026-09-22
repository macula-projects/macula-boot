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
package dev.macula.boot.starter.cloud.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ResourceServerConfiguration.CorsCondition} Gateway CORS 配置条件测试。
 *
 * @author Rain
 * @since 2026/9/21
 */
class CorsConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CorsConditionConfiguration.class);

    @Test
    void matchesBootFourGatewayCorsPrefix() {
        contextRunner
            .withPropertyValues("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-origins[0]=*")
            .run(context -> assertThat(context).hasBean("corsMarker"));
    }

    @Test
    void doesNotMatchLegacyGatewayCorsPrefix() {
        contextRunner
            .withPropertyValues("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins[0]=*")
            .run(context -> assertThat(context).doesNotHaveBean("corsMarker"));
    }

    /**
     * 仅注册受 CORS 条件控制的标记 Bean。
     *
     * @since 2026/9/21
     */
    @Configuration(proxyBeanMethods = false)
    static class CorsConditionConfiguration {

        @Bean
        @Conditional(ResourceServerConfiguration.CorsCondition.class)
        String corsMarker() {
            return "configured";
        }
    }
}
