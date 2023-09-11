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

package dev.macula.boot.starter.rocketmq.config;

import dev.macula.boot.starter.rocketmq.DefaultRocketMQLocalTransactionListener;
import dev.macula.boot.starter.rocketmq.instrument.GrayFilterMessageHookImpl;
import dev.macula.boot.starter.rocketmq.instrument.GrayRocketMQConsumerPostProcessor;
import dev.macula.boot.starter.rocketmq.instrument.GrayRocketMQProducerAspect;
import org.apache.rocketmq.client.consumer.MQConsumer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code RocketMQAutoConfiguration} RocketMQ自动配置
 *
 * @author rain
 * @since 2022/11/30 14:02
 */
@AutoConfiguration
@EnableConfigurationProperties(GrayRocketMQProperties.class)
public class RocketMQAutoConfiguration {

    @Bean
    public RocketMQLocalTransactionListener rocketMQTransactionListener() {
        return new DefaultRocketMQLocalTransactionListener();
    }

    @Configuration
    @ConditionalOnProperty(value = {"macula.gray.rocketmq.gray-on"}, matchIfMissing = false)
    @ConditionalOnClass({MQConsumer.class})
    static class RocketConsumerConfiguration {
        @Bean
        public GrayFilterMessageHookImpl grayFilterMessageHookImpl(GrayRocketMQProperties grayRocketMQProperties) {
            return new GrayFilterMessageHookImpl(grayRocketMQProperties);
        }

        @Bean
        public BeanPostProcessor grayRocketMQConsumerPostProcessor(
            GrayFilterMessageHookImpl grayFilterMessageHookImpl) {
            return new GrayRocketMQConsumerPostProcessor(grayFilterMessageHookImpl);
        }
    }

    @Configuration
    @ConditionalOnProperty(value = {"macula.gray.rocketmq.gray-on"}, matchIfMissing = false)
    @ConditionalOnClass({MQProducer.class})
    static class RocketMQProducerConfiguration {
        @Bean
        public GrayRocketMQProducerAspect grayRocketMQProducerAspect(GrayRocketMQProperties grayRocketMQProperties) {
            return new GrayRocketMQProducerAspect(grayRocketMQProperties);
        }
    }

}
