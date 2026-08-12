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
package dev.macula.boot.starter.rocketmq;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.rocketmq.config.Constants;
import org.junit.jupiter.api.Test;

/**
 * {@code TxMqMessageTest} RocketMQ事务消息单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class TxMqMessageTest {

    @Test
    void carriesTransactionRoutingMetadata() {
        TxMqMessage<String> message = new TxMqMessage<>("payload", TxMqMessageTest.class, "create", "order-7");

        assertThat(message.getPayload()).isEqualTo("payload");
        assertThat(message.getHeaders().get(Constants.BEAN_CLASS_NAME)).isEqualTo(TxMqMessageTest.class.getName());
        assertThat(message.getHeaders().get(Constants.BIZ_NAME)).isEqualTo("create");
        assertThat(message.getHeaders().get(Constants.CHECK_ID)).isEqualTo("order-7");
    }

    @Test
    void rendersBytePayloadWithoutDumpingItsContents() {
        TxMqMessage<byte[]> message = new TxMqMessage<>(new byte[] {1, 2, 3}, TxMqMessageTest.class, "create", "7");

        assertThat(message.toString()).contains("byte[3]");
    }
}
