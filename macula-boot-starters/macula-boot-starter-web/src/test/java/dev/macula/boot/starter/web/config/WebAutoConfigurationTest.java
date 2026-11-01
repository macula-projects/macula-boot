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

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import dev.macula.boot.starter.web.filter.TenantFilter;
import dev.macula.boot.starter.web.json.MappingApiJacksonHttpMessageConverter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.security.ResourceAccessException;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link WebAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class WebAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, WebAutoConfiguration.class));

    @Test
    void createsDefaultWebInfrastructureInServletApplication() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(JacksonProperties.class);
            assertThat(context).hasSingleBean(MaculaWebMvcConfigurer.class);
            assertThat(context).hasSingleBean(ControllerExceptionAdvice.class);
            assertThat(context).hasSingleBean(ControllerResponseAdvice.class);
            assertThat(context).hasSingleBean(TenantFilter.class);
        });
    }

    @Test
    void replacesDefaultStringAndJsonConverters() {
        webContextRunner.run(context -> {
            HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
            context.getBean(MaculaWebMvcConfigurer.class).configureMessageConverters(builder);
            HttpMessageConverters converters = builder.build();

            assertThat(converters).filteredOn(StringHttpMessageConverter.class::isInstance)
                .singleElement().satisfies(converter -> assertThat(
                    ((StringHttpMessageConverter) converter).getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8));
            assertThat(converters).filteredOn(MappingApiJacksonHttpMessageConverter.class::isInstance).hasSize(1);
            assertThat(converters).noneMatch(JacksonJsonHttpMessageConverter.class::isInstance);
        });
    }

    @Test
    void acceptsNullForPrimitiveRequestFields() {
        webContextRunner.run(context -> {
            PrimitiveRequest request = context.getBean(ObjectMapper.class)
                .readValue("{\"count\":null,\"enabled\":null}", PrimitiveRequest.class);
            assertThat(request.count).isZero();
            assertThat(request.enabled).isFalse();
        });
    }

    @Test
    void writesJsonWithExplicitUtf8Charset() {
        webContextRunner.run(context -> {
            MappingApiJacksonHttpMessageConverter converter = new MappingApiJacksonHttpMessageConverter(
                context.getBean(ObjectMapper.class), context.getBean(JacksonProperties.class));
            MockHttpOutputMessage output = new MockHttpOutputMessage();
            converter.write(Map.of("message", "中文"), MediaType.APPLICATION_JSON, output);

            assertThat(output.getHeaders().getContentType().getCharset()).isEqualTo(StandardCharsets.UTF_8);
            assertThat(new String(output.getBodyAsBytes(), StandardCharsets.UTF_8)).contains("中文");
        });
    }

    @Test
    void prefixesIndentedSseJsonLinesWithData() {
        webContextRunner.withPropertyValues("spring.jackson.serialization.indent-output=true").run(context -> {
            MappingApiJacksonHttpMessageConverter converter = new MappingApiJacksonHttpMessageConverter(
                context.getBean(ObjectMapper.class), context.getBean(JacksonProperties.class));
            MockHttpOutputMessage output = new MockHttpOutputMessage();
            output.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
            converter.write(Map.of("message", "hello"), MediaType.APPLICATION_JSON, output);

            String body = output.getBodyAsString();
            assertThat(body).contains("\ndata:");
            assertThat(body.lines().skip(1)).allMatch(line -> line.startsWith("data:"));
            assertThat(context.getBean(ObjectMapper.class).readTree(body.replace("\ndata:", "\n"))
                .get("message").asText()).isEqualTo("hello");
        });
    }

    @Test
    void allowsResponseAndExceptionAdviceToBeDisabled() {
        webContextRunner.withPropertyValues("macula.web.exception-advice=false", "macula.web.response-advice=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ControllerExceptionAdvice.class);
                assertThat(context).doesNotHaveBean(ControllerResponseAdvice.class);
                assertThat(context).hasSingleBean(TenantFilter.class);
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
        webContextRunner.withBean(dev.macula.boot.security.PasskeyIssuer.class,
            () -> resource -> resource.setPasskey("test-credential")).run(context -> {
                assertThat(context).hasSingleBean(dev.macula.boot.security.PasskeyIssuer.class);
                assertThat(context).hasSingleBean(ControllerResponseAdvice.class);
            });
    }

    @Test
    void bindsIssuanceSwitchAndRejectsMissingIssuerOnlyWhenEnabled() {
        for (String setting : List.of("", "false", "true")) {
            PasskeyResource resource = mock(PasskeyResource.class);
            PasskeyIssuer issuer = mock(PasskeyIssuer.class);
            var runner = webContextRunner.withBean(PasskeyIssuer.class, () -> issuer);
            if (!setting.isEmpty()) {
                runner = runner.withPropertyValues("macula.security.passkey.enabled=" + setting);
            }
            runner.run(context -> {
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
        advice.beforeBodyWrite(List.of(resource), returnType, MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/items")),
            new ServletServerHttpResponse(new MockHttpServletResponse()));
    }

    private List<PasskeyResource> resourceResponse() {
        return List.of();
    }

    /**
     * 验证历史请求中基本类型字段的空值兼容性。
     *
     * @since 6.1.0
     */
    static class PrimitiveRequest {
        public int count;
        public boolean enabled;
    }
}
