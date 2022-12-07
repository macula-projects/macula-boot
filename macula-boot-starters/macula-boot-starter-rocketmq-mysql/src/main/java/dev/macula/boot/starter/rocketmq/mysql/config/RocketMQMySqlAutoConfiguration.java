/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.rocketmq.mysql.config;

import dev.macula.boot.starter.rocketmq.mysql.RocketMQMySqlReplicator;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@code RocketMQMySqlAutoConfiguration} 配置
 *
 * @author rain
 * @since 2022/12/4 23:04
 */
@AutoConfiguration
@EnableConfigurationProperties(RocketMQMysqlProperties.class)
public class RocketMQMySqlAutoConfiguration {

    @Bean
    public RocketMQMySqlReplicator rocketMQMySqlReplicator(RocketMQMysqlProperties properties) {
        return new RocketMQMySqlReplicator(properties);
    }
}
