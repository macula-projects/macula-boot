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
package dev.macula.boot.starter.sender.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.macula.boot.starter.sender.Message;
import dev.macula.boot.starter.sender.MessageSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link ReliableMessageSendService} 可靠消息发送逻辑测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class ReliableMessageSendServiceTest {

    private LocalMessageRepository repository;
    private MessageSender sender;
    private ReliableMessageSendService service;

    @BeforeEach
    void setUp() {
        repository = mock(LocalMessageRepository.class);
        sender = mock(MessageSender.class);
        service = new ReliableMessageSendService(repository, sender);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void savesAndSendsImmediatelyOutsideTransaction() {
        when(sender.send(any(Message.class))).thenReturn("msg-1");

        service.saveAndSend(message());

        ArgumentCaptor<LocalMessage> captor = ArgumentCaptor.forClass(LocalMessage.class);
        verify(repository).save(captor.capture());
        verify(repository).update(captor.getValue());
        assertThat(captor.getValue().getStatus()).isEqualTo(LocalMessage.STATUS_SUCCESS);
        assertThat(captor.getValue().getMsgId()).isEqualTo("msg-1");
    }

    @Test
    void defersSendingUntilTransactionCommit() {
        when(sender.send(any(Message.class))).thenReturn("msg-2");
        TransactionSynchronizationManager.initSynchronization();

        service.saveAndSend(message());

        verify(repository).save(any(LocalMessage.class));
        verify(sender, never()).send(any(Message.class));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

        verify(sender).send(any(Message.class));
        verify(repository).update(any(LocalMessage.class));
    }

    @Test
    void recordsFailedSendForLaterRetry() {
        when(sender.send(any(Message.class))).thenThrow(new IllegalStateException("broker unavailable"));

        service.saveAndSend(message());

        ArgumentCaptor<LocalMessage> captor = ArgumentCaptor.forClass(LocalMessage.class);
        verify(repository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LocalMessage.STATUS_ERROR);
        assertThat(captor.getValue().getRetryTime()).isEqualTo(1);
    }

    private Message message() {
        return Message.builder().topic("orders").msgKey("order-7").msg("{\"id\":7}").build();
    }
}
