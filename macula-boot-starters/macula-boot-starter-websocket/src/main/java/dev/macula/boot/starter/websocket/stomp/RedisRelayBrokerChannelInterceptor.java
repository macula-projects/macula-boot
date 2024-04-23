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

package dev.macula.boot.starter.websocket.stomp;

import dev.macula.boot.starter.websocket.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * <b>RedisRelayBrokerChannelInterceptor</b> 将Broker Channel的消息中转给redis订阅
 * </p>
 *
 * @author Rain
 * @since 2024/4/18
 */
@RequiredArgsConstructor
public class RedisRelayBrokerChannelInterceptor implements ExecutorChannelInterceptor {

    public final static String WEBSOCKET_REDIS_TOPIC_NAME = "macula:websocket:topic";
    public final static String WEBSOCKET_REDIS_MESSAGE_HEADER_NAME = "RDS";

    private final RedisTemplate<String, Message<?>> redisTemplate;
    private final WebSocketProperties properties;

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        // 中转BrokerMessageHandler的MESSAGE消息到REDIS[BrokerMessageHandler是真正发送出去的消息]
        if (handler instanceof AbstractBrokerMessageHandler) {
            String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
            // 排除/user前缀的消息
            if (checkDestinationPrefix((AbstractBrokerMessageHandler) handler, destination)) {
                SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
                if (SimpMessageType.MESSAGE.equals(messageType)) {
                    // 给发给REDIS订阅的消息打上标识，防止重复发送消息到redis
                    if (!message.getHeaders().containsKey(WEBSOCKET_REDIS_MESSAGE_HEADER_NAME)) {
                        Message<?> redisMessage = MessageBuilder.fromMessage(message)
                                .setHeader(WEBSOCKET_REDIS_MESSAGE_HEADER_NAME, "0")
                                .build();
                        redisTemplate.convertAndSend(WEBSOCKET_REDIS_TOPIC_NAME, redisMessage);
                        // 中止本地发送订阅，让redis订阅去发送
                        return null;
                    }
                }
            }
        }
        return message;
    }

    protected boolean checkDestinationPrefix(AbstractBrokerMessageHandler handler, @Nullable String destination) {
        if (destination == null) {
            return true;
        }

        if (CollectionUtils.isEmpty(handler.getDestinationPrefixes())) {
            return !destination.startsWith(properties.getUserDestinationPrefix());
        }

        for (String prefix : handler.getDestinationPrefixes()) {
            if (destination.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
