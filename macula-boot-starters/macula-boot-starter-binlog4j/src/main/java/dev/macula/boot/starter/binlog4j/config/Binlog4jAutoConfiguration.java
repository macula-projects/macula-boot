/*
 * Copyright (c) 2023-2026 Macula
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

package dev.macula.boot.starter.binlog4j.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

/**
 * 注册 Binlog4j 客户端初始化组件的自动配置。
 *
 * @author rain
 * @since 5.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(Binlog4jAutoProperties.class)
public class Binlog4jAutoConfiguration {
    @Bean
    public Binlog4jInitializationBeanProcessor binlog4jAutoInitializing(Binlog4jAutoProperties properties,
        @Nullable RedissonClient binlog4jRedissonClient) {
        return new Binlog4jInitializationBeanProcessor(properties.getClientConfigs(), binlog4jRedissonClient);
    }
}
