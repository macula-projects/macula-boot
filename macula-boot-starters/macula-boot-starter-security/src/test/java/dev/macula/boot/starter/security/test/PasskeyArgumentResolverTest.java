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

import dev.macula.boot.starter.security.annotation.Passkey;
import dev.macula.boot.starter.security.authorization.PasskeyAccessDeniedException;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.starter.security.web.PasskeyArgumentResolver;
import dev.macula.boot.starter.security.web.PasskeyArgumentResolverPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证参数只绑定一次、资源属性路径受限，以及凭证校验失败时明确拒绝。
 *
 * @author Rain
 * @since 6.1.0
 */
class PasskeyArgumentResolverTest {

    private final AtomicReference<Object> bound = new AtomicReference<>();
    private final AtomicInteger bindingCalls = new AtomicInteger();
    private final HmacPasskeyService service =
        new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
    private MockHttpServletRequest request;
    private HandlerMethodArgumentResolver delegate;
    private PasskeyArgumentResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        authenticate("alice");
        request = new MockHttpServletRequest();
        request.addHeader("X-Resource-Passkey", service.issue("alice", "123"));
        delegate = mock(HandlerMethodArgumentResolver.class);
        when(delegate.supportsParameter(any())).thenReturn(true);
        when(delegate.resolveArgument(any(), any(), any(), isNull())).thenAnswer(invocation -> {
            bindingCalls.incrementAndGet();
            return bound.get();
        });
        resolver = new PasskeyArgumentResolver(Collections.singletonList(delegate), () -> service, () -> true);
    }

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesOnceAndReturnsOriginalScalarOrDto() throws Exception {
        assertThat(resolver.supportsParameter(parameter("scalar"))).isTrue();
        assertThat(resolver.supportsParameter(parameter("plain"))).isFalse();
        for (Object id : Arrays.asList("123", 123L, 123, (short)123, (byte)123, BigInteger.valueOf(123))) {
            bound.set(id);
            assertThat(resolve("scalar")).isSameAs(id);
        }
        Payload payload = new Payload("123");
        bound.set(payload);
        assertThat(resolve("property")).isSameAs(payload);
        Nested nested = new Nested(new Key("123"));
        bound.set(nested);
        assertThat(resolve("nested")).isSameAs(nested);
        assertThat(bindingCalls.get()).isEqualTo(8);
    }

    @Test
    void rejectsMissingOrInvalidResourceWithoutChangingTheBoundObject() {
        for (Object id : Arrays.asList(null, "", " ", 1.5, Collections.singletonList("123"),
            Collections.singletonMap("id", "123"), new String[] {"123"})) {
            bound.set(id);
            assertDenied("scalar");
        }
        for (Object dto : Arrays.asList(null, new Payload(null), new Payload(" "), new Payload(1.5),
            new Object(), Collections.singletonMap("orderId", "123"))) {
            bound.set(dto);
            assertDenied("property");
        }
        bound.set(new Nested(null));
        assertDenied("nested");
        bound.set(new Payload("123"));
        for (String method : Arrays.asList("classPath", "expression", "indexed", "call", "blank", "unknown")) {
            assertDenied(method);
        }
    }

    @Test
    void checksTrustedIdentityAndDoesNotGiveRolesAnImplicitBypass() {
        bound.set("123");
        SecurityContextHolder.clearContext();
        assertDenied("scalar");
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "test", "alice", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertDenied("scalar");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "unused"));
        assertDenied("scalar");
        authenticate("bob");
        assertDenied("scalar");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "alice", "unused", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_ROOT"))));
        bound.set("456");
        assertDenied("scalar");
    }

    @Test
    void rejectsDisabledMissingServiceOrMissingCredential() {
        bound.set("123");
        resolver = new PasskeyArgumentResolver(Collections.singletonList(delegate), () -> {
            throw new AssertionError("Disabled verifier must not be resolved");
        }, () -> false);
        assertDenied("scalar");
        resolver = new PasskeyArgumentResolver(Collections.singletonList(delegate), () -> null, () -> true);
        assertDenied("scalar");
        resolver = new PasskeyArgumentResolver(Collections.singletonList(delegate), () -> {
            throw new NoSuchBeanDefinitionException(PasskeyService.class);
        }, () -> true);
        assertDenied("scalar");
        resolver = new PasskeyArgumentResolver(Collections.singletonList(delegate), () -> service, () -> true);
        request.removeHeader("X-Resource-Passkey");
        assertDenied("scalar");
    }

    @Test
    void preservesBindingFailures() throws Exception {
        when(delegate.resolveArgument(any(), any(), any(), isNull()))
            .thenThrow(new IllegalArgumentException("binding failure"));
        assertThatThrownBy(() -> resolve("scalar"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("binding failure");
    }

    @Test
    void postProcessorPreservesOriginalResolversAndInstallsOnlyOnce() throws Exception {
        AtomicInteger lazyCalls = new AtomicInteger();
        PasskeyArgumentResolverPostProcessor processor = new PasskeyArgumentResolverPostProcessor(() -> {
            lazyCalls.incrementAndGet();
            return service;
        }, () -> true);
        Object unrelated = new Object();
        assertThat(processor.postProcessAfterInitialization(unrelated, "other")).isSameAs(unrelated);
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setArgumentResolvers(Collections.singletonList(delegate));
        assertThat(processor.postProcessAfterInitialization(adapter, "adapter")).isSameAs(adapter);
        assertThat(adapter.getArgumentResolvers()).hasSize(2);
        assertThat(adapter.getArgumentResolvers().get(0)).isInstanceOf(PasskeyArgumentResolver.class);
        assertThat(adapter.getArgumentResolvers().get(1)).isSameAs(delegate);
        processor.postProcessAfterInitialization(adapter, "adapter");
        assertThat(adapter.getArgumentResolvers()).hasSize(2);
        assertThat(lazyCalls.get()).isZero();
        bound.set("123");
        resolver = (PasskeyArgumentResolver)adapter.getArgumentResolvers().get(0);
        assertThat(resolve("scalar")).isEqualTo("123");
        assertThat(lazyCalls.get()).isEqualTo(1);
        assertThat(bindingCalls.get()).isEqualTo(1);
    }

    @Test
    void rejectsUninitializedOrRecursiveDelegation() {
        PasskeyArgumentResolverPostProcessor processor =
            new PasskeyArgumentResolverPostProcessor(() -> service, () -> true);
        assertThatThrownBy(() -> processor.postProcessAfterInitialization(new RequestMappingHandlerAdapter(), "empty"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PasskeyArgumentResolver(Collections.emptyList(), () -> service, () -> true))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasskeyArgumentResolver(Collections.singletonList(resolver), () -> service, () -> true))
            .isInstanceOf(IllegalArgumentException.class);
    }

    static void authenticate(String name) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(name, "unused", Collections.emptyList()));
    }

    private MethodParameter parameter(String name) throws NoSuchMethodException {
        return new MethodParameter(Endpoints.class.getMethod(name, Object.class), 0);
    }

    private Object resolve(String method) throws Exception {
        return resolver.resolveArgument(parameter(method), new ModelAndViewContainer(), new ServletWebRequest(request), null);
    }

    private void assertDenied(String method) {
        assertThatThrownBy(() -> resolve(method)).isInstanceOf(PasskeyAccessDeniedException.class);
    }

    /** Declares supported and intentionally invalid argument paths. @since 6.1.0 */
    public static class Endpoints {
        public void scalar(@Passkey Object id) { }
        public void plain(Object id) { }
        public void property(@Passkey("orderId") Object input) { }
        public void nested(@Passkey("order.id") Object input) { }
        public void classPath(@Passkey("class.name") Object input) { }
        public void expression(@Passkey("#p0.orderId") Object input) { }
        public void indexed(@Passkey("orderId[0]") Object input) { }
        public void call(@Passkey("orderId.toString()") Object input) { }
        public void blank(@Passkey(" ") Object input) { }
        public void unknown(@Passkey("missing") Object input) { }
    }

    /** Carries a resource property whose type can vary across rejection cases. @since 6.1.0 */
    public static class Payload {
        private final Object orderId;
        public Payload(Object orderId) { this.orderId = orderId; }
        public Object getOrderId() { return orderId; }
    }

    /** Carries an optional nested resource. @since 6.1.0 */
    public static class Nested {
        private final Key order;
        public Nested(Key order) { this.order = order; }
        public Key getOrder() { return order; }
    }

    /** Supplies the nested resource ID getter. @since 6.1.0 */
    public static class Key {
        private final String id;
        public Key(String id) { this.id = id; }
        public String getId() { return id; }
    }
}
