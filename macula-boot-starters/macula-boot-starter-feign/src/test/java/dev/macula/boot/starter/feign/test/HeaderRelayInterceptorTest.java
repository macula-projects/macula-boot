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

package dev.macula.boot.starter.feign.test;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.starter.feign.interceptor.FeignHeaderRelayProperties;
import dev.macula.boot.starter.feign.interceptor.HeaderRelayInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * <b>HeaderRelayInterceptorTest</b> Feign 请求头传递拦截器测试
 * </p>
 * <p>
 * 测试Authorization token、trace id、灰度版本头等是否正确传递给下游服务
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class HeaderRelayInterceptorTest {

    private HeaderRelayInterceptor interceptor;
    private RequestTemplate requestTemplate;
    private FeignHeaderRelayProperties feignHeaderRelayProperties;

    @BeforeEach
    void setUp() {
        interceptor = new HeaderRelayInterceptor(feignHeaderRelayProperties);
        requestTemplate = new RequestTemplate();
        // 清空上下文
        RequestContextHolder.resetRequestAttributes();
        GrayVersionContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        GrayVersionContextHolder.clear();
    }

    /**
     * 测试有请求上下文时，正确传递Authorization和已有的trace id
     */
    @Test
    void testApplyWithRequestContext() {
        // given: 设置mock request with headers
        MockHttpServletRequest request = new MockHttpServletRequest();
        String testToken = "Bearer test-jwt-token";
        String testSid = "test-trace-id-12345";
        request.addHeader(SecurityConstants.AUTHORIZATION_KEY, testToken);
        request.addHeader(GlobalConstants.FEIGN_REQ_ID, testSid);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 验证头被正确添加
        Map<String, Collection<String>> headers = requestTemplate.headers();

        // trace id 应该和传入的一致
        Assertions.assertTrue(headers.containsKey(GlobalConstants.FEIGN_REQ_ID));
        Assertions.assertTrue(headers.get(GlobalConstants.FEIGN_REQ_ID).contains(testSid));

        // Authorization token 应该正确传递
        Assertions.assertTrue(headers.containsKey(SecurityConstants.AUTHORIZATION_KEY));
        Assertions.assertTrue(headers.get(SecurityConstants.AUTHORIZATION_KEY).contains(testToken));
    }

    /**
     * 测试没有trace id时，自动生成一个
     */
    @Test
    void testApplyGeneratesSidWhenMissing() {
        // given: 设置mock request without trace id
        MockHttpServletRequest request = new MockHttpServletRequest();
        String testToken = "Bearer test-jwt-token";
        request.addHeader(SecurityConstants.AUTHORIZATION_KEY, testToken);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 生成了sid
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertTrue(headers.containsKey(GlobalConstants.FEIGN_REQ_ID));
        Collection<String> sidValues = headers.get(GlobalConstants.FEIGN_REQ_ID);
        Assertions.assertEquals(1, sidValues.size());
        String sid = sidValues.iterator().next();
        Assertions.assertFalse(sidValues.iterator().next().isEmpty(), "SID should not be empty");
    }

    /**
     * 测试当Authorization已经存在，不覆盖
     */
    @Test
    void testDoesNotOverrideExistingAuthorization() {
        // given: 请求已经有Authorization头
        MockHttpServletRequest request = new MockHttpServletRequest();
        String existingToken = "Bearer existing-token";
        request.addHeader(SecurityConstants.AUTHORIZATION_KEY, "should-not-be-used");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        requestTemplate.header(SecurityConstants.AUTHORIZATION_KEY, existingToken);

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 保持原有的token不变
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertTrue(headers.containsKey(SecurityConstants.AUTHORIZATION_KEY));
        Assertions.assertTrue(headers.get(SecurityConstants.AUTHORIZATION_KEY).contains(existingToken));
        // 不应该添加第二个相同头
        Assertions.assertEquals(1, headers.get(SecurityConstants.AUTHORIZATION_KEY).size());
    }

    /**
     * 测试灰度版本头正确传递
     */
    @Test
    void testGrayVersionHeader() {
        // given: 设置灰度版本上下文
        String grayVersion = "v1";
        GrayVersionContextHolder.setGrayVersion(grayVersion);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 灰度版本头添加到请求
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertTrue(headers.containsKey(GlobalConstants.GRAY_VERSION_TAG));
        Assertions.assertTrue(headers.get(GlobalConstants.GRAY_VERSION_TAG).contains(grayVersion));
    }

    /**
     * 测试没有灰度版本时不添加头
     */
    @Test
    void testNoGrayVersionWhenNotSet() {
        // given: 没有设置灰度版本
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        // GrayVersionContextHolder is empty

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 不添加灰度版本头
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertFalse(headers.containsKey(GlobalConstants.GRAY_VERSION_TAG));
    }

    /**
     * 测试没有Web请求上下文时，仍然添加trace id
     */
    @Test
    void testApplyWithoutRequestContext() {
        // given: 没有请求上下文
        // RequestContextHolder is empty

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 仍然生成trace id
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertTrue(headers.containsKey(GlobalConstants.FEIGN_REQ_ID));
        Collection<String> sidValues = headers.get(GlobalConstants.FEIGN_REQ_ID);
        Assertions.assertEquals(1, sidValues.size());
        Assertions.assertFalse(sidValues.iterator().next().isEmpty());
    }
}
