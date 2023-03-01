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

import dev.macula.boot.starter.sender.ReliableMessageCompensator;

import java.util.Date;

/**
 * {@code LocalTableBasedReliableMessageCompensator} 本地消息发送补偿
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
public class LocalTableBasedReliableMessageCompensator implements ReliableMessageCompensator {
    private final ReliableMessageSendService reliableMessageSendService;

    public LocalTableBasedReliableMessageCompensator(ReliableMessageSendService reliableMessageSendService) {
        this.reliableMessageSendService = reliableMessageSendService;
    }

    @Override
    public void compensate(Date startDate, int sizePreTask) {
        this.reliableMessageSendService.loadAndResend(startDate, sizePreTask);
    }
}
