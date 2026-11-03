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
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies ownership expressions and rejection paths through Spring Security 5.7 method proxies.
 *
 * @author Rain
 * @since 5.0.21
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
    void acceptsMatchingOwnerAndRejectsOtherNullOrInvalidOwner() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThat(queries.order("alice").getBuyerUsername()).isEqualTo("alice");
            assertThat(queries.document("alice").getCreatedBy()).isEqualTo("alice");
            assertThatThrownBy(() -> queries.order("bob")).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> queries.order(null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(queries::missing).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(queries::invalid).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void resolvesNestedOwnerAndRejectsMissingNestedData() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThat(queries.wrapped(new OrderView("alice")).getData().getBuyerUsername()).isEqualTo("alice");
            assertThatThrownBy(() -> queries.wrapped(new OrderView("bob")))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> queries.wrapped(null)).isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    void rejectsMissingAnonymousAndUnauthenticatedBeforeExecutingMethod() {
        runner.run(context -> {
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("alice"))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "test-key", "alice", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertDeniedBeforeInvocation();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "unused"));
        assertDeniedBeforeInvocation();
    }

    @Test
    void adminAndRootDoNotBypassOwnerOrPasskeyChecks() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "alice", "unused", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_ROOT"))));
        HmacPasskeyService service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "different"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        runner.withBean(PasskeyService.class, () -> service).run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThatThrownBy(() -> queries.order("bob")).isInstanceOf(AccessDeniedException.class);
            int calls = context.getBean(QueryState.class).calls;
            assertThatThrownBy(() -> queries.input("123")).isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isEqualTo(calls);
        });
    }

    @Test
    void leavesUnannotatedMethodsUnchanged() {
        SecurityContextHolder.clearContext();
        runner.run(context -> assertThat(context.getBean(OwnerQueries.class).unprotected()).isEqualTo("public"));
    }

    @Test
    void inputExpressionsRequireValidCredentialBeforeExecution() {
        authenticate("alice");
        HmacPasskeyService service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
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
    void rejectsMixedLiteralDisabledAndMissingRequestBeforeExecution() {
        authenticate("alice");
        runner.run(context -> {
            OwnerQueries queries = context.getBean(OwnerQueries.class);
            assertThatThrownBy(() -> queries.mixed("123")).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(queries::literal).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> queries.input("123")).isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
        HmacPasskeyService service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        runner.withBean(PasskeyService.class, () -> service).run(context -> {
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).input("123"))
                .isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
    }

    @Test
    void interfaceBridgeAndNamedParametersWorkAcrossJdkAndClassProxies() {
        authenticate("alice");
        runner.run(context -> {
            @SuppressWarnings("unchecked")
            GenericCommands<String> commands = (GenericCommands<String>)context.getBean(GenericCommands.class);
            assertThat(commands.use("alice")).isEqualTo("alice");
            assertThatThrownBy(() -> commands.use("bob")).isInstanceOf(AccessDeniedException.class);
        });

        HmacPasskeyService service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        for (boolean classProxy : new boolean[] {false, true}) {
            QueryState state = new QueryState();
            ProxyFactory factory = new ProxyFactory(new NamedCommandsImpl(state));
            factory.setProxyTargetClass(classProxy);
            factory.addAdvisor(new CheckOwnerAdvisor(() -> service, () -> true));
            @SuppressWarnings("unchecked")
            NamedCommands<String> commands = (NamedCommands<String>)factory.getProxy();
            assertThat(AopUtils.isCglibProxy(commands)).isEqualTo(classProxy);
            assertThat(commands.detail("123")).isEqualTo("123");
            assertThatThrownBy(() -> commands.detail("456")).isInstanceOf(AccessDeniedException.class);
            assertThat(state.calls).isEqualTo(1);
        }
    }

    @Test
    void pmsAndPasskeyMustBothAllowTheOperation() {
        authenticate("alice");
        HmacPasskeyService service = new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        PermissionGate gate = new PermissionGate();
        runner.withBean(PasskeyService.class, () -> service).withBean("pms", PermissionGate.class, () -> gate)
            .run(context -> {
                OwnerQueries queries = context.getBean(OwnerQueries.class);
                assertMethodSecurityRunsBeforeCheckOwner(queries);
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

    private void assertMethodSecurityRunsBeforeCheckOwner(OwnerQueries queries) {
        Advisor[] advisors = ((Advised)queries).getAdvisors();
        int methodSecurity = -1;
        int checkOwner = -1;
        for (int i = 0; i < advisors.length; i++) {
            if (advisors[i].getAdvice() instanceof MethodSecurityInterceptor) {
                methodSecurity = i;
            }
            if (advisors[i] instanceof CheckOwnerAdvisor || advisors[i].getAdvice() instanceof CheckOwnerAdvisor) {
                checkOwner = i;
            }
        }
        assertThat(methodSecurity).isGreaterThanOrEqualTo(0);
        assertThat(checkOwner).isGreaterThan(methodSecurity);
    }

    private void assertDeniedBeforeInvocation() {
        runner.run(context -> {
            assertThatThrownBy(() -> context.getBean(OwnerQueries.class).order("alice"))
                .isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(QueryState.class).calls).isZero();
        });
    }

    static void authenticate(String name) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            name, "unused", Collections.emptyList()));
    }

    /**
     * Supplies isolated Spring Security 5.7 method-security infrastructure.
     * @since 5.0.21
     */
    @Configuration(proxyBeanMethods = false)
    @EnableGlobalMethodSecurity(prePostEnabled = true, order = Ordered.LOWEST_PRECEDENCE - 1)
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
        QueryState queryState() {
            return new QueryState();
        }

        @Bean
        OwnerQueries ownerQueries(QueryState state) {
            return new OwnerQueries(state);
        }
    }

    /** Tracks method execution independently of the proxy. @since 5.0.21 */
    public static class QueryState {
        private int calls;
    }

    /** Example queries using different ownership expressions. @since 5.0.21 */
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
            return input.getOrderId();
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

    /** Order ownership projection. @since 5.0.21 */
    public static class OrderView {
        private final String buyerUsername;

        public OrderView(String buyerUsername) {
            this.buyerUsername = buyerUsername;
        }

        public String getBuyerUsername() {
            return buyerUsername;
        }
    }

    /** Document ownership projection. @since 5.0.21 */
    public static class DocumentView {
        private final String createdBy;

        public DocumentView(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getCreatedBy() {
            return createdBy;
        }
    }

    /** Explicit application response wrapper. @since 5.0.21 */
    public static class WrappedOrder {
        private final OrderView data;

        public WrappedOrder(OrderView data) {
            this.data = data;
        }

        public OrderView getData() {
            return data;
        }
    }

    /** Input DTO with a resource identifier. @since 5.0.21 */
    public static class Input {
        private final String orderId;

        public Input(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderId() {
            return orderId;
        }
    }

    /** Interface annotation exercises JDK proxies and bridged generic methods. @since 5.0.21 */
    public interface GenericCommands<T> {
        @CheckOwner("returnObject")
        T use(T input);
    }

    /** Implements the generic query contract with a concrete return type. @since 5.0.21 */
    public static class StringCommands implements GenericCommands<String> {
        @Override
        public String use(String input) {
            return input;
        }
    }

    /** Carries a named expression on a generic interface declaration. @since 5.0.21 */
    public interface NamedCommands<T> {
        @CheckOwner("#orderId")
        T detail(T orderId);
    }

    /** Renames the implementation parameter while retaining interface metadata. @since 5.0.21 */
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

    /** Minimal PMS contract for method-security composition tests. @since 5.0.21 */
    public static class PermissionGate {
        private boolean allowed;

        public boolean hasPermission(String permission) {
            return allowed;
        }
    }
}
