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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * <p>
 * <b>WebSocketAutoConfiguration</b>配置基于STOMP的websocket
 * </p>
 *
 * @author wzh
 * @version 2018-08-12 18:38
 **/
@AutoConfiguration
@ConditionalOnProperty(prefix = "macula.websocket.stomp", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({WebSocketSecurityConfiguration.class, WebSocketRedisConfiguration.class})
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {

    private final RedisTemplate<String, Message<?>> webSocketRedisTemplate;

    public final static String USER_PREFIX = "/user";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 自定义调度器，用于控制心跳线程
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        // 线程池线程数，心跳连接开线程
        taskScheduler.setPoolSize(1);
        // 线程名前缀
        taskScheduler.setThreadNamePrefix("websocket-heartbeat-thread-");
        // 初始化
        taskScheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);

        // 客户端发送/app开头的消息，@MessageMapping注解处理 {@see SimpAnnotationMethodMessageHandler}
        registry.setApplicationDestinationPrefixes("/app");

        // 客户端订阅个人消息要以/user开头，{@see SimpleBrokerMessageHandler}
        registry.setUserDestinationPrefix(USER_PREFIX);
        registry.configureBrokerChannel().interceptors(new RedisRelayBrokerChannelInterceptor(webSocketRedisTemplate));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Object raw = message.getHeaders().get(SimpMessageHeaderAccessor.NATIVE_HEADERS);
                    if (raw instanceof Map) {
                        Object name = ((Map) raw).get("name");
                        if (name instanceof ArrayList) {
                            // 设置当前访问器的认证用户
                            accessor.setUser(new User(((ArrayList) name).get(0).toString()));
                        }
                    }
                }
                return message;
            }
        });
    }

    static class User implements Principal {
        private final String name;
        public User(String name) {
            this.name = name;
        }
        @Override
        public String getName() {
            return name;
        }
    }
}


