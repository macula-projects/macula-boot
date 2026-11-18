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

import dev.macula.boot.result.ApiResultCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 使用实际资源服务器安全链验证 CORS 在认证前执行，无外部服务依赖。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class CorsSecurityWebFilterChainTest {
    private static final String ORIGIN = "https://frontend.example";

    /** 认证成功与失败均保留状态、正文及唯一的跨域头。 */
    @ParameterizedTest
    @ValueSource(strings = {"valid", "invalid", "missing"})
    void authenticationResponsesIncludeCorsHeaders(String token) {
        runWithSource("configured", (configuration, http, introspector, source) -> {
            when(introspector.introspect("valid")).thenReturn(Mono.just(new DefaultOAuth2AuthenticatedPrincipal(
                "user", Map.of("sub", "user"), List.of())));
            when(introspector.introspect("invalid")).thenReturn(Mono.error(new BadOpaqueTokenException("invalid")));
            var request = MockServerHttpRequest.get("https://gateway.example/orders").header(HttpHeaders.ORIGIN, ORIGIN);
            if (!token.equals("missing")) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            var exchange = MockServerWebExchange.from(request);
            WebFilterChain downstream = current -> {
                // 模拟后续 HandlerMapping 使用同一规则处理 CORS，检查不重复追加响应头。
                assertThat(new DefaultCorsProcessor().process(source.getCorsConfiguration(current), current)).isTrue();
                current.getResponse().setStatusCode(HttpStatus.OK);
                return current.getResponse().setComplete();
            };
            execute(configuration, http, exchange, downstream);
            assertThat(exchange.getResponse().getHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .containsExactly(ORIGIN);
            assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(token.equals("valid") ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
            if (token.equals("missing")) {
                assertThat(exchange.getResponse().getBodyAsString().block())
                    .contains(ApiResultCode.TOKEN_INVALID_OR_EXPIRED.getCode());
            } else if (token.equals("invalid")) {
                // 保留 Resource Server 默认无效 Token 入口的空正文与 Bearer challenge。
                assertThat(exchange.getResponse().getBodyAsString().block()).isEmpty();
                assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
                    .startsWith("Bearer");
            }
        });
    }

    /** 预检无需凭据，拒绝的来源、方法及请求头不能触发内省或业务。 */
    @ParameterizedTest
    @ValueSource(strings = {"allowed", "origin", "method", "header"})
    void preflightFinishesBeforeAuthentication(String scenario) {
        runWithSource("configured", (configuration, http, introspector, source) -> {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.options("https://gateway.example/orders")
                .header(HttpHeaders.ORIGIN, scenario.equals("origin") ? "https://untrusted.example" : ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, scenario.equals("method") ? "DELETE" : "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, scenario.equals("header") ? "X-Untrusted" : "Authorization"));
            WebFilterChain downstream = mock(WebFilterChain.class);
            execute(configuration, http, exchange, downstream);
            assertThat(exchange.getResponse().getStatusCode())
                .satisfies(status -> {
                    if (scenario.equals("allowed")) {
                        assertThat(status).isIn(null, HttpStatus.OK);
                    } else {
                        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
                    }
                });
            assertThat(exchange.getResponse().getBodyAsString().block()).isEmpty();
            verifyNoInteractions(introspector, downstream);
        });
    }

    /** 配置的实际请求也必须拒绝非法来源，不能进入业务。 */
    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example"})
    void rejectsActualCrossOriginRequest(String origin) {
        runWithSource("configured", (configuration, http, introspector, source) -> {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://gateway.example/orders")
                .header(HttpHeaders.ORIGIN, origin).header(HttpHeaders.AUTHORIZATION, "Bearer valid"));
            WebFilterChain downstream = mock(WebFilterChain.class);
            execute(configuration, http, exchange, downstream);
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(introspector, downstream);
        });
    }

    /** 无配置源或空映射的装配不失败，同源 Bearer 认证仍有效。 */
    @ParameterizedTest
    @ValueSource(strings = {"absent", "empty", "configured"})
    void sameOriginAuthenticationSurvivesMissingOrEmptyConfiguration(String mode) {
        runWithSource(mode, (configuration, http, introspector, source) -> {
            when(introspector.introspect("valid")).thenReturn(Mono.just(new DefaultOAuth2AuthenticatedPrincipal(
                "user", Map.of("sub", "user"), List.of())));
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://gateway.example/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid"));
            execute(configuration, http, exchange, current -> {
                current.getResponse().setStatusCode(HttpStatus.OK);
                return current.getResponse().setComplete();
            });
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
            verify(introspector).introspect("valid");
        });
    }

    /** 创建实际配置对象，内省器用测试替身替换以禁止网络访问。 */
    private void runWithSource(String mode, Scenario scenario) {
        var introspector = mock(ReactiveOpaqueTokenIntrospector.class);
        var configuration = spy(new ResourceServerConfiguration(new OAuth2ResourceServerProperties(), mock(RedisTemplate.class)));
        doReturn(introspector).when(configuration).opaqueTokenIntrospector();
        var properties = new GlobalCorsProperties();
        if (mode.equals("configured")) {
            var cors = new CorsConfiguration();
            cors.setAllowedOrigins(List.of(ORIGIN));
            cors.setAllowedMethods(List.of(HttpMethod.GET.name()));
            cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION));
            properties.getCorsConfigurations().put("/**", cors);
        }
        var runner = new ApplicationContextRunner();
        if (!mode.equals("absent")) {
            runner = runner.withBean("corsConfigurationSource", CorsConfigurationSource.class,
                () -> configuration.corsConfigurationSource(properties));
        }
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var http = ServerHttpSecurity.http();
            ReflectionTestUtils.invokeMethod(http, "setApplicationContext", context.getSourceApplicationContext());
            scenario.run(configuration, http, introspector, configuration.corsConfigurationSource(properties));
        });
    }

    /** 经实际安全链分派请求，并模拟 HTTP 适配器在过滤链结束后完成无正文响应。 */
    private void execute(ResourceServerConfiguration configuration, ServerHttpSecurity http,
            MockServerWebExchange exchange, WebFilterChain downstream) {
        var security = configuration.securityWebFilterChain(http, (authentication, context) -> authentication
            .<AuthorizationResult>map(value -> new AuthorizationDecision(value.isAuthenticated()))
            .defaultIfEmpty(new AuthorizationDecision(false)));
        // CorsWebFilter 可返回 Mono.empty()，由外层 HTTP 适配器完成响应；模拟 exchange 不会自动完成。
        new WebFilterChainProxy(security).filter(exchange, downstream)
            .then(Mono.defer(exchange.getResponse()::setComplete)).block();
    }

    /** 测试场景共享实际配置和模拟内省器。
     * @since 6.1.0
     */
    private interface Scenario {
        /** 在指定 CORS 装配模式下运行断言。 */
        void run(ResourceServerConfiguration configuration, ServerHttpSecurity http,
            ReactiveOpaqueTokenIntrospector introspector, CorsConfigurationSource source);
    }
}
