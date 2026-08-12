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
package dev.macula.boot.starter.cloud.alibaba.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;

/**
 * {@link NacosDelayRegistrationListener} 延迟注册行为测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class NacosDelayRegistrationListenerTest {

    @Test
    void resolvesActualWebServerPortBeforeRegistering() {
        NacosServiceRegistry registry = mock(NacosServiceRegistry.class);
        NacosRegistration registration = mock(NacosRegistration.class);
        WebServerApplicationContext context = mock(WebServerApplicationContext.class);
        WebServer webServer = mock(WebServer.class);
        when(registration.getPort()).thenReturn(-1);
        when(context.getWebServer()).thenReturn(webServer);
        when(webServer.getPort()).thenReturn(9080);

        new NacosDelayRegistrationListener(registry, registration, context)
            .onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registration).setPort(9080);
        verify(registry).register(registration);
    }

    @Test
    void preservesExplicitRegistrationPort() {
        NacosServiceRegistry registry = mock(NacosServiceRegistry.class);
        NacosRegistration registration = mock(NacosRegistration.class);
        WebServerApplicationContext context = mock(WebServerApplicationContext.class);
        when(registration.getPort()).thenReturn(8081);

        new NacosDelayRegistrationListener(registry, registration, context)
            .onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registry).register(registration);
    }
}
