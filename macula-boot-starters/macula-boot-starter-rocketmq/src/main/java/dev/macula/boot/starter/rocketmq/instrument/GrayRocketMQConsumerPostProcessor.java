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

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        if (o instanceof DefaultMQPushConsumer)
            return createDefaultMQPushConsumerProxy(o);
        if (o instanceof DefaultMQPullConsumer)
            return createDefaultMQPullConsumerProxy(o);
        if (o instanceof DefaultLitePullConsumer)
            return createDefaultMQLitePullConsumerProxy(o);
        return o;
    }

    private Object createDefaultMQLitePullConsumerProxy(Object bean) {
        try {
            DefaultLitePullConsumer defaultMQPushConsumer = (DefaultLitePullConsumer)bean;
            Field field = defaultMQPushConsumer.getClass().getDeclaredField("defaultLitePullConsumerImpl");
            field.setAccessible(true);
            Object defaultLitePullConsumerImplObj = field.get(defaultMQPushConsumer);
            DefaultLitePullConsumerImpl defaultMQPushConsumerImpl =
                (DefaultLitePullConsumerImpl)defaultLitePullConsumerImplObj;
            reflectToAddFilterMessageHook((MQConsumerInner)defaultMQPushConsumerImpl);
            return bean;
        } catch (Exception e) {
            log.error("createDefaultMQLitePullConsumerProxy in lane failed", e);
            return bean;
        }
    }

    private Object createDefaultMQPushConsumerProxy(Object bean) {
        try {
            DefaultMQPushConsumer defaultMQPushConsumer = (DefaultMQPushConsumer)bean;
            DefaultMQPushConsumerImpl defaultMQPushConsumerImpl = defaultMQPushConsumer.getDefaultMQPushConsumerImpl();
            reflectToAddFilterMessageHook(defaultMQPushConsumerImpl);
            return bean;
        } catch (Exception e) {
            log.error("createDefaultMQPushConsumerProxy in lane failed", e);
            return bean;
        }
    }

    private Object createDefaultMQPullConsumerProxy(Object bean) {
        try {
            DefaultMQPullConsumer defaultMQPullConsumer = (DefaultMQPullConsumer)bean;
            DefaultMQPullConsumerImpl defaultMQPullConsumerImpl = defaultMQPullConsumer.getDefaultMQPullConsumerImpl();
            reflectToAddFilterMessageHook((MQConsumerInner)defaultMQPullConsumerImpl);
            return bean;
        } catch (Exception e) {
            log.error("createDefaultMQPullConsumerProxy in lane failed", e);
            return bean;
        }
    }

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
