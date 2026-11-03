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

package dev.macula.boot.starter.security.web;

import dev.macula.boot.starter.security.annotation.Passkey;
import dev.macula.boot.starter.security.authorization.PasskeyAccessDeniedException;
import dev.macula.boot.starter.security.passkey.PasskeyRequestResolver;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 委托原有 MVC 解析器完成参数绑定，再校验请求凭证与该参数的资源编号。
 * 属性路径只读取 JavaBean getter，不执行 SpEL，也不允许访问反射元数据。
 *
 * @author Rain
 * @since 6.1.0
 */
public class PasskeyArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Pattern PROPERTY_PATH =
        Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*");

    private final HandlerMethodArgumentResolverComposite delegates = new HandlerMethodArgumentResolverComposite();
    private final Supplier<PasskeyService> services;
    private final BooleanSupplier enabled;
    private final PasskeyRequestResolver credentials = new PasskeyRequestResolver();

    /**
     * 保留原解析器的顺序，内部委托链不包含本装饰器，避免递归解析。
     * @param resolvers MVC 原有的有序参数解析器
     * @param services 延迟获取凭证校验服务
     * @param enabled 延迟读取功能开关
     */
    public PasskeyArgumentResolver(List<HandlerMethodArgumentResolver> resolvers,
        Supplier<PasskeyService> services, BooleanSupplier enabled) {
        if (resolvers == null || resolvers.isEmpty()) {
            throw new IllegalArgumentException("Passkey requires MVC argument resolvers");
        }
        for (HandlerMethodArgumentResolver resolver : resolvers) {
            if (resolver instanceof PasskeyArgumentResolver) {
                throw new IllegalArgumentException("Passkey argument resolvers cannot delegate to themselves");
            }
        }
        delegates.addResolvers(resolvers);
        this.services = services;
        this.enabled = enabled;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Passkey.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
        NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        // 复用 Spring 的类型转换、JSON 反序列化和校验，不自行再次读取请求体。
        Object argument = delegates.resolveArgument(parameter, container, request, binderFactory);
        Passkey annotation = parameter.getParameterAnnotation(Passkey.class);
        if (annotation == null || !enabled.getAsBoolean()) {
            throw denied();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || new AuthenticationTrustResolverImpl().isAnonymous(authentication)
            || !StringUtils.hasText(authentication.getName())) {
            throw denied();
        }
        String resourceId = resourceId(argument, annotation.value());
        // 凭证仅来自 Header 或 URL Query，不能信任 DTO 自带的 passkey 字段。
        String credential = credentials.resolve(request.getNativeRequest(HttpServletRequest.class));
        PasskeyService service;
        try {
            service = services.get();
        } catch (NoSuchBeanDefinitionException ex) {
            // 缺少校验能力时明确拒绝，不能因 Bean 未装配而放行受保护请求。
            throw denied();
        }
        if (service == null || !service.verify(authentication.getName(), resourceId, credential)) {
            throw denied();
        }
        return argument;
    }

    private String resourceId(Object argument, String path) {
        Object value = argument;
        if (!path.isEmpty()) {
            if (!PROPERTY_PATH.matcher(path).matches()) {
                throw denied();
            }
            try {
                for (String property : path.split("\\.")) {
                    // 只沿普通 Bean 属性逐层读取；不开放 class 导航、容器索引或自动补建嵌套对象。
                    if ("class".equals(property) || value == null || value instanceof Class
                        || value instanceof Map || value instanceof Collection || value.getClass().isArray()) {
                        throw denied();
                    }
                    BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
                    if (wrapper.getPropertyDescriptor(property).getReadMethod() == null) {
                        throw denied();
                    }
                    value = wrapper.getPropertyValue(property);
                }
            } catch (RuntimeException ex) {
                // 缺属性、嵌套值为空或 getter 失败均拒绝，不向响应泄漏 DTO 结构和底层异常。
                throw denied();
            }
        }
        if (value instanceof String && StringUtils.hasText((String)value)) {
            return (String)value;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long || value instanceof BigInteger) {
            // 整数使用稳定的十进制表示，与签发时的资源编号保持一致。
            return value.toString();
        }
        throw denied();
    }

    private PasskeyAccessDeniedException denied() {
        return new PasskeyAccessDeniedException("Resource access denied");
    }
}
