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
}
