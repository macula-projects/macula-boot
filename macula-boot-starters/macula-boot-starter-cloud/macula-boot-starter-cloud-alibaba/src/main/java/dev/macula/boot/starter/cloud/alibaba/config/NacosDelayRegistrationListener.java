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

package dev.macula.boot.starter.cloud.alibaba.config;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * 延迟注册服务到 Nacos
 *
 * @author Gordian
 */
public class NacosDelayRegistrationListener implements ApplicationListener<ApplicationReadyEvent> {

    private final NacosServiceRegistry nacosServiceRegistry;
    private final NacosRegistration nacosRegistration;
    private final WebServerApplicationContext webServerApplicationContext;

    public NacosDelayRegistrationListener(NacosServiceRegistry nacosServiceRegistry,
                                          NacosRegistration nacosRegistration,
                                          WebServerApplicationContext webServerApplicationContext) {
        this.nacosServiceRegistry = nacosServiceRegistry;
        this.nacosRegistration = nacosRegistration;
        this.webServerApplicationContext = webServerApplicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (nacosRegistration.getPort() < 0) {
            // 获取当前应用的端口，以确保注册到 Nacos 的实例信息的端口与实际运行端口一致
            int port = webServerApplicationContext.getWebServer().getPort();
            nacosRegistration.setPort(port);
        }

        nacosServiceRegistry.register(nacosRegistration);
    }

}
