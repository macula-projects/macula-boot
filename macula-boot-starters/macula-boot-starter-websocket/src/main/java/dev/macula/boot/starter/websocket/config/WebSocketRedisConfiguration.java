/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.websocket.config;

import dev.macula.boot.starter.websocket.stomp.RedisRelayBrokerChannelInterceptor;
import dev.macula.boot.starter.websocket.stomp.RedisRelaySubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/**
 * <p>
 * <b>WebSocketRedisConfiguration</b> WebSocket的REDIS配置，借助该配置将消息发送给REDIS，解决集群问题
 * </p>
 *
 * @author Rain
 * @since 2024/4/17
 */
@Configuration
public class WebSocketRedisConfiguration {

    public static final String WEBSOCKET_REDIS_TEMPLATE_NAME = "webSocketRedisTemplate";

    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic(RedisRelayBrokerChannelInterceptor.WEBSOCKET_REDIS_TOPIC_NAME);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory,
                                                              MessageListenerAdapter listenerAdapter,
                                                              ChannelTopic channelTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, channelTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisRelaySubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(RedisSerializer.java());
        return adapter;
    }

    @Bean
    RedisRelaySubscriber redisRelaySubscriber(SimpMessageSendingOperations simpMessageSendingOperations) {
        return new RedisRelaySubscriber(simpMessageSendingOperations);
    }

    @Bean
    @ConditionalOnMissingBean(name = WEBSOCKET_REDIS_TEMPLATE_NAME)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<String, Message<?>> webSocketRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Message<?>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.java());
        template.setHashValueSerializer(RedisSerializer.java());
        template.afterPropertiesSet();
        return template;
    }
}
