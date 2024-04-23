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

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

import java.util.Collection;

/**
 * <p>
 * <b>WebSocketSecurityConfiguration</b> WebSocket基于Spring Security的安全配置
 * </p>
 *
 * @author Rain
 * @since 2024/4/17
 */
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfiguration extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final Collection<MessageSecurityMetaSourceCustomizer> customizers;

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {

        if (properties.isPermitTest()) {
            messages.nullDestMatcher().permitAll()
                    .simpDestMatchers("/app/test/**").permitAll()
                    .simpSubscribeDestMatchers("/user/queue/test/**", "/topic/test/**").permitAll();
        }

        customizers.forEach(customizer -> {
            customizer.customize(messages);
        });

        // 兜底，所有漏网之鱼都要登录认证通过
        messages.anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
