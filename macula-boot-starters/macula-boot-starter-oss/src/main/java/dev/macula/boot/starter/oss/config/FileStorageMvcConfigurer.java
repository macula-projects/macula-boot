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

package dev.macula.boot.starter.oss.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code FileStorageMvcConfigurer} MVC配置
 *
 * @author rain
 * @since 2023/5/9 11:34
 */
@RequiredArgsConstructor
public class FileStorageMvcConfigurer implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    /**
     * 配置本地存储的访问地址
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (FileStorageProperties.Local local : properties.getLocal()) {
            if (local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns())
                    .addResourceLocations("file:" + local.getBasePath());
            }
        }
        for (FileStorageProperties.LocalPlus local : properties.getLocalPlus()) {
            if (local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns())
                    .addResourceLocations("file:" + local.getStoragePath());
            }
        }
    }
}
