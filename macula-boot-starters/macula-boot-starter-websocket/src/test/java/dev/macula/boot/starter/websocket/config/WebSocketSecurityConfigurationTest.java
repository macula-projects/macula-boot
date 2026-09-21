/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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
package dev.macula.boot.starter.websocket.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * {@link WebSocketSecurityConfiguration} Spring Security 7 消息授权测试。
 *
 * @author Rain
 * @since 2026/9/21
 */
class WebSocketSecurityConfigurationTest {

    @Test
    void appliesCustomRulesBeforeAuthenticatedFallback() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setPermitTest(false);
        MessageSecurityMetaSourceCustomizer customizer = messages -> messages
            .simpTypeMatchers(SimpMessageType.CONNECT).permitAll();
        WebSocketSecurityConfiguration configuration = new WebSocketSecurityConfiguration(properties,
            List.of(customizer));
        AuthorizationManager<Message<?>> manager = configuration.messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.builder());

        assertThat(manager.authorize(() -> null, message(SimpMessageType.CONNECT)).isGranted()).isTrue();
        assertThat(manager.authorize(() -> null, message(SimpMessageType.MESSAGE)).isGranted()).isFalse();
    }

    @Test
    void keepsCsrfChannelInterceptorDisabled() {
        WebSocketSecurityConfiguration configuration = new WebSocketSecurityConfiguration(new WebSocketProperties(),
            List.of());
        Message<byte[]> message = message(SimpMessageType.CONNECT);

        assertThat(configuration.csrfChannelInterceptor().preSend(message, null)).isSameAs(message);
    }

    private static Message<byte[]> message(SimpMessageType type) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
