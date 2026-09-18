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

package dev.macula.example.provider1.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 从 {@code example} 前缀绑定配置，用于演示 Nacos 配置动态刷新。
 *
 * @author Rain
 * @since 5.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "example")
@RefreshScope
public class ExampleConfig {
    private String test;
}
