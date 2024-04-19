/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.websocket.stomp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/**
 * <p>
 * <b>RedisRelaySubscriber</b> 订阅从Message Broker转发过来的消息
 * </p>
 *
 * @author Rain
 * @since 2024/4/17
 */
@Slf4j
@RequiredArgsConstructor
public class RedisRelaySubscriber {

    private final SimpMessageSendingOperations simpMessageTemplate;

    /**
     * 接收MessageBroker中转消息并转发出去
     *
     * @param message websocket消息
     */
    public void onMessage(Message<?> message) {
        simpMessageTemplate.send(message);
    }
}
