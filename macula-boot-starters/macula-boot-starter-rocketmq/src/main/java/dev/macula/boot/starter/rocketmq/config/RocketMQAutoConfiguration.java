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
import dev.macula.boot.starter.rocketmq.instrument.GrayDefaultRocketMQListenerContainerPostProcessor;
import dev.macula.boot.starter.rocketmq.instrument.GrayFilterMessageHookImpl;
import dev.macula.boot.starter.rocketmq.instrument.GrayRocketMQConsumerPostProcessor;
import dev.macula.boot.starter.rocketmq.instrument.GrayRocketMQProducerAspect;
import org.apache.rocketmq.client.consumer.MQConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * {@code RocketMQAutoConfiguration} RocketMQ自动配置
 *
 * @author rain
 * @since 2022/11/30 14:02
 */
@AutoConfiguration(after = org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class)
@EnableConfigurationProperties(GrayRocketMQProperties.class)
public class RocketMQAutoConfiguration {

    @Bean
    @Primary
    public RocketMQMessageConverter rocketMQMessageConverter() {
        ByteArrayMessageConverter byteArrayConverter = new ByteArrayMessageConverter();
        byteArrayConverter.setContentTypeResolver(null);
        // 使用 Jackson 3，同时保留历史消息的日期格式和反序列化默认行为。
        JsonMapper mapper = JsonMapper.builder().configureForJackson2()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        CompositeMessageConverter messageConverter = new CompositeMessageConverter(List.of(
            byteArrayConverter, new StringMessageConverter(), new JacksonJsonMessageConverter(mapper)));
        return new RocketMQMessageConverter() {
            @Override
            public MessageConverter getMessageConverter() {
                return messageConverter;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DefaultMQProducer.class)
    public RocketMQLocalTransactionListener rocketMQTransactionListener() {
        return new DefaultRocketMQLocalTransactionListener();
    }

    /**
     * 灰度 RocketMQ 消费端增强配置。
     *
     * @since 5.0.0
     */
    @Configuration
    @ConditionalOnProperty(value = {"macula.rocketmq.gray.enabled"})
    @ConditionalOnClass({MQConsumer.class})
    static class RocketConsumerConfiguration {
        @Bean
        public GrayFilterMessageHookImpl grayFilterMessageHookImpl(GrayRocketMQProperties grayRocketMQProperties,
                                                                   Environment environment, DiscoveryClient discoveryClient) {

            return new GrayFilterMessageHookImpl(grayRocketMQProperties, environment, discoveryClient);
        }

        @Bean
        public BeanPostProcessor grayRocketMQConsumerPostProcessor(GrayFilterMessageHookImpl grayFilterMessageHookImpl) {
            return new GrayRocketMQConsumerPostProcessor(grayFilterMessageHookImpl);
        }

        @Bean
        public GrayDefaultRocketMQListenerContainerPostProcessor grayDefaultRocketMQListenerContainerPostProcessor(
                RocketMQProperties rocketMQProperties, GrayFilterMessageHookImpl grayFilterMessageHookImpl) {

            return new GrayDefaultRocketMQListenerContainerPostProcessor(rocketMQProperties, grayFilterMessageHookImpl);
        }
    }

    /**
     * 灰度 RocketMQ 生产端增强配置。
     *
     * @since 5.0.0
     */
    @Configuration
    @ConditionalOnProperty(value = {"macula.rocketmq.gray.enabled"}, matchIfMissing = false)
    @ConditionalOnClass({MQProducer.class})
    static class RocketMQProducerConfiguration {
        @Bean
        public GrayRocketMQProducerAspect grayRocketMQProducerAspect(GrayRocketMQProperties grayRocketMQProperties) {
            return new GrayRocketMQProducerAspect(grayRocketMQProperties);
        }
    }
}
