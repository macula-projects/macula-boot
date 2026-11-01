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

import dev.macula.boot.starter.security.annotation.CheckOwner;
import dev.macula.boot.starter.security.authorization.CheckOwnerAdvisor;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies ownership expressions and rejection paths through real method-security proxies.
 *
 * @author Macula
 * @since 6.1.0
 */
class CheckOwnerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(MethodSecurityConfiguration.class);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void acceptsMatchingOwnerAndDifferentBusinessProperties() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThat(queries.order("alice").buyerUsername()).isEqualTo("alice");
            assertThat(queries.document("alice").createdBy()).isEqualTo("alice");
        });
    }

    @Test
    void rejectsOtherOwner() {
        authenticate("alice");
        runner.run(context -> assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("bob"))
            .isInstanceOf(AccessDeniedException.class));
    }

    @Test
    void rejectsNullResultAndNullOwner() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThatThrownBy(queries::missing).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> queries.order(null)).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void resolvesNestedOwnerAndRejectsMissingNestedData() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThat(queries.wrapped(new OrderView("alice")).data().buyerUsername()).isEqualTo("alice");
            assertThatThrownBy(() -> queries.wrapped(new OrderView("bob")))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> queries.wrapped(null)).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void rejectsMissingAuthenticationBeforeExecutingMethod() {
        SecurityContextHolder.clearContext();
        runner.run(context -> {
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("alice"))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
    }

    @Test
    void rejectsAnonymousBeforeExecutingMethod() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "test-key", "alice", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertDeniedBeforeInvocation();
    }

    @Test
    void rejectsUnauthenticatedBeforeExecutingMethod() {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.unauthenticated("alice", "unused"));
        assertDeniedBeforeInvocation();
    }

    @Test
    void doesNotGrantAdminOrRootAnOwnershipBypass() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "alice", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_ROOT"))));
        runner.run(context -> assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("bob"))
            .isInstanceOf(AccessDeniedException.class));
    }

    @Test
    void invalidPropertyDoesNotReturnData() {
        authenticate("alice");
        runner.run(context -> assertThatThrownBy(() -> context.getBean(OwnerQueries.class).invalid())
            .isInstanceOf(RuntimeException.class));
    }

    @Test
    void leavesUnannotatedMethodsUnchanged() {
        SecurityContextHolder.clearContext();
        runner.run(context -> assertThat(context.getBean(OwnerQueries.class).unprotected()).isEqualTo("public"));
    }

    @Test
    void inputExpressionsRequireValidCredentialBeforeExecution() {
        authenticate("alice");
        var service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        runner.withBean(PasskeyService.class, () -> service).run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThat(queries.input("123")).isEqualTo("123");
            assertThat(queries.named("123")).isEqualTo("123");
            assertThat(queries.nested(new Input("123"))).isEqualTo("123");
            int calls = context.getBean(QueryState.class).calls;
            assertThatThrownBy(() -> queries.input("456")).isInstanceOf(AccessDeniedException.class);
            request.removeHeader("X-Resource-Passkey");
            assertThatThrownBy(() -> queries.input("123")).isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isEqualTo(calls);
        });
    }

    @Test
    void rejectsMixedAndLiteralExpressionsBeforeExecution() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThatThrownBy(() -> queries.mixed("123")).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(queries::literal).isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
    }

    @Test
    void inputModeRejectsDisabledServiceAndMissingRequest() {
        authenticate("alice");
        runner.run(context -> assertThatThrownBy(() -> context.getBean(OwnerQueries.class).input("123"))
            .isInstanceOf(AccessDeniedException.class));
        var service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        runner.withBean(PasskeyService.class, () -> service).run(context ->
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).input("123"))
                .isInstanceOf(AccessDeniedException.class));
    }

    @Test
    void interfaceAndBridgeMethodRetainOwnerProtection() {
        authenticate("alice");
        runner.run(context -> {
            GenericCommands<?> commands = context.getBean(GenericCommands.class);
            @SuppressWarnings("unchecked")
            GenericCommands<String> strings = (GenericCommands<String>) commands;
            assertThat(strings.use("alice")).isEqualTo("alice");
            assertThatThrownBy(() -> strings.use("bob")).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void namedInterfaceParameterWorksAcrossJdkAndClassProxies() {
        authenticate("alice");
        var service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        for (boolean classProxy : List.of(false, true)) {
            QueryState state = new QueryState();
            ProxyFactory factory = new ProxyFactory(new NamedCommandsImpl(state));
            factory.setProxyTargetClass(classProxy);
            factory.addAdvisor(new CheckOwnerAdvisor(() -> service, () -> true));
            @SuppressWarnings("unchecked")
            NamedCommands<String> commands = (NamedCommands<String>) factory.getProxy();
            assertThat(AopUtils.isCglibProxy(commands)).isEqualTo(classProxy);
            assertThat(commands.detail("123")).isEqualTo("123");
            assertThatThrownBy(() -> commands.detail("456")).isInstanceOf(AccessDeniedException.class);
            assertThat(state.calls).isEqualTo(1);
        }
    }

    @Test
    void pmsAndPasskeyMustBothAllowTheOperation() {
        authenticate("alice");
        var service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        PermissionGate gate = new PermissionGate();
        runner.withBean(PasskeyService.class, () -> service).withBean("pms", PermissionGate.class, () -> gate)
            .run(context -> {
                OwnerQueries queries = context.getBean(OwnerQueries.class);
                assertThatThrownBy(() -> queries.cancel("123")).isInstanceOf(AccessDeniedException.class);
                assertThat(context.getBean(QueryState.class).calls).isZero();
                gate.allowed = true;
                queries.cancel("123");
                assertThat(context.getBean(QueryState.class).calls).isEqualTo(1);
                request.removeHeader("X-Resource-Passkey");
                assertThatThrownBy(() -> queries.cancel("123")).isInstanceOf(AccessDeniedException.class);
                assertThat(context.getBean(QueryState.class).calls).isEqualTo(1);
            });
    }

    private void assertDeniedBeforeInvocation() {
        runner.run(context -> {
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("alice"))
                .isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
    }

    static void authenticate(String name) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(name, "unused", List.of()));
    }

    /**
     * Supplies isolated method-security infrastructure.
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        static CheckOwnerAdvisor checkOwnerAdvisor(ObjectProvider<PasskeyService> services) {
            return new CheckOwnerAdvisor(services::getObject, () -> services.getIfAvailable() != null);
        }

        @Bean
        GenericCommands<String> genericCommands() {
            return new StringCommands();
        }

        @Bean
        static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
            return new AnnotationTemplateExpressionDefaults();
        }

        @Bean
        QueryState queryState() {
            return new QueryState();
        }

        @Bean
        OwnerQueries ownerQueries(QueryState state) {
            return new OwnerQueries(state);
        }
    }

    /**
     * Tracks method execution independently of the proxy.
     * @since 6.1.0
     */
    public static class QueryState {
        private int calls;
    }

    /**
     * Example queries using different ownership expressions.
     * @since 6.1.0
     */
    public static class OwnerQueries {
        private final QueryState state;

        public OwnerQueries(QueryState state) {
            this.state = state;
        }

        @CheckOwner("returnObject.buyerUsername")
        public OrderView order(String owner) {
            state.calls++;
            return new OrderView(owner);
        }

        @CheckOwner("returnObject.createdBy")
        public DocumentView document(String owner) {
            return new DocumentView(owner);
        }

        @CheckOwner("returnObject.data?.buyerUsername")
        public WrappedOrder wrapped(OrderView data) {
            return new WrappedOrder(data);
        }

        @CheckOwner("returnObject.buyerUsername")
        public OrderView missing() {
            return null;
        }

        @CheckOwner("returnObject.nonexistent")
        public OrderView invalid() {
            return new OrderView("alice");
        }

        @CheckOwner("#p0")
        public String input(String id) {
            state.calls++;
            return id;
        }

        @CheckOwner("#orderId")
        public String named(String orderId) {
            state.calls++;
            return orderId;
        }

        @CheckOwner("#a0.orderId")
        public String nested(Input input) {
            state.calls++;
            return input.orderId();
        }

        @CheckOwner("#p0 + returnObject")
        public String mixed(String id) {
            state.calls++;
            return id;
        }

        @CheckOwner("'returnObject.owner'")
        public String literal() {
            state.calls++;
            return "alice";
        }

        @CheckOwner("#p0")
        @PreAuthorize("@pms.hasPermission('order:cancel')")
        public void cancel(String id) {
            state.calls++;
        }

        public String unprotected() {
            return "public";
        }
    }

    /**
     * Order ownership projection.
     * @since 6.1.0
     */
    public record OrderView(String buyerUsername) {
    }

    /**
     * Document ownership projection.
     * @since 6.1.0
     */
    public record DocumentView(String createdBy) {
    }

    /**
     * Explicit application response wrapper.
     * @since 6.1.0
     */
    public record WrappedOrder(OrderView data) {
    }

    /**
     * Input DTO with a resource identifier.
     * @since 6.1.0
     */
    public record Input(String orderId) {
    }

    /**
     * Interface annotation exercises JDK proxies and bridged generic methods.
     * @param <T> result type
     * @since 6.1.0
     */
    public interface GenericCommands<T> {
        @CheckOwner("returnObject")
        T use(T input);
    }

    /**
     * Implements the generic query contract with a concrete return type.
     * @since 6.1.0
     */
    public static class StringCommands implements GenericCommands<String> {
        @Override
        public String use(String input) {
            return input;
        }
    }

    /**
     * Carries a named expression on a generic interface declaration.
     * @param <T> resource ID type
     * @since 6.1.0
     */
    public interface NamedCommands<T> {
        @CheckOwner("#orderId")
        T detail(T orderId);
    }

    /**
     * Renames the implementation parameter while retaining interface authorization metadata.
     * @since 6.1.0
     */
    public static class NamedCommandsImpl implements NamedCommands<String> {
        private final QueryState state;

        public NamedCommandsImpl(QueryState state) {
            this.state = state;
        }

        @Override
        public String detail(String id) {
            state.calls++;
            return id;
        }
    }

    /**
     * Minimal PMS contract for method-security composition tests.
     * @since 6.1.0
     */
    public static class PermissionGate {
        private boolean allowed;

        public boolean hasPermission(String permission) {
            return allowed;
        }
    }

}
