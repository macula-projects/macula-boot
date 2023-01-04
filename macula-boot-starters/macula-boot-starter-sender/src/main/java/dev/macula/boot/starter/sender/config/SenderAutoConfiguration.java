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

package dev.macula.boot.starter.sender.config;

import dev.macula.boot.starter.sender.MessageSender;
import dev.macula.boot.starter.sender.ReliableMessageCompensator;
import dev.macula.boot.starter.sender.ReliableMessageSender;
import dev.macula.boot.starter.sender.rocketmq.RocketMQMessageSender;
import dev.macula.boot.starter.sender.support.*;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * {@code SenderAutoConfiguration} 配置
 *
 * @author rain
 * @since 2023/1/3 15:15
 */
@AutoConfiguration
@ConfigurationProperties(prefix = "macula.sender")
public class SenderAutoConfiguration {

    @Setter
    private String messageTable = "MACULA_MSG";

    @Bean
    public ReliableMessageSender reliableMessageSender(ReliableMessageSendService reliableMessageSendService) {
        return new LocalTableBasedReliableMessageSender(reliableMessageSendService);
    }

    @Bean
    public ReliableMessageCompensator reliableMessageCompensator(ReliableMessageSendService reliableMessageSendService) {
        return new LocalTableBasedReliableMessageCompensator(reliableMessageSendService);
    }

    @Bean
    public ReliableMessageSendService reliableMessageSendService(LocalMessageRepository localMessageRepository,
                                                                 MessageSender messageSender) {
        return new ReliableMessageSendService(localMessageRepository, messageSender);
    }

    @Bean
    public LocalMessageRepository localMessageRepository(DataSource dataSource) {
        return new JdbcTemplateBasedLocalMessageRepository(dataSource, messageTable);
    }

    @Bean
    public MessageSender messageSender() {
        return new RocketMQMessageSender();
    }
}
