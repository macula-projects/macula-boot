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
package dev.macula.boot.starter.sender.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.macula.boot.starter.sender.MessageSender;
import dev.macula.boot.starter.sender.ReliableMessageCompensator;
import dev.macula.boot.starter.sender.ReliableMessageSender;
import dev.macula.boot.starter.sender.support.LocalMessageRepository;
import dev.macula.boot.starter.sender.support.ReliableMessageSendService;

import javax.sql.DataSource;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link SenderAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class SenderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SenderAutoConfiguration.class))
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(RocketMQTemplate.class, () -> mock(RocketMQTemplate.class));

    @Test
    void createsReliableMessagePipelineFromInfrastructureBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LocalMessageRepository.class);
            assertThat(context).hasSingleBean(MessageSender.class);
            assertThat(context).hasSingleBean(ReliableMessageSendService.class);
            assertThat(context).hasSingleBean(ReliableMessageSender.class);
            assertThat(context).hasSingleBean(ReliableMessageCompensator.class);
        });
    }
}
