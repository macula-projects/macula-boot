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

package dev.macula.boot.starter.sender.support;

import dev.macula.boot.starter.sender.Message;
import dev.macula.boot.starter.sender.MessageSender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code ReliableMessageSendService} 本地消息发送线程
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
@Slf4j
public class SendMessageTask implements Runnable {

    private final LocalMessageRepository localMessageRepository;
    private final MessageSender messageSender;
    @Getter
    private final LocalMessage localMessage;

    public SendMessageTask(LocalMessageRepository localMessageRepository, MessageSender messageSender,
        LocalMessage localMessage) {
        this.localMessageRepository = localMessageRepository;
        this.messageSender = messageSender;
        this.localMessage = localMessage;
    }

    @Override
    public void run() {
        log.debug("begin to run send task for {}", this.localMessage);
        Message message = this.localMessage.toMessage();
        try {
            String msgId = this.messageSender.send(message);
            this.localMessage.sendSuccess(msgId);
            log.info("success to send to mq, message is {}", message);
        } catch (Exception e) {
            this.localMessage.sendError();
            log.error("failed to send message {}", message, e);
        }
        this.localMessageRepository.update(localMessage);
        log.debug("success to run send task for {}", this.localMessage);
    }
}
