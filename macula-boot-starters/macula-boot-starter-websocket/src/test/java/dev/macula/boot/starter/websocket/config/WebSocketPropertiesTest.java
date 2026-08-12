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

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * {@link WebSocketProperties} 配置绑定测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class WebSocketPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsWebSocketSettings() {
        contextRunner
            .withPropertyValues("macula.websocket.enabled=false", "macula.websocket.endpoint[0]=/events", "macula.websocket.broker-destination-prefixes[0]=/broadcast", "macula.websocket.heartbeat[0]=5000", "macula.websocket.heartbeat[1]=7000")
            .run(context -> {
                WebSocketProperties properties = context.getBean(WebSocketProperties.class);
                assertThat(properties.isEnabled()).isFalse();
                assertThat(properties.getEndpoint()).containsExactly("/events");
                assertThat(properties.getBrokerDestinationPrefixes()).containsExactly("/broadcast");
                assertThat(properties.getHeartbeat()).containsExactly(5_000L, 7_000L);
            });
    }

    /**
     * 测试配置，用于注册 WebSocket 配置属性。
     *
     * @since 2026/8/12
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(WebSocketProperties.class)
    static class PropertiesConfiguration {
    }
}
