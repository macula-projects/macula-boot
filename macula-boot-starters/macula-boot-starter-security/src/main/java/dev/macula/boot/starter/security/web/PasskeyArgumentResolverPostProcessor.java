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

import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 在 MVC 适配器初始化后，将 Passkey 装饰器安装到原参数解析器列表之前，且只安装一次。
 * 依赖延迟到请求校验时获取，避免提前创建 MVC 或安全配置 Bean。
 *
 * @author Rain
 * @since 5.0.21
 */
public class PasskeyArgumentResolverPostProcessor implements BeanPostProcessor {

    private final Supplier<PasskeyService> services;
    private final BooleanSupplier enabled;

    /**
     * 创建基础设施处理器，不立即解析应用中的服务和配置。
     * @param services 延迟获取凭证校验服务
     * @param enabled 延迟读取功能开关
     */
    public PasskeyArgumentResolverPostProcessor(Supplier<PasskeyService> services, BooleanSupplier enabled) {
        this.services = services;
        this.enabled = enabled;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof RequestMappingHandlerAdapter)) {
            return bean;
        }
        RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter)bean;
        // 初始化完成后才能取得内置及应用自定义解析器，避免破坏 Spring 原有绑定行为。
        List<HandlerMethodArgumentResolver> originals = adapter.getArgumentResolvers();
        if (originals == null || originals.isEmpty()) {
            throw new IllegalStateException("Passkey requires initialized MVC argument resolvers");
        }
        for (int i = 0; i < originals.size(); i++) {
            if (originals.get(i) instanceof PasskeyArgumentResolver) {
                // 装饰器必须排在内置解析器之前；重复处理同一适配器时保持幂等。
                if (i != 0) {
                    throw new IllegalStateException("Passkey must precede MVC argument resolvers");
                }
                return bean;
            }
        }
        List<HandlerMethodArgumentResolver> decorated = new ArrayList<>(originals.size() + 1);
        // 对外列表优先校验带注解参数，装饰器内部只委托原列表，不包含自身。
        decorated.add(new PasskeyArgumentResolver(originals, services, enabled));
        decorated.addAll(originals);
        adapter.setArgumentResolvers(decorated);
        return bean;
    }
}
