/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

package dev.macula.boot.starter.auditlog.test.listener;

import dev.macula.boot.starter.auditlog.event.OperLogEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * <b>TestAuditLogListener</b> 测试用审计日志事件监听器，用于捕获发布的事件
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@Component
public class TestAuditLogListener {

    @Getter
    private final List<OperLogEvent> receivedEvents = new ArrayList<>();

    /**
     * 监听操作日志事件，保存到列表供测试断言
     *
     * @param event 操作日志事件
     */
    @EventListener
    public void onOperLogEvent(OperLogEvent event) {
        receivedEvents.add(event);
    }

    /**
     * 清空接收的事件，用于测试隔离
     */
    public void clear() {
        receivedEvents.clear();
    }
}
