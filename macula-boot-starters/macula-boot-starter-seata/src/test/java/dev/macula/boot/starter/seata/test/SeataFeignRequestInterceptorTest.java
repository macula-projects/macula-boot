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

import dev.macula.boot.starter.seata.feign.SeataFeignRequestInterceptor;
import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * <b>SeataFeignRequestInterceptorTest</b> Seata Feign 请求拦截器测试
 * </p>
 * <p>
 * 测试当RootContext中有XID时，是否正确添加到Feign请求头
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class SeataFeignRequestInterceptorTest {

    private final SeataFeignRequestInterceptor interceptor = new SeataFeignRequestInterceptor();
    private RequestTemplate requestTemplate;

    @BeforeEach
    void setUp() {
        requestTemplate = new RequestTemplate();
        RootContext.unbind();
    }

    @AfterEach
    void tearDown() {
        RootContext.unbind();
    }

    /**
     * 测试RootContext中有XID时，添加XID到请求头
     */
    @Test
    void testApplyAddsXidHeaderWhenPresent() {
        // given: RootContext绑定了XID
        String testXid = "feign-seata-xid-98765";
        RootContext.bind(testXid);

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 请求头包含KEY_XID，值正确
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertTrue(headers.containsKey(RootContext.KEY_XID));
        Collection<String> xidValues = headers.get(RootContext.KEY_XID);
        Assertions.assertEquals(1, xidValues.size());
        Assertions.assertTrue(xidValues.contains(testXid));
    }

    /**
     * 测试RootContext中没有XID时，不添加请求头
     */
    @Test
    void testApplyDoesNothingWhenNoXid() {
        // given: RootContext没有绑定XID
        // RootContext is empty

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 请求头不包含KEY_XID
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertFalse(headers.containsKey(RootContext.KEY_XID));
    }

    /**
     * 测试XID为空字符串时，不添加请求头
     */
    @Test
    void testApplyDoesNothingWhenEmptyXid() {
        // given: XID为空字符串
        RootContext.bind("");

        // when: 应用拦截器
        interceptor.apply(requestTemplate);

        // then: 请求头不包含KEY_XID
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Assertions.assertFalse(headers.containsKey(RootContext.KEY_XID));
    }
}
