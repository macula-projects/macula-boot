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
package dev.macula.boot.starter.rocketmq.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.rocketmq.DefaultRocketMQLocalTransactionListener;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * {@link RocketMQAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class RocketMQAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RocketMQAutoConfiguration.class));

    @Test
    void createsMessageConverterAndGrayPropertiesWithoutConnectingToBroker() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RocketMQMessageConverter.class);
            assertThat(context).hasSingleBean(GrayRocketMQProperties.class);
            assertThat(context).doesNotHaveBean(DefaultRocketMQLocalTransactionListener.class);
            assertThat(context.getBean(GrayRocketMQProperties.class).isEnabled()).isFalse();
        });
    }

    @Test
    void usesJackson3AndReadsHistoricalJsonMessages() {
        contextRunner.run(context -> {
            MessageConverter converter = context.getBean(RocketMQMessageConverter.class).getMessageConverter();
            assertThat(((CompositeMessageConverter) converter).getConverters())
                .filteredOn(JacksonJsonMessageConverter.class::isInstance).hasSize(1);
            String json = "{\"id\":9007199254740993,\"createdAt\":\"2026-10-09T12:34:56\","
                + "\"attempts\":null,\"unknown\":true}";
            OrderEvent event = (OrderEvent) converter.fromMessage(MessageBuilder
                .withPayload(json.getBytes(StandardCharsets.UTF_8)).build(), OrderEvent.class);
            assertThat(event).isNotNull();
            assertThat(event.id).isEqualTo(9007199254740993L);
            assertThat(event.createdAt).isEqualTo(LocalDateTime.of(2026, 10, 9, 12, 34, 56));
            assertThat(event.attempts).isZero();

            Message<?> encoded = converter.toMessage(event, null);
            assertThat(encoded).isNotNull();
            String payload = new String((byte[]) encoded.getPayload(), StandardCharsets.UTF_8);
            assertThat(payload).contains("\"createdAt\":\"2026-10-09T12:34:56\"")
                .contains("\"id\":9007199254740993");
            OrderEvent decoded = (OrderEvent) converter.fromMessage(encoded, OrderEvent.class);
            assertThat(decoded).usingRecursiveComparison().isEqualTo(event);
        });
    }

    @Test
    void preservesStringAndByteArrayMessages() {
        MessageConverter converter = new RocketMQAutoConfiguration().rocketMQMessageConverter().getMessageConverter();
        byte[] bytes = {0, 1, -1};
        assertThat(converter.toMessage(bytes, null).getPayload()).isEqualTo(bytes);
        assertThat(converter.fromMessage(MessageBuilder.withPayload(bytes).build(), byte[].class)).isEqualTo(bytes);
        assertThat(converter.toMessage("hello", null).getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(converter.fromMessage(MessageBuilder.withPayload("hello".getBytes(StandardCharsets.UTF_8)).build(),
            String.class)).isEqualTo("hello");
    }

    /**
     * 验证消息格式和 Java 时间类型兼容性的事件。
     *
     * @since 6.1.0
     */
    static class OrderEvent {
        public long id;
        public LocalDateTime createdAt;
        public int attempts;
    }
}
