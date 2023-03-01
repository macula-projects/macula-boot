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

package dev.macula.boot.starter.sender.support;

import cn.hutool.core.collection.CollectionUtil;
import dev.macula.boot.starter.sender.Message;
import dev.macula.boot.starter.sender.MessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * {@code ReliableMessageSendService} 本地消息发送服务
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
@Slf4j
public class ReliableMessageSendService {
    private final LocalMessageRepository localMessageRepository;
    private final MessageSender messageSender;

    public ReliableMessageSendService(LocalMessageRepository localMessageRepository, MessageSender messageSender) {
        this.localMessageRepository = localMessageRepository;
        this.messageSender = messageSender;
    }

    /**
     * 加载未发送的消息，进行重新发送
     *
     * @param startDate   最后更新时间（需要补偿的开始时间）
     * @param sizePreTask 每个任务的条数
     */
    public void loadAndResend(Date startDate, int sizePreTask) {
        Date latestUpdateTime = startDate;
        List<LocalMessage> localMessages =
            this.localMessageRepository.loadNotSuccessByUpdateGt(latestUpdateTime, sizePreTask);
        while (CollectionUtil.isNotEmpty(localMessages)) {
            log.info("load {} task by {} to resend", localMessages.size(), latestUpdateTime);

            retrySend(localMessages);

            latestUpdateTime = calLatestUpdateTime(localMessages);
            log.info("next time is {}", latestUpdateTime);

            localMessages = this.localMessageRepository.loadNotSuccessByUpdateGt(latestUpdateTime, sizePreTask);
        }
        log.info("End to load task by {}", startDate);
    }

    public void saveAndSend(Message message) {
        LocalMessage localMessage = convertToLocalMessage(message);

        saveToDB(localMessage);

        SendMessageTask sendMessageTask = buildTask(localMessage);

        addCallbackOrRunTask(sendMessageTask);
    }

    private void addCallbackOrRunTask(SendMessageTask sendMessageTask) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 添加监听器，在事务提交后触发后续任务
            TransactionSynchronization transactionSynchronization = new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    sendMessageTask.run();
                }
            };
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
            log.info("success to register synchronization for message {}", sendMessageTask.getLocalMessage());
        } else {
            // 没有可以事务，直接触发后续任务
            log.info("No Transaction !!! begin to run task for message {}", sendMessageTask.getLocalMessage());
            sendMessageTask.run();
            log.info("No Transaction !!! success to run task for message {}", sendMessageTask.getLocalMessage());
        }
    }

    private SendMessageTask buildTask(LocalMessage localMessage) {
        return new SendMessageTask(this.localMessageRepository, this.messageSender, localMessage);
    }

    private void saveToDB(LocalMessage localMessage) {
        this.localMessageRepository.save(localMessage);
    }

    private LocalMessage convertToLocalMessage(Message message) {
        return LocalMessage.apply(message);
    }

    private Date calLatestUpdateTime(List<LocalMessage> localMessages) {
        return localMessages.stream().map(localMessage -> localMessage.getUpdateTime()).max(Comparator.naturalOrder())
            .orElse(new Date());
    }

    private void retrySend(List<LocalMessage> localMessages) {
        Date now = new Date();
        localMessages.stream().filter(message -> message.needRetry(now))
            .map(localMessage -> new SendMessageTask(this.localMessageRepository, messageSender, localMessage))
            .forEach(task -> task.run());
    }
}
