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

package dev.macula.boot.starter.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.security.ResourceAccessException;
import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import dev.macula.boot.starter.web.filter.TenantFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies Web auto-configuration and Passkey issuer property binding on Spring Boot 2.7.
 *
 * @author Rain
 * @since 5.0.21
 */
class WebAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, WebAutoConfiguration.class));

    @Test
    void createsDefaultWebInfrastructureAndAllowsAdviceToBeDisabled() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObjectMapper.class).hasSingleBean(JacksonProperties.class)
                .hasSingleBean(MaculaWebMvcConfigurer.class).hasSingleBean(ControllerExceptionAdvice.class)
                .hasSingleBean(ControllerResponseAdvice.class).hasSingleBean(TenantFilter.class);
        });
        webContextRunner.withPropertyValues("macula.web.exception-advice=false", "macula.web.response-advice=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ControllerExceptionAdvice.class)
                    .doesNotHaveBean(ControllerResponseAdvice.class).hasSingleBean(TenantFilter.class);
            });
    }

    @Test
    void doesNotActivateInNonWebApplication() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations
            .of(JacksonAutoConfiguration.class, WebAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(WebAutoConfiguration.class));
    }

    @Test
    void acceptsOptionalResourceIssuerWithoutSecurityDependency() {
        webContextRunner.withBean(PasskeyIssuer.class,
            () -> resource -> resource.setPasskey("test-credential")).run(context -> {
                assertThat(context).hasSingleBean(PasskeyIssuer.class)
                    .hasSingleBean(ControllerResponseAdvice.class);
            });
    }

    @Test
    void bindsIssuanceSwitchAndRejectsMissingIssuerOnlyWhenEnabled() throws Exception {
        for (String setting : new String[] {"", "false", "true"}) {
            PasskeyResource resource = mock(PasskeyResource.class);
            PasskeyIssuer issuer = mock(PasskeyIssuer.class);
            WebApplicationContextRunner configured = webContextRunner.withBean(PasskeyIssuer.class, () -> issuer);
            if (!setting.isEmpty()) {
                configured = configured.withPropertyValues("macula.security.passkey.enabled=" + setting);
            }
            configured.run(context -> {
                processList(context.getBean(ControllerResponseAdvice.class), resource);
                if ("true".equals(setting)) {
                    verify(issuer).issue(resource);
                } else {
                    verifyNoInteractions(issuer, resource);
                }
            });
        }
        webContextRunner.withPropertyValues("macula.security.passkey.enabled=true").run(context ->
            assertThatThrownBy(() -> processList(context.getBean(ControllerResponseAdvice.class),
                mock(PasskeyResource.class))).isInstanceOf(ResourceAccessException.class));
    }

    private void processList(ControllerResponseAdvice advice, PasskeyResource resource) throws Exception {
        MethodParameter returnType = new MethodParameter(
            WebAutoConfigurationTest.class.getDeclaredMethod("resourceResponse"), -1);
        advice.beforeBodyWrite(Collections.singletonList(resource), returnType, MediaType.APPLICATION_JSON,
            MappingJackson2HttpMessageConverter.class,
            new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/items")),
            new ServletServerHttpResponse(new MockHttpServletResponse()));
    }

    private List<PasskeyResource> resourceResponse() {
        return Collections.emptyList();
    }
}
