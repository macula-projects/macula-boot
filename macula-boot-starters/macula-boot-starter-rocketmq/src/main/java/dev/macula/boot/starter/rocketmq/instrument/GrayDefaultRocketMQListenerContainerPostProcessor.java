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

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionMetaHolder;
import dev.macula.boot.starter.rocketmq.DefaultRocketMQListenerContainerProxy;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * {@code GrayDefaultRocketMQListenerContainerPostProcessor} DefaultRocketMQListenerContainer灰度的统一处理
 *
 * @author rain
 * @since 2023/9/13 16:04
 */
@RequiredArgsConstructor
public class GrayDefaultRocketMQListenerContainerPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private final RocketMQProperties properties;

    private final GrayFilterMessageHookImpl grayFilterMessageHook;

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
            throws BeansException {
        if (bean instanceof DefaultRocketMQListenerContainer) {
            DefaultRocketMQListenerContainer container = (DefaultRocketMQListenerContainer) bean;

            // 灰度环境下需要隔离consumerGroup
            if (StrUtil.isNotEmpty(GrayVersionMetaHolder.getGrayVersion())) {
                container.setConsumerGroup(
                        container.getConsumerGroup() + CharPool.DASHED + GrayVersionMetaHolder.getGrayVersion());
            }

            // 配置默认命名空间
            if (StringUtils.hasText(properties.getConsumer().getNamespace())) {
                if (StrUtil.isEmpty(container.getNamespace())) {
                    container.setNamespace(properties.getConsumer().getNamespace());
                }
            }

            bean = new DefaultRocketMQListenerContainerProxy(container);
            ((DefaultRocketMQListenerContainerProxy) bean).setName(beanName);
            ((DefaultRocketMQListenerContainerProxy) bean).setApplicationContext(applicationContext);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {

        if (bean instanceof DefaultRocketMQListenerContainer) {
            // 配置消息过滤钩子
            DefaultRocketMQListenerContainer container = (DefaultRocketMQListenerContainer) bean;
            container.getConsumer().getDefaultMQPushConsumerImpl().registerFilterMessageHook(this.grayFilterMessageHook);
        }

        return bean;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 由于这个类要在bean初始化的时候获取灰度版本，而灰度版本是nacos元数据，系统默认是在注册的时候获取的，所以这里要提前处理
        if (StrUtil.isEmpty(GrayVersionMetaHolder.getGrayVersion())) {
            GrayVersionMetaHolder.setGrayVersion(applicationContext.getEnvironment()
                    .getProperty("spring.cloud.nacos.discovery.metadata." + GlobalConstants.GRAY_VERSION_TAG));
        }
    }
}
