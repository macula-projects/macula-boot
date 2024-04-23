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

import dev.macula.boot.starter.websocket.annotation.EnableWebSocketMessageBroker;
import dev.macula.boot.starter.websocket.stomp.RedisRelayBrokerChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * <p>
 * <b>WebSocketAutoConfiguration</b>配置基于STOMP的websocket
 * </p>
 *
 * @author wzh
 * @version 2018-08-12 18:38
 **/
@AutoConfiguration
@ConditionalOnProperty(prefix = "macula.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({WebSocketRedisConfiguration.class, WebSocketSecurityConfiguration.class})
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
@RequiredArgsConstructor
@Slf4j
public class WebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final RedisTemplate<String, Message<?>> webSocketRedisTemplate;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(properties.getEndpoint())
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

        registry.enableSimpleBroker(properties.getBrokerDestinationPrefixes())
                .setHeartbeatValue(webSocketProperties.getHeartbeat())
                .setTaskScheduler(taskScheduler);

        // 客户端发送/app开头的消息，@MessageMapping注解处理 {@see SimpAnnotationMethodMessageHandler}
        registry.setApplicationDestinationPrefixes(properties.getAppDestinationPrefixes());

        // 客户端订阅个人消息要以/user开头，{@see SimpleBrokerMessageHandler}
        registry.setUserDestinationPrefix(properties.getUserDestinationPrefix());
        registry.configureBrokerChannel().interceptors(new RedisRelayBrokerChannelInterceptor(webSocketRedisTemplate, properties));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Authentication user = SecurityContextHolder.getContext().getAuthentication();
                    if (!(user instanceof AnonymousAuthenticationToken)) {
                        accessor.setUser(user);
                    }
                }
                return message;
            }
        });
    }
}


