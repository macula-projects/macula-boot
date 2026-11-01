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
import dev.macula.boot.starter.security.config.SecurityAutoConfiguration;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.security.PasskeyIssuer;
import java.time.Clock;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies ownership templates through the Starter's existing auto-configuration.
 *
 * @author Macula
 * @since 6.1.0
 */
class SecurityAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
        .withUserConfiguration(SecurityInfrastructure.class);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void configuresTemplatesAndEnforcesOwnershipThroughStarter() {
        CheckOwnerTest.authenticate("alice");
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AnnotationTemplateExpressionDefaults.class);
            CheckOwnerTest.OwnerQueries queries = context.getBean(CheckOwnerTest.OwnerQueries.class);
            assertThat(queries.order("alice").buyerUsername()).isEqualTo("alice");
            assertThatThrownBy(() -> queries.order("bob")).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void backsOffForUserProvidedTemplateDefaults() {
        CheckOwnerTest.authenticate("alice");
        runner.withUserConfiguration(CustomTemplateConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AnnotationTemplateExpressionDefaults.class);
            assertThat(context.getBean(AnnotationTemplateExpressionDefaults.class))
                .isSameAs(context.getBean("customTemplateDefaults"));
            assertThat(context).doesNotHaveBean("templateExpressionDefaults");
            assertThat(context.getBean(CheckOwnerTest.OwnerQueries.class).order("alice").buyerUsername())
                .isEqualTo("alice");
        });
    }

    @Test
    void defaultsToDisabledAndRejectsInvalidEnabledConfiguration() {
        CheckOwnerTest.authenticate("alice");
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(PasskeyService.class)
                .doesNotHaveBean(PasskeyIssuer.class).doesNotHaveBean(Clock.class);
            assertThatThrownBy(() -> context.getBean(CheckOwnerTest.OwnerQueries.class).input("123"))
                .isInstanceOf(AccessDeniedException.class);
        });
        runner.withPropertyValues("macula.security.passkey.enabled=true")
            .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("macula.security.passkey.enabled=true",
            "macula.security.passkey.secret-base64=" + Base64.getEncoder().encodeToString(new byte[32]),
            "macula.security.passkey.ttl=0s").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void explicitDisableSkipsSigningConfigurationAndKeepsOwnershipProtection() {
        CheckOwnerTest.authenticate("alice");
        runner.withPropertyValues("macula.security.passkey.enabled=false",
            "macula.security.passkey.secret-base64=invalid", "macula.security.passkey.ttl=0s").run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(PasskeyService.class)
                    .doesNotHaveBean(PasskeyIssuer.class).doesNotHaveBean(Clock.class);
                var queries = context.getBean(CheckOwnerTest.OwnerQueries.class);
                assertThat(queries.order("alice").buyerUsername()).isEqualTo("alice");
                assertThatThrownBy(() -> queries.order("bob")).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> queries.input("123")).isInstanceOf(AccessDeniedException.class);
            });
    }

    @Test
    void enabledServiceUsesApplicationClockAndSupportsReplacement() {
        CheckOwnerTest.authenticate("alice");
        runner.withPropertyValues("macula.security.passkey.enabled=true",
            "macula.security.passkey.secret-base64=" + Base64.getEncoder().encodeToString(new byte[32]))
            .withBean(Clock.class, () -> PasskeyServiceTest.CLOCK).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(Clock.class);
                var service = context.getBean(PasskeyService.class);
                assertThat(service.verify("alice", "123", service.issue("alice", "123"))).isTrue();
                assertThat(context.getBean(Clock.class)).isSameAs(PasskeyServiceTest.CLOCK);
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                var queries = context.getBean(CheckOwnerTest.OwnerQueries.class);
                assertThat(queries.input("123")).isEqualTo("123");
                assertThatThrownBy(() -> queries.input("456")).isInstanceOf(AccessDeniedException.class);
            });
        PasskeyService custom = new PasskeyService() {
            @Override
            public String issue(String subject, String resourceId) { return "custom"; }
            @Override
            public boolean verify(String subject, String resourceId, String token) { return "custom".equals(token); }
        };
        runner.withBean(PasskeyService.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PasskeyService.class).hasSingleBean(PasskeyIssuer.class);
            assertThat(context.getBean(PasskeyIssuer.class)).isSameAs(custom);
        });
    }

    /**
     * Supplies servlet security dependencies without external authentication services.
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
    static class SecurityInfrastructure {
        @Bean
        static SpringUtil springUtil() {
            return new SpringUtil();
        }

        @Bean
        RequestMappingHandlerMapping requestMappingHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        CheckOwnerTest.OwnerQueries ownerQueries() {
            return new CheckOwnerTest.OwnerQueries(new CheckOwnerTest.QueryState());
        }
    }

    /**
     * Provides application-owned annotation template defaults.
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomTemplateConfiguration {
        @Bean
        static AnnotationTemplateExpressionDefaults customTemplateDefaults() {
            return new AnnotationTemplateExpressionDefaults();
        }
    }
}
