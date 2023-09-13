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

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * {@code RocketMQConsumerNSPostProcessor} 消息消费端的namespace统一注入
 *
 * @author rain
 * @since 2023/9/13 16:04
 */
@RequiredArgsConstructor
public class RocketMQConsumerNSPostProcessor implements BeanPostProcessor {

    private final RocketMQProperties properties;

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
        throws BeansException {
        // DefaultRocketMQListenerContainer是监听器实现类
        if (bean instanceof DefaultRocketMQListenerContainer) {
            DefaultRocketMQListenerContainer container = (DefaultRocketMQListenerContainer)bean;
            // 开启消息隔离情况下获取隔离配置，此处隔离topic，根据自己的需求隔离group或者tag
            if (StringUtils.hasText(properties.getConsumer().getNamespace())) {
                if (StrUtil.isEmpty(container.getNamespace())) {
                    container.setNamespace(properties.getConsumer().getNamespace());
                }
            }
            return container;
        }
        return bean;
    }
}
