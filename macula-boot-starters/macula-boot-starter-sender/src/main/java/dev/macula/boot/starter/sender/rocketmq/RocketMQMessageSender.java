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

package dev.macula.boot.starter.sender.rocketmq;

import dev.macula.boot.starter.sender.Message;
import dev.macula.boot.starter.sender.MessageSender;
import dev.macula.boot.starter.sender.MessageSenderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * {@code RocketMQMessageSender} 发送消息给RocketMQ
 *
 * @author rain
 * @since 2023/1/3 15:27
 */
@RequiredArgsConstructor
@Slf4j
public class RocketMQMessageSender implements MessageSender {

    private final RocketMQTemplate template;

    @Override
    public String send(Message message) {
        try {
            String destination = message.getTopic();
            if (StringUtils.hasLength(message.getTag())) {
                destination = destination + ":" + message.getTag();
            }

            SendResult result;
            if (message.isOrderly()) {
                result =
                    template.syncSendOrderly(destination, MessageBuilder.withPayload(message), message.getMsgKey());
            } else {
                result = template.syncSend(destination, MessageBuilder.withPayload(message).build());
            }

            if (SendStatus.SEND_OK.equals(result.getSendStatus())) {
                return result.getMsgId();
            } else {
                throw new MessageSenderException(result.getSendStatus());
            }
        } catch (MessagingException ex) {
            throw new MessageSenderException(ex);
        }
    }
}
