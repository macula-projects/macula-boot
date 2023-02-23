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

package dev.macula.boot.starter.system.config;

import dev.macula.boot.starter.system.controller.SystemController;
import dev.macula.boot.starter.system.remote.SystemFeignClient;
import dev.macula.boot.starter.system.service.PermissionService;
import dev.macula.boot.starter.system.service.SystemService;
import dev.macula.boot.starter.system.service.impl.SystemServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * {@code SystemAutoConfiguration} 配置
 *
 * @author rain
 * @since 2023/1/12 19:11
 */
@AutoConfiguration
@EnableFeignClients(clients = {SystemFeignClient.class})
@RequiredArgsConstructor
public class SystemAutoConfiguration {
    private final SystemFeignClient client;

    @Bean
    public PermissionService permissionService() {
        return new PermissionService(systemService());
    }

    public SystemService systemService() {
        return new SystemServiceImpl(client);
    }

    @Bean
    public SystemController systemController() {
        return new SystemController(systemService());
    }
}
