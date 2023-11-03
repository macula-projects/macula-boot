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

package dev.macula.boot.starter.rocketmq.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.hook.FilterMessageHook;
import org.apache.rocketmq.client.impl.consumer.*;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code RocketMQConsumerPostProcessor} 消费端插桩
 *
 * @author rain
 * @since 2023/9/7 18:13
 */
@Slf4j
@RequiredArgsConstructor
public class GrayRocketMQConsumerPostProcessor implements BeanPostProcessor {

    private final GrayFilterMessageHookImpl grayFilterMessageHook;

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("deprecation")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultMQPushConsumer)
            return createDefaultMQPushConsumerProxy(bean);
        if (bean instanceof DefaultMQPullConsumer)
            return createDefaultMQPullConsumerProxy(bean);
        if (bean instanceof DefaultLitePullConsumer)
            return createDefaultMQLitePullConsumerProxy(bean);
        if (bean instanceof DefaultRocketMQListenerContainer)
            return createDefaultRocketMQListenerContainerProxy(bean);
        return bean;
    }

    private Object createDefaultRocketMQListenerContainerProxy(Object bean) {
        DefaultRocketMQListenerContainer defaultRocketMQListenerContainer = (DefaultRocketMQListenerContainer)bean;
        DefaultMQPushConsumer consumer = defaultRocketMQListenerContainer.getConsumer();
        createDefaultMQPushConsumerProxy(consumer);
        return bean;
    }

    private Object createDefaultMQLitePullConsumerProxy(Object bean) {
        try {
            DefaultLitePullConsumer defaultMQPullConsumer = (DefaultLitePullConsumer)bean;
            Field field = defaultMQPullConsumer.getClass().getDeclaredField("defaultLitePullConsumerImpl");
            field.setAccessible(true);
            Object defaultLitePullConsumerImplObj = field.get(defaultMQPullConsumer);
            DefaultLitePullConsumerImpl defaultMQPushConsumerImpl =
                (DefaultLitePullConsumerImpl)defaultLitePullConsumerImplObj;
            reflectToAddFilterMessageHook(defaultMQPushConsumerImpl);
            return bean;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug(e.getMessage());
            return bean;
        }
    }

    private Object createDefaultMQPushConsumerProxy(Object bean) {
        try {
            DefaultMQPushConsumer defaultMQPushConsumer = (DefaultMQPushConsumer)bean;
            // defaultMQPushConsumer.getDefaultMQPushConsumerImpl();
            Field field = defaultMQPushConsumer.getClass().getDeclaredField("defaultMQPushConsumerImpl");
            field.setAccessible(true);
            Object defaultMQPushConsumerImplObj = field.get(defaultMQPushConsumer);
            DefaultMQPushConsumerImpl defaultMQPushConsumerImpl =
                (DefaultMQPushConsumerImpl)defaultMQPushConsumerImplObj;
            reflectToAddFilterMessageHook(defaultMQPushConsumerImpl);
            return bean;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug(e.getMessage());
            return bean;
        }
    }

    @SuppressWarnings("deprecation")
    private Object createDefaultMQPullConsumerProxy(Object bean) {
        try {
            DefaultMQPullConsumer defaultMQPullConsumer = (DefaultMQPullConsumer)bean;
            DefaultMQPullConsumerImpl defaultMQPullConsumerImpl = defaultMQPullConsumer.getDefaultMQPullConsumerImpl();
            reflectToAddFilterMessageHook(defaultMQPullConsumerImpl);
            return bean;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug(e.getMessage());
            return bean;
        }
    }

    @SuppressWarnings("unchecked")
    private void reflectToAddFilterMessageHook(MQConsumerInner consumerInner)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = consumerInner.getClass().getDeclaredField("filterMessageHookList");
        field.setAccessible(true);
        Object originalHookObj = field.get(consumerInner);
        List<FilterMessageHook> replacedHooks = new ArrayList<>();
        replacedHooks.add(this.grayFilterMessageHook);
        if (originalHookObj != null) {
            List<FilterMessageHook> originalHooks = (List<FilterMessageHook>)originalHookObj;
            replacedHooks.addAll(originalHooks);
        }
        field.set(consumerInner, replacedHooks);
        Field pullAPIWrapperField = consumerInner.getClass().getDeclaredField("pullAPIWrapper");
        pullAPIWrapperField.setAccessible(true);
        PullAPIWrapper originalPullAPIWrapper = (PullAPIWrapper)pullAPIWrapperField.get(consumerInner);
        Field filterMessageHookListInPullAPIWrapperField =
            originalPullAPIWrapper.getClass().getDeclaredField("filterMessageHookList");
        filterMessageHookListInPullAPIWrapperField.setAccessible(true);
        filterMessageHookListInPullAPIWrapperField.set(originalPullAPIWrapper, replacedHooks);
    }
}
