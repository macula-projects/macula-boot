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
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.starter.security.config.SecurityAutoConfiguration;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.starter.security.web.PasskeyArgumentResolver;
import dev.macula.boot.starter.security.web.PasskeyArgumentResolverPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Clock;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 验证 MVC 凭证基础设施的延迟装配，以及签发服务和配置的可替换行为。
 *
 * @author Rain
 * @since 5.0.21
 */
class SecurityAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
        .withUserConfiguration(SecurityInfrastructure.class);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void installsMvcInfrastructureWithoutCreatingDisabledSigningBeans() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PasskeyArgumentResolverPostProcessor.class)
                .doesNotHaveBean(PasskeyService.class).doesNotHaveBean(PasskeyIssuer.class).doesNotHaveBean(Clock.class);
            assertThat(context.getBeanFactory().getBeanDefinition("passkeyArgumentResolverPostProcessor").getRole())
                .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
            assertThat(context.getBean(RequestMappingHandlerAdapter.class).getArgumentResolvers().get(0))
                .isInstanceOf(PasskeyArgumentResolver.class);
            PasskeyArgumentResolverTest.authenticate("alice");
            assertThatThrownBy(() -> resolve(context.getBean(RequestMappingHandlerAdapter.class), "credential"))
                .isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void backsOffForApplicationProcessor() {
        PasskeyArgumentResolverPostProcessor custom =
            new PasskeyArgumentResolverPostProcessor(() -> null, () -> false);
        runner.withBean("customPasskeyProcessor", PasskeyArgumentResolverPostProcessor.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PasskeyArgumentResolverPostProcessor.class);
            assertThat(context.getBean(PasskeyArgumentResolverPostProcessor.class)).isSameAs(custom);
            assertThat(context).doesNotHaveBean("passkeyArgumentResolverPostProcessor");
        });
    }

    @Test
    void rejectsInvalidEnabledConfiguration() {
        runner.withPropertyValues("macula.security.passkey.enabled=true")
            .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("macula.security.passkey.enabled=true",
            "macula.security.passkey.secret-base64=" + Base64.getEncoder().encodeToString(new byte[32]),
            "macula.security.passkey.ttl=0s").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void explicitDisableSkipsInvalidSigningConfigurationAndRejectsProtectedArguments() {
        PasskeyArgumentResolverTest.authenticate("alice");
        runner.withPropertyValues("macula.security.passkey.enabled=false",
            "macula.security.passkey.secret-base64=invalid", "macula.security.passkey.ttl=0s").run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(PasskeyService.class)
                    .doesNotHaveBean(PasskeyIssuer.class).doesNotHaveBean(Clock.class);
                assertThatThrownBy(() -> resolve(context.getBean(RequestMappingHandlerAdapter.class), "credential"))
                    .isInstanceOf(AccessDeniedException.class);
            });
    }

    @Test
    void enabledServiceUsesApplicationClockAndVerifiesBoundArguments() {
        PasskeyArgumentResolverTest.authenticate("alice");
        runner.withPropertyValues("macula.security.passkey.enabled=true",
            "macula.security.passkey.secret-base64=" + Base64.getEncoder().encodeToString(new byte[32]))
            .withBean(Clock.class, () -> PasskeyServiceTest.CLOCK).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(Clock.class).hasSingleBean(PasskeyService.class);
                PasskeyService service = context.getBean(PasskeyService.class);
                assertThat(context.getBean(Clock.class)).isSameAs(PasskeyServiceTest.CLOCK);
                try {
                    assertThat(resolve(context.getBean(RequestMappingHandlerAdapter.class), service.issue("alice", "123")))
                        .isEqualTo("123");
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
                assertThatThrownBy(() -> resolve(context.getBean(RequestMappingHandlerAdapter.class),
                    service.issue("alice", "456"))).isInstanceOf(AccessDeniedException.class);
            });
    }

    @Test
    void backsOffForApplicationPasskeyService() {
        PasskeyService custom = new PasskeyService() {
            @Override
            public String issue(String subject, String resourceId) { return "custom"; }
            @Override
            public boolean verify(String subject, String resourceId, String token) { return "custom".equals(token); }
        };
        runner.withPropertyValues("macula.security.passkey.enabled=true")
            .withBean(PasskeyService.class, () -> custom).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(PasskeyService.class).hasSingleBean(PasskeyIssuer.class);
                assertThat(context.getBean(PasskeyIssuer.class)).isSameAs(custom);
            });
    }

    private Object resolve(RequestMappingHandlerAdapter adapter, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", token);
        MethodParameter parameter = new MethodParameter(
            PasskeyArgumentResolverTest.Endpoints.class.getMethod("scalar", Object.class), 0);
        return adapter.getArgumentResolvers().get(0).resolveArgument(parameter, new ModelAndViewContainer(),
            new ServletWebRequest(request), null);
    }

    /** Supplies servlet security and an initialized MVC adapter without external services. @since 5.0.21 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
    static class SecurityInfrastructure {
        @Bean
        static SpringUtil springUtil() { return new SpringUtil(); }
        @Bean
        RequestMappingHandlerMapping requestMappingHandlerMapping() { return new RequestMappingHandlerMapping(); }
        @Bean
        JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }
        @Bean
        RequestMappingHandlerAdapter handlerAdapter() {
            RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
            adapter.setArgumentResolvers(Collections.singletonList(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) { return true; }
                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                    NativeWebRequest request, WebDataBinderFactory binderFactory) { return "123"; }
            }));
            return adapter;
        }
    }

}
