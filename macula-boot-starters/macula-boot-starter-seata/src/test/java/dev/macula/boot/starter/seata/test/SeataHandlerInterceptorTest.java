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

package dev.macula.boot.starter.seata.test;

import dev.macula.boot.starter.seata.web.SeataHandlerInterceptor;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * <p>
 * <b>SeataHandlerInterceptorTest</b> Seata XID 传递拦截器测试
 * </p>
 * <p>
 * 测试从HTTP请求头获取XID并绑定到RootContext，请求完成后正确解绑
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class SeataHandlerInterceptorTest {

    private final SeataHandlerInterceptor interceptor = new SeataHandlerInterceptor();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RootContext.unbind(); // 确保清空
    }

    @AfterEach
    void tearDown() {
        RootContext.unbind();
    }

    /**
     * 测试请求头中有XID而RootContext为空时，应该绑定XID
     */
    @Test
    void testPreHandleBindsXidWhenPresent() {
        // given: 请求头包含XID
        String testXid = "test-seata-xid-12345";
        request.addHeader(RootContext.KEY_XID, testXid);

        // when: preHandle
        boolean result = interceptor.preHandle(request, response, null);

        // then: 继续处理，XID已绑定
        Assertions.assertTrue(result);
        Assertions.assertEquals(testXid, RootContext.getXID());
    }

    /**
     * 测试请求头中没有XID，RootContext也为空，不绑定
     */
    @Test
    void testPreHandleDoesNothingWhenNoXid() {
        // given: 请求头没有XID
        // when: preHandle
        boolean result = interceptor.preHandle(request, response, null);

        // then: 继续处理，XID仍然为空
        Assertions.assertTrue(result);
        Assertions.assertNull(RootContext.getXID());
    }

    /**
     * 测试RootContext已经有XID，不覆盖（因为已经绑定了）
     */
    @Test
    void testPreHandleKeepsExistingXidWhenAlreadyBound() {
        // given: RootContext已有XID
        String existingXid = "existing-xid";
        String requestXid = "request-xid";
        RootContext.bind(existingXid);
        request.addHeader(RootContext.KEY_XID, requestXid);

        // when: preHandle
        interceptor.preHandle(request, response, null);

        // then: 保持原有的XID不变
        Assertions.assertEquals(existingXid, RootContext.getXID());
    }

    /**
     * 测试afterCompletion正确解绑
     */
    @Test
    void testAfterCompletionUnbindsCorrectly() {
        // given: 请求中有XID，并且已经绑定
        String testXid = "test-xid-for-unbind";
        request.addHeader(RootContext.KEY_XID, testXid);
        interceptor.preHandle(request, response, null);
        Assertions.assertEquals(testXid, RootContext.getXID());

        // when: afterCompletion
        interceptor.afterCompletion(request, response, null, null);

        // then: XID被解绑，现在为空
        Assertions.assertNull(RootContext.getXID());
    }

    /**
     * 测试当请求没有XID时，afterCompletion不做任何事
     */
    @Test
    void testAfterCompletionDoesNothingWhenNoXidInRequest() {
        // given: RootContext没有XID，请求也没有
        // when
        interceptor.afterCompletion(request, response, null, null);

        // then
        Assertions.assertNull(RootContext.getXID());
    }
}
