/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

package dev.macula.boot.starter.dubbo.test;

import dev.macula.boot.starter.dubbo.rpc.filter.ConsumerAttachmentFilter;
import dev.macula.boot.starter.dubbo.rpc.filter.ProviderAttachmentFilter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * <b>DubboAttachmentFilterTest</b> Dubbo 附件过滤器测试
 * </p>
 * <p>
 * 测试消费者和提供者之间传递用户名上下文功能
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class DubboAttachmentFilterTest {

    private ConsumerAttachmentFilter consumerFilter;
    private ProviderAttachmentFilter providerFilter;
    private Invoker<Object> invoker;
    private Invocation invocation;

    @BeforeEach
    void setUp() {
        consumerFilter = new ConsumerAttachmentFilter();
        providerFilter = new ProviderAttachmentFilter();
        invoker = new MockInvoker<>();
        invocation = new MockInvocation();
        // 清空SecurityContext
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RpcContext.removeClientAttachment();
        RpcContext.removeServerAttachment();
    }

    /**
     * 测试消费者过滤器设置用户名附件
     */
    @Test
    void testConsumerFilterWithAuthenticatedUser() {
        // given: 设置认证信息
        String expectedUsername = "test-user";
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(expectedUsername, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when: 执行消费者过滤器
        Result result = consumerFilter.invoke(invoker, invocation);

        // then: 附件中应该包含用户名
        String username = RpcContext.getClientAttachment().getAttachment(ConsumerAttachmentFilter.USER_NAME);
        Assertions.assertEquals(expectedUsername, username, "Consumer should attach username to RPC context");
    }

    /**
     * 测试消费者过滤器在没有认证信息时使用默认用户名
     */
    @Test
    void testConsumerFilterWithoutAuthentication() {
        // given: 没有设置认证信息
        // SecurityContextHolder is empty

        // when: 执行消费者过滤器
        Result result = consumerFilter.invoke(invoker, invocation);

        // then: 使用默认背景用户名
        String username = RpcContext.getClientAttachment().getAttachment(ConsumerAttachmentFilter.USER_NAME);
        Assertions.assertNotNull(username, "Default username should be set");
    }

    /**
     * 测试提供者过滤器从附件获取用户名并设置到SecurityContext
     */
    @Test
    void testProviderFilterSetsSecurityContext() {
        // given: 在服务端附件设置用户名
        String expectedUsername = "provider-user";
        RpcContext.getServerAttachment().setAttachment(ConsumerAttachmentFilter.USER_NAME, expectedUsername);

        // when: 执行提供者过滤器
        Result result = providerFilter.invoke(invoker, invocation);

        // then: SecurityContext应该设置正确的认证
        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication should be set in SecurityContext");
        Assertions.assertEquals(expectedUsername,
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Username should match the one from attachment");
    }

    /**
     * 测试提供者过滤器在没有用户名附件时不修改SecurityContext
     */
    @Test
    void testProviderFilterWithoutUserNameAttachment() {
        // given: 没有设置用户名附件
        // SecurityContext is empty

        // when: 执行提供者过滤器
        Result result = providerFilter.invoke(invoker, invocation);

        // then: SecurityContext仍然为空认证
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication should remain null when no username attachment");
    }

    /**
     * 测试Filter的Activate注解配置正确
     */
    @Test
    void testFilterActivateAnnotations() {
        // 检查注解，通过反射验证
        Activate consumerActivate = consumerFilter.getClass().getAnnotation(Activate.class);
        Assertions.assertNotNull(consumerActivate);
        Assertions.assertArrayEquals(new String[]{"consumer"}, consumerActivate.group());

        Activate providerActivate = providerFilter.getClass().getAnnotation(Activate.class);
        Assertions.assertNotNull(providerActivate);
        Assertions.assertArrayEquals(new String[]{"provider"}, providerActivate.group());
    }

    /**
     * 模拟Invoker
     */
    private static class MockInvoker<T> implements Invoker<T> {
        @Override
        public URL getUrl() {
            return URL.valueOf("dubbo://127.0.0.1:12345/service");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {

        }

        @Override
        public Class<T> getInterface() {
            return (Class<T>) Object.class;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return new Result() {
                @Override
                public Object getValue() {
                    return "mock-result";
                }

                @Override
                public void setValue(Object value) {

                }

                @Override
                public Throwable getException() {
                    return null;
                }

                @Override
                public void setException(Throwable t) {

                }

                @Override
                public boolean hasException() {
                    return false;
                }

                @Override
                public Object recreate() throws Throwable {
                    return null;
                }

                @Override
                public Map<String, String> getAttachments() {
                    return Map.of();
                }

                @Override
                public Map<String, Object> getObjectAttachments() {
                    return Map.of();
                }

                @Override
                public void addAttachments(Map<String, String> map) {

                }

                @Override
                public void addObjectAttachments(Map<String, Object> map) {

                }

                @Override
                public void setAttachments(Map<String, String> map) {

                }

                @Override
                public void setObjectAttachments(Map<String, Object> map) {

                }

                @Override
                public String getAttachment(String key) {
                    return "";
                }

                @Override
                public Object getObjectAttachment(String key) {
                    return null;
                }

                @Override
                public String getAttachment(String key, String defaultValue) {
                    return "";
                }

                @Override
                public Object getObjectAttachment(String key, Object defaultValue) {
                    return null;
                }

                @Override
                public void setAttachment(String key, String value) {

                }

                @Override
                public void setAttachment(String key, Object value) {

                }

                @Override
                public void setObjectAttachment(String key, Object value) {

                }

                @Override
                public Result whenCompleteWithContext(BiConsumer<Result, Throwable> fn) {
                    return null;
                }

                @Override
                public <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fn) {
                    return null;
                }

                @Override
                public Result get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Result get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            };
        }
    }

    /**
     * 模拟Invocation
     */
    private static class MockInvocation implements Invocation {
        @Override
        public String getTargetServiceUniqueName() {
            return "";
        }

        @Override
        public String getProtocolServiceKey() {
            return "";
        }

        @Override
        public String getMethodName() {
            return "testMethod";
        }

        @Override
        public String getServiceName() {
            return "";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class[0];
        }

        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Override
        public Map<String, String> getAttachments() {
            return null;
        }

        @Override
        public Map<String, Object> getObjectAttachments() {
            return Map.of();
        }

        @Override
        public Map<String, Object> copyObjectAttachments() {
            return Map.of();
        }

        @Override
        public void foreachAttachment(Consumer<Map.Entry<String, Object>> consumer) {

        }

        @Override
        public void setAttachment(String key, String value) {

        }

        @Override
        public void setAttachment(String key, Object value) {
        }

        @Override
        public void setObjectAttachment(String key, Object value) {

        }

        @Override
        public void setAttachmentIfAbsent(String key, String value) {

        }

        @Override
        public void setAttachmentIfAbsent(String key, Object value) {

        }

        @Override
        public void setObjectAttachmentIfAbsent(String key, Object value) {

        }

        @Override
        public String getAttachment(String key) {
            return null;
        }

        @Override
        public Object getObjectAttachment(String key) {
            return null;
        }

        @Override
        public String getAttachment(String key, String defaultValue) {
            return "";
        }

        @Override
        public Object getObjectAttachment(String key, Object defaultValue) {
            return null;
        }

        @Override
        public Invoker<?> getInvoker() {
            return null;
        }

        @Override
        public void setServiceModel(ServiceModel serviceModel) {

        }

        @Override
        public ServiceModel getServiceModel() {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Map<Object, Object> getAttributes() {
            return Map.of();
        }

        @Override
        public void addInvokedInvoker(Invoker<?> invoker) {

        }

        @Override
        public List<Invoker<?>> getInvokedInvokers() {
            return List.of();
        }
    }
}
