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

package dev.macula.boot.starter.task.compat.xxljob;

import com.aizuda.snailjob.client.job.core.Scanner;
import com.aizuda.snailjob.client.job.core.IJobExecutor;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.cache.JobExecutorInfoCache;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.job.core.dto.JobExecutorInfo;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovers XXL-compatible handlers and exposes them through SnailJob's scanner contract.
 *
 * @author Rain
 * @since 6.1.0
 */
public final class XxlJobSnailJobScanner implements Scanner, DisposableBean {

    private static final Method BRIDGE_METHOD = ReflectionUtils.findMethod(XxlJobSnailJobExecutor.class, "execute",
        JobArgs.class);

    private final ConfigurableApplicationContext applicationContext;
    private final List<XxlJobSnailJobExecutor> initializedExecutors = new ArrayList<>();
    private List<JobExecutorInfo> executorInfos;

    public XxlJobSnailJobScanner(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public synchronized List<JobExecutorInfo> doScan() {
        if (executorInfos != null) {
            return executorInfos;
        }
        List<HandlerDefinition> definitions = discoverHandlers();
        validateDefinitions(definitions);

        List<JobExecutorInfo> discovered = new ArrayList<>(definitions.size());
        try {
            for (HandlerDefinition definition : definitions) {
                XxlJobSnailJobExecutor executor = definition.executor();
                executor.initialize();
                initializedExecutors.add(executor);
                discovered.add(new JobExecutorInfo(definition.name(), BRIDGE_METHOD, null, null, null, executor));
            }
        } catch (RuntimeException exception) {
            destroyInitializedExecutors();
            throw exception;
        }
        executorInfos = List.copyOf(discovered);
        return executorInfos;
    }

    @Override
    public synchronized void destroy() {
        destroyInitializedExecutors();
        executorInfos = Collections.emptyList();
    }

    private List<HandlerDefinition> discoverHandlers() {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        List<HandlerDefinition> definitions = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            if (beanFactory.containsBeanDefinition(beanName) && beanFactory.getBeanDefinition(beanName).isLazyInit()) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
            Map<Method, XxlJob> methods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<XxlJob>) method ->
                    AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class));
            if (methods.isEmpty()) {
                continue;
            }
            methods.forEach((method, annotation) -> definitions.add(createDefinition(bean, method, annotation)));
        }
        return definitions;
    }

    private HandlerDefinition createDefinition(Object bean, Method method, XxlJob annotation) {
        Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
        ReflectionUtils.makeAccessible(invocableMethod);
        Method initMethod = resolveLifecycleMethod(bean, annotation.init(), "init");
        Method destroyMethod = resolveLifecycleMethod(bean, annotation.destroy(), "destroy");
        return new HandlerDefinition(annotation.value(), invocableMethod,
            new XxlJobSnailJobExecutor(bean, invocableMethod, initMethod, destroyMethod));
    }

    private Method resolveLifecycleMethod(Object bean, String methodName, String lifecycle) {
        if (!StringUtils.hasText(methodName)) {
            return null;
        }
        Method targetMethod = ReflectionUtils.findMethod(AopUtils.getTargetClass(bean), methodName);
        if (targetMethod == null || targetMethod.getParameterCount() != 0) {
            throw new IllegalStateException("XXL-compatible " + lifecycle + " method must exist and have no arguments: "
                + methodName);
        }
        Method invocableMethod = AopUtils.selectInvocableMethod(targetMethod, bean.getClass());
        ReflectionUtils.makeAccessible(invocableMethod);
        return invocableMethod;
    }

    private void validateDefinitions(List<HandlerDefinition> definitions) {
        Set<String> nativeExecutorNames = discoverNativeExecutorNames();
        Set<String> names = new LinkedHashSet<>();
        for (HandlerDefinition definition : definitions) {
            if (!StringUtils.hasText(definition.name())) {
                throw new IllegalStateException("XXL-compatible executor name must not be blank");
            }
            if (!names.add(definition.name()) || nativeExecutorNames.contains(definition.name())
                || JobExecutorInfoCache.isExisted(definition.name())) {
                throw new IllegalStateException("Duplicate SnailJob executor name: " + definition.name());
            }
            for (Class<?> parameterType : definition.method().getParameterTypes()) {
                if (parameterType.isPrimitive()) {
                    throw new IllegalStateException("XXL-compatible handler parameters must not be primitive: "
                        + definition.method());
                }
            }
        }
    }

    private Set<String> discoverNativeExecutorNames() {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        Set<String> names = new LinkedHashSet<>();
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            if (beanFactory.containsBeanDefinition(beanName) && beanFactory.getBeanDefinition(beanName).isLazyInit()) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
            if (IJobExecutor.class.isAssignableFrom(targetClass)) {
                names.add(targetClass.getName());
            }
            JobExecutor classAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, JobExecutor.class);
            if (classAnnotation != null) {
                names.add(classAnnotation.name());
            }
            Map<Method, JobExecutor> methods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<JobExecutor>) method ->
                    AnnotatedElementUtils.findMergedAnnotation(method, JobExecutor.class));
            methods.values().forEach(annotation -> names.add(annotation.name()));
        }
        return names;
    }

    private void destroyInitializedExecutors() {
        RuntimeException failure = null;
        for (int index = initializedExecutors.size() - 1; index >= 0; index--) {
            try {
                initializedExecutors.get(index).destroy();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        initializedExecutors.clear();
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Describes one validated XXL-compatible handler before registration.
     *
     * @since 6.1.0
     */
    private record HandlerDefinition(String name, Method method, XxlJobSnailJobExecutor executor) {

        private HandlerDefinition {
        }
    }
}
