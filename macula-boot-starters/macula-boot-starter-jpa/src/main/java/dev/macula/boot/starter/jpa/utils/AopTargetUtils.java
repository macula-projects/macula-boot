/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
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

package dev.macula.boot.starter.jpa.utils;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * 获取被代理对象的原始目标对象
 * <p>
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 16/3/17.
 */
public class AopTargetUtils {

    /**
     * 获取目标对象
     *
     * @param proxy 被代理对象
     * @param <T>   目标类型
     * @return 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getTarget(Object proxy) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            return getJdkDynamicProxyTargetObject(proxy);
        } else if (ClassUtils.isCglibProxy(proxy)) {
            return getCglibProxyTargetObject(proxy);
        } else {
            return (T)proxy;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getCglibProxyTargetObject(Object proxy) {
        try {
            Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            h.setAccessible(true);
            Object dynamicAdvisedInterceptor = h.get(proxy);
            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            return (T)(((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getJdkDynamicProxyTargetObject(Object proxy) {
        try {
            Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
            h.setAccessible(true);
            Object proxy_ = h.get(proxy);
            Field f = proxy_.getClass().getDeclaredField("target");
            f.setAccessible(true);
            return (T)f.get(proxy_);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
